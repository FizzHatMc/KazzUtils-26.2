package de.kazz

import de.kazz.command.KazzCommandRegistry
import de.kazz.command.impl.KazzCommand
import de.kazz.config.KazzConfig
import de.kazz.config.categories.CombatConfig
import de.kazz.config.categories.FarmingConfig
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object KazzUtilsClient : ClientModInitializer {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-client")

    override fun onInitializeClient() {
        LOGGER.info("KazzUtils client initializer loaded.")

        // Force-initialize all category objects so they register themselves
        // with KazzConfig before we load the config from disk.
        CombatConfig
        FarmingConfig

        // Load config from disk. This must happen before any feature reads config values.
        KazzConfig.load()

        // Register commands
        KazzCommandRegistry.register(KazzCommand())
        KazzCommandRegistry.registerAll()

    }
}
