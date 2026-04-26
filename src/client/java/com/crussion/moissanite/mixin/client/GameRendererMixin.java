package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.features.general.Zoom;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void moissanite$cancelHurtCamera(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
		if (Boolean.TRUE.equals(UiDefinitions.NO_TILT.get())) {
			ci.cancel();
		}
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void moissanite$applyZoomFov(Camera camera, float partialTicks, boolean useConfiguredFov,
			CallbackInfoReturnable<Float> cir) {
		Float currentFov = cir.getReturnValue();
		cir.setReturnValue(Zoom.applyZoomFov(currentFov != null ? currentFov.floatValue() : 70.0F));
	}
}
