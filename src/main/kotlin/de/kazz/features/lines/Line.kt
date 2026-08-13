package de.kazz.features.lines

import de.kazz.config.ConfigColor

/**
 * Style of line rendering.
 */
enum class LineStyle {
    /** Filled rectangular prism along the start→end vector. */
    SOLID,
    /** Only the 12 edges of the prism, respecting [Line.thickness]. */
    WIREFRAME,
}

/**
 * A single line to be rendered in-world between two points.
 *
 * @param startX       World X coordinate of the start point
 * @param startY       World Y coordinate of the start point
 * @param startZ       World Z coordinate of the start point
 * @param endX         World X coordinate of the end point
 * @param endY         World Y coordinate of the end point
 * @param endZ         World Z coordinate of the end point
 * @param seeThrough   Whether the line should be visible through walls
 * @param color        The ARGB colour of the line
 * @param thickness    Cross-section width/height of the line in blocks (default 0.1)
 * @param style        How the line is rendered (SOLID or WIREFRAME)
 * @param ttlSeconds   Time-to-live in seconds (fractional allowed).
 *                     `null` means the line persists until the world changes.
 * @param createdAtTick Game tick when this line was created (set by [LineManager])
 */
data class Line(
    val startX: Double,
    val startY: Double,
    val startZ: Double,
    val endX: Double,
    val endY: Double,
    val endZ: Double,
    val seeThrough: Boolean,
    val color: ConfigColor,
    val thickness: Double = 0.1,
    val style: LineStyle = LineStyle.SOLID,
    val ttlSeconds: Double? = null,
    val createdAtTick: Long = 0L,
)