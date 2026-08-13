package de.kazz

import de.kazz.command.KazzCommandRegistry
import de.kazz.command.impl.KazzCommand
import de.kazz.command.impl.TestSackTracking
import de.kazz.command.impl.whatsgoingon
import de.kazz.config.KazzConfig
import de.kazz.config.categories.CombatConfig
import de.kazz.config.categories.FarmingConfig
import de.kazz.config.ui.hud.HudManager
import de.kazz.config.ui.hud.HudRenderer
import de.kazz.config.ui.hud.HudTextLine
import de.kazz.features.farming.PickupLogHud
import de.kazz.features.generic.SackInventoryScanner
import de.kazz.features.generic.SackItemDatabase
import de.kazz.features.generic.SackTracker
import de.kazz.features.generic.SackTrackerData
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object KazzUtilsClient : ClientModInitializer {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-client")

    override fun onInitializeClient() {
        LOGGER.info("KazzUtils client initializer loaded.")

        // Force-initialize all category objects so they register themselves
        // with KazzConfig before we load the config from disk.
        @Suppress("unused")
        val _combat = CombatConfig
        @Suppress("unused")
        val _farming = FarmingConfig

        // Load config from disk. This must happen before any feature reads config values.
        KazzConfig.load()

        // Initialize HUD system (loads positions from disk)
        HudRenderer.initialize()

        // Initialize sack item database (loads from mod resources)
        SackItemDatabase.initialize()

        // Initialize sack tracker data (loads from disk or creates from database)
        SackTrackerData.initialize()

        // Register commands
        KazzCommandRegistry.register(KazzCommand())
        KazzCommandRegistry.register(TestSackTracking())
        KazzCommandRegistry.register(whatsgoingon())
        KazzCommandRegistry.registerAll()

        HudManager.register(SackTracker())

        // Register chat message listener
        SackTracker.listenToChatMessages()

        // Register sack inventory scanner to detect when sack menus open
        SackInventoryScanner.initialize()

    }
}
