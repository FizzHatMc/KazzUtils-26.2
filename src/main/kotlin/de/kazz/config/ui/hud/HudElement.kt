package de.kazz.config.ui.hud

import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Rectangle

/**
 * Abstract base class for all HUD elements.
 *
 * Each element has a position ([x], [y]), scale ([scale], [scaleX], [scaleY]),
 * and can display text lines or custom rendered content.
 *
 * To create a text-based HUD element, override [renderContent].
 * For non-text elements (health bars, sprites, etc.), override [renderCustom].
 */
abstract class HudElement(
    val id: String,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var enabled: Boolean = true,
    var layer: HudLayer = HudLayer.DEFAULT
) {
    /**
     * Defines the text content to display.
     * Override this for simple text-based HUD elements.
     * The default implementation returns an empty list.
     */
    open fun renderContent(): List<HudTextLine> = emptyList()

    /**
     * Custom rendering logic for non-text elements.
     * Called before text lines are drawn.
     * The [graphics] is already translated and scaled to this element's position.
     * Override this for sprite-based, bar-based, or any custom HUD elements.
     */
    open fun renderCustom(graphics: GuiGraphicsExtractor) { /* no-op by default */ }

    /**
     * Renders this HUD element onto the screen.
     * This method handles positioning, scaling, and calling [renderCustom] and [renderContent].
     *
     * @param graphics the GUI graphics instance
     * @param tickCounter the delta tracker for animations
     */
    fun render(graphics: GuiGraphicsExtractor, tickCounter: DeltaTracker) {
        if (!enabled) return

        val matrices = graphics.pose()
        matrices.pushMatrix()
        matrices.translate(x, y)
        matrices.scale(scale * scaleX, scale * scaleY)

        // Draw custom content first (if any)
        renderCustom(graphics)

        // Draw text lines
        val font = Minecraft.getInstance().font
        val lineHeight = font.lineHeight + 2 // 2px spacing between lines

        renderContent().forEachIndexed { index, line ->
            line.draw(graphics, 0, index * lineHeight.toInt(), scale * scaleY)
        }

        matrices.popMatrix()
    }

    /**
     * Returns the bounding rectangle of this HUD element in screen coordinates.
     * Used for hit detection (e.g., drag UI) and bounds checking.
     */
    open fun getBounds(): Rectangle {
        val lines = renderContent()
        if (lines.isEmpty()) return Rectangle(x.toInt(), y.toInt(), 0, 0)

        val font = Minecraft.getInstance().font
        val maxWidth = lines.maxOf { it.width() }
        val totalHeight = lines.size * (font.lineHeight + 2)

        return Rectangle(
            (x * scale * scaleX).toInt(),
            (y * scale * scaleY).toInt(),
            (maxWidth * scale * scaleX).toInt(),
            (totalHeight * scale * scaleY).toInt()
        )
    }
}

/**
 * Z-ordering layers for HUD elements.
 * Elements in higher layers render on top of elements in lower layers.
 */
enum class HudLayer(val zOffset: Int) {
    BACKGROUND(-100),
    LOW(-50),
    DEFAULT(0),
    HIGH(50),
    OVERLAY(100)
}