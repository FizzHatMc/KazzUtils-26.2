package de.kazz.features.general.sack

import com.mojang.blaze3d.platform.InputConstants
import de.kazz.inventory.InventoryScanner
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import kotlin.collections.iterator

/**
 * Listens for sack menu openings, scans the inventory contents, and updates
 * [SackTracker] / [SackTrackerData] with the absolute "truth" values from the inventory.
 *
 * Also registers a keybinding ("Open Sack Tracker") that opens the [SackTrackerScreen] GUI.
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

    // ---------- Keybind logic ----------

    // Key mapping must be declared as a field (initialized at class-load time),
    // NOT inside initialize(), to ensure it is registered with the game's
    // internal keybinding map before the Controls screen is constructed.
    // This follows the official Fabric docs pattern:
    // https://docs.fabricmc.net/develop/key-mappings
    private val KEY_CATEGORY: KeyMapping.Category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("kazzutils", "main")
    )
    private val openGuiKeybind: KeyMapping = KeyMappingHelper.registerKeyMapping(
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
            private var tickCounter = 0

            override fun onStartTick(mc: Minecraft) {
                // Handle pending sack scans
                if (scanRequested) {
                    scanTicksRemaining--
                    if (scanTicksRemaining <= 0) {
                        scanRequested = false
                        performScan()
                    }
                }

                // Handle keybind press — open the SackTrackerScreen GUI
                if (openGuiKeybind.consumeClick()) {
                    LOGGER.info("Open Sack Tracker GUI keybind pressed!")
                    mc.execute {
                        mc.gui.setScreen(SackTrackerScreen())
                    }
                }

                // Periodic auto-save every 5 seconds (100 ticks at 20 TPS)
                tickCounter++
                if (tickCounter >= 100) {
                    tickCounter = 0
                    SackTrackerData.save()
                }
            }
        })
    }

    // ---------- Scan logic ----------

    /**
     * Performs the inventory scan on the current screen.
     * For each non-empty slot, checks the tooltip for a "Stored: X/Y" line
     * and updates [SackTrackerData] with the absolute stored amount.
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
                // Update both the in-memory tracker and persistent data
                SackTracker.trackedItems[slot.itemName] = storedAmount
                SackTrackerData.setAmount(slot.itemName, storedAmount)
                updatedCount++
                LOGGER.debug("Set \"${slot.itemName}\" -> $storedAmount")
            }
        }

        LOGGER.info("Sack scan complete: updated $updatedCount items out of ${scannedSlots.size} non-empty slots")

        // Save immediately after scan to persist amounts
        SackTrackerData.save()
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
}
