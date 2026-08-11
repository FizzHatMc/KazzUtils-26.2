package de.kazz.config.ui

import de.kazz.config.KazzConfig
import de.kazz.config.ui.theme.ConfigThemeManager
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

class ConfigScreen(
    parent: Screen? = null
) : Screen(Component.literal("KazzUtils Config")) {

    private val LOGGER = LoggerFactory.getLogger("kazzutils-config-ui")
    private var parentScreen: Screen? = parent
    private var renderCount = 0

    private lateinit var sidebar: ConfigSidebarWidget
    private lateinit var contentPanel: ConfigContentPanel

    init {
        LOGGER.info("ConfigScreen constructor called, parent=$parent")
    }

    override fun init() {
        LOGGER.info("ConfigScreen.init() called, width=$width, height=$height")
        super.init()

        val theme = ConfigThemeManager.getActive()
        val categories = KazzConfig.getCategories()
        LOGGER.info("Categories count: ${categories.size}")

        if (categories.isEmpty()) {
            LOGGER.warn("No categories found!")
            return
        }

        categories.forEach { LOGGER.info("  Category: ${it.name} (${it.subCategories.size} sub-categories)") }

        val sidebarWidth = theme.sidebarWidth
        sidebar = ConfigSidebarWidget(
            x = 0, y = 0, width = sidebarWidth, height = height,
            categories = categories, selectedIndex = 0,
            onCategorySelected = { index ->
                if (index in categories.indices) {
                    LOGGER.info("Category selected: ${categories[index].name}")
                    contentPanel.setCategory(categories[index])
                }
            }
        )
        addRenderableWidget(sidebar)
        LOGGER.info("Sidebar added")

        val contentX = sidebarWidth
        val contentWidth = width - sidebarWidth
        contentPanel = ConfigContentPanel(
            x = contentX, y = 0, width = contentWidth, height = height,
            screenHeight = height
        )
        addRenderableWidget(contentPanel)
        LOGGER.info("Content panel added")

        contentPanel.setCategory(categories[0])
        LOGGER.info("Initial category set: ${categories[0].name}")
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        renderCount++
        if (renderCount <= 3) {
            LOGGER.info("ConfigScreen.extractRenderState() called (render #$renderCount)")
        }
        try {
            val theme = ConfigThemeManager.getActive()
            graphics.fill(0, 0, width, height, theme.backgroundColor)
            super.extractRenderState(graphics, mouseX, mouseY, delta)
        } catch (e: Exception) {
            LOGGER.error("Error in extractRenderState", e)
        }
    }

    override fun onClose() {
        LOGGER.info("ConfigScreen.onClose() called")
        KazzConfig.save()
        val parent = parentScreen
        if (parent != null) {
            LOGGER.info("Returning to parent screen")
            minecraft?.gui?.setScreen(parent)
        }
    }

    override fun removed() {
        LOGGER.info("ConfigScreen.removed() called")
        LOGGER.info("Stack trace:", Exception("Stack trace for removed()"))
    }

    override fun tick() {
        LOGGER.info("ConfigScreen.tick() called")
    }

    override fun isPauseScreen(): Boolean = false
}