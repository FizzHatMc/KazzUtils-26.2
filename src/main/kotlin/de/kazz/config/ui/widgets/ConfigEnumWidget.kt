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

class ConfigEnumWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<*>,
    private val screenHeight: Int
) : AbstractWidget(x, y, width, height, Component.empty()) {

    private var isOpen = false; private var hoveredIndex = -1
    private val enumConstants: Array<out Enum<*>>

    init {
        require(property.type == ConfigType.ENUM)
        enumConstants = (property.enumClass ?: error("ENUM property must have enumClass set")).enumConstants
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive()) return false
        if (isOpen) {
            val dy = getDropdownY(); val ih = height
            for (i in enumConstants.indices) {
                val iy = dy + i * ih
                if (event.y().toInt() in iy until (iy + ih) && event.x().toInt() in getX() until (getX() + width)) {
                    @Suppress("UNCHECKED_CAST") (property as ConfigProperty<Enum<*>>).currentValue = enumConstants[i]
                    isOpen = false; playDownSound(Minecraft.getInstance().soundManager); return true
                }
            }
            isOpen = false; return true
        }
        if (isMouseOver(event.x(), event.y())) { isOpen = true; playDownSound(Minecraft.getInstance().soundManager); return true }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (isOpen) { val ry = (mouseY.toInt() - getDropdownY()) / height; hoveredIndex = if (ry in enumConstants.indices) ry else -1 }
        else hoveredIndex = -1
    }

    private fun getDropdownY(): Int {
        val dh = enumConstants.size * height
        return if (getY() + height + dh <= screenHeight) getY() + height else getY() - dh
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive(); val font = Minecraft.getInstance().font; val cv = property.currentValue as Enum<*>
        graphics.fill(getX(), getY(), getX() + width, getY() + height, theme.inputFieldBg)
        graphics.outline(getX(), getY(), width, height, theme.inputFieldBorder)
        graphics.text(font, cv.name, getX() + 4, getY() + (height - font.lineHeight) / 2, theme.propertyValueColor, true)
        graphics.text(font, if (isOpen) "▲" else "▼", getX() + width - 12, getY() + height / 2 - font.lineHeight / 2, theme.propertyValueColor, true)
        if (isOpen) {
            val dy = getDropdownY(); val ih = height
            for (i in enumConstants.indices) {
                val iy = dy + i * ih; val h = i == hoveredIndex
                graphics.fill(getX(), iy, getX() + width, iy + ih, if (h) theme.dropdownHoverBg else theme.dropdownBg)
                val s = enumConstants[i] == cv; val t = if (s) "> ${enumConstants[i].name} <" else enumConstants[i].name
                graphics.text(font, t, getX() + 4, iy + (ih - font.lineHeight) / 2, theme.dropdownTextColor, true)
            }
            graphics.outline(getX(), dy, width, enumConstants.size * ih, theme.inputFieldBorder)
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}