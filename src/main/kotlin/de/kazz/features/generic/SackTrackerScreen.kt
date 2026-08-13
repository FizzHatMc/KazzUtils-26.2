package de.kazz.features.generic

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

/**
 * Custom UI for managing sack tracker item filters.
 *
 * Layout:
 * ┌─────────────────────────────────────────────┐
 * │  Sack Tracker                    [Close] X  │
 * ├──────────────────┬──────────────────────────┤
 * │  Tracked (2)     │  Trackable    [Search…]  │
 * │                  │                          │
 * │  • Enchanted     │  • Cobblestone           │
 * │    Cobblestone   │  • Wheat                 │
 * │  • Enchanted     │  • Carrot                │
 * │    Iron          │  • Potato                │
 * │                  │  • …                     │
 * │                  │                          │
 * ├──────────────────┴──────────────────────────┤
 * │  [Save & Close]                             │
 * └─────────────────────────────────────────────┘
 *
 * Clicking an item on the right moves it to the left (tracked).
 * Clicking an item on the left moves it to the right (untracked).
 */
class SackTrackerScreen(
    parent: Screen? = null
) : Screen(Component.literal("Sack Tracker")) {

    private val LOGGER = LoggerFactory.getLogger("sack-tracker-screen")
    private var parentScreen: Screen? = parent

    // ── Layout constants ──────────────────────────────────────

    companion object {
        private const val SCREEN_WIDTH = 420
        private const val SCREEN_HEIGHT = 320
        private const val PANEL_MARGIN = 8
        private const val PANEL_HEADER_HEIGHT = 20
        private const val SEARCH_BAR_HEIGHT = 20
        private const val ITEM_HEIGHT = 14
        private const val ITEM_PADDING = 2
        private const val BUTTON_HEIGHT = 20
        private const val SCROLLBAR_WIDTH = 6
        private const val CLOSE_BUTTON_SIZE = 14
    }

    // ── State ─────────────────────────────────────────────────

    /** All items from the database, keyed by name. */
    private val allItems: List<SackItemEntry> = SackItemDatabase.getAllItems().sortedBy { it.name }

    /** Currently displayed items in the right panel (filtered by search). */
    private val filteredItems: MutableList<SackItemEntry> = allItems.toMutableList()

    /** Scroll offset for the left panel (tracked items). */
    private var leftScrollOffset = 0

    /** Scroll offset for the right panel (trackable items). */
    private var rightScrollOffset = 0

    /** Search box for filtering the right panel. */
    private var searchBox: EditBox? = null

    /** Current search query. */
    private var searchQuery = ""

    /** Whether the save button is hovered. */
    private var saveButtonHovered = false

    /** Whether the close button is hovered. */
    private var closeButtonHovered = false

    // ── Screen lifecycle ──────────────────────────────────────

    override fun init() {
        super.init()
        // Create search box
        val searchX = getRightPanelX() + PANEL_MARGIN
        val searchY = getTopY() + PANEL_HEADER_HEIGHT + PANEL_MARGIN
        val searchWidth = getRightPanelWidth() - PANEL_MARGIN * 2 - SCROLLBAR_WIDTH

        searchBox = EditBox(
            minecraft.font,
            searchX, searchY,
            searchWidth, SEARCH_BAR_HEIGHT,
            Component.literal("Search items...")
        ).apply {
            setMaxLength(64)
            setHint(Component.literal("Search items..."))
            setResponder { query ->
                searchQuery = query
                updateFilteredItems()
            }
        }
        addRenderableWidget(searchBox!!)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(graphics)
        renderBorder(graphics)
        renderTitle(graphics)
        renderCloseButton(graphics, mouseX, mouseY)
        renderLeftPanel(graphics, mouseX, mouseY)
        renderRightPanel(graphics, mouseX, mouseY)
        renderSaveButton(graphics, mouseX, mouseY)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick)

        val mx = event.x().toInt()
        val my = event.y().toInt()

        // Close button
        if (isInsideCloseButton(mx, my)) {
            onClose()
            return true
        }

        // Save button
        if (isInsideSaveButton(mx, my)) {
            saveAndClose()
            return true
        }

        // Left panel items (tracked)
        val leftClickedItem = getItemAtPosition(mx, my, isLeftPanel = true)
        if (leftClickedItem != null) {
            SackTrackerData.setTracked(leftClickedItem.name, false)
            LOGGER.info("Untracked: ${leftClickedItem.name}")
            return true
        }

        // Right panel items (trackable)
        val rightClickedItem = getItemAtPosition(mx, my, isLeftPanel = false)
        if (rightClickedItem != null) {
            SackTrackerData.setTracked(rightClickedItem.name, true)
            LOGGER.info("Tracked: ${rightClickedItem.name}")
            return true
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = x.toInt()
        val my = y.toInt()
        val deltaY = scrollY

        // Left panel scroll
        if (isInsideLeftPanel(mx, my)) {
            val maxScroll = getMaxLeftScroll()
            leftScrollOffset = (leftScrollOffset - deltaY.toInt()).coerceIn(0, maxScroll)
            return true
        }

        // Right panel scroll
        if (isInsideRightPanel(mx, my)) {
            val maxScroll = getMaxRightScroll()
            rightScrollOffset = (rightScrollOffset - deltaY.toInt()).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        SackTrackerData.save()
        val parent = parentScreen
        if (parent != null) {
            minecraft.gui.setScreen(parent)
        } else {
            minecraft.gui.setScreen(null)
        }
    }

    override fun isPauseScreen(): Boolean = false

    // ── Layout helpers ────────────────────────────────────────

    private fun getScreenLeft(): Int = (width - SCREEN_WIDTH) / 2
    private fun getScreenTop(): Int = (height - SCREEN_HEIGHT) / 2
    private fun getScreenRight(): Int = getScreenLeft() + SCREEN_WIDTH
    private fun getScreenBottom(): Int = getScreenTop() + SCREEN_HEIGHT

    private fun getTopY(): Int = getScreenTop() + PANEL_MARGIN + 10 // +10 for title
    private fun getBottomY(): Int = getScreenBottom() - PANEL_MARGIN - BUTTON_HEIGHT - PANEL_MARGIN

    private fun getLeftPanelX(): Int = getScreenLeft() + PANEL_MARGIN
    private fun getLeftPanelWidth(): Int = (SCREEN_WIDTH - PANEL_MARGIN * 3) / 2
    private fun getLeftPanelRight(): Int = getLeftPanelX() + getLeftPanelWidth()

    private fun getRightPanelX(): Int = getLeftPanelRight() + PANEL_MARGIN
    private fun getRightPanelWidth(): Int = getLeftPanelWidth()

    private fun getListStartY(): Int = getTopY() + PANEL_HEADER_HEIGHT + PANEL_MARGIN + SEARCH_BAR_HEIGHT + PANEL_MARGIN
    private fun getListEndY(): Int = getBottomY() - PANEL_MARGIN
    private fun getListHeight(): Int = getListEndY() - getListStartY()
    private fun getVisibleItemCount(): Int = getListHeight() / (ITEM_HEIGHT + ITEM_PADDING)

    private fun isInsideLeftPanel(x: Int, y: Int): Boolean {
        return x in getLeftPanelX()..(getLeftPanelX() + getLeftPanelWidth()) &&
                y in getListStartY()..getListEndY()
    }

    private fun isInsideRightPanel(x: Int, y: Int): Boolean {
        return x in getRightPanelX()..(getRightPanelX() + getRightPanelWidth()) &&
                y in getListStartY()..getListEndY()
    }

    private fun isInsideCloseButton(x: Int, y: Int): Boolean {
        val cx = getScreenRight() - PANEL_MARGIN - CLOSE_BUTTON_SIZE
        val cy = getScreenTop() + PANEL_MARGIN
        return x in cx..(cx + CLOSE_BUTTON_SIZE) && y in cy..(cy + CLOSE_BUTTON_SIZE)
    }

    private fun isInsideSaveButton(x: Int, y: Int): Boolean {
        val bx = (width - 100) / 2
        val by = getScreenBottom() - PANEL_MARGIN - BUTTON_HEIGHT
        return x in bx..(bx + 100) && y in by..(by + BUTTON_HEIGHT)
    }

    // ── Rendering ─────────────────────────────────────────────

    private fun renderBackground(graphics: GuiGraphicsExtractor) {
        // Dim the background
        graphics.fill(0, 0, width, height, 0x80000000.toInt())
        // Main panel background
        graphics.fill(getScreenLeft(), getScreenTop(), getScreenRight(), getScreenBottom(), 0xFF1A1A2E.toInt())
        // Border
        graphics.fill(getScreenLeft(), getScreenTop(), getScreenRight(), getScreenTop() + 1, 0xFF4A4A6E.toInt())
        graphics.fill(getScreenLeft(), getScreenBottom() - 1, getScreenRight(), getScreenBottom(), 0xFF4A4A6E.toInt())
        graphics.fill(getScreenLeft(), getScreenTop(), getScreenLeft() + 1, getScreenBottom(), 0xFF4A4A6E.toInt())
        graphics.fill(getScreenRight() - 1, getScreenTop(), getScreenRight(), getScreenBottom(), 0xFF4A4A6E.toInt())
    }

    private fun renderBorder(graphics: GuiGraphicsExtractor) {
        // Divider between left and right panels
        val dividerX = getLeftPanelRight()
        graphics.fill(dividerX, getTopY(), dividerX + 1, getBottomY(), 0xFF4A4A6E.toInt())
    }

    private fun renderTitle(graphics: GuiGraphicsExtractor) {
        val font = minecraft.font
        val title = "Sack Tracker"
        val titleX = getScreenLeft() + PANEL_MARGIN
        val titleY = getScreenTop() + PANEL_MARGIN
        graphics.text(font, title, titleX, titleY, 0xFFFFFFFF.toInt(), true)
    }

    private fun renderCloseButton(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        closeButtonHovered = isInsideCloseButton(mouseX, mouseY)
        val cx = getScreenRight() - PANEL_MARGIN - CLOSE_BUTTON_SIZE
        val cy = getScreenTop() + PANEL_MARGIN
        val color = if (closeButtonHovered) 0xFFFF5555.toInt() else 0xFF666688.toInt()
        graphics.fill(cx, cy, cx + CLOSE_BUTTON_SIZE, cy + CLOSE_BUTTON_SIZE, color)
        val font = minecraft.font
        graphics.text(font, "X", cx + 3, cy + 2, 0xFFFFFFFF.toInt(), true)
    }

    private fun renderLeftPanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val font = minecraft.font
        val trackedItems = SackTrackerData.getTrackedItems()
        val x = getLeftPanelX() + PANEL_MARGIN
        val y = getTopY() + PANEL_MARGIN

        // Header
        graphics.text(font, "Tracked (${trackedItems.size})", x, y, 0xFF88FF88.toInt(), true)

        // Items
        val listY = getListStartY()
        val visibleCount = getVisibleItemCount()
        val sortedTracked = trackedItems.entries.sortedBy { it.key }

        // Clamp scroll
        val maxScroll = (sortedTracked.size - visibleCount).coerceAtLeast(0)
        leftScrollOffset = leftScrollOffset.coerceIn(0, maxScroll)

        for (i in leftScrollOffset until (leftScrollOffset + visibleCount).coerceAtMost(sortedTracked.size)) {
            val (name, data) = sortedTracked[i]
            val itemY = listY + (i - leftScrollOffset) * (ITEM_HEIGHT + ITEM_PADDING)
            val itemColor = if (mouseY in itemY..(itemY + ITEM_HEIGHT) &&
                mouseX in x..(getLeftPanelX() + getLeftPanelWidth() - PANEL_MARGIN - SCROLLBAR_WIDTH)
            ) {
                0xFF444466.toInt() // hover highlight
            } else {
                0xFF2A2A3E.toInt()
            }
            graphics.fill(x, itemY, getLeftPanelX() + getLeftPanelWidth() - PANEL_MARGIN - SCROLLBAR_WIDTH, itemY + ITEM_HEIGHT, itemColor)
            val displayText = if (data.amount > 0) "$name §7(${data.amount})" else name
            graphics.text(font, Component.literal(displayText), x + 2, itemY + 1, 0xFFFFFFFF.toInt(), true)
        }

        // Scrollbar
        if (sortedTracked.size > visibleCount) {
            renderScrollbar(graphics, x, getLeftPanelX() + getLeftPanelWidth() - PANEL_MARGIN - SCROLLBAR_WIDTH,
                getListStartY(), getListEndY(), leftScrollOffset, sortedTracked.size, visibleCount)
        }
    }

    private fun renderRightPanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val font = minecraft.font
        val x = getRightPanelX() + PANEL_MARGIN
        val y = getTopY() + PANEL_MARGIN

        // Header
        graphics.text(font, "Trackable", x, y, 0xFFFFAA44.toInt(), true)

        // Items
        val listY = getListStartY()
        val visibleCount = getVisibleItemCount()

        // Filter out already-tracked items from the right panel
        val trackedNames = SackTrackerData.getTrackedNames()
        val availableItems = filteredItems.filter { it.name !in trackedNames }

        // Clamp scroll
        val maxScroll = (availableItems.size - visibleCount).coerceAtLeast(0)
        rightScrollOffset = rightScrollOffset.coerceIn(0, maxScroll)

        for (i in rightScrollOffset until (rightScrollOffset + visibleCount).coerceAtMost(availableItems.size)) {
            val item = availableItems[i]
            val itemY = listY + (i - rightScrollOffset) * (ITEM_HEIGHT + ITEM_PADDING)
            val itemColor = if (mouseY in itemY..(itemY + ITEM_HEIGHT) &&
                mouseX in x..(getRightPanelX() + getRightPanelWidth() - PANEL_MARGIN - SCROLLBAR_WIDTH)
            ) {
                0xFF444466.toInt() // hover highlight
            } else {
                0xFF2A2A3E.toInt()
            }
            graphics.fill(x, itemY, getRightPanelX() + getRightPanelWidth() - PANEL_MARGIN - SCROLLBAR_WIDTH, itemY + ITEM_HEIGHT, itemColor)
            graphics.text(font, item.name, x + 2, itemY + 1, 0xFFFFFFFF.toInt(), true)
        }

        // Scrollbar
        if (availableItems.size > visibleCount) {
            renderScrollbar(graphics, x, getRightPanelX() + getRightPanelWidth() - PANEL_MARGIN - SCROLLBAR_WIDTH,
                getListStartY(), getListEndY(), rightScrollOffset, availableItems.size, visibleCount)
        }
    }

    private fun renderScrollbar(
        graphics: GuiGraphicsExtractor,
        panelX: Int, scrollbarX: Int,
        topY: Int, bottomY: Int,
        scrollOffset: Int, totalItems: Int, visibleItems: Int
    ) {
        val scrollbarHeight = bottomY - topY
        val thumbHeight = (scrollbarHeight * visibleItems / totalItems).coerceAtLeast(10)
        val maxScroll = totalItems - visibleItems
        val thumbY = if (maxScroll > 0) {
            topY + (scrollbarHeight - thumbHeight) * scrollOffset / maxScroll
        } else {
            topY
        }
        graphics.fill(scrollbarX, topY, scrollbarX + SCROLLBAR_WIDTH, bottomY, 0xFF333355.toInt())
        graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF6666AA.toInt())
    }

    private fun renderSaveButton(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        saveButtonHovered = isInsideSaveButton(mouseX, mouseY)
        val bx = (width - 100) / 2
        val by = getScreenBottom() - PANEL_MARGIN - BUTTON_HEIGHT
        val color = if (saveButtonHovered) 0xFF44AA44.toInt() else 0xFF338833.toInt()
        graphics.fill(bx, by, bx + 100, by + BUTTON_HEIGHT, color)
        val font = minecraft.font
        val text = "Save && Close"
        val textWidth = font.width(text)
        graphics.text(font, text, bx + (100 - textWidth) / 2, by + (BUTTON_HEIGHT - 8) / 2, 0xFFFFFFFF.toInt(), true)
    }

    // ── Hit detection ─────────────────────────────────────────

    /**
     * Returns the item at the given mouse position in the specified panel, or null.
     */
    private fun getItemAtPosition(mx: Int, my: Int, isLeftPanel: Boolean): SackItemEntry? {
        if (isLeftPanel && !isInsideLeftPanel(mx, my)) return null
        if (!isLeftPanel && !isInsideRightPanel(mx, my)) return null

        val listY = getListStartY()
        val visibleCount = getVisibleItemCount()
        val offset = if (isLeftPanel) leftScrollOffset else rightScrollOffset

        val items: List<SackItemEntry> = if (isLeftPanel) {
            SackTrackerData.getTrackedItems().keys.sorted().mapNotNull { name ->
                SackItemDatabase.getItem(name)
            }
        } else {
            val trackedNames = SackTrackerData.getTrackedNames()
            filteredItems.filter { it.name !in trackedNames }
        }

        val relativeY = my - listY
        val itemIndex = relativeY / (ITEM_HEIGHT + ITEM_PADDING) + offset

        if (itemIndex in items.indices) {
            val itemY = listY + (itemIndex - offset) * (ITEM_HEIGHT + ITEM_PADDING)
            if (my in itemY..(itemY + ITEM_HEIGHT)) {
                return items[itemIndex]
            }
        }

        return null
    }

    private fun getMaxLeftScroll(): Int {
        val trackedCount = SackTrackerData.getTrackedItems().size
        return (trackedCount - getVisibleItemCount()).coerceAtLeast(0)
    }

    private fun getMaxRightScroll(): Int {
        val trackedNames = SackTrackerData.getTrackedNames()
        val availableCount = filteredItems.count { it.name !in trackedNames }
        return (availableCount - getVisibleItemCount()).coerceAtLeast(0)
    }

    // ── Search ────────────────────────────────────────────────

    private fun updateFilteredItems() {
        filteredItems.clear()
        filteredItems.addAll(SackItemDatabase.searchItems(searchQuery))
        rightScrollOffset = 0
    }

    // ── Actions ───────────────────────────────────────────────

    private fun saveAndClose() {
        SackTrackerData.forceSave()
        onClose()
    }
}