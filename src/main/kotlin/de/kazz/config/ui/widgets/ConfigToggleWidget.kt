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

class ConfigToggleWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<Boolean>
) : AbstractWidget(x, y, width, height, Component.empty()) {

    init { require(property.type == ConfigType.BOOLEAN) }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive() || !isMouseOver(event.x(), event.y())) return false
        property.currentValue = !property.currentValue
        playDownSound(Minecraft.getInstance().soundManager)
        return true
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive()
        val isOn = property.currentValue
        val tw = 40; val th = 20
        val tx = getX() + width - tw; val ty = getY() + (height - th) / 2
        graphics.fill(tx, ty, tx + tw, ty + th, if (isOn) theme.toggleOnColor else theme.toggleOffColor)
        val ks = 16; val ky = ty + (th - ks) / 2
        val kx = if (isOn) tx + tw - ks - 2 else tx + 2
        graphics.fill(kx, ky, kx + ks, ky + ks, theme.toggleKnobColor)
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}