package de.kazz.config.ui.widgets

import de.kazz.config.ConfigGroup
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ConfigGroupWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val group: ConfigGroup,
    private val isExpanded: Boolean,
    private val onToggle: () -> Unit
) : AbstractWidget(x, y, width, height, Component.empty()) {

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive() || !isMouseOver(event.x(), event.y())) return false
        onToggle(); playDownSound(Minecraft.getInstance().soundManager); return true
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive(); val font = Minecraft.getInstance().font
        val h = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        graphics.fill(getX(), getY(), getX() + width, getY() + height, if (isExpanded) theme.groupExpandedBg else theme.groupHeaderBg)
        graphics.horizontalLine(getX(), getX() + width, getY() + height - 1, theme.borderColor)
        val ch = if (isExpanded) "▼" else "▶"
        graphics.text(font, ch, getX() + theme.smallPadding, getY() + (height - font.lineHeight) / 2, theme.groupHeaderTextColor, true)
        graphics.text(font, group.name, getX() + theme.smallPadding + font.width(ch) + theme.smallPadding, getY() + (height - font.lineHeight) / 2, theme.groupHeaderTextColor, true)
        if (h && group.description.isNotEmpty()) {
            val tt = group.description; val tw = font.width(tt) + 8; val th = font.lineHeight + 6
            val tx = (mouseX + 8).coerceAtMost(width - tw - 8); val ty = (mouseY - th - 4).coerceAtLeast(0)
            graphics.fill(tx, ty, tx + tw, ty + th, 0xCC000000.toInt())
            graphics.outline(tx, ty, tw, th, theme.borderColor)
            graphics.text(font, tt, tx + 4, ty + 3, 0xFFFFFFFF.toInt(), true)
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}