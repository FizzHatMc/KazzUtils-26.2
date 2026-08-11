package de.kazz.config.categories

import de.kazz.config.ConfigCategoryScope
import de.kazz.config.ConfigColor
import de.kazz.config.ConfigGroupScope
import de.kazz.config.ConfigSubCategoryScope

/**
 * Combat category config.
 *
 * Access values directly:
 * ```kotlin
 * val enabled = CombatConfig.Dungeon.DianaFeatures.dianaWaypoint.value
 * CombatConfig.Dungeon.DianaFeatures.dianaWaypoint.value = false
 * ```
 */
object CombatConfig : ConfigCategoryScope("Combat") {

    /**
     * Dungeon sub-category.
     */
    object Dungeon : ConfigSubCategoryScope("Dungeon", "combat") {

        val bossTimer = toggle(
            name = "Boss Timer",
            description = "Shows the boss timer while in a dungeon",
            default = true
        )

        /**
         * Diana Features group (hider).
         */
        object DianaFeatures : ConfigGroupScope(
            "Diana Features",
            "Features related to the Diana event",
            "combat.dungeon"
        ) {
            val dianaWaypoint = toggle(
                name = "Enable Diana Waypoint",
                description = "Enables automatic Waypoints for the Diana Event.",
                default = false
            )

            val waypointRange = intSlider(
                name = "Waypoint Range",
                description = "Range in blocks for the waypoint",
                default = 50,
                min = 10,
                max = 200
            )

            val waypointColor = color(
                name = "Waypoint Color",
                description = "Color of the waypoint marker",
                default = ConfigColor.RED
            )
        }

        val bossTimerColor = color(
            name = "Boss Timer Color",
            description = "Color of the boss timer text",
            default = ConfigColor.WHITE
        )
    }

    /**
     * General sub-category.
     */
    object General : ConfigSubCategoryScope("General", "combat") {

        val autoHeal = toggle(
            name = "Auto Heal",
            description = "Automatically heal when low on health",
            default = true
        )

        val healThreshold = doubleSlider(
            name = "Heal Threshold",
            description = "Health percentage at which to auto-heal",
            default = 20.0,
            min = 0.0,
            max = 100.0
        )

        val potionMessage = text(
            name = "Potion Message",
            description = "Message to display when a potion is used",
            default = "Healing..."
        )
    }
}