package de.kazz.features.waypoints

import de.kazz.config.ConfigColor
import net.minecraft.client.Minecraft
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Public API for managing in-world waypoints.
 *
 * Usage:
 * ```kotlin
 * // Add a waypoint at (100, 64, 200) that is see-through, red, lasting 5 seconds
 * WaypointManager.addWaypoint(100.0, 64.0, 200.0, seeThrough = true, ConfigColor.RED, ttlSeconds = 5.0)
 *
 * // Add a persistent waypoint (until world change)
 * WaypointManager.addWaypoint(100.0, 64.0, 200.0, seeThrough = false, ConfigColor.BLUE)
 * ```
 */
object WaypointManager {

    private val waypoints = CopyOnWriteArrayList<Waypoint>()

    /**
     * Add a waypoint to be rendered in-world.
     *
     * @param x           World X coordinate
     * @param y           World Y coordinate
     * @param z           World Z coordinate
     * @param seeThrough  Whether the waypoint is visible through walls
     * @param color       The colour of the waypoint
     * @param ttlSeconds  Time-to-live in seconds (fractional allowed).
     *                    `null` means persistent until world change.
     */
    fun addWaypoint(
        x: Double,
        y: Double,
        z: Double,
        seeThrough: Boolean,
        color: ConfigColor,
        ttlSeconds: Double? = null,
    ) {
        val tick = Minecraft.getInstance().level?.gameTime ?: 0L
        waypoints.add(
            Waypoint(
                x = x,
                y = y,
                z = z,
                seeThrough = seeThrough,
                color = color,
                ttlSeconds = ttlSeconds,
                createdAtTick = tick,
            )
        )
    }

    /**
     * Remove a specific waypoint.
     */
    fun removeWaypoint(waypoint: Waypoint): Boolean = waypoints.remove(waypoint)

    /**
     * Remove all waypoints.
     */
    fun clearAll() {
        waypoints.clear()
    }

    /**
     * Remove all waypoints that have a TTL (non-persistent ones).
     * Called when the world changes.
     */
    fun clearTemporary() {
        waypoints.removeAll { it.ttlSeconds != null }
    }

    /**
     * Returns a snapshot of all active waypoints.
     * Called by [WaypointRenderer] during the extraction phase.
     */
    internal fun getActiveWaypoints(): List<Waypoint> {
        val level = Minecraft.getInstance().level ?: return emptyList()
        val currentTick = level.gameTime

        // Remove expired waypoints first
        waypoints.removeAll { wp ->
            wp.ttlSeconds?.let { ttl ->
                val elapsedTicks = currentTick - wp.createdAtTick
                elapsedTicks >= (ttl * 20.0)
            } ?: false
        }

        return waypoints.toList()
    }
}