package de.kazz.config

/**
 * A simple ARGB color wrapper.
 * Stored as a 32-bit packed int: 0xAARRGGBB.
 *
 * Provides convenience for both serialization and rendering.
 */
@JvmInline
value class ConfigColor(val argb: Int) {

    companion object {
        val WHITE = ConfigColor(0xFFFFFFFF.toInt())
        val BLACK = ConfigColor(0xFF000000.toInt())
        val RED = ConfigColor(0xFFFF0000.toInt())
        val GREEN = ConfigColor(0xFF00FF00.toInt())
        val BLUE = ConfigColor(0xFF0000FF.toInt())
        val TRANSPARENT = ConfigColor(0x00000000)

        /**
         * Create a color from individual 0–255 ARGB components.
         */
        fun of(alpha: Int, red: Int, green: Int, blue: Int): ConfigColor {
            val packed = (alpha.coerceIn(0, 255) shl 24) or
                    (red.coerceIn(0, 255) shl 16) or
                    (green.coerceIn(0, 255) shl 8) or
                    blue.coerceIn(0, 255)
            return ConfigColor(packed)
        }

        /**
         * Create from RGB (fully opaque).
         */
        fun of(red: Int, green: Int, blue: Int): ConfigColor = of(255, red, green, blue)

        /**
         * Parse from a hex string like "#FF00FF" or "#FF00FFFF" (with alpha) or "0xAARRGGBB".
         */
        fun fromHex(hex: String): ConfigColor {
            val cleaned = hex.removePrefix("#").removePrefix("0x")
            val value = cleaned.toLong(16)
            return when (cleaned.length) {
                6 -> ConfigColor((0xFF000000L or (value shl 8) or (value shr 16)).toInt())
                8 -> ConfigColor(value.toInt())
                else -> throw IllegalArgumentException("Invalid hex color: $hex")
            }
        }
    }

    val alpha: Int get() = (argb shr 24) and 0xFF
    val red: Int get() = (argb shr 16) and 0xFF
    val green: Int get() = (argb shr 8) and 0xFF
    val blue: Int get() = argb and 0xFF

    /**
     * Returns the hex string representation (e.g., "#FF00FF").
     * If the color is not fully opaque, includes alpha (e.g., "#80FF00FF").
     */
    fun toHex(): String {
        return if (alpha == 255) {
            "#%06X".format(red * 65536 + green * 256 + blue)
        } else {
            "#%08X".format(argb)
        }
    }

    override fun toString(): String = toHex()
}