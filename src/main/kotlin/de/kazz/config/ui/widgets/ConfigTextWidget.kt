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

class ConfigTextWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<String>
) : AbstractWidget(x, y, width, height, Component.empty()) {

    private var isFocused = false
    private val textBuffer = StringBuilder(property.currentValue)
    private var cursorPos = textBuffer.length

    init { require(property.type == ConfigType.STRING) }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive()) return false
        if (isMouseOver(event.x(), event.y())) {
            isFocused = true
            val font = Minecraft.getInstance().font
            val rx = (event.x() - (getX() + 4)).toInt().coerceAtLeast(0)
            val text = textBuffer.toString()
            cursorPos = font.plainSubstrByWidth(text, rx).length
            return true
        }
        isFocused = false; return false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!isFocused) return false
        when (event.key()) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                isFocused = false; property.currentValue = textBuffer.toString(); return true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                isFocused = false; textBuffer.clear(); textBuffer.append(property.currentValue)
                cursorPos = textBuffer.length; return true
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursorPos > 0) { textBuffer.deleteCharAt(cursorPos - 1); cursorPos-- }; return true
            }
            GLFW.GLFW_KEY_DELETE -> {
                if (cursorPos < textBuffer.length) textBuffer.deleteCharAt(cursorPos); return true
            }
            GLFW.GLFW_KEY_LEFT -> { if (cursorPos > 0) cursorPos--; return true }
            GLFW.GLFW_KEY_RIGHT -> { if (cursorPos < textBuffer.length) cursorPos++; return true }
            GLFW.GLFW_KEY_HOME -> { cursorPos = 0; return true }
            GLFW.GLFW_KEY_END -> { cursorPos = textBuffer.length; return true }
            else -> return false
        }
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (!isFocused) return false
        val cp = event.codepoint(); if (cp < 32 || cp > 126) return false
        textBuffer.insert(cursorPos, cp.toChar()); cursorPos++; return true
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive(); val font = Minecraft.getInstance().font
        graphics.fill(getX(), getY(), getX() + width, getY() + height, theme.inputFieldBg)
        graphics.outline(getX(), getY(), width, height, if (isFocused) theme.inputFieldFocusedBorder else theme.inputFieldBorder)
        val tx = getX() + 4; val ty = getY() + (height - font.lineHeight) / 2; val dt = textBuffer.toString()
        graphics.text(font, dt, tx, ty, theme.propertyValueColor, true)
        if (isFocused && (System.currentTimeMillis() / 500) % 2 == 0L) {
            val cx = tx + font.width(dt.substring(0, cursorPos.coerceAtMost(dt.length)))
            graphics.verticalLine(cx, ty, ty + font.lineHeight, 0xFFFFFFFF.toInt())
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}