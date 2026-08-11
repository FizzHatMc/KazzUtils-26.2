package de.kazz.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

/**
 * Central registry for all KazzUtils client-side commands.
 *
 * ### Usage
 * ```kotlin
 * // In your ClientModInitializer:
 * KazzCommandRegistry.register(KazzHubCommand)
 * KazzCommandRegistry.register(Kazz2Command)
 * KazzCommandRegistry.registerAll()
 * ```
 *
 * ### How it works
 * - **Simple commands** ([SimpleKazzCommand]): The primary name and all aliases are registered
 *   as separate literal nodes, each sharing the same execution handler.
 * - **Parameter commands** ([ParameterKazzCommand]): The [build] method produces a full
 *   literal node with its argument tree. The primary name uses this node directly. Each alias
 *   is registered as a separate literal node that redirects to (and shares the child tree of)
 *   the primary node.
 *
 * This is a client-side only registry. Server commands are not supported.
 */
object KazzCommandRegistry {

    private val simpleCommands = mutableListOf<SimpleKazzCommand>()
    private val parameterCommands = mutableListOf<ParameterKazzCommand>()

    /**
     * Registers a simple (no-argument) command.
     *
     * @param command the command to register
     */
    fun register(command: SimpleKazzCommand) {
        simpleCommands.add(command)
    }

    /**
     * Registers a parameterized command (with arguments).
     *
     * @param command the command to register
     */
    fun register(command: ParameterKazzCommand) {
        parameterCommands.add(command)
    }

    /**
     * Registers all previously collected commands with Brigadier via the
     * [ClientCommandRegistrationCallback] event.
     *
     * Call this once in your [net.fabricmc.api.ClientModInitializer.onInitializeClient].
     * After this method is called, the collected commands are cleared and cannot be
     * re-registered (call [register] again before another [registerAll] if needed).
     */
    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            // ── Register simple commands ──────────────────────────────────
            for (cmd in simpleCommands) {
                val primaryNode = ClientCommands.literal(cmd.name)
                    .executes { ctx -> cmd.execute(ctx.getSource()) }
                    .build()

                dispatcher.root.addChild(primaryNode)

                // Register aliases pointing to the same handler
                for (alias in cmd.aliases) {
                    val aliasNode = ClientCommands.literal(alias)
                        .executes { ctx -> cmd.execute(ctx.getSource()) }
                        .build()
                    dispatcher.root.addChild(aliasNode)
                }
            }

            // ── Register parameter commands ──────────────────────────────
            for (cmd in parameterCommands) {
                val primaryNode = cmd.build()
                dispatcher.root.addChild(primaryNode)

                // Register aliases: each alias creates a literal node with the
                // same child tree as the primary node (redirects are not needed
                // since we share the child arguments via copies).
                for (alias in cmd.aliases) {
                    val aliasBuilder = ClientCommands.literal(alias)
                    // Copy all children from the primary node to the alias
                    for (child in primaryNode.children) {
                        aliasBuilder.then(child)
                    }
                    // Copy the executor from the primary node if present
                    if (primaryNode.command != null) {
                        aliasBuilder.executes(primaryNode.command)
                    }
                    dispatcher.root.addChild(aliasBuilder.build())
                }
            }

            // Clear the lists to free memory, prevent accidental re-registration
            simpleCommands.clear()
            parameterCommands.clear()
        }
    }
}