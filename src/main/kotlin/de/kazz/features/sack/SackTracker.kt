package de.kazz.features.sack

import de.kazz.config.ConfigColor
import de.kazz.config.ui.hud.HudElement
import de.kazz.config.ui.hud.HudTextLine
import de.kazz.config.ui.hud.HudTextSegment
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.slf4j.LoggerFactory


class SackTracker : HudElement("sack_tracker") {

    override fun renderContent(): List<HudTextLine> {
        val lines = mutableListOf<HudTextLine>()
        for ((key, value) in trackedItems) {
            // Only render items that the user has selected for tracking
            if (key in trackedItemsFilter) {
                lines.add(HudTextLine(listOf(HudTextSegment("$key - $value", ConfigColor.RED))))
            }
        }
        return lines
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger("sack-tracker")

        /**
         * All items currently tracked by the scanner (absolute values from scanning + chat deltas).
         */
        var trackedItems = mutableMapOf<String, Int>()

        /**
         * Set of item names that the user has selected to display on the HUD.
         * Only items in this set will be rendered by [renderContent].
         *
         * This is synchronized with [SackTrackerData] on initialization.
         */
        val trackedItemsFilter: MutableSet<String> = mutableSetOf()

        /**
         * Initializes the tracked items filter from persistent data.
         * Called automatically during mod init via [SackTrackerData.initialize].
         */
        fun syncFromData() {
            trackedItemsFilter.clear()
            trackedItemsFilter.addAll(SackTrackerData.getTrackedNames())
            // Also sync amounts from persistent data
            // Add ALL tracked items to trackedItems, even if amount is 0,
            // so they appear in the HUD render loop
            for ((name, data) in SackTrackerData.getAllItems()) {
                if (data.tracked) {
                    trackedItems[name] = data.amount
                }
            }
        }

        fun change(addItem: String, amount: Int) {
            val temp = trackedItems[addItem] ?: 0
            trackedItems[addItem] = temp + amount
            // Also update persistent data
            SackTrackerData.addAmount(addItem, amount)
        }

        fun set(addItem: String, amount: Int) {
            trackedItems[addItem] = amount
            // Also update persistent data
            SackTrackerData.setAmount(addItem, amount)
        }

        /**
         * Toggles whether an item is tracked (displayed on the HUD).
         * Returns the new state: true if now tracked, false if now untracked.
         */
        fun toggleItem(itemName: String): Boolean {
            return if (itemName in trackedItemsFilter) {
                trackedItemsFilter.remove(itemName)
                SackTrackerData.setTracked(itemName, false)
                false
            } else {
                trackedItemsFilter.add(itemName)
                SackTrackerData.setTracked(itemName, true)
                true
            }
        }

        /**
         * Returns true if the given item is in the user's tracked filter.
         */
        fun isItemTracked(itemName: String): Boolean = itemName in trackedItemsFilter

        fun listenToChatMessages() {
            ClientReceiveMessageEvents.ALLOW_GAME.register { message: Component, overlay: Boolean ->
                val seen = mutableSetOf<String>()
                extractHoverText(message).forEach { hoverText ->
                    if (seen.add(hoverText)) {
                        parseSackHoverText(hoverText)
                    }
                }

                true
            }
        }

        private fun extractHoverText(component: Component): List<String> {
            val result = mutableListOf<String>()

            val hoverEvent = component.style.getHoverEvent()
            if (hoverEvent != null && hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
                val showText = hoverEvent as? HoverEvent.ShowText
                if (showText != null) {
                    result.add(showText.value().string)
                }
            }

            for (sibling in component.siblings) {
                result.addAll(extractHoverText(sibling))
            }

            return result
        }

        /**
         * Parses a sack hover text string and updates tracked items accordingly.
         *
         * Expected format:
         *   Added items:
         *     +160 Enchanted Cobblestone (Enchanted Mining Sack)
         *     -20,118 Cobblestone (Mining Sack, Mining Sack)
         *
         * This message can be disabled in the settings.
         *
         * Each item line follows:  [+/-][amount] [item name] ([sack name])
         */
        fun parseSackHoverText(hoverText: String) {
            val lines = hoverText.lines()
            val itemLineRegex = Regex("""^\s+([+-]\d[\d,]*)\s+(.+?)\s+\(.+\)\s*$""")

            for (line in lines) {
                val match = itemLineRegex.matchEntire(line)
                if (match != null) {
                    val amountStr = match.groupValues[1].replace(",", "")
                    val itemName = match.groupValues[2].trim()
                    val amount = amountStr.toIntOrNull() ?: continue

                    change(itemName, amount)
                }
            }
        }
    }
}
