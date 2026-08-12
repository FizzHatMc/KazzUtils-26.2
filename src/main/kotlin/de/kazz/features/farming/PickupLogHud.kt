package de.kazz.features.farming

import de.kazz.config.categories.FarmingConfig
import de.kazz.config.ui.hud.HudElement
import de.kazz.config.ui.hud.HudTextLine
import de.kazz.config.ui.hud.HudTextSegment

class PickupLogHud : HudElement("pickup_log") {
    override fun renderContent(): List<HudTextLine> = listOf(
        HudTextLine(listOf(
            HudTextSegment("Profit tracker: ", FarmingConfig.Garden.profitCountColor.value),
            HudTextSegment("15", FarmingConfig.Garden.profitCountColor.value),
            HudTextSegment(" Ender Pearls", FarmingConfig.Garden.profitCountColor.value)
        ))
    )
}