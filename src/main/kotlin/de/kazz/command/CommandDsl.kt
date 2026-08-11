package de.kazz.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

/**
 * DSL for constructing Brigadier command trees in a clean, readable way.
 *
 * ## Usage Example
 * ```kotlin
 * import de.kazz.command.CommandDsl.*
 *
 * val node = literal("kazz2") {
 *     argument("action", word()) {
 *         executes { source, context ->
 *             val action = getString(context, "action")
 *             source.sendFeedback(Component.literal("Running: $action"))
 *             1
 *         }
 *     }
 * }
 * ```
 *
 * This produces the equivalent of:
 * `/kazz2 <action>` where action is a single word.
 */
object CommandDsl {

    // ──────────────────────────────────────────────
    // Literal builders
    // ──────────────────────────────────────────────

    /**
     * Creates a literal command node (e.g., `/kazz`, `/warp`).
     *
     * @param name the literal command name
     * @param block receiver scope for configuring children, executions, and requirements
     * @return a built [LiteralCommandNode]
     */
    fun literal(
        name: String,
        block: LiteralNodeBuilder.() -> Unit
    ): LiteralCommandNode<FabricClientCommandSource> {
        val builder = LiteralNodeBuilder(name)
        builder.block()
        return builder.build()
    }

    /**
     * Creates an argument command node (e.g., `<action>`, `<count>`).
     *
     * @param name the argument name used to retrieve the value later
     * @param type the Brigadier [ArgumentType] (e.g., [word], [greedyString], [integer])
     * @param block receiver scope for configuring suggestions, executions, and children
     * @return a built [ArgumentCommandNode]
     */
    fun <T> argument(
        name: String,
        type: ArgumentType<T>,
        block: ArgumentNodeBuilder<T>.() -> Unit
    ): com.mojang.brigadier.tree.ArgumentCommandNode<FabricClientCommandSource, T> {
        val builder = ArgumentNodeBuilder<T>(name, type)
        builder.block()
        return builder.build()
    }

    // ──────────────────────────────────────────────
    // Argument type shortcuts
    // ──────────────────────────────────────────────

    /** A single-word string argument (no spaces). */
    fun word(): StringArgumentType = StringArgumentType.word()

    /** A greedy string argument that consumes the rest of the input (spaces allowed). */
    fun greedyString(): StringArgumentType = StringArgumentType.greedyString()

    /** A quoted string argument. */
    fun string(): StringArgumentType = StringArgumentType.string()

    /** An integer argument. */
    fun integer(): IntegerArgumentType = IntegerArgumentType.integer()

    /** An integer argument with a minimum value. */
    fun integer(min: Int): IntegerArgumentType = IntegerArgumentType.integer(min)

    /** An integer argument within a range. */
    fun integer(min: Int, max: Int): IntegerArgumentType = IntegerArgumentType.integer(min, max)

    // ──────────────────────────────────────────────
    // Value getters
    // ──────────────────────────────────────────────

    /** Retrieves a string argument value from the context. */
    fun getString(context: CommandContext<FabricClientCommandSource>, name: String): String =
        StringArgumentType.getString(context, name)

    /** Retrieves an integer argument value from the context. */
    fun getInteger(context: CommandContext<FabricClientCommandSource>, name: String): Int =
        IntegerArgumentType.getInteger(context, name)
}

// ──────────────────────────────────────────────
// Builder classes
// ──────────────────────────────────────────────

/**
 * Builder for a literal command node.
 *
 * Use via [CommandDsl.literal]:
 * ```kotlin
 * literal("mycommand") {
 *     executes { source, ctx -> ... }
 *     then(argument("arg", word()) { ... })
 * }
 * ```
 */
class LiteralNodeBuilder(private val name: String) {

    private val children = mutableListOf<CommandNode<FabricClientCommandSource>>()
    private var executor: ((FabricClientCommandSource, CommandContext<FabricClientCommandSource>) -> Int)? = null
    private var requirement: ((FabricClientCommandSource) -> Boolean)? = null

    /**
     * Adds a child node (literal or argument) to this node.
     */
    fun then(node: CommandNode<FabricClientCommandSource>) {
        children.add(node)
    }

    /**
     * Sets the execution handler for this node.
     *
     * @param handler receives (source, context) and returns 1 for success, 0 or negative for failure
     */
    fun executes(handler: (FabricClientCommandSource, CommandContext<FabricClientCommandSource>) -> Int) {
        executor = handler
    }

    /**
     * Restricts who can use this command.
     *
     * @param predicate returns true if the source is allowed to execute this command
     */
    fun requires(predicate: (FabricClientCommandSource) -> Boolean) {
        requirement = predicate
    }

    internal fun build(): LiteralCommandNode<FabricClientCommandSource> {
        val builder = ClientCommands.literal(name)
        if (requirement != null) {
            builder.requires(requirement!!)
        }
        children.forEach { builder.then(it) }
        if (executor != null) {
            builder.executes { ctx ->
                executor!!(ctx.getSource(), ctx)
            }
        }
        return builder.build()
    }
}

/**
 * Builder for an argument command node.
 *
 * Use via [CommandDsl.argument]:
 * ```kotlin
 * argument("action", word()) {
 *     executes { source, ctx -> ... }
 *     suggests { ctx, builder -> ... }
 * }
 * ```
 */
class ArgumentNodeBuilder<T>(private val name: String, private val type: ArgumentType<T>) {

    private val children = mutableListOf<CommandNode<FabricClientCommandSource>>()
    private var executor: ((FabricClientCommandSource, CommandContext<FabricClientCommandSource>) -> Int)? = null
    private var suggestionProvider: com.mojang.brigadier.suggestion.SuggestionProvider<FabricClientCommandSource>? = null
    private var requirement: ((FabricClientCommandSource) -> Boolean)? = null

    /**
     * Adds a child node to this argument.
     */
    fun then(node: CommandNode<FabricClientCommandSource>) {
        children.add(node)
    }

    /**
     * Sets the execution handler for this argument node.
     */
    fun executes(handler: (FabricClientCommandSource, CommandContext<FabricClientCommandSource>) -> Int) {
        executor = handler
    }

    /**
     * Adds a suggestion provider for tab-completion.
     *
     * @param provider receives (context, builder) and suggests values
     */
    fun suggests(provider: com.mojang.brigadier.suggestion.SuggestionProvider<FabricClientCommandSource>) {
        suggestionProvider = provider
    }

    /**
     * Restricts who can use this command.
     */
    fun requires(predicate: (FabricClientCommandSource) -> Boolean) {
        requirement = predicate
    }

    @Suppress("UNCHECKED_CAST")
    internal fun build(): com.mojang.brigadier.tree.ArgumentCommandNode<FabricClientCommandSource, T> {
        val builder = RequiredArgumentBuilder.argument<FabricClientCommandSource, T>(name, type)
        if (requirement != null) {
            builder.requires(requirement!!)
        }
        if (suggestionProvider != null) {
            builder.suggests(suggestionProvider!!)
        }
        children.forEach { builder.then(it) }
        if (executor != null) {
            builder.executes { ctx ->
                executor!!(ctx.getSource(), ctx)
            }
        }
        return builder.build() as com.mojang.brigadier.tree.ArgumentCommandNode<FabricClientCommandSource, T>
    }
}