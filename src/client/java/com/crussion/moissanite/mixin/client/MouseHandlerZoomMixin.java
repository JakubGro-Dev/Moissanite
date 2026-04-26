package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.general.Zoom;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerZoomMixin {
	@Shadow
	private double accumulatedDX;

	@Shadow
	private double accumulatedDY;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void moissanite$consumeZoomScroll(long windowHandle, double horizontalAmount, double verticalAmount,
			CallbackInfo ci) {
		if (Zoom.handleMouseScroll(horizontalAmount, verticalAmount)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;turnPlayer(D)V"))
	private void moissanite$scaleZoomSensitivity(CallbackInfo ci) {
		double sensitivityScale = Zoom.sensitivityScale();
		if (sensitivityScale >= 1.0D) {
			return;
		}

		accumulatedDX *= sensitivityScale;
		accumulatedDY *= sensitivityScale;
	}
}
