package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.CustomSkyVisuals;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void moissanite$applyForcedSkyPhase(ClientLevel clientLevel, float partialTick, Camera camera, SkyRenderState skyRenderState, CallbackInfo ci) {
		CustomSkyVisuals.applyForcedSkyPhase(skyRenderState);
	}
}
