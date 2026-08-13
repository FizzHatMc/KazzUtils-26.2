package de.kazz.inventory

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory

/**
 * A general-purpose utility for reading the currently open inventory screen
 * and extracting item + tooltip information from each slot.
 */
object InventoryScanner {

    private val LOGGER = LoggerFactory.getLogger("inventory-scanner")

    /**
     * Represents a scanned slot in an inventory.
     */
    data class ScannedSlot(
        val slotIndex: Int,
        val itemName: String,
        val tooltipLines: List<String>
    )

    /**
     * Scans the currently open screen's container menu and returns a map of
     * slot index -> [ScannedSlot] for all non-empty slots that belong to the
     * actual container (not the player's inventory).
     *
     * Tooltip lines are stripped of formatting codes (§) for clean parsing.
     */
    fun scanCurrentScreen(): Map<Int, ScannedSlot> {
        val mc = Minecraft.getInstance()
        val player: Player = mc.player ?: return emptyMap()
        val guiScreen: Screen = mc.gui.screen() ?: return emptyMap()
        if (guiScreen !is AbstractContainerScreen<*>) return emptyMap()

        val menu = guiScreen.menu
        val level: Level = mc.level ?: return emptyMap()
        val tooltipContext: Item.TooltipContext = Item.TooltipContext.of(level)

        // The player's inventory slots are always the last 36 slots (3 rows of 9 + hotbar).
        // Container slots come first. For a 54-slot container (6 rows of 9), the first
        // `menu.slots.size - 36` slots belong to the container.
        // Player inventory is the last 36 slots. Only scan the container's own slots.
        val containerSlotCount = menu.slots.size - 36
        val result = mutableMapOf<Int, ScannedSlot>()

        for ((i, slot) in menu.slots.withIndex()) {
            if (i >= containerSlotCount) continue // skip player inventory slots

            val stack: ItemStack = slot.item
            if (stack.isEmpty) continue

            val tooltipLines = stack.getTooltipLines(tooltipContext, player, TooltipFlag.Default.NORMAL)
            val strippedLines = tooltipLines.map { stripFormatting(it.getString()) }

            if (strippedLines.isEmpty()) continue

            val itemName = strippedLines[0]

            result[slot.index] = ScannedSlot(
                slotIndex = slot.index,
                itemName = itemName,
                tooltipLines = strippedLines
            )
        }

        return result
    }

    /**
     * Strips Minecraft formatting codes (§ followed by a character) from a string.
     */
    private fun stripFormatting(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]"), "")
    }
}
