package de.kazz.config

import org.slf4j.LoggerFactory

/**
 * Root config singleton for KazzUtils.
 *
 * This is the central entry point for defining and accessing config properties.
 *
 * ## Defining Config Properties
 *
 * Use the DSL functions inside the [init] block:
 *
 * ```kotlin
 * object KazzConfig {
 *     init {
 *         category("Combat") {
 *             subCategory("Dungeon") {
 *                 toggle(
 *                     name = "Boss Timer",
 *                     description = "Shows the boss timer while in a dungeon",
 *                     default = true
 *                 ) var bossTimer
 *
 *                 group("Diana Features", "Features related to the Diana event") {
 *                     toggle(
 *                         name = "Enable Diana Waypoint",
 *                         description = "Enables automatic Waypoints for the Diana Event.",
 *                         default = false
 *                     ) var dianaWaypoint
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Accessing Config Values
 *
 * ```kotlin
 * // By key (recommended for runtime access):
 * val enabled = KazzConfig.getBoolean("combat.dungeon.dianaWaypoint")
 *
 * // By generic type:
 * val range = KazzConfig.get<Int>("combat.dungeon.dianaWaypoints.waypointRange")
 *
 * // Setting a value:
 * KazzConfig.set("combat.dungeon.dianaWaypoint", true)
 * ```
 */
object KazzConfig {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-config")

    /** All registered categories (the top-level tree structure). */
    private val categories = mutableListOf<ConfigCategory>()

    /**
     * Flat registry of all properties, keyed by their dot-separated path.
     * This is populated during [init] by the DSL builder blocks.
     */
    private val propertyRegistry = mutableMapOf<String, ConfigProperty<*>>()

    /** Whether the config has unsaved changes. */
    private var dirty = false

    init {
        // Initialize the config — currently no default categories defined.
        // The user will add their own via the init block.
        // Subclasses / extensions can add categories like:
        //
        // init {
        //     category("Combat") { ... }
        //     category("Farming") { ... }
        // }
    }

    // ── DSL Builder Functions ────────────────────────────

    /**
     * Creates a new category page.
     *
     * @param name  Display name of the category (e.g., "Combat", "Farming")
     * @param block Receiver lambda for configuring sub-categories
     * @return The created [ConfigCategory]
     */
    fun category(name: String, block: CategoryBuilder.() -> Unit): ConfigCategory {
        val key = name.cleanKey()
        val builder = CategoryBuilder(key)
        builder.block()
        val cat = builder.build(name)
        categories.add(cat)
        return cat
    }

    // ── Accessors ────────────────────────────────────────

    /**
     * Get a typed config property by its key.
     *
     * @param key Dot-separated path (e.g., "combat.dungeon.bossTimer")
     * @return The [ConfigProperty] or null if not found
     */
    fun property(key: String): ConfigProperty<*>? = propertyRegistry[key]

    /**
     * Get the current value of a property as a specific type.
     *
     * @param key Dot-separated path
     * @return The current value, or null if the property doesn't exist
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = propertyRegistry[key]?.currentValue as? T

    /**
     * Get a boolean property value.
     *
     * @param key Dot-separated path
     * @param default Fallback value if the property doesn't exist or is not a boolean
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        val prop = propertyRegistry[key] ?: return default
        return (prop.currentValue as? Boolean) ?: default
    }

    /**
     * Get an integer property value.
     */
    fun getInt(key: String, default: Int = 0): Int {
        val prop = propertyRegistry[key] ?: return default
        return (prop.currentValue as? Int) ?: default
    }

    /**
     * Get a double property value.
     */
    fun getDouble(key: String, default: Double = 0.0): Double {
        val prop = propertyRegistry[key] ?: return default
        return (prop.currentValue as? Double) ?: default
    }

    /**
     * Get a string property value.
     */
    fun getString(key: String, default: String = ""): String {
        val prop = propertyRegistry[key] ?: return default
        return (prop.currentValue as? String) ?: default
    }

    /**
     * Get a color property value.
     */
    fun getColor(key: String, default: ConfigColor = ConfigColor.WHITE): ConfigColor {
        val prop = propertyRegistry[key] ?: return default
        return (prop.currentValue as? ConfigColor) ?: default
    }

    /**
     * Set a property's value by its key.
     *
     * @param key   Dot-separated path
     * @param value The new value
     */
    fun <T> set(key: String, value: T) {
        val prop = propertyRegistry[key] ?: return
        @Suppress("UNCHECKED_CAST")
        (prop as ConfigProperty<T>).currentValue = value
    }

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
        ConfigStorage.load(categories)
        dirty = false
    }

    /**
     * Save the config to disk.
     * The UI should call this when the config screen is closed.
     */
    fun save() {
        LOGGER.info("Saving config to disk...")
        ConfigStorage.save(categories)
        dirty = false
    }

    // ── Internal Registration ─────────────────────────────

    /**
     * Register a property into the flat registry.
     * Called by the delegate's [ConfigPropertyDelegate.provideDelegate].
     */
    internal fun registerProperty(property: ConfigProperty<*>) {
        propertyRegistry[property.key] = property
        property.onChanged = { markDirty() }
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
    fun getCategories(): List<ConfigCategory> = categories.toList()

    /**
     * Check if the config has unsaved changes.
     */
    fun isDirty(): Boolean = dirty
}