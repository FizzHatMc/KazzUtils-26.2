package de.kazz.config.ui

import de.kazz.config.KazzConfig
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ConfigScreen(
    parent: Screen? = null
) : Screen(Component.literal("KazzUtils Config")) {

    private var parentScreen: Screen? = parent

    private lateinit var sidebar: ConfigSidebarWidget
    private lateinit var contentPanel: ConfigContentPanel

    override fun init() {
        super.init()

        val theme = ConfigThemeManager.getActive()
        val categories = KazzConfig.getCategories()
        if (categories.isEmpty()) return

        val sidebarWidth = theme.sidebarWidth
        sidebar = ConfigSidebarWidget(
            x = 0, y = 0, width = sidebarWidth, height = height,
            categories = categories, selectedIndex = 0,
            onCategorySelected = { index ->
                if (index in categories.indices) {
                    contentPanel.setCategory(categories[index])
                }
            }
        )
        addRenderableWidget(sidebar)

        val contentX = sidebarWidth
        val contentWidth = width - sidebarWidth
        contentPanel = ConfigContentPanel(
            x = contentX, y = 0, width = contentWidth, height = height,
            screenHeight = height
        )
        addRenderableWidget(contentPanel)

        contentPanel.setCategory(categories[0])
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val theme = ConfigThemeManager.getActive()
        graphics.fill(0, 0, width, height, theme.backgroundColor)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onClose() {
        KazzConfig.save()
        val parent = parentScreen
        if (parent != null) {
            minecraft?.gui?.setScreen(parent)
        }
    }

    override fun isPauseScreen(): Boolean = false
}