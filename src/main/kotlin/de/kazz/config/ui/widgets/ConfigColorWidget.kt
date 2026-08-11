package de.kazz.config.ui.widgets

import de.kazz.config.ConfigColor
import de.kazz.config.ConfigProperty
import de.kazz.config.ConfigType
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ConfigColorWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val property: ConfigProperty<ConfigColor>,
    private val screenHeight: Int
) : AbstractWidget(x, y, width, height, Component.empty()) {

    /** Whether the color preset picker is open. Public so the content panel can manage rendering order. */
    var isOpen = false

    private val presets = listOf(
        ConfigColor.WHITE, ConfigColor.BLACK, ConfigColor.RED, ConfigColor.GREEN, ConfigColor.BLUE, ConfigColor.TRANSPARENT,
        ConfigColor.of(255, 255, 165, 0), ConfigColor.of(255, 128, 0, 128), ConfigColor.of(255, 0, 255, 255),
        ConfigColor.of(255, 255, 255, 0), ConfigColor.of(255, 128, 128, 128), ConfigColor.of(255, 255, 192, 203)
    )
    private val perRow = 6; private val rows get() = (presets.size + perRow - 1) / perRow

    init { require(property.type == ConfigType.COLOR) }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive()) return false
        if (isOpen) {
            val theme = ConfigThemeManager.getActive(); val gsy = getY() + height + theme.smallPadding; val ss = theme.colorPresetSize; val g = 4
            for (i in presets.indices) {
                val c = i % perRow; val r = i / perRow; val sx = getX() + c * (ss + g); val sy = gsy + r * (ss + g)
                if (event.x().toInt() in sx until (sx + ss) && event.y().toInt() in sy until (sy + ss)) {
                    property.currentValue = presets[i]; isOpen = false; playDownSound(Minecraft.getInstance().soundManager); return true
                }
            }
            isOpen = false; return true
        }
        if (isMouseOver(event.x(), event.y())) { isOpen = true; playDownSound(Minecraft.getInstance().soundManager); return true }
        return false
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive(); val font = Minecraft.getInstance().font; val cc = property.currentValue
        val ss = height - 4; val sx = getX() + 2; val sy = getY() + 2
        graphics.fill(sx, sy, sx + ss, sy + ss, cc.argb); graphics.outline(sx, sy, ss, ss, theme.colorPresetBorder)
        graphics.text(font, cc.toHex(), sx + ss + 6, getY() + (height - font.lineHeight) / 2, theme.propertyValueColor, true)
        graphics.text(font, if (isOpen) "▲" else "▼", getX() + width - 12, getY() + (height - font.lineHeight) / 2, theme.propertyValueColor, true)
        if (isOpen) {
            val gsx = getX(); val gsy = getY() + height + theme.smallPadding; val ss2 = theme.colorPresetSize; val g = 4
            val gw = perRow * (ss2 + g) - g; val gh = rows * (ss2 + g) - g
            graphics.fill(gsx - 2, gsy - 2, gsx + gw + 2, gsy + gh + 2, theme.dropdownBg)
            graphics.outline(gsx - 2, gsy - 2, gw + 4, gh + 4, theme.inputFieldBorder)
            for (i in presets.indices) {
                val c = i % perRow; val r = i / perRow; val x2 = gsx + c * (ss2 + g); val y2 = gsy + r * (ss2 + g)
                graphics.fill(x2, y2, x2 + ss2, y2 + ss2, presets[i].argb)
                graphics.outline(x2, y2, ss2, ss2, if (presets[i] == cc) theme.colorPresetSelectedBorder else theme.colorPresetBorder)
            }
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}