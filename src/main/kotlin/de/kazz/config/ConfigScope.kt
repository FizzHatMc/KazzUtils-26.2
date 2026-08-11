package de.kazz.config

/**
 * Cleans a display name into a key-friendly lowercase string without spaces.
 */
internal fun String.cleanKey(): String {
    return lowercase().replace(" ", "").replace("-", "").replace("_", "")
}

/**
 * Base class for a config category page (e.g., "Combat", "Farming").
 *
 * Each category is a top-level `object` in its own file.
 *
 * Usage:
 * ```kotlin
 * object CombatConfig : ConfigCategoryScope("Combat") {
 *     object Dungeon : ConfigSubCategoryScope("Dungeon", "combat") {
 *         val bossTimer = toggle(...)
 *     }
 * }
 * ```
 */
abstract class ConfigCategoryScope(val name: String) {

    /** Cleaned key used in dot-paths and JSON (e.g., "combat"). */
    val key: String = name.cleanKey()

    /** Sub-categories registered under this category. */
    internal val subCategories = mutableListOf<ConfigSubCategoryScope>()

    init {
        KazzConfig.registerCategory(this)
        // Force initialization of all nested sub-category objects so they
        // register themselves with this category eagerly.
        this::class.nestedClasses.forEach { nested ->
            if (ConfigSubCategoryScope::class.java.isAssignableFrom(nested.java)) {
                nested.objectInstance
            }
        }
    }

    /**
     * Called by [ConfigSubCategoryScope] to register itself with this category.
     */
    internal fun registerSubCategory(subCategory: ConfigSubCategoryScope) {
        subCategories.add(subCategory)
    }

    /**
     * Build the immutable tree node for this category.
     */
    internal fun build(): ConfigCategory {
        return ConfigCategory(
            name = name,
            key = key,
            subCategories = subCategories.map { it.build() }
        )
    }
}

/**
 * Base class for a sub-category page within a category (e.g., "Dungeon", "Garden").
 *
 * Usage:
 * ```kotlin
 * object Dungeon : ConfigSubCategoryScope("Dungeon", "combat") {
 *     val bossTimer = toggle(...)
 *     object DianaFeatures : ConfigGroupScope("Diana Features", "desc", "combat.dungeon") { ... }
 * }
 * ```
 */
abstract class ConfigSubCategoryScope(
    val name: String,
    parentKey: String
) {
    /** Dot-separated path prefix (e.g., "combat.dungeon"). */
    val key: String = "$parentKey.${name.cleanKey()}"

    /** Direct properties in this sub-category (not in a group). */
    internal val directProperties = mutableListOf<ConfigProperty<*>>()

    /** Collapsible groups within this sub-category. */
    internal val groups = mutableListOf<ConfigGroupScope>()

    init {
        // Find the parent category and register with it
        KazzConfig.findCategory(parentKey)?.registerSubCategory(this)
        // Force initialization of all nested group objects so they
        // register themselves with this sub-category eagerly.
        this::class.nestedClasses.forEach { nested ->
            if (ConfigGroupScope::class.java.isAssignableFrom(nested.java)) {
                nested.objectInstance
            }
        }
    }

    /**
     * Called by [ConfigGroupScope] to register itself with this sub-category.
     */
    internal fun registerGroup(group: ConfigGroupScope) {
        groups.add(group)
    }

    internal fun build(): ConfigSubCategory {
        return ConfigSubCategory(
            name = name,
            key = key,
            directProperties = directProperties.toList(),
            groups = groups.map { it.build() }
        )
    }

    // ── Factory functions ─────────────────────────────────

    /**
     * A boolean toggle property.
     *
     * Usage:
     * ```kotlin
     * val bossTimer = toggle(name = "Boss Timer", description = "...", default = true)
     * // Access: bossTimer.value
     * ```
     */
    fun toggle(
        name: String,
        description: String,
        default: Boolean = false
    ): ConfigValue<Boolean> {
        val prop = createProperty(name, description, default, ConfigType.BOOLEAN)
        return ConfigValue(prop)
    }

    /**
     * An integer property with optional min/max constraints.
     */
    fun intSlider(
        name: String,
        description: String,
        default: Int = 0,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE
    ): ConfigValue<Int> {
        val prop = createProperty(
            name = name,
            description = description,
            defaultValue = default,
            type = ConfigType.INT,
            min = min.toDouble(),
            max = max.toDouble()
        )
        return ConfigValue(prop)
    }

    /**
     * A double property with optional min/max constraints.
     */
    fun doubleSlider(
        name: String,
        description: String,
        default: Double = 0.0,
        min: Double = Double.MIN_VALUE,
        max: Double = Double.MAX_VALUE
    ): ConfigValue<Double> {
        val prop = createProperty(
            name = name,
            description = description,
            defaultValue = default,
            type = ConfigType.DOUBLE,
            min = min,
            max = max
        )
        return ConfigValue(prop)
    }

    /**
     * A string text property.
     */
    fun text(
        name: String,
        description: String,
        default: String = ""
    ): ConfigValue<String> {
        val prop = createProperty(name, description, default, ConfigType.STRING)
        return ConfigValue(prop)
    }

    /**
     * An enum choice property (dropdown / cycle selector).
     *
     * Usage:
     * ```kotlin
     * enum class WaypointMode { PIN, CIRCLE, DYNAMIC }
     * val waypointMode = enumChoice("Mode", "Waypoint display mode", WaypointMode.PIN)
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : Enum<E>> enumChoice(
        name: String,
        description: String,
        default: E
    ): ConfigValue<E> {
        val prop = ConfigProperty(
            key = "$key.${name.cleanKey()}",
            name = name,
            description = description,
            defaultValue = default,
            currentValue = default,
            type = ConfigType.ENUM,
            enumClass = (default::class.java as Class<out Enum<*>>)
        )
        KazzConfig.registerProperty(prop)
        directProperties.add(prop)
        return ConfigValue(prop)
    }

    /**
     * A color property.
     */
    fun color(
        name: String,
        description: String,
        default: ConfigColor = ConfigColor.WHITE
    ): ConfigValue<ConfigColor> {
        val prop = createProperty(name, description, default, ConfigType.COLOR)
        return ConfigValue(prop)
    }

    /**
     * An action button (no persistent value, fires a callback on click).
     *
     * Usage:
     * ```kotlin
     * actionButton("Reset", "Resets all settings") {
     *     KazzConfig.resetAll()
     * }
     * ```
     */
    fun actionButton(
        name: String,
        description: String,
        action: () -> Unit
    ) {
        val key = "$key.${name.cleanKey()}"
        val prop = ConfigProperty(
            key = key,
            name = name,
            description = description,
            defaultValue = Unit,
            currentValue = Unit,
            type = ConfigType.ACTION_BUTTON,
            action = action
        )
        KazzConfig.registerProperty(prop)
        directProperties.add(prop)
    }

    // ── Internal helpers ──────────────────────────────────

    @PublishedApi
    internal fun <T> createProperty(
        name: String,
        description: String,
        defaultValue: T,
        type: ConfigType,
        min: Double? = null,
        max: Double? = null
    ): ConfigProperty<T> {
        val prop = ConfigProperty(
            key = "$key.${name.cleanKey()}",
            name = name,
            description = description,
            defaultValue = defaultValue,
            currentValue = defaultValue,
            type = type,
            min = min,
            max = max
        )
        KazzConfig.registerProperty(prop)
        directProperties.add(prop)
        return prop
    }
}

/**
 * Base class for a collapsible group (hider) within a sub-category (e.g., "Diana Features").
 *
 * Usage:
 * ```kotlin
 * object DianaFeatures : ConfigGroupScope("Diana Features", "Features related to the Diana event", "combat.dungeon") {
 *     val dianaWaypoint = toggle(...)
 * }
 * ```
 */
abstract class ConfigGroupScope(
    val name: String,
    val description: String,
    parentKey: String
) {
    /** Dot-separated path prefix (e.g., "combat.dungeon.dianafeatures"). */
    val key: String = "$parentKey.${name.cleanKey()}"

    /** Properties within this group. */
    internal val properties = mutableListOf<ConfigProperty<*>>()

    init {
        // Find the parent sub-category and register with it
        KazzConfig.findSubCategory(parentKey)?.registerGroup(this)
    }

    internal fun build(): ConfigGroup {
        return ConfigGroup(
            name = name,
            description = description,
            key = key,
            properties = properties.toList()
        )
    }

    // ── Same factory functions as ConfigSubCategoryScope ──────

    fun toggle(
        name: String,
        description: String,
        default: Boolean = false
    ): ConfigValue<Boolean> {
        val prop = createProperty(name, description, default, ConfigType.BOOLEAN)
        return ConfigValue(prop)
    }

    fun intSlider(
        name: String,
        description: String,
        default: Int = 0,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE
    ): ConfigValue<Int> {
        val prop = createProperty(
            name = name,
            description = description,
            defaultValue = default,
            type = ConfigType.INT,
            min = min.toDouble(),
            max = max.toDouble()
        )
        return ConfigValue(prop)
    }

    fun doubleSlider(
        name: String,
        description: String,
        default: Double = 0.0,
        min: Double = Double.MIN_VALUE,
        max: Double = Double.MAX_VALUE
    ): ConfigValue<Double> {
        val prop = createProperty(
            name = name,
            description = description,
            defaultValue = default,
            type = ConfigType.DOUBLE,
            min = min,
            max = max
        )
        return ConfigValue(prop)
    }

    fun text(
        name: String,
        description: String,
        default: String = ""
    ): ConfigValue<String> {
        val prop = createProperty(name, description, default, ConfigType.STRING)
        return ConfigValue(prop)
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Enum<E>> enumChoice(
        name: String,
        description: String,
        default: E
    ): ConfigValue<E> {
        val prop = ConfigProperty(
            key = "$key.${name.cleanKey()}",
            name = name,
            description = description,
            defaultValue = default,
            currentValue = default,
            type = ConfigType.ENUM,
            enumClass = (default::class.java as Class<out Enum<*>>)
        )
        KazzConfig.registerProperty(prop)
        properties.add(prop)
        return ConfigValue(prop)
    }

    fun color(
        name: String,
        description: String,
        default: ConfigColor = ConfigColor.WHITE
    ): ConfigValue<ConfigColor> {
        val prop = createProperty(name, description, default, ConfigType.COLOR)
        return ConfigValue(prop)
    }

    fun actionButton(
        name: String,
        description: String,
        action: () -> Unit
    ) {
        val key = "$key.${name.cleanKey()}"
        val prop = ConfigProperty(
            key = key,
            name = name,
            description = description,
            defaultValue = Unit,
            currentValue = Unit,
            type = ConfigType.ACTION_BUTTON,
            action = action
        )
        KazzConfig.registerProperty(prop)
        properties.add(prop)
    }

    @PublishedApi
    internal fun <T> createProperty(
        name: String,
        description: String,
        defaultValue: T,
        type: ConfigType,
        min: Double? = null,
        max: Double? = null
    ): ConfigProperty<T> {
        val prop = ConfigProperty(
            key = "$key.${name.cleanKey()}",
            name = name,
            description = description,
            defaultValue = defaultValue,
            currentValue = defaultValue,
            type = type,
            min = min,
            max = max
        )
        KazzConfig.registerProperty(prop)
        properties.add(prop)
        return prop
    }
}