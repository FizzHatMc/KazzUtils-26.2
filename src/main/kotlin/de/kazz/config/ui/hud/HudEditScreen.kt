package de.kazz.config.ui.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Rectangle

/**
 * Edit screen for repositioning and resizing HUD elements.
 *
 * Shows the game world behind a dark transparent overlay, with all enabled
 * HUD elements highlighted by bounding boxes. The user can:
 * - Click and drag any element to move it
 * - Click and drag the bottom-right corner handle to resize uniformly
 * - Press ESC to save all changes and close
 */
class HudEditScreen(
    parent: Screen? = null
) : Screen(Component.literal("Edit HUD Layout")) {

    private var parentScreen: Screen? = parent

    // Drag state
    private var draggingElement: HudElement? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    // Resize state
    private var resizingElement: HudElement? = null
    private var initialScale = 1f
    private var resizeStartMouseX = 0f
    private var resizeStartMouseY = 0f

    companion object {
        private const val HANDLE_SIZE = 8
        private const val BORDER_COLOR = 0xFFFF4444.toInt()
        private const val HANDLE_COLOR = 0xFFFFAA00.toInt()
        private const val OVERLAY_COLOR = 0x80000000.toInt()
        private const val MIN_SCALE = 0.1f
        private const val MAX_SCALE = 5.0f
        private const val DEFAULT_BOX_SIZE = 20
    }

    override fun init() {
        super.init()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // Draw dark transparent overlay so the user sees the game world is still there
        graphics.fill(0, 0, width, height, OVERLAY_COLOR)

        // Render all enabled elements at their current positions
        for (element in HudManager.getAll()) {
            if (!element.enabled) continue

            // Render the element content at its position
            val matrices = graphics.pose()
            matrices.pushMatrix()
            matrices.translate(element.x, element.y)
            matrices.scale(element.scale * element.scaleX, element.scale * element.scaleY)

            element.renderCustom(graphics)

            val font = Minecraft.getInstance().font
            val lineHeight = font.lineHeight + 2
            element.renderContent().forEachIndexed { index, line ->
                line.draw(graphics, 0, index * lineHeight, element.scale * element.scaleY)
            }

            matrices.popMatrix()

            // Compute and draw bounding box
            val bounds = getElementScreenBounds(element)
            if (bounds != null) {
                // Draw bounding box outline
                graphics.outline(bounds.x, bounds.y, bounds.width, bounds.height, BORDER_COLOR)

                // Draw resize handle at bottom-right corner
                val handleX = bounds.x + bounds.width - HANDLE_SIZE
                val handleY = bounds.y + bounds.height - HANDLE_SIZE
                graphics.fill(handleX, handleY, handleX + HANDLE_SIZE, handleY + HANDLE_SIZE, HANDLE_COLOR)
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick)

        val mx = event.x().toFloat()
        val my = event.y().toFloat()

        // Check resize handles first (they take priority over dragging)
        for (element in HudManager.getAll().reversed()) {
            if (!element.enabled) continue
            val bounds = getElementScreenBounds(element) ?: continue
            if (isInsideResizeHandle(bounds, mx.toInt(), my.toInt())) {
                resizingElement = element
                initialScale = element.scale
                resizeStartMouseX = mx
                resizeStartMouseY = my
                return true
            }
        }

        // Check element bodies for dragging
        for (element in HudManager.getAll().reversed()) {
            if (!element.enabled) continue
            val bounds = getElementScreenBounds(element) ?: continue
            if (bounds.contains(mx.toInt(), my.toInt())) {
                draggingElement = element
                dragOffsetX = mx - element.x
                dragOffsetY = my - element.y
                return true
            }
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (event.button() != 0) return super.mouseDragged(event, dx, dy)

        val mx = event.x().toFloat()
        val my = event.y().toFloat()

        // Handle dragging
        val dragEl = draggingElement
        if (dragEl != null) {
            dragEl.x = mx - dragOffsetX
            dragEl.y = my - dragOffsetY
            return true
        }

        // Handle resizing (uniform scale based on width change)
        val resizeEl = resizingElement
        if (resizeEl != null) {
            val bounds = getElementScreenBounds(resizeEl) ?: return true
            val originalWidth = bounds.width.toFloat()
            if (originalWidth > 0f) {
                val deltaX = mx - resizeStartMouseX
                val scaleFactor = (originalWidth + deltaX) / originalWidth
                resizeEl.scale = (initialScale * scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            }
            return true
        }

        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0) {
            draggingElement = null
            resizingElement = null
        }
        return super.mouseReleased(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        // Save all element positions and scales to disk
        HudManager.save()
        val parent = parentScreen
        if (parent != null) {
            minecraft.gui.setScreen(parent)
        } else {
            minecraft.gui.setScreen(null)
        }
    }

    override fun isPauseScreen(): Boolean = false

    // ── Helper methods ────────────────────────────────────────

    /**
     * Computes the screen-space bounding box of a HUD element.
     * Returns null if the element has no content and no custom renderer to measure.
     */
    private fun getElementScreenBounds(element: HudElement): Rectangle? {
        val lines = element.renderContent()

        if (lines.isNotEmpty()) {
            val font = Minecraft.getInstance().font
            val maxWidth = lines.maxOf { it.width() }
            val totalHeight = lines.size * (font.lineHeight + 2)

            val scaledWidth = (maxWidth * element.scale * element.scaleX).toInt().coerceAtLeast(1)
            val scaledHeight = (totalHeight * element.scale * element.scaleY).toInt().coerceAtLeast(1)

            return Rectangle(element.x.toInt(), element.y.toInt(), scaledWidth, scaledHeight)
        }

        // For custom-rendered elements with no text content, use a default box
        // so the user can still see and interact with them
        return Rectangle(
            element.x.toInt(),
            element.y.toInt(),
            (DEFAULT_BOX_SIZE * element.scale * element.scaleX).toInt(),
            (DEFAULT_BOX_SIZE * element.scale * element.scaleY).toInt()
        )
    }

    /**
     * Checks if a point is inside the resize handle at the bottom-right corner of a bounding box.
     */
    private fun isInsideResizeHandle(bounds: Rectangle, x: Int, y: Int): Boolean {
        val handleX = bounds.x + bounds.width - HANDLE_SIZE
        val handleY = bounds.y + bounds.height - HANDLE_SIZE
        return x in handleX..(handleX + HANDLE_SIZE) && y in handleY..(handleY + HANDLE_SIZE)
    }
}