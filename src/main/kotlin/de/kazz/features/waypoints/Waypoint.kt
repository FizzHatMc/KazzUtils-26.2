package de.kazz.features.waypoints

import de.kazz.config.ConfigColor

/**
 * A single waypoint to be rendered in-world.
 *
 * @param x            World X coordinate (center of the block)
 * @param y            World Y coordinate (center of the block)
 * @param z            World Z coordinate (center of the block)
 * @param seeThrough   Whether the waypoint should be visible through walls
 * @param color        The ARGB colour of the waypoint
 * @param ttlSeconds   Time-to-live in seconds (fractional allowed).
 *                     `null` means the waypoint persists until the world changes.
 * @param createdAtTick Game tick when this waypoint was created (set by [WaypointManager])
 */
data class Waypoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val seeThrough: Boolean,
    val color: ConfigColor,
    val ttlSeconds: Double? = null,
    val createdAtTick: Long = 0L,
)