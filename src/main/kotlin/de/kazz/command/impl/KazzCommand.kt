package de.kazz.command.impl

import de.kazz.command.CommandDsl.literal
import de.kazz.command.ParameterKazzCommand
import de.kazz.config.ui.ConfigScreen
import de.kazz.features.general.sack.SackTrackerScreen
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import com.mojang.brigadier.tree.LiteralCommandNode
import de.kazz.config.ConfigColor
import de.kazz.features.waypoints.WaypointManager

class KazzCommand(
    override val name: String = "kazzutils",
    override val aliases: List<String> = listOf("kazzhub", "kazz", "k", "kaz"),
) : ParameterKazzCommand {

    override fun build(): LiteralCommandNode<FabricClientCommandSource> = literal(name) {
        // No arguments → open config screen (backward compatible)
        executes { source, _ ->
            Minecraft.getInstance().execute {
                Minecraft.getInstance().gui.setScreen(ConfigScreen())
            }
            1
        }

        // /kazzutils sacktracker → open SackTrackerScreen
        then(literal("sacktracker") {
            executes { source, _ ->
                Minecraft.getInstance().execute {
                    Minecraft.getInstance().gui.setScreen(SackTrackerScreen())
                }
                1
            }
        })

        then(literal("test") {
            executes { source, context ->
                WaypointManager.addWaypoint(100.0, 64.0, 200.0, seeThrough = true, ConfigColor.RED, ttlSeconds = 5.0)
                1
            }
        })
    }
}
