package de.kazz.features.borderbox

import de.kazz.config.ConfigColor
import net.minecraft.client.Minecraft
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Public API for managing in-world border boxes.
 *
 * Usage:
 * ```kotlin
 * // Add a 2×2×2 red wireframe box at (100, 64, 200) with 0.1-thick edges, see-through, lasting 5 seconds
 * BorderBoxManager.addBorderBox(
 *     centerX = 100.0, centerY = 64.0, centerZ = 200.0,
 *     seeThrough = true, color = ConfigColor.RED,
 *     thickness = 0.1, scale = 2.0, ttlSeconds = 5.0,
 * )
 *
 * // Add a persistent 1×1×1 blue box (until world change)
 * BorderBoxManager.addBorderBox(
 *     centerX = 100.0, centerY = 64.0, centerZ = 200.0,
 *     seeThrough = false, color = ConfigColor.BLUE,
 *     thickness = 0.05, scale = 1.0,
 * )
 * ```
 */
object BorderBoxManager {

    private val borderBoxes = CopyOnWriteArrayList<BorderBox>()

    /**
     * Add a border box to be rendered in-world.
     *
     * @param centerX     World X coordinate of the box center
     * @param centerY     World Y coordinate of the box center
     * @param centerZ     World Z coordinate of the box center
     * @param seeThrough  Whether the box is visible through walls
     * @param color       The colour of the wireframe edges
     * @param thickness   Cross-section width/height of each wireframe edge in blocks
     * @param scale       Uniform scale factor. A scale of 2.0 spans a 2×2×2 block area.
     * @param ttlSeconds  Time-to-live in seconds (fractional allowed).
     *                    `null` means persistent until world change.
     */
    fun addBorderBox(
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        seeThrough: Boolean,
        color: ConfigColor,
        thickness: Double,
        scale: Double,
        ttlSeconds: Double? = null,
    ) {
        val tick = Minecraft.getInstance().level?.gameTime ?: 0L
        borderBoxes.add(
            BorderBox(
                centerX = centerX,
                centerY = centerY,
                centerZ = centerZ,
                seeThrough = seeThrough,
                color = color,
                thickness = thickness,
                scale = scale,
                ttlSeconds = ttlSeconds,
                createdAtTick = tick,
            )
        )
    }

    /**
     * Remove a specific border box.
     */
    fun removeBorderBox(borderBox: BorderBox): Boolean = borderBoxes.remove(borderBox)

    /**
     * Remove all border boxes.
     */
    fun clearAll() {
        borderBoxes.clear()
    }

    /**
     * Remove all border boxes that have a TTL (non-persistent ones).
     * Called when the world changes.
     */
    fun clearTemporary() {
        borderBoxes.removeAll { it.ttlSeconds != null }
    }

    /**
     * Returns a snapshot of all active border boxes.
     * Called by [BorderBoxRenderer] during the extraction phase.
     */
    internal fun getActiveBorderBoxes(): List<BorderBox> {
        val level = Minecraft.getInstance().level ?: return emptyList()
        val currentTick = level.gameTime

        // Remove expired boxes first
        borderBoxes.removeAll { box ->
            box.ttlSeconds?.let { ttl ->
                val elapsedTicks = currentTick - box.createdAtTick
                elapsedTicks >= (ttl * 20.0)
            } ?: false
        }

        return borderBoxes.toList()
    }
}