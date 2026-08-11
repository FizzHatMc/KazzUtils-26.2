package de.kazz.config.ui.widgets

import de.kazz.config.ConfigProperty
import de.kazz.config.ConfigType
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class ConfigSliderWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<*>
) : AbstractWidget(x, y, width, height, Component.empty()) {

    private val isDouble = property.type == ConfigType.DOUBLE
    private val min = property.min ?: 0.0
    private val max = property.max ?: 100.0
    private var isDragging = false
    private var isEditing = false
    private val editBuffer = StringBuilder()

    init { require(property.type == ConfigType.INT || property.type == ConfigType.DOUBLE) }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive() || !isMouseOver(event.x(), event.y())) return false
        // Check if clicking on the value text area (right side) to start typing
        val font = Minecraft.getInstance().font
        val valueText = getValueText()
        val valueWidth = font.width(valueText) + 8
        val valueX = getX() + width - valueWidth - 4
        if (event.x() >= valueX && event.x() <= getX() + width) {
            isEditing = true
            editBuffer.clear()
            editBuffer.append(getValueText())
            return true
        }
        isDragging = true
        updateValue(event.x())
        playDownSound(Minecraft.getInstance().soundManager)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (isDragging) { isDragging = false; return true }
        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (isDragging) { updateValue(event.x()); return true }
        return false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!isEditing) return false
        when (event.key()) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commitEdit(); return true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                isEditing = false; return true
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (editBuffer.isNotEmpty()) editBuffer.deleteCharAt(editBuffer.length - 1); return true
            }
            GLFW.GLFW_KEY_DELETE -> {
                editBuffer.clear(); return true
            }
            else -> return false
        }
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (!isEditing) return false
        val cp = event.codepoint()
        // Allow digits, minus sign, and decimal point
        if (cp == '-'.code || cp == '.'.code || (cp >= '0'.code && cp <= '9'.code)) {
            // Don't allow multiple decimal points
            if (cp == '.'.code && editBuffer.contains('.')) return true
            // Don't allow minus except at start
            if (cp == '-'.code && editBuffer.isNotEmpty()) return true
            editBuffer.append(cp.toChar())
            return true
        }
        return false
    }

    private fun getValueText(): String {
        val cv = currentValue()
        return if (isDouble) String.format("%.1f", cv) else cv.toInt().toString()
    }

    private fun currentValue(): Double {
        return when (property.type) {
            ConfigType.INT -> (property.currentValue as Int).toDouble()
            ConfigType.DOUBLE -> property.currentValue as Double
            else -> min
        }
    }

    private fun commitEdit() {
        try {
            val v = editBuffer.toString().toDouble().coerceIn(min, max)
            if (isDouble) @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Double>).currentValue = v
            else @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Int>).currentValue = v.toInt()
        } catch (_: NumberFormatException) { }
        isEditing = false
    }

    private fun updateValue(mouseX: Double) {
        val sl = getX() + 10
        val sr = getX() + width - 10
        val sw = sr - sl
        val rx = (mouseX - sl).coerceIn(0.0, sw.toDouble())
        val f = rx / sw
        val raw = min + f * (max - min)
        if (isDouble) @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Double>).currentValue = raw
        else @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Int>).currentValue = raw.toInt()
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive()
        val font = Minecraft.getInstance().font
        val sy = getY() + height / 2 - 2
        val sh = 4
        val sl = getX() + 10
        val sr = getX() + width - 10
        val sw = sr - sl
        val cv = currentValue()
        val f = ((cv - min) / (max - min)).coerceIn(0.0, 1.0)

        // Slider track
        graphics.fill(sl, sy, sr, sy + sh, theme.sliderTrack)
        val fe = sl + (sw * f).toInt()
        if (fe > sl) graphics.fill(sl, sy, fe, sy + sh, theme.sliderThumb)

        // Slider thumb
        val ts = 10
        val tx = (sl + (sw * f).toInt()) - ts / 2
        val ty = getY() + height / 2 - ts / 2
        graphics.fill(tx, ty, tx + ts, ty + ts, theme.sliderThumb)

        // Value text (right side) - clickable for editing
        val displayText = if (isEditing) editBuffer.toString() + "█" else getValueText()
        val vt = displayText
        val vx = sr + 8
        val vy = getY() + (height - font.lineHeight) / 2
        graphics.text(font, vt, vx, vy, theme.propertyValueColor, true)

        // Draw a subtle underline when editing
        if (isEditing) {
            val tw = font.width(vt)
            graphics.horizontalLine(vx, vx + tw, vy + font.lineHeight + 1, theme.inputFieldFocusedBorder)
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}