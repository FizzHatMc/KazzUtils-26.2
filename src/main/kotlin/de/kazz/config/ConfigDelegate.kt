package de.kazz.config

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A Kotlin property delegate that creates and registers a [ConfigProperty]
 * when the property is declared inside a DSL builder block.
 */
class ConfigPropertyDelegate<T>(
    private val keyPrefix: String,
    private val displayName: String,
    private val description: String,
    private val defaultValue: T,
    private val type: ConfigType,
    private val enumClass: Class<out Enum<*>>? = null,
    private val min: Double? = null,
    private val max: Double? = null,
    private val action: (() -> Unit)? = null
) {
    /**
     * Callback invoked by [provideDelegate] with the created [ConfigProperty].
     * The DSL builder sets this to register the property in its list.
     */
    var registrationCallback: ((ConfigProperty<T>) -> Unit)? = null

    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadWriteProperty<Any?, T> {
        val fullKey = "$keyPrefix.${prop.name}"
        val p = ConfigProperty(
            key = fullKey,
            name = displayName,
            description = description,
            defaultValue = defaultValue,
            currentValue = defaultValue,
            type = type,
            enumClass = enumClass,
            min = min,
            max = max,
            action = action
        )
        // Register with the global config registry
        KazzConfig.registerProperty(p)
        registrationCallback?.invoke(p)
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = p.currentValue
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                p.currentValue = value
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// DSL Builder classes
// ────────────────────────────────────────────────────────────

/**
 * Builder for a category page.
 */
class CategoryBuilder(val keyPrefix: String) {
    private val subCategoryBuilders = mutableListOf<SubCategoryBuilder>()

    /**
     * Adds a sub-category to this category.
     *
     * @param name  Display name of the sub-category (e.g., "Dungeon")
     * @param block Receiver lambda for configuring the sub-category
     */
    fun subCategory(name: String, block: SubCategoryBuilder.() -> Unit) {
        val subKey = "$keyPrefix.${name.cleanKey()}"
        val builder = SubCategoryBuilder(subKey, name)
        builder.block()
        subCategoryBuilders.add(builder)
    }

    internal fun build(name: String): ConfigCategory {
        return ConfigCategory(
            name = name,
            key = keyPrefix,
            subCategories = subCategoryBuilders.map { it.build() }
        )
    }
}

/**
 * Builder for a sub-category page.
 */
class SubCategoryBuilder(val keyPrefix: String, private val displayName: String) {
    private val directProperties = mutableListOf<ConfigProperty<*>>()
    private val groupBuilders = mutableListOf<GroupBuilder>()

    // ── Toggle (Boolean) ──────────────────────────────────

    /**
     * A boolean toggle property.
     *
     * Usage:
     * ```kotlin
     * toggle(name = "Enable Feature", description = "...", default = false) var myFeature
     * ```
     */
    fun toggle(
        name: String,
        description: String,
        default: Boolean = false
    ): ConfigPropertyDelegate<Boolean> {
        val delegate = ConfigPropertyDelegate<Boolean>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.BOOLEAN
        )
        delegate.registrationCallback = { directProperties.add(it) }
        return delegate
    }

    // ── Integer Slider / Input ────────────────────────────

    /**
     * An integer property with optional min/max constraints.
     */
    fun intSlider(
        name: String,
        description: String,
        default: Int = 0,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE
    ): ConfigPropertyDelegate<Int> {
        val delegate = ConfigPropertyDelegate<Int>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.INT,
            min = min.toDouble(),
            max = max.toDouble()
        )
        delegate.registrationCallback = { directProperties.add(it) }
        return delegate
    }

    // ── Double Slider / Input ─────────────────────────────

    /**
     * A double property with optional min/max constraints.
     */
    fun doubleSlider(
        name: String,
        description: String,
        default: Double = 0.0,
        min: Double = Double.MIN_VALUE,
        max: Double = Double.MAX_VALUE
    ): ConfigPropertyDelegate<Double> {
        val delegate = ConfigPropertyDelegate<Double>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.DOUBLE,
            min = min,
            max = max
        )
        delegate.registrationCallback = { directProperties.add(it) }
        return delegate
    }

    // ── String Text ───────────────────────────────────────

    /**
     * A string text property.
     */
    fun text(
        name: String,
        description: String,
        default: String = ""
    ): ConfigPropertyDelegate<String> {
        val delegate = ConfigPropertyDelegate<String>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.STRING
        )
        delegate.registrationCallback = { directProperties.add(it) }
        return delegate
    }

    // ── Enum Choice ───────────────────────────────────────

    /**
     * An enum choice property (dropdown / cycle selector).
     *
     * Usage:
     * ```kotlin
     * enum class WaypointMode { PIN, CIRCLE, DYNAMIC }
     *
     * // In subCategory:
     * enumChoice("Mode", "Waypoint display mode", WaypointMode.PIN) var waypointMode
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : Enum<E>> enumChoice(
        name: String,
        description: String,
        default: E
    ): ConfigPropertyDelegate<E> {
        val delegate = ConfigPropertyDelegate<E>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.ENUM,
            enumClass = (default::class.java as Class<out Enum<*>>)
        )
        delegate.registrationCallback = { directProperties.add(it) }
        return delegate
    }

    // ── Color ─────────────────────────────────────────────

    /**
     * A color property.
     */
    fun color(
        name: String,
        description: String,
        default: ConfigColor = ConfigColor.WHITE
    ): ConfigPropertyDelegate<ConfigColor> {
        val delegate = ConfigPropertyDelegate<ConfigColor>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.COLOR
        )
        delegate.registrationCallback = { directProperties.add(it) }
        return delegate
    }

    // ── Action Button ─────────────────────────────────────

    /**
     * An action button (no persistent value, fires a callback on click).
     *
     * Usage:
     * ```kotlin
     * actionButton("Reset", "Resets all settings") {
     *     KazzConfig.resetAll()
     * }
     * ```
     *
     * Note: This does NOT use the delegate pattern (no `var` declaration)
     * because action buttons have no storable value.
     */
    fun actionButton(
        name: String,
        description: String,
        action: () -> Unit
    ) {
        val key = "$keyPrefix.${name.cleanKey()}"
        val prop = ConfigProperty<Unit>(
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

    // ── Group (Hider) ─────────────────────────────────────

    /**
     * Creates a collapsible group (hider) within this sub-category.
     *
     * @param name        Display name of the group (e.g., "Diana Features")
     * @param description Tooltip text for the group
     * @param block       Receiver lambda for configuring properties inside the group
     */
    fun group(name: String, description: String, block: GroupBuilder.() -> Unit) {
        val groupKey = "$keyPrefix.${name.cleanKey()}"
        val builder = GroupBuilder(groupKey, name, description)
        builder.block()
        groupBuilders.add(builder)
    }

    internal fun build(): ConfigSubCategory {
        return ConfigSubCategory(
            name = displayName,
            key = keyPrefix,
            directProperties = directProperties.toList(),
            groups = groupBuilders.map { it.build() }
        )
    }
}

/**
 * Builder for a collapsible group (hider) within a sub-category.
 */
class GroupBuilder(val keyPrefix: String, private val displayName: String, val description: String) {
    private val properties = mutableListOf<ConfigProperty<*>>()

    // ── Same factory functions as SubCategoryBuilder ──────

    fun toggle(
        name: String,
        description: String,
        default: Boolean = false
    ): ConfigPropertyDelegate<Boolean> {
        val delegate = ConfigPropertyDelegate<Boolean>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.BOOLEAN
        )
        delegate.registrationCallback = { properties.add(it) }
        return delegate
    }

    fun intSlider(
        name: String,
        description: String,
        default: Int = 0,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE
    ): ConfigPropertyDelegate<Int> {
        val delegate = ConfigPropertyDelegate<Int>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.INT,
            min = min.toDouble(),
            max = max.toDouble()
        )
        delegate.registrationCallback = { properties.add(it) }
        return delegate
    }

    fun doubleSlider(
        name: String,
        description: String,
        default: Double = 0.0,
        min: Double = Double.MIN_VALUE,
        max: Double = Double.MAX_VALUE
    ): ConfigPropertyDelegate<Double> {
        val delegate = ConfigPropertyDelegate<Double>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.DOUBLE,
            min = min,
            max = max
        )
        delegate.registrationCallback = { properties.add(it) }
        return delegate
    }

    fun text(
        name: String,
        description: String,
        default: String = ""
    ): ConfigPropertyDelegate<String> {
        val delegate = ConfigPropertyDelegate<String>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.STRING
        )
        delegate.registrationCallback = { properties.add(it) }
        return delegate
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Enum<E>> enumChoice(
        name: String,
        description: String,
        default: E
    ): ConfigPropertyDelegate<E> {
        val delegate = ConfigPropertyDelegate<E>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.ENUM,
            enumClass = (default::class.java as Class<out Enum<*>>)
        )
        delegate.registrationCallback = { properties.add(it) }
        return delegate
    }

    fun color(
        name: String,
        description: String,
        default: ConfigColor = ConfigColor.WHITE
    ): ConfigPropertyDelegate<ConfigColor> {
        val delegate = ConfigPropertyDelegate<ConfigColor>(
            keyPrefix = keyPrefix,
            displayName = name,
            description = description,
            defaultValue = default,
            type = ConfigType.COLOR
        )
        delegate.registrationCallback = { properties.add(it) }
        return delegate
    }

    fun actionButton(
        name: String,
        description: String,
        action: () -> Unit
    ) {
        val key = "$keyPrefix.${name.cleanKey()}"
        val prop = ConfigProperty<Unit>(
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

    internal fun build(): ConfigGroup {
        return ConfigGroup(
            name = displayName,
            description = description,
            key = keyPrefix,
            properties = properties.toList()
        )
    }
}

// ────────────────────────────────────────────────────────────
// Internal helpers
// ────────────────────────────────────────────────────────────

/**
 * Cleans a display name into a key-friendly lowercase string without spaces.
 */
internal fun String.cleanKey(): String {
    return lowercase().replace(" ", "").replace("-", "").replace("_", "")
}