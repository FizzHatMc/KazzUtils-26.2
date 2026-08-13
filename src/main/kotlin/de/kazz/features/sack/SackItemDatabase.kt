package de.kazz.features.sack

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

/**
 * Represents a single item that can be stored in a sack.
 *
 * @property name The display name of the item (e.g., "Enchanted Cobblestone")
 * @property id The Minecraft registry ID for rendering later (e.g., "minecraft:cobblestone")
 */
data class SackItemEntry(
    val name: String,
    val id: String
)

/**
 * Represents a sack type with its possible items.
 */
data class SackType(
    val name: String,
    val items: List<SackItemEntry>
)

/**
 * Represents the full database structure from sack_items.json.
 */
data class SackDatabase(
    val sacks: List<SackType>
)

/**
 * Loads and provides access to the sack items database from sack_items.json.
 *
 * This database is shipped with the mod and contains all known sack types
 * and their possible items. It is read-only at runtime.
 */
object SackItemDatabase {

    private val LOGGER = LoggerFactory.getLogger("sack-item-database")
    private val gson = Gson()

    /** All sack types loaded from the JSON. */
    private var database: SackDatabase = SackDatabase(emptyList())

    /** Flat list of all items across all sacks, keyed by item name. */
    private val allItemsMap: MutableMap<String, SackItemEntry> = mutableMapOf()

    /**
     * Initializes the database by loading sack_items.json from mod resources.
     * Must be called once during mod initialization.
     */
    fun initialize() {
        try {
            val resourceStream = javaClass.getResourceAsStream("/assets/kazzutils/sack_data/sack_items.json")
                ?: run {
                    // Fallback: try from classpath directly
                    val classLoaderStream = SackItemDatabase::class.java.classLoader
                        .getResourceAsStream("assets/kazzutils/sack_data/sack_items.json")
                    if (classLoaderStream == null) {
                        LOGGER.error("Could not find sack_items.json in resources!")
                        return
                    }
                    classLoaderStream
                }

            val reader = InputStreamReader(resourceStream)
            database = gson.fromJson(reader, SackDatabase::class.java)
            reader.close()

            // Build the flat map
            for (sack in database.sacks) {
                for (item in sack.items) {
                    allItemsMap[item.name] = item
                }
            }

            LOGGER.info("Loaded ${database.sacks.size} sack types with ${allItemsMap.size} total items")
        } catch (e: Exception) {
            LOGGER.error("Failed to load sack_items.json: ${e.message}", e)
        }
    }

    /**
     * Returns all sack types.
     */
    fun getSacks(): List<SackType> = database.sacks

    /**
     * Returns all items across all sacks as a flat list.
     */
    fun getAllItems(): List<SackItemEntry> = allItemsMap.values.toList()

    /**
     * Returns the [SackItemEntry] for a given item name, or null if not found.
     */
    fun getItem(name: String): SackItemEntry? = allItemsMap[name]

    /**
     * Searches items by name (case-insensitive, partial match).
     *
     * @param query The search query
     * @return Matching items sorted alphabetically
     */
    fun searchItems(query: String): List<SackItemEntry> {
        if (query.isBlank()) return getAllItems().sortedBy { it.name }
        val lower = query.lowercase()
        return allItemsMap.values
            .filter { it.name.lowercase().contains(lower) }
            .sortedBy { it.name }
    }

    /**
     * Returns the sack type(s) that contain the given item name.
     */
    fun findSacksForItem(itemName: String): List<SackType> {
        return database.sacks.filter { sack ->
            sack.items.any { it.name == itemName }
        }
    }
}