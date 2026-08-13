package de.kazz.features.general.sack

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.collections.iterator

/**
 * Represents the persisted data for a single tracked/trackable item.
 *
 * @property id The Minecraft registry ID (e.g., "minecraft:cobblestone")
 * @property amount The last-known amount of this item in the sack
 * @property tracked Whether the user has selected this item for HUD display
 */
data class TrackedItemData(
    val id: String,
    var amount: Int = 0,
    var tracked: Boolean = false
)

/**
 * Manages persistence of sack tracker data to disk.
 *
 * Data is stored in `<config_dir>/kazzutils/sack_tracker_data.json`.
 * All items from [SackItemDatabase] are pre-populated on first load.
 * The file is auto-saved whenever tracked status or amounts change.
 */
object SackTrackerData {

    private val LOGGER = LoggerFactory.getLogger("sack-tracker-data")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val configDir by lazy {
        FabricLoader.getInstance().gameDir.resolve("config").resolve("kazzutils")
    }

    private val dataFile by lazy { configDir.resolve("sack_tracker_data.json") }

    /** All items keyed by display name. */
    private val items: MutableMap<String, TrackedItemData> = mutableMapOf()

    /** Whether data has been modified since last save. */
    private var dirty = false

    /**
     * Initializes the tracker data.
     * Loads from disk if available, otherwise creates from the database.
     * Must be called once during mod initialization.
     */
    fun initialize() {
        if (Files.exists(dataFile)) {
            loadFromDisk()
        } else {
            // First run: populate from the database
            initializeFromDatabase()
            save()
        }
        // Sync tracked items filter and amounts into SackTracker for HUD rendering
        SackTracker.syncFromData()
        LOGGER.info("Loaded ${items.size} sack tracker items (${getTrackedItems().size} tracked)")
    }

    /**
     * Populates the data from [SackItemDatabase] on first run.
     */
    private fun initializeFromDatabase() {
        for (entry in SackItemDatabase.getAllItems()) {
            items[entry.name] = TrackedItemData(
                id = entry.id,
                amount = 0,
                tracked = false
            )
        }
    }

    /**
     * Loads data from the JSON file on disk.
     */
    private fun loadFromDisk() {
        try {
            val json = Files.readString(dataFile)
            val root = JsonParser.parseString(json).asJsonObject

            // Load tracked items
            val trackedObj = root.getAsJsonObject("items") ?: return
            for ((name, element) in trackedObj.entrySet()) {
                val obj = element.asJsonObject
                val id = obj.get("id")?.asString ?: continue
                val amount = obj.get("amount")?.asInt ?: 0
                val tracked = obj.get("tracked")?.asBoolean ?: false
                items[name] = TrackedItemData(id = id, amount = amount, tracked = tracked)
            }

            // If any items from the database are missing (e.g., mod updated with new items), add them
            for (entry in SackItemDatabase.getAllItems()) {
                if (entry.name !in items) {
                    items[entry.name] = TrackedItemData(id = entry.id, amount = 0, tracked = false)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load sack_tracker_data.json: ${e.message}", e)
            // Fall back to database initialization
            initializeFromDatabase()
        }
    }

    /**
     * Saves data to disk if dirty.
     * Also saves immediately if [force] is true.
     */
    fun save(force: Boolean = false) {
        if (!force && !dirty && Files.exists(dataFile)) return
        try {
            Files.createDirectories(configDir)
            val root = JsonObject()
            val itemsObj = JsonObject()

            for ((name, data) in items) {
                val obj = JsonObject()
                obj.addProperty("id", data.id)
                obj.addProperty("amount", data.amount)
                obj.addProperty("tracked", data.tracked)
                itemsObj.add(name, obj)
            }

            root.add("items", itemsObj)

            val json = gson.toJson(root)
            val tempFile = dataFile.resolveSibling("sack_tracker_data.json.tmp")
            Files.writeString(tempFile, json)
            Files.move(tempFile, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            dirty = false
        } catch (e: IOException) {
            LOGGER.error("Failed to save sack_tracker_data.json: ${e.message}", e)
        }
    }

    // ── Accessors ──────────────────────────────────────────────

    /**
     * Returns all items (both tracked and untracked).
     */
    fun getAllItems(): Map<String, TrackedItemData> = items.toMap()

    /**
     * Returns only the items the user has marked as tracked.
     */
    fun getTrackedItems(): Map<String, TrackedItemData> {
        return items.filter { it.value.tracked }
    }

    /**
     * Returns the data for a specific item, or null if not found.
     */
    fun getItem(name: String): TrackedItemData? = items[name]

    /**
     * Toggles the tracked status of an item.
     * Returns the new tracked state.
     */
    fun toggleTracked(name: String): Boolean {
        val data = items[name] ?: return false
        data.tracked = !data.tracked
        dirty = true
        return data.tracked
    }

    /**
     * Sets the tracked status of an item directly.
     * Also updates the in-memory [SackTracker] state for HUD rendering.
     */
    fun setTracked(name: String, tracked: Boolean) {
        val data = items[name] ?: return
        data.tracked = tracked
        dirty = true

        // Sync with SackTracker in-memory state for HUD rendering
        if (tracked) {
            SackTracker.trackedItemsFilter.add(name)
            // Ensure the item appears in trackedItems even if amount is 0
            if (name !in SackTracker.trackedItems) {
                SackTracker.trackedItems[name] = data.amount
            }
        } else {
            SackTracker.trackedItemsFilter.remove(name)
        }
    }

    /**
     * Updates the amount for an item.
     * Also updates the in-memory [SackTracker] state for HUD rendering.
     */
    fun setAmount(name: String, amount: Int) {
        val data = items[name] ?: return
        data.amount = amount
        dirty = true

        // Sync with SackTracker in-memory state for HUD rendering
        SackTracker.trackedItems[name] = amount
    }

    /**
     * Adds a delta to an item's amount (for chat-based updates).
     * Also updates the in-memory [SackTracker] state for HUD rendering.
     */
    fun addAmount(name: String, delta: Int) {
        val data = items[name] ?: return
        data.amount += delta
        dirty = true

        // Sync with SackTracker in-memory state for HUD rendering
        SackTracker.trackedItems[name] = data.amount
    }

    /**
     * Returns the set of tracked item names (for compatibility with [SackTracker.trackedItemsFilter]).
     */
    fun getTrackedNames(): Set<String> {
        return items.filter { it.value.tracked }.keys
    }

    /**
     * Force-save immediately regardless of dirty state.
     */
    fun forceSave() {
        dirty = true
        save()
    }
}