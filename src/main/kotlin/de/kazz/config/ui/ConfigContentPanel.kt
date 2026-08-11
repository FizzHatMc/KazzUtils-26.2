package de.kazz.config.ui

import de.kazz.config.*
import de.kazz.config.ui.theme.ConfigThemeManager
import de.kazz.config.ui.widgets.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ConfigContentPanel(
    x: Int, y: Int, width: Int, height: Int,
    private val screenHeight: Int
) : AbstractWidget(x, y, width, height, Component.empty()) {

    private var selectedCategory: ConfigCategory? = null
    private var scrollOffset = 0
    private var contentHeight = 0
    private val expandedGroups = mutableSetOf<String>()
    private val propertyWidgets = mutableListOf<AbstractWidget>()
    private val groupWidgets = mutableListOf<AbstractWidget>()
    private var hoveredProperty: ConfigProperty<*>? = null
    private var draggingWidget: AbstractWidget? = null
    private var openDropdownWidget: AbstractWidget? = null

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive() || !isMouseOver(event.x(), event.y())) return false

        // If a dropdown is open, let it handle the click first
        if (openDropdownWidget != null) {
            if (openDropdownWidget!!.mouseClicked(event, doubleClick)) return true
            openDropdownWidget = null; return true
        }

        for (w in propertyWidgets) {
            if (w.mouseClicked(event, doubleClick)) {
                if (w is ConfigColorWidget && w.isOpen) openDropdownWidget = w
                if (w is ConfigEnumWidget && w.isOpen) openDropdownWidget = w
                draggingWidget = w
                return true
            }
        }
        for (w in groupWidgets) { if (w.mouseClicked(event, doubleClick)) return true }
        return false
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingWidget?.mouseReleased(event)
        draggingWidget = null
        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        // Forward drag events to the widget that was clicked (if any)
        if (draggingWidget != null) {
            if (draggingWidget!!.mouseDragged(event, dx, dy)) return true
        }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        for (w in propertyWidgets) w.mouseMoved(mouseX, mouseY)
        for (w in groupWidgets) w.mouseMoved(mouseX, mouseY)
        updateHoveredProperty(mouseX.toInt(), mouseY.toInt())
    }

    private fun updateHoveredProperty(mx: Int, my: Int) {
        hoveredProperty = null
        val category = selectedCategory ?: return
        val theme = ConfigThemeManager.getActive()
        val cx = getX() + theme.padding
        val cw = width - theme.padding * 2 - theme.scrollbarWidth - theme.smallPadding
        var cy = getY() + theme.padding + scrollOffset

        for (sub in category.subCategories) {
            cy += theme.bannerHeight + theme.smallPadding
            for (prop in sub.directProperties) {
                if (mx in cx until (cx + cw) && my in cy until (cy + theme.propertyHeight)) {
                    hoveredProperty = prop; return
                }
                cy += theme.propertyHeight + theme.smallPadding
            }
            for (group in sub.groups) {
                cy += theme.groupHeaderHeight + theme.smallPadding
                if (expandedGroups.contains(group.key)) {
                    for (prop in group.properties) {
                        if (mx in (cx + theme.padding) until (cx + cw) && my in cy until (cy + theme.propertyHeight)) {
                            hoveredProperty = prop; return
                        }
                        cy += theme.propertyHeight + theme.smallPadding
                    }
                }
            }
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        val maxScroll = (contentHeight - height).coerceAtLeast(0)
        scrollOffset = (scrollOffset - (verticalAmount * 20).toInt()).coerceIn(-maxScroll, 0)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        for (w in propertyWidgets) { if (w.keyPressed(event)) return true }
        return false
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        for (w in propertyWidgets) { if (w.charTyped(event)) return true }; return false
    }

    fun setCategory(category: ConfigCategory) {
        selectedCategory = category; scrollOffset = 0; rebuildWidgets()
    }

    private fun rebuildWidgets() {
        propertyWidgets.clear(); groupWidgets.clear()
        openDropdownWidget = null; draggingWidget = null
        val category = selectedCategory ?: return
        val theme = ConfigThemeManager.getActive()
        val cx = getX() + theme.padding
        val cw = width - theme.padding * 2 - theme.scrollbarWidth - theme.smallPadding
        var cy = getY() + theme.padding

        val widgetWidth = (cw * 0.45).toInt().coerceIn(100, 250)

        for (sub in category.subCategories) {
            cy += theme.bannerHeight + theme.smallPadding
            for (prop in sub.directProperties) {
                val widgetX = cx + cw - widgetWidth
                createWidgetForProperty(prop, widgetX, cy, widgetWidth, theme.propertyHeight)?.let { propertyWidgets.add(it) }
                cy += theme.propertyHeight + theme.smallPadding
            }
            for (group in sub.groups) {
                val exp = expandedGroups.contains(group.key)
                groupWidgets.add(ConfigGroupWidget(cx, cy, cw, theme.groupHeaderHeight, group, exp, {
                    if (exp) expandedGroups.remove(group.key) else expandedGroups.add(group.key)
                    rebuildWidgets()
                }))
                cy += theme.groupHeaderHeight + theme.smallPadding
                if (exp) {
                    for (prop in group.properties) {
                        val widgetX = cx + theme.padding + cw - theme.padding - widgetWidth
                        createWidgetForProperty(prop, widgetX, cy, widgetWidth, theme.propertyHeight)?.let { propertyWidgets.add(it) }
                        cy += theme.propertyHeight + theme.smallPadding
                    }
                }
            }
        }
        contentHeight = cy - getY() + theme.padding
    }

    private fun createWidgetForProperty(prop: ConfigProperty<*>, x: Int, y: Int, w: Int, h: Int): AbstractWidget? {
        return when (prop.type) {
            ConfigType.BOOLEAN -> @Suppress("UNCHECKED_CAST") ConfigToggleWidget(x, y, w, h, prop as ConfigProperty<Boolean>)
            ConfigType.INT, ConfigType.DOUBLE -> ConfigSliderWidget(x, y, w, h, prop)
            ConfigType.STRING -> @Suppress("UNCHECKED_CAST") ConfigTextWidget(x, y, w, h, prop as ConfigProperty<String>)
            ConfigType.ENUM -> ConfigEnumWidget(x, y, w, h, prop, screenHeight)
            ConfigType.COLOR -> @Suppress("UNCHECKED_CAST") ConfigColorWidget(x, y, w, h, prop as ConfigProperty<ConfigColor>, screenHeight)
            ConfigType.ACTION_BUTTON -> ConfigActionButtonWidget(x, y, w, h, prop)
        }
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        try {
            val theme = ConfigThemeManager.getActive()
            val font = Minecraft.getInstance().font
            val category = selectedCategory ?: return

            graphics.fill(getX(), getY(), getX() + width, getY() + height, theme.contentBackground)
            graphics.enableScissor(getX(), getY(), getX() + width, getY() + height)

            val cx = getX() + theme.padding
            val cw = width - theme.padding * 2 - theme.scrollbarWidth - theme.smallPadding
            var cy = getY() + theme.padding + scrollOffset

            val widgetWidth = (cw * 0.45).toInt().coerceIn(100, 250)
            val labelWidth = cw - widgetWidth - theme.smallPadding

            // Render labels and banners
            for (sub in category.subCategories) {
                val by = cy
                graphics.fill(cx, by, cx + cw, by + theme.bannerHeight, theme.bannerBackground)
                graphics.text(font, sub.name, cx + theme.padding, by + (theme.bannerHeight - font.lineHeight) / 2, theme.bannerTextColor, true)
                cy += theme.bannerHeight + theme.smallPadding

                for (prop in sub.directProperties) {
                    val nameTruncated = if (font.width(prop.name) > labelWidth)
                        font.plainSubstrByWidth(prop.name, labelWidth - 3) + "..." else prop.name
                    graphics.text(font, nameTruncated, cx, cy + (theme.propertyHeight - font.lineHeight) / 2, theme.propertyLabelColor, true)
                    cy += theme.propertyHeight + theme.smallPadding
                }
                for (group in sub.groups) {
                    cy += theme.groupHeaderHeight + theme.smallPadding
                    if (expandedGroups.contains(group.key)) {
                        for (prop in group.properties) {
                            val nameTruncated = if (font.width(prop.name) > labelWidth - theme.padding)
                                font.plainSubstrByWidth(prop.name, labelWidth - theme.padding - 3) + "..." else prop.name
                            graphics.text(font, nameTruncated, cx + theme.padding, cy + (theme.propertyHeight - font.lineHeight) / 2, theme.propertyLabelColor, true)
                            cy += theme.propertyHeight + theme.smallPadding
                        }
                    }
                }
            }

            // Render child widgets inside scissor (NOT open dropdowns)
            for (w in propertyWidgets) {
                if (w !is ConfigColorWidget || !w.isOpen) {
                    if (w !is ConfigEnumWidget || !w.isOpen) {
                        w.extractRenderState(graphics, mouseX, mouseY, delta)
                    }
                }
            }
            for (w in groupWidgets) {
                w.extractRenderState(graphics, mouseX, mouseY, delta)
            }

            graphics.disableScissor()

            // Render open dropdowns on top (outside scissor)
            for (w in propertyWidgets) {
                if ((w is ConfigColorWidget && w.isOpen) || (w is ConfigEnumWidget && w.isOpen)) {
                    w.extractRenderState(graphics, mouseX, mouseY, delta)
                }
            }

            // Tooltip on hover
            val hovered = hoveredProperty
            if (hovered != null && hovered.description.isNotEmpty()) {
                val lines = listOf(hovered.name, hovered.description)
                val maxTw = lines.maxOfOrNull { font.width(it) } ?: 0
                val tw = maxTw + 12; val th = lines.size * (font.lineHeight + 2) + 6
                val tx = (mouseX + 12).coerceIn(0, getX() + width - tw - 4)
                val ty = (mouseY - th - 8).coerceAtLeast(0)
                graphics.fill(tx, ty, tx + tw, ty + th, 0xCC111122.toInt())
                graphics.outline(tx, ty, tw, th, theme.borderColor)
                var lcy = ty + 4
                for (line in lines) {
                    graphics.text(font, line, tx + 6, lcy, if (line == hovered.name) 0xFFFFAA00.toInt() else 0xFFCCCCCC.toInt(), true)
                    lcy += font.lineHeight + 2
                }
            }

            // Scrollbar
            val maxScroll = (contentHeight - height).coerceAtLeast(0)
            if (maxScroll > 0) {
                val sbx = getX() + width - theme.scrollbarWidth
                graphics.fill(sbx, getY(), sbx + theme.scrollbarWidth, getY() + height, theme.scrollbarTrack)
                val th = (height.toFloat() * (height.toFloat() / contentHeight.toFloat())).toInt().coerceAtLeast(20)
                val ty = getY() + ((-scrollOffset.toFloat() / maxScroll.toFloat()) * (height - th).toFloat()).toInt()
                graphics.fill(sbx, ty, sbx + theme.scrollbarWidth, ty + th, theme.scrollbarThumb)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
}