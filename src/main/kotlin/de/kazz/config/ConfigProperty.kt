package de.kazz.config

/**
 * A single typed config property.
 *
 * @param key          Dot-separated path (e.g., "combat.dungeon.bossTimer")
 * @param name         Human-readable display name
 * @param description  Tooltip / help text
 * @param defaultValue The default value (used as fallback)
 * @param currentValue The current (possibly user-modified) value
 * @param type         The [ConfigType] for serialization discrimination
 * @param enumClass    For ENUM type, the enum class; null otherwise
 * @param min          For INT/DOUBLE, the minimum value; null otherwise
 * @param max          For INT/DOUBLE, the maximum value; null otherwise
 * @param action       For ACTION_BUTTON type, a callback invoked when the button is pressed
 */
class ConfigProperty<T>(
    val key: String,
    val name: String,
    val description: String,
    val defaultValue: T,
    currentValue: T,
    val type: ConfigType,
    val enumClass: Class<out Enum<*>>? = null,
    val min: Double? = null,
    val max: Double? = null,
    val action: (() -> Unit)? = null
) {
    /**
     * The current value. Setting this triggers the [onChanged] callback.
     */
    var currentValue: T = currentValue
        set(value) {
            field = value
            onChanged?.invoke(this)
        }

    /**
     * Callback invoked whenever [currentValue] changes.
     * Used by [KazzConfig] to mark the config as dirty for saving.
     */
    var onChanged: ((ConfigProperty<T>) -> Unit)? = null

    /**
     * If non-null and returns true, this property should be hidden from the UI.
     * The property's value is preserved regardless of visibility.
     */
    var hiddenWhen: (() -> Boolean)? = null

    /**
     * Reset this property to its default value.
     */
    fun reset() {
        currentValue = defaultValue
    }

    /**
     * Returns the current value as a [String] for serialization.
     */
    fun serializeValue(): String = when (type) {
        ConfigType.BOOLEAN -> (currentValue as Boolean).toString()
        ConfigType.INT -> (currentValue as Int).toString()
        ConfigType.DOUBLE -> (currentValue as Double).toString()
        ConfigType.STRING -> currentValue as String
        ConfigType.ENUM -> (currentValue as Enum<*>).name
        ConfigType.COLOR -> (currentValue as ConfigColor).toHex()
        ConfigType.ACTION_BUTTON -> "" // action buttons have no persistent value
    }

    /**
     * Deserialize a [String] back into the typed value.
     */
    @Suppress("UNCHECKED_CAST")
    fun deserializeValue(value: String): T {
        val result: Any = when (type) {
            ConfigType.BOOLEAN -> value.toBoolean()
            ConfigType.INT -> value.toInt()
            ConfigType.DOUBLE -> value.toDouble()
            ConfigType.STRING -> value
            ConfigType.ENUM -> {
                val enumClass = enumClass
                    ?: throw IllegalStateException("enumClass must be set for ENUM type")
                val constants = enumClass.enumConstants as Array<Enum<*>>
                constants.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: defaultValue as Enum<*>
            }
            ConfigType.COLOR -> ConfigColor.fromHex(value)
            ConfigType.ACTION_BUTTON -> defaultValue as Any
        }
        return result as T
    }

    override fun toString(): String = "ConfigProperty(key='$key', name='$name', value=$currentValue)"
}