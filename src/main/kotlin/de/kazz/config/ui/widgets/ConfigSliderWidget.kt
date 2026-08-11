package de.kazz.config.ui.widgets

import de.kazz.config.ConfigProperty
import de.kazz.config.ConfigType
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ConfigSliderWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<*>
) : AbstractWidget(x, y, width, height, Component.empty()) {

    private val isDouble = property.type == ConfigType.DOUBLE
    private val min = property.min ?: 0.0
    private val max = property.max ?: 100.0
    private var isDragging = false

    init { require(property.type == ConfigType.INT || property.type == ConfigType.DOUBLE) }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive() || !isMouseOver(event.x(), event.y())) return false
        isDragging = true; updateValue(event.x())
        playDownSound(Minecraft.getInstance().soundManager); return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (isDragging) { isDragging = false; return true }; return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (isDragging) { updateValue(event.x()); return true }; return false
    }

    private fun updateValue(mouseX: Double) {
        val sw = width - 20
        val rx = (mouseX - getX() - 10).coerceIn(0.0, sw.toDouble())
        val f = rx / sw; val raw = min + f * (max - min)
        if (isDouble) @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Double>).currentValue = raw
        else @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Int>).currentValue = raw.toInt()
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive(); val font = Minecraft.getInstance().font
        val sy = getY() + height / 2 - 2; val sh = 4; val sl = getX() + 10; val sr = getX() + width - 10; val sw = sr - sl
        val cv = when (property.type) {
            ConfigType.INT -> (property.currentValue as Int).toDouble()
            ConfigType.DOUBLE -> property.currentValue as Double
            else -> min
        }
        val f = ((cv - min) / (max - min)).coerceIn(0.0, 1.0)
        graphics.fill(sl, sy, sr, sy + sh, theme.sliderTrack)
        val fe = sl + (sw * f).toInt(); if (fe > sl) graphics.fill(sl, sy, fe, sy + sh, theme.sliderThumb)
        val ts = 10; val tx = (sl + (sw * f).toInt()) - ts / 2; val ty = getY() + height / 2 - ts / 2
        graphics.fill(tx, ty, tx + ts, ty + ts, theme.sliderThumb)
        val vt = if (isDouble) String.format("%.1f", cv) else cv.toInt().toString()
        graphics.text(font, vt, sr + 8, getY() + (height - font.lineHeight) / 2, theme.propertyValueColor, true)
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}