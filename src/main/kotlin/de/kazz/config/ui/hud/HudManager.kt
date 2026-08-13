package de.kazz.config.ui.hud

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.collections.iterator

/**
 * Singleton registry for all custom HUD elements.
 *
 * Elements are identified by a unique string ID and can be looked up,
 * iterated, and persisted to disk alongside the main config.
 *
 * Position data is saved to `<config_dir>/kazzutils/hud.json` using
 * the same atomic-write strategy as [ConfigStorage].
 */
object HudManager {

    private val elements = mutableMapOf<String, HudElement>()

    private val hudConfigDir by lazy {
        val gameDir = FabricLoader.getInstance().gameDir
        gameDir.resolve("config").resolve("kazzutils")
    }

    private val hudConfigFile by lazy { hudConfigDir.resolve("hud.json") }

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // ── Element Registration ──────────────────────────────

    /**
     * Register a HUD element.
     * If an element with the same ID already exists, it is replaced.
     *
     * @param element the HUD element to register
     */
    fun register(element: HudElement) {
        elements[element.id] = element
    }

    /**
     * Get a registered element by its ID.
     */
    fun get(id: String): HudElement? = elements[id]

    /**
     * Get all registered elements.
     */
    fun getAll(): Collection<HudElement> = elements.values

    /**
     * Get all enabled elements, sorted by their [HudElement.layer] for correct Z-ordering.
     */
    fun getEnabled(): List<HudElement> = elements.values
        .filter { it.enabled }
        .sortedBy { it.layer.zOffset }

    /**
     * Check if an element with the given ID is registered.
     */
    fun contains(id: String): Boolean = elements.containsKey(id)

    /**
     * Remove a registered element by its ID.
     */
    fun remove(id: String) {
        elements.remove(id)
    }

    // ── Persistence ───────────────────────────────────────

    /**
     * Save all element positions, scales, and enabled states to disk.
     */
    fun save() {
        try {
            Files.createDirectories(hudConfigDir)
            val root = JsonObject()

            for ((id, element) in elements) {
                val obj = JsonObject()
                obj.addProperty("x", element.x)
                obj.addProperty("y", element.y)
                obj.addProperty("scale", element.scale)
                obj.addProperty("scaleX", element.scaleX)
                obj.addProperty("scaleY", element.scaleY)
                obj.addProperty("enabled", element.enabled)
                obj.addProperty("layer", element.layer.name)
                root.add(id, obj)
            }

            val json = gson.toJson(root)
            val tempFile = hudConfigFile.resolveSibling("hud.json.tmp")
            Files.writeString(tempFile, json)
            Files.move(tempFile, hudConfigFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            System.err.println("[KazzUtils] Failed to save HUD positions: ${e.message}")
        }
    }

    /**
     * Load all element positions, scales, and enabled states from disk.
     * Only applies values to already-registered elements.
     */
    fun load() {
        if (!Files.exists(hudConfigFile)) return

        try {
            val json = Files.readString(hudConfigFile)
            val root = JsonParser.parseString(json).asJsonObject

            for ((id, element) in elements) {
                val obj = root.getAsJsonObject(id) ?: continue
                element.x = obj.get("x")?.asFloat ?: element.x
                element.y = obj.get("y")?.asFloat ?: element.y
                element.scale = obj.get("scale")?.asFloat ?: element.scale
                element.scaleX = obj.get("scaleX")?.asFloat ?: element.scaleX
                element.scaleY = obj.get("scaleY")?.asFloat ?: element.scaleY
                element.enabled = obj.get("enabled")?.asBoolean ?: element.enabled
                obj.get("layer")?.let {
                    try { element.layer = HudLayer.valueOf(it.asString) } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            System.err.println("[KazzUtils] Failed to load HUD positions: ${e.message}")
        }
    }

    // ── Batch Operations ──────────────────────────────────

    /**
     * Reset all element positions to their defaults (0, 0, scale 1).
     * Does NOT change enabled state.
     */
    fun resetPositions() {
        for (element in elements.values) {
            element.x = 0f
            element.y = 0f
            element.scale = 1f
            element.scaleX = 1f
            element.scaleY = 1f
        }
    }

    /**
     * Disable all elements.
     */
    fun disableAll() {
        for (element in elements.values) {
            element.enabled = false
        }
    }

    /**
     * Enable all elements.
     */
    fun enableAll() {
        for (element in elements.values) {
            element.enabled = true
        }
    }
}