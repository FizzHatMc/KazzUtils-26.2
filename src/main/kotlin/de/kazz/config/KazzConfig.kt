package de.kazz.config

import org.slf4j.LoggerFactory

/**
 * Central registry and save/load hub for the KazzUtils config system.
 *
 * Config categories are defined as separate `object`s in their own files.
 * This singleton collects them, handles serialization, and provides
 * save/load capabilities.
 *
 * ## Defining Config Categories
 *
 * Create separate files for each top-level category:
 *
 * ```kotlin
 * // CombatConfig.kt
 * object CombatConfig : ConfigCategoryScope("Combat") {
 *     object Dungeon : ConfigSubCategoryScope("Dungeon", "combat") {
 *         val bossTimer = toggle(
 *             name = "Boss Timer",
 *             description = "Shows the boss timer while in a dungeon",
 *             default = true
 *         )
 *
 *         object DianaFeatures : ConfigGroupScope(
 *             "Diana Features",
 *             "Features related to the Diana event",
 *             "combat.dungeon"
 *         ) {
 *             val dianaWaypoint = toggle(
 *                 name = "Enable Diana Waypoint",
 *                 description = "Enables automatic Waypoints for the Diana Event.",
 *                 default = false
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## Accessing Config Values
 *
 * ```kotlin
 * val enabled = CombatConfig.Dungeon.DianaFeatures.dianaWaypoint.value
 * CombatConfig.Dungeon.DianaFeatures.dianaWaypoint.value = false
 * ```
 */
object KazzConfig {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-config")

    /** All registered category scopes (the top-level tree structure). */
    private val categoryScopes = mutableListOf<ConfigCategoryScope>()

    /** Flat registry of all properties, keyed by their dot-separated path. */
    private val propertyRegistry = mutableMapOf<String, ConfigProperty<*>>()

    /** Whether the config has unsaved changes. */
    private var dirty = false

    // ── Category Registry ─────────────────────────────────

    /**
     * Register a category scope. Called automatically by [ConfigCategoryScope.init].
     */
    internal fun registerCategory(category: ConfigCategoryScope) {
        categoryScopes.add(category)
    }

    /**
     * Find a category scope by its key.
     */
    internal fun findCategory(key: String): ConfigCategoryScope? {
        return categoryScopes.find { it.key == key }
    }

    /**
     * Find a sub-category scope by its parent category's key.
     * This looks through all categories' sub-categories.
     */
    internal fun findSubCategory(parentKey: String): ConfigSubCategoryScope? {
        for (category in categoryScopes) {
            for (sub in category.subCategories) {
                if (sub.key == parentKey) return sub
            }
        }
        return null
    }

    /**
     * Build and return the immutable tree of all registered categories.
     */
    private fun buildCategories(): List<ConfigCategory> {
        return categoryScopes.map { it.build() }
    }

    // ── Property Registry ─────────────────────────────────

    /**
     * Register a property into the flat registry.
     * Called by the factory functions in [ConfigSubCategoryScope] and [ConfigGroupScope].
     */
    internal fun registerProperty(property: ConfigProperty<*>) {
        propertyRegistry[property.key] = property
        property.onChanged = { markDirty() }
    }

    /**
     * Get a typed config property by its key.
     */
    fun property(key: String): ConfigProperty<*>? = propertyRegistry[key]

    /**
     * Reset a single property to its default value.
     */
    fun reset(key: String) {
        propertyRegistry[key]?.reset()
    }

    /**
     * Reset all properties to their defaults.
     */
    fun resetAll() {
        for (prop in propertyRegistry.values) {
            prop.reset()
        }
    }

    // ── Save / Load ──────────────────────────────────────

    /**
     * Load the config from disk.
     * Called once during mod initialization.
     */
    fun load() {
        LOGGER.info("Loading config from disk...")
        ConfigStorage.load(buildCategories())
        dirty = false
    }

    /**
     * Save the config to disk.
     * The UI should call this when the config screen is closed.
     */
    fun save() {
        LOGGER.info("Saving config to disk...")
        ConfigStorage.save(buildCategories())
        dirty = false
    }

    /**
     * Mark the config as having unsaved changes.
     */
    internal fun markDirty() {
        dirty = true
    }

    /**
     * Get all categories (for UI iteration).
     */
    fun getCategories(): List<ConfigCategory> = buildCategories()

    /**
     * Check if the config has unsaved changes.
     */
    fun isDirty(): Boolean = dirty
}