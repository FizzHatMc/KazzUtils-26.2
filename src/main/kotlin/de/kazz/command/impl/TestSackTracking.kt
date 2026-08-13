package de.kazz.command.impl

import com.mojang.brigadier.tree.LiteralCommandNode
import de.kazz.command.CommandDsl.getInteger
import de.kazz.command.CommandDsl.getString
import de.kazz.command.CommandDsl.integer
import de.kazz.command.CommandDsl.literal
import de.kazz.command.CommandDsl.word
import de.kazz.command.ParameterKazzCommand
import de.kazz.features.sack.SackTracker
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource


class TestSackTracking(
    override val name: String = "addTracking",
    override val aliases: List<String> = listOf()
) : ParameterKazzCommand {

    override fun build(): LiteralCommandNode<FabricClientCommandSource> = literal(name) {
        argument("name", word()) {
            argument("testAmount", integer()) {
                executes { source, ctx ->
                    SackTracker.change(getString(ctx,"name"), getInteger(ctx,"testAmount"))
                    1
                }
            }
        }

    }

}