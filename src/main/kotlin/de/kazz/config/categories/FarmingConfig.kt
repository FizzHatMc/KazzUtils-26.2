package de.kazz.config.categories

import de.kazz.config.ConfigCategoryScope
import de.kazz.config.ConfigSubCategoryScope

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
    }
}