package de.kazz.config

/**
 * A typed wrapper around a [ConfigProperty] that provides direct `.value` access.
 *
 * Usage:
 * ```kotlin
 * val bossTimer = toggle(name = "Boss Timer", description = "...", default = true)
 * // Access:
 * val enabled = bossTimer.value
 * bossTimer.value = false
 * ```
 */
class ConfigValue<T> @PublishedApi internal constructor(
    val property: ConfigProperty<T>
) {
    /**
     * Get or set the current value directly.
     */
    var value: T
        get() = property.currentValue
        set(v) { property.currentValue = v }

    /**
     * Register a callback for when this value changes.
     */
    fun onChanged(callback: (T) -> Unit) {
        property.onChanged = { callback(it.currentValue) }
    }

    override fun toString(): String = "ConfigValue(${property.key} = ${property.currentValue})"
}