package de.kazz.config.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry as FabricHudRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

/**
 * Client-side HUD renderer that hooks into Fabric's HUD element system.
 *
 * Registers a single Fabric HUD element that iterates over all enabled elements
 * from [HudManager] and renders them in Z-order (by [HudLayer]).
 *
 * This must be initialized during client-side mod initialization.
 */
object HudRenderer {

    private const val MOD_ID = "kazzutils"
    private const val RENDERER_ID = "hud_renderer"

    private var initialized = false

    /**
     * Identifier used for the Fabric HUD element registration.
     */
    private val rendererIdentifier = Identifier.fromNamespaceAndPath(MOD_ID, RENDERER_ID)

    /**
     * Initialize the HUD renderer.
     *
     * Registers a single element with Fabric's [FabricHudRegistry] that acts as
     * a dispatcher to all custom [HudElement]s registered in [HudManager].
     *
     * Should be called once during [net.fabricmc.api.ClientModInitializer.onInitializeClient].
     */
    fun initialize() {
        if (initialized) return
        initialized = true

        FabricHudRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            rendererIdentifier,
            ::renderAll
        )

    }

    /**
     * Render all enabled HUD elements.
     */
    private fun renderAll(graphics: GuiGraphicsExtractor, tickCounter: DeltaTracker) {
        for (element in HudManager.getEnabled()) {
            element.render(graphics, tickCounter)
        }
    }

    /**
     * Save all HUD element positions to disk.
     * Should be called when the game saves or the config screen closes.
     */
    fun savePositions() {
        HudManager.save()
    }
}