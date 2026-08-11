package de.kazz

import de.kazz.command.KazzCommandRegistry
import de.kazz.command.impl.KazzCommand
import de.kazz.config.KazzConfig
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object KazzUtilsClient : ClientModInitializer {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-client")

    override fun onInitializeClient() {
        LOGGER.info("KazzUtils client initializer loaded.")

        // Load config from disk. This must happen before any feature reads config values.
        KazzConfig.load()

        // Register commands
        KazzCommandRegistry.register(KazzCommand())
        KazzCommandRegistry.registerAll()
    }
}
