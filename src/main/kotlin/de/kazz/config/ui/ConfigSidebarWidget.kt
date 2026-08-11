package de.kazz.config.ui

import de.kazz.config.ConfigCategory
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Left sidebar widget showing the list of config categories.
 *
 * Click a category to select it. The selected category is highlighted.
 */
class ConfigSidebarWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val categories: List<ConfigCategory>,
    private var selectedIndex: Int = 0,
    private val onCategorySelected: (Int) -> Unit
) : AbstractWidget(x, y, width, height, Component.empty()) {

    private val buttonHeight = 36

    /**
     * Update the selected index from outside (e.g., when the screen initializes).
     */
    fun setSelectedIndex(index: Int) {
        selectedIndex = index
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!isActive()) return false

        for (i in categories.indices) {
            val btnY = getY() + i * buttonHeight
            if (event.x().toInt() in getX() until (getX() + width) &&
                event.y().toInt() in btnY until (btnY + buttonHeight)) {
                if (selectedIndex != i) {
                    selectedIndex = i
                    onCategorySelected(i)
                    playDownSound(Minecraft.getInstance().soundManager)
                }
                return true
            }
        }
        return false
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive()
        val font = Minecraft.getInstance().font

        // Sidebar background
        graphics.fill(getX(), getY(), getX() + width, getY() + height, theme.sidebarBackground)

        // Draw a separator line on the right edge
        graphics.verticalLine(getX() + width - 1, getY(), getY() + height, theme.borderColor)

        // Draw category buttons
        for (i in categories.indices) {
            val category = categories[i]
            val btnY = getY() + i * buttonHeight
            val isSelected = i == selectedIndex
            val isHovered = mouseX in getX() until (getX() + width) &&
                    mouseY in btnY until (btnY + buttonHeight)

            // Button background
            val bgColor = when {
                isSelected -> theme.selectedCategoryBg
                isHovered -> theme.categoryHoverBg
                else -> 0 // transparent
            }
            if (bgColor != 0) {
                graphics.fill(getX(), btnY, getX() + width, btnY + buttonHeight, bgColor)
            }

            // Bottom separator
            graphics.horizontalLine(getX(), getX() + width, btnY + buttonHeight - 1, theme.borderColor)

            // Category name text
            val textX = getX() + theme.padding
            val textY = btnY + (buttonHeight - font.lineHeight) / 2
            val textColor = if (isSelected) 0xFFFFFFFF.toInt() else theme.categoryTextColor
            graphics.text(font, category.name, textX, textY, textColor, true)
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        // TODO: Add narration for accessibility
    }
}