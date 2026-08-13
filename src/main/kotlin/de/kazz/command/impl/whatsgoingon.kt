package de.kazz.command.impl

import de.kazz.command.SimpleKazzCommand
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

class whatsgoingon(override val name: String = "test", override val aliases: List<String> = listOf()) : SimpleKazzCommand {
    override fun execute(source: FabricClientCommandSource): Int {
        source.sendFeedback(Component.literal("Hallo"))
        return 1
    }
}