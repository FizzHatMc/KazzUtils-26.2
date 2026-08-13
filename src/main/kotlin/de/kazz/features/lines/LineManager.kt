package de.kazz.features.lines

import de.kazz.config.ConfigColor
import net.minecraft.client.Minecraft
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Public API for managing in-world lines.
 *
 * Usage:
 * ```kotlin
 * // Add a line from (100, 64, 200) to (150, 70, 200) that is see-through, red, lasting 5 seconds
 * LineManager.addLine(
 *     startX = 100.0, startY = 64.0, startZ = 200.0,
 *     endX = 150.0, endY = 70.0, endZ = 200.0,
 *     seeThrough = true, color = ConfigColor.RED, ttlSeconds = 5.0,
 * )
 *
 * // Add a persistent line (until world change)
 * LineManager.addLine(
 *     startX = 100.0, startY = 64.0, startZ = 200.0,
 *     endX = 150.0, endY = 70.0, endZ = 200.0,
 *     seeThrough = false, color = ConfigColor.BLUE,
 * )
 * ```
 */
object LineManager {

    private val lines = CopyOnWriteArrayList<Line>()

    /**
     * Add a line to be rendered in-world.
     *
     * @param startX      World X coordinate of the start point
     * @param startY      World Y coordinate of the start point
     * @param startZ      World Z coordinate of the start point
     * @param endX        World X coordinate of the end point
     * @param endY        World Y coordinate of the end point
     * @param endZ        World Z coordinate of the end point
     * @param seeThrough  Whether the line is visible through walls
     * @param color       The colour of the line
     * @param thickness   Cross-section width/height of the line in blocks (default 0.1)
     * @param style       How the line is rendered (SOLID or WIREFRAME)
     * @param ttlSeconds  Time-to-live in seconds (fractional allowed).
     *                    `null` means persistent until world change.
     */
    fun addLine(
        startX: Double,
        startY: Double,
        startZ: Double,
        endX: Double,
        endY: Double,
        endZ: Double,
        seeThrough: Boolean,
        color: ConfigColor,
        thickness: Double = 0.1,
        style: LineStyle = LineStyle.SOLID,
        ttlSeconds: Double? = null,
    ) {
        val tick = Minecraft.getInstance().level?.gameTime ?: 0L
        lines.add(
            Line(
                startX = startX,
                startY = startY,
                startZ = startZ,
                endX = endX,
                endY = endY,
                endZ = endZ,
                seeThrough = seeThrough,
                color = color,
                thickness = thickness,
                style = style,
                ttlSeconds = ttlSeconds,
                createdAtTick = tick,
            )
        )
    }

    /**
     * Remove a specific line.
     */
    fun removeLine(line: Line): Boolean = lines.remove(line)

    /**
     * Remove all lines.
     */
    fun clearAll() {
        lines.clear()
    }

    /**
     * Remove all lines that have a TTL (non-persistent ones).
     * Called when the world changes.
     */
    fun clearTemporary() {
        lines.removeAll { it.ttlSeconds != null }
    }

    /**
     * Returns a snapshot of all active lines.
     * Called by [LineRenderer] during the extraction phase.
     */
    internal fun getActiveLines(): List<Line> {
        val level = Minecraft.getInstance().level ?: return emptyList()
        val currentTick = level.gameTime

        // Remove expired lines first
        lines.removeAll { line ->
            line.ttlSeconds?.let { ttl ->
                val elapsedTicks = currentTick - line.createdAtTick
                elapsedTicks >= (ttl * 20.0)
            } ?: false
        }

        return lines.toList()
    }
}