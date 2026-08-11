package de.kazz.command

import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

/**
 * Interface for a client-side command that accepts arguments.
 *
 * Implement this interface and use the [CommandDsl] helpers inside [build] to
 * create the command's argument tree in a clean, readable way.
 *
 * ## Usage Example — Single word argument
 * ```kotlin
 * import de.kazz.command.CommandDsl.*
 *
 * object Kazz2Command : ParameterKazzCommand {
 *     override val name = "kazz2"
 *     override val aliases = listOf("k2", "kazz2utils")
 *
 *     override fun build(): LiteralCommandNode<FabricClientCommandSource> = literal(name) {
 *         argument("action", word()) {
 *             executes { source, ctx ->
 *                 val action = getString(ctx, "action")
 *                 source.sendFeedback(Component.literal("§eRunning action: $action"))
 *                 1
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Usage Example — Greedy string (spaces allowed)
 * ```kotlin
 * object TitleCommand : ParameterKazzCommand {
 *     override val name = "title"
 *     override val aliases = listOf("t")
 *
 *     override fun build(): LiteralCommandNode<FabricClientCommandSource> = literal(name) {
 *         argument("text", greedyString()) {
 *             executes { source, ctx ->
 *                 val text = getString(ctx, "text")
 *                 source.sendFeedback(Component.literal("§aTitle: $text"))
 *                 1
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Usage Example — Multiple arguments
 * ```kotlin
 * object WarpCommand : ParameterKazzCommand {
 *     override val name = "warp"
 *     override val aliases = emptyList()
 *
 *     override fun build(): LiteralCommandNode<FabricClientCommandSource> = literal(name) {
 *         argument("x", integer()) {
 *             argument("y", integer()) {
 *                 argument("z", integer()) {
 *                     executes { source, ctx ->
 *                         val x = getInteger(ctx, "x")
 *                         val y = getInteger(ctx, "y")
 *                         val z = getInteger(ctx, "z")
 *                         source.sendFeedback(Component.literal("§aWarping to ($x, $y, $z)"))
 *                         1
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Note: The [build] method should return a **literal node** with the full argument tree
 * attached as children. The registry will wrap this node (and its aliases) under the
 * dispatcher's root.
 */
interface ParameterKazzCommand {

    /**
     * The primary command name (e.g., "kazz2" → `/kazz2`).
     * Must be lowercase with no spaces.
     */
    val name: String

    /**
     * Alternative command names that redirect to the same execution tree.
     * Each alias is registered as a separate literal command node.
     * Can be empty if no aliases are needed.
     */
    val aliases: List<String>

    /**
     * Builds the full command tree using [CommandDsl.literal] and its argument helpers.
     *
     * The returned node should be a literal node (the command name) with all argument
     * children and execution handlers attached.
     *
     * @return a fully constructed [LiteralCommandNode] ready for registration
     */
    fun build(): LiteralCommandNode<FabricClientCommandSource>
}