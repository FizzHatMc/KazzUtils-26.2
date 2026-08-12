package de.kazz.config.categories

import de.kazz.config.ConfigCategoryScope
import de.kazz.config.ConfigColor
import de.kazz.config.ConfigSubCategoryScope
import kotlin.random.Random

/**
 * Farming category config.
 *
 * Access values directly:
 * ```kotlin
 * val harvest = FarmingConfig.Garden.autoHarvest.value
 * FarmingConfig.Garden.autoHarvest.value = true
 * ```
 */
object FarmingConfig : ConfigCategoryScope("Farming") {

    /**
     * Garden sub-category.
     */
    object Garden : ConfigSubCategoryScope("Garden", "farming") {

        val autoHarvest = toggle(
            name = "Auto Harvest",
            description = "Automatically harvest mature crops",
            default = false
        )

        val harvestRadius = intSlider(
            name = "Harvest Radius",
            description = "Radius in blocks to auto-harvest",
            default = 5,
            min = 1,
            max = 20
        )

        enum class FarmingMode {
            MANUAL, SEMI_AUTO, FULL_AUTO
        }

        val farmingMode = enumChoice(
            name = "Farming Mode",
            description = "Select the farming automation mode",
            default = FarmingMode.SEMI_AUTO
        )

        val testButton = actionButton(
            name = "Test Button",
            description = "Sends a test message in chat"
        ) {
            val player = net.minecraft.client.Minecraft.getInstance().player
            player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("Hello from button"))
        }

        val profitCountColor = color(
            name = "Waypoint Color",
            description = "Color of the waypoint marker",
            default = ConfigColor.RED
        )
    }
}