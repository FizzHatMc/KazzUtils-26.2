package de.kazz.command.impl

import de.kazz.command.SimpleKazzCommand
import de.kazz.config.KazzConfig
import de.kazz.config.categories.CombatConfig
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

class KazzCommand(
    override val name: String = "kazzutils",
    override val aliases: List<String> = listOf("k","kazzhub","kaz"),
) : SimpleKazzCommand {
    override fun execute(source: FabricClientCommandSource): Int {

        return 1
    }
}