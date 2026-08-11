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

class ConfigActionButtonWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<*>
) : AbstractWidget(x, y, width, height, Component.empty()) {

    init { require(property.type == ConfigType.ACTION_BUTTON) }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive() || !isMouseOver(event.x(), event.y())) return false
        property.action?.invoke(); playDownSound(Minecraft.getInstance().soundManager); return true
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive(); val font = Minecraft.getInstance().font
        val h = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        graphics.fill(getX(), getY(), getX() + width, getY() + height, if (h) theme.actionButtonHoverBg else theme.actionButtonBg)
        graphics.outline(getX(), getY(), width, height, theme.borderColor)
        val t = property.name; val tw = font.width(t)
        graphics.text(font, t, getX() + (width - tw) / 2, getY() + (height - font.lineHeight) / 2, theme.actionButtonTextColor, true)
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}