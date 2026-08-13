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
import de.kazz.features.sack.SackInventoryScanner
import de.kazz.features.sack.SackItemDatabase
import de.kazz.features.sack.SackTracker
import de.kazz.features.sack.SackTrackerData
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object KazzUtilsClient : ClientModInitializer {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-client")

    override fun onInitializeClient() {
        LOGGER.info("KazzUtils client initializer loaded.")

        // ── 1. Register commands FIRST ────────────────────────────────
        // Commands must be registered before any other initialization that
        // could potentially throw an exception, to ensure they are always
        // available regardless of feature initialization failures.
        KazzCommandRegistry.register(KazzCommand())
        KazzCommandRegistry.register(TestSackTracking())
        KazzCommandRegistry.register(whatsgoingon())
        KazzCommandRegistry.registerAll()

        // ── 2. Initialize config ──────────────────────────────────────
        // Force-initialize all category objects so they register themselves
        // with KazzConfig before we load the config from disk.
        @Suppress("unused")
        val _combat = CombatConfig
        @Suppress("unused")
        val _farming = FarmingConfig

        // Load config from disk. This must happen before any feature reads config values.
        try {
            KazzConfig.load()
        } catch (e: Exception) {
            LOGGER.error("Failed to load config: ${e.message}", e)
        }

        // ── 3. Initialize HUD system ──────────────────────────────────
        try {
            HudRenderer.initialize()
        } catch (e: Exception) {
            LOGGER.error("Failed to initialize HUD renderer: ${e.message}", e)
        }

        // ── 4. Initialize sack item database ──────────────────────────
        try {
            SackItemDatabase.initialize()
        } catch (e: Exception) {
            LOGGER.error("Failed to initialize sack item database: ${e.message}", e)
        }

        // ── 5. Initialize sack tracker data ───────────────────────────
        try {
            SackTrackerData.initialize()
        } catch (e: Exception) {
            LOGGER.error("Failed to initialize sack tracker data: ${e.message}", e)
        }

        // ── 6. Register HUD elements and listeners ────────────────────
        HudManager.register(SackTracker())

        // Register chat message listener
        SackTracker.listenToChatMessages()

        // Register sack inventory scanner to detect when sack menus open
        SackInventoryScanner.initialize()
    }
}
