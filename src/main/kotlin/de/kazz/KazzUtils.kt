package de.kazz

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object KazzUtils : ModInitializer {
	const val MOD_ID: String = "kazzutils"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Initialising KazzUtils infrastructure...")
		LOGGER.info("Core, Data, and Feature modules are available.")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
