package de.kazz.mixin

import de.kazz.features.borderbox.BorderBoxRenderer
import de.kazz.features.lines.LineRenderer
import de.kazz.features.waypoints.WaypointRenderer
import net.minecraft.client.renderer.GameRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GameRenderer::class)
class GameRendererMixin {
    @Inject(method = ["close"], at = [At("RETURN")])
    private fun onGameRendererClose(ci: CallbackInfo) {
        WaypointRenderer.close()
        LineRenderer.close()
        BorderBoxRenderer.close()
    }
}
