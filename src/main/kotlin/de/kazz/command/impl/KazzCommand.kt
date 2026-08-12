package de.kazz.command.impl

import de.kazz.command.SimpleKazzCommand
import de.kazz.config.ui.ConfigScreen
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class KazzCommand(
    override val name: String = "kazzutils",
    override val aliases: List<String> = listOf("k","kazzhub","kaz","kazz"),
) : SimpleKazzCommand {
    override fun execute(source: FabricClientCommandSource): Int {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().gui.setScreen(ConfigScreen())
        }
        return 1
    }
}