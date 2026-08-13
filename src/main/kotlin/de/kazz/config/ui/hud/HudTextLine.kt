package de.kazz.config.ui.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.ARGB
import de.kazz.config.ConfigColor

/**
 * A single colored fragment of text within a [HudTextLine].
 * Multiple segments form one line displayed on screen.
 */
data class HudTextSegment(
    val text: String,
    val color: ConfigColor = ConfigColor.WHITE,
    val shadow: Boolean = true,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: Float = -1f
)

/**
 * Alignment options for a [HudTextLine].
 */
enum class HudTextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * A single line of HUD text composed of one or more colored [HudTextSegment]s.
 * The line is drawn onto the screen using the provided [GuiGraphicsExtractor].
 */
class HudTextLine(
    val segments: List<HudTextSegment>,
    val alignment: HudTextAlignment = HudTextAlignment.LEFT
) {
    /**
     * Draws this text line at the given position on the screen.
     *
     * @param graphics the GUI graphics instance to draw with
     * @param baseX the X position to start drawing at
     * @param baseY the Y position to start drawing at
     * @param scale the current scale factor of the parent element
     */
    fun draw(graphics: GuiGraphicsExtractor, baseX: Int, baseY: Int, scale: Float) {
        if (segments.isEmpty()) return

        val font = Minecraft.getInstance().font

        val totalWidth = segments.sumOf { segment ->
            font.width(segment.text)
        }

        val startX = when (alignment) {
            HudTextAlignment.LEFT -> baseX
            HudTextAlignment.CENTER -> baseX - (totalWidth / 2)
            HudTextAlignment.RIGHT -> baseX - totalWidth
        }

        var cursorX = startX

        for (segment in segments) {
            val argbColor = ARGB.opaque(segment.color.argb)

            if (segment.shadow) {
                graphics.text(font, segment.text, cursorX, baseY, argbColor, true)
            } else {
                graphics.text(font, segment.text, cursorX, baseY, argbColor, false)
            }

            cursorX += font.width(segment.text)
        }
    }

    /**
     * Returns the width of this text line in pixels.
     */
    fun width(): Int {
        if (segments.isEmpty()) return 0
        val font = Minecraft.getInstance().font
        return segments.sumOf { font.width(it.text) }
    }

    companion object {
        /**
         * Creates a single-segment [HudTextLine] with the given text and color.
         */
        fun simple(text: String, color: ConfigColor = ConfigColor.WHITE): HudTextLine {
            return HudTextLine(listOf(HudTextSegment(text, color)))
        }

        /**
         * Creates a [HudTextLine] from a builder pattern where each argument is a pair of (text, color).
         */
        fun build(vararg segments: Pair<String, ConfigColor>): HudTextLine {
            return HudTextLine(segments.map { (text, color) ->
                HudTextSegment(text, color)
            })
        }
    }
}