package de.kazz.features.generic

import com.mojang.blaze3d.platform.InputConstants
import de.kazz.inventory.InventoryScanner
import de.kazz.mixin.AbstractContainerScreenAccessor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

/**
 * Listens for sack menu openings, scans the inventory contents, and updates
 * [SackTracker] with the absolute "truth" values from the inventory.
 *
 * Also registers a keybinding ("Toggle Sack Tracking") that the user can press
 * while hovering over a slot in a sack menu to toggle tracking of that item.
 *
 * The keybinding is declared as a top-level field so it is registered with the
 * game's internal keybinding map at class-load time, which is required for it
 * to appear in the Controls screen (per official Fabric keymapping docs).
 */
object SackInventoryScanner {

    private val LOGGER = LoggerFactory.getLogger("sack-inventory-scanner")

    // ---------- Scan-on-open logic ----------

    private var scanRequested = false
    private var scanTicksRemaining = 0

    // ---------- Keybind toggle logic ----------

    // Key mapping must be declared as a field (initialized at class-load time),
    // NOT inside initialize(), to ensure it is registered with the game's
    // internal keybinding map before the Controls screen is constructed.
    // This follows the official Fabric docs pattern:
    // https://docs.fabricmc.net/develop/key-mappings
    private val KEY_CATEGORY: KeyMapping.Category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("kazzutils", "sack_tracker")
    )
    private val toggleKeybind: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.kazzutils.sack_tracker_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KEY_CATEGORY
        )
    )

    /**
     * Registers screen listeners and tick listener.
     * Called once during mod initialization.
     * The keybinding is registered at class-load time via the field above.
     */
    fun initialize() {
        LOGGER.info("Registered keybinding: key.kazzutils.sack_tracker_toggle -> K (category.kazzutils.sack_tracker)")

        // Register screen open listener
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is AbstractContainerScreen<*>) {
                val title = screen.title.string.trim()
                if (title.endsWith("Sack", ignoreCase = true)) {
                    LOGGER.info("Sack screen detected: \"$title\" — scheduling inventory scan")
                    scanRequested = true
                    scanTicksRemaining = 5
                }
            }
        }

        // Register a single persistent tick listener
        ClientTickEvents.START_CLIENT_TICK.register(object : ClientTickEvents.StartTick {
            override fun onStartTick(mc: Minecraft) {
                // Handle pending sack scans
                if (scanRequested) {
                    scanTicksRemaining--
                    if (scanTicksRemaining <= 0) {
                        scanRequested = false
                        performScan()
                    }
                }

                // Handle keybind press for toggling item tracking
                if (toggleKeybind.consumeClick()) {
                    LOGGER.info("Toggle keybind pressed!")
                    handleToggleKeybind(mc)
                }
            }
        })
    }

    // ---------- Keybind handler ----------

    /**
     * Called when the toggle keybinding is pressed.
     * If the user is in a sack screen and hovering over a container slot,
     * toggles tracking for that item.
     */
    private fun handleToggleKeybind(mc: Minecraft) {
        LOGGER.info("handleToggleKeybind called")
        val screen = mc.gui.screen()
        if (screen !is AbstractContainerScreen<*>) {
            LOGGER.info("Not in a container screen, ignoring")
            mc.player?.sendSystemMessage(
                Component.literal("§c[SackTracker] You must be in a sack menu to use this!")
            )
            return
        }

        val accessor = screen as? AbstractContainerScreenAccessor
        val hoveredSlot = accessor?.getHoveredSlot()
        LOGGER.info("hoveredSlot = $hoveredSlot")
        if (hoveredSlot == null) {
            mc.player?.sendSystemMessage(
                Component.literal("§c[SackTracker] Hover over an item first!")
            )
            return
        }

        // Skip player inventory slots — only track items in the container
        val containerSlotCount = screen.menu.slots.size - 36
        val slotIndex = screen.menu.slots.indexOf(hoveredSlot)
        LOGGER.info("slotIndex = $slotIndex, containerSlotCount = $containerSlotCount")
        if (slotIndex >= containerSlotCount) {
            mc.player?.sendSystemMessage(
                Component.literal("§c[SackTracker] Only sack items can be tracked, not your inventory items!")
            )
            return
        }

        val stack = hoveredSlot.item
        if (stack.isEmpty) return

        // Get the item name, stripping any Minecraft formatting codes so it matches the
        // clean names stored by InventoryScanner and SackTracker
        val itemName = stripFormatting(stack.hoverName.string).trim()
        LOGGER.info("Toggling tracking for: $itemName")
        toggleItemAndNotify(mc, itemName)
    }

    private fun toggleItemAndNotify(mc: Minecraft, itemName: String) {
        val nowTracked = SackTracker.toggleItem(itemName)
        val message = if (nowTracked) {
            "§a[SackTracker] Now tracking: §f$itemName"
        } else {
            "§e[SackTracker] Stopped tracking: §f$itemName"
        }
        mc.player?.sendSystemMessage(Component.literal(message))
    }

    // ---------- Scan logic ----------

    /**
     * Performs the inventory scan on the current screen.
     * For each non-empty slot, checks the tooltip for a "Stored: X/Y" line
     * and updates [SackTracker] with the absolute stored amount.
     */
    private fun performScan() {
        val mc = Minecraft.getInstance()
        val screen = mc.gui.screen() ?: return
        if (screen !is AbstractContainerScreen<*>) return

        val scannedSlots = InventoryScanner.scanCurrentScreen()
        LOGGER.info("Sack scan: found ${scannedSlots.size} non-empty slots")

        var updatedCount = 0
        for ((_, slot) in scannedSlots) {
            val storedAmount = parseStoredAmount(slot.tooltipLines)
            if (storedAmount != null) {
                SackTracker.trackedItems[slot.itemName] = storedAmount
                updatedCount++
                LOGGER.debug("Set \"${slot.itemName}\" -> $storedAmount")
            }
        }

        LOGGER.info("Sack scan complete: updated $updatedCount items out of ${scannedSlots.size} non-empty slots")
    }

    /**
     * Regex to parse the "Stored: X/Y" line from a sack item tooltip.
     */
    private val storedRegex = Regex("""Stored:\s+([\d,]+)/.*""")

    /**
     * Searches through tooltip lines for a "Stored: X/Y" pattern and returns the
     * parsed number if found, null otherwise.
     */
    private fun parseStoredAmount(tooltipLines: List<String>): Int? {
        for (line in tooltipLines) {
            val match = storedRegex.matchEntire(line.trim())
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toIntOrNull()
            }
        }
        return null
    }

    /**
     * Strips Minecraft formatting codes (§ followed by a character) from a string.
     */
    private fun stripFormatting(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]"), "")
    }
}