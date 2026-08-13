package de.kazz.features.borderbox

import de.kazz.config.ConfigColor

/**
 * A wireframe border box rendered in-world around a center point.
 *
 * @param centerX      World X coordinate of the box center
 * @param centerY      World Y coordinate of the box center
 * @param centerZ      World Z coordinate of the box center
 * @param seeThrough   Whether the box should be visible through walls
 * @param color        The ARGB colour of the wireframe edges
 * @param thickness    Cross-section width/height of each wireframe edge in blocks
 * @param scale        Uniform scale factor. A scale of 2.0 means the box spans
 *                     a 2×2×2 block area centered on [centerX/Y/Z].
 * @param ttlSeconds   Time-to-live in seconds (fractional allowed).
 *                     `null` means the box persists until the world changes.
 * @param createdAtTick Game tick when this box was created (set by [BorderBoxManager])
 */
data class BorderBox(
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val seeThrough: Boolean,
    val color: ConfigColor,
    val thickness: Double,
    val scale: Double,
    val ttlSeconds: Double? = null,
    val createdAtTick: Long = 0L,
)