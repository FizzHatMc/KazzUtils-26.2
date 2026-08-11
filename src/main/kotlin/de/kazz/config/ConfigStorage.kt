package de.kazz.config

import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Handles reading and writing the config JSON file to disk.
 *
 * Config file location: `<minecraft_dir>/config/kazzutils/config.json`
 */
object ConfigStorage {

    private val configDir: Path by lazy {
        val gameDir = FabricLoader.getInstance().gameDir
        gameDir.resolve("config").resolve("kazzutils")
    }

    private val configFile: Path by lazy { configDir.resolve("config.json") }

    /**
     * Load the config from disk.
     * If the file doesn't exist or is malformed, the config tree's defaults are kept.
     */
    fun load(categories: List<ConfigCategory>) {
        if (!Files.exists(configFile)) {
            // No config file yet — create one with defaults
            save(categories)
            return
        }

        try {
            val json = Files.readString(configFile)
            ConfigSerializer.deserialize(json, categories)
        } catch (e: Exception) {
            // Malformed config — reset to defaults and overwrite
            resetToDefaults(categories)
            save(categories)
        }
    }

    /**
     * Save the current config tree to disk.
     * Creates the directory if it doesn't exist.
     */
    fun save(categories: List<ConfigCategory>) {
        try {
            Files.createDirectories(configDir)
            val json = ConfigSerializer.serialize(categories)
            val tempFile = configFile.resolveSibling("config.json.tmp")
            Files.writeString(tempFile, json)
            Files.move(tempFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            // Log the error — in a real mod you'd use the mod logger
            System.err.println("[KazzUtils] Failed to save config: ${e.message}")
        }
    }

    /**
     * Reset all properties in the tree to their default values.
     */
    private fun resetToDefaults(categories: List<ConfigCategory>) {
        for (category in categories) {
            for (subCategory in category.subCategories) {
                for (prop in subCategory.directProperties) {
                    prop.reset()
                }
                for (group in subCategory.groups) {
                    for (prop in group.properties) {
                        prop.reset()
                    }
                }
            }
        }
    }
}