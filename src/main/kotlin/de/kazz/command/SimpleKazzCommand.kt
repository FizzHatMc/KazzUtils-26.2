package de.kazz.command

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

/**
 * Interface for a simple client-side command with no arguments.
 *
 * ## Usage Example
 * ```kotlin
 * object PingCommand : SimpleKazzCommand {
 *     override val name = "ping"
 *     override val aliases = listOf("pong", "pingpong")
 *
 *     override fun execute(source: FabricClientCommandSource): Int {
 *         source.sendFeedback(Component.literal("§aPong!"))
 *         return 1
 *     }
 * }
 *
 * // In your client initializer:
 * KazzCommandRegistry.register(PingCommand)
 * KazzCommandRegistry.registerAll()
 * ```
 *
 * The command will be registered as:
 * - `/ping` (primary name)
 * - `/pong` (alias)
 * - `/pingpong` (alias)
 * All pointing to the same execution logic.
 */
interface SimpleKazzCommand {

    /**
     * The primary command name (e.g., "ping" → `/ping`).
     * Must be lowercase with no spaces.
     */
    val name: String

    /**
     * Alternative command names that redirect to the same execution.
     * Each alias is registered as a separate literal command node.
     * Can be empty if no aliases are needed.
     */
    val aliases: List<String>

    /**
     * Called when the command is executed.
     *
     * @param source the command source (player, command block, etc.)
     * @return 1 for success, 0 or negative for failure
     */
    fun execute(source: FabricClientCommandSource): Int
}