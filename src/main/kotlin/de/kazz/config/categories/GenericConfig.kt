package de.kazz.config.categories

import de.kazz.config.ConfigCategoryScope
import de.kazz.config.ConfigColor
import de.kazz.config.ConfigSubCategoryScope

/**
 * Generic category config.
 *
 * Access values directly:
 * ```kotlin
 * val enabled = GenericConfig.Sack.enabled.value
 * GenericConfig.Sack.enabled.value = false
 * ```
 */
object GenericConfig : ConfigCategoryScope("Generic") {

    /**
     * Sack sub-category.
     */
    object Sack : ConfigSubCategoryScope("Sack", "generic") {

        val enabled = toggle(
            name = "Enable Sack Tracker",
            description = "Toggles the sack tracking HUD overlay. Use \"/kazzutils sacktracker\" or press \"k\" to open the Selection Menu.",
            default = true
        )

        val useCustomColors = toggle(
            name = "Use Custom Colors",
            description = "If off, uses default colors for the sack tracker display",
            default = false
        )

        val nameColor = color(
            name = "Name Color",
            description = "Color of the item name text in the sack tracker",
            default = ConfigColor.WHITE
        ).showWhen { useCustomColors.value }

        val amountColor = color(
            name = "Amount Color",
            description = "Color of the item amount text in the sack tracker",
            default = ConfigColor.GREEN
        ).showWhen { useCustomColors.value }
    }
}