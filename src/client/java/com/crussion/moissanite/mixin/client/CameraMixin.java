package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Unique
	private static final float OLD_SHIFT_DROP = 0.08f;

	@Shadow
	private Entity entity;

	@Shadow
	private float eyeHeight;

	@Shadow
	private float eyeHeightOld;

	@Inject(method = "tick", at = @At("TAIL"))
	private void moissanite$applyNoShiftAnimation(CallbackInfo ci) {
		if (!(entity instanceof LocalPlayer player)) {
			return;
		}

		if (!Boolean.TRUE.equals(UiDefinitions.NO_SHIFT_ANIMATION.get())) {
			return;
		}

		float targetEyeHeight = moissanite$targetEyeHeight(player);
		eyeHeightOld = targetEyeHeight;
		eyeHeight = targetEyeHeight;
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F")
	)
	private float moissanite$redirectEyeHeight(Entity instance) {
		if (!(instance instanceof LocalPlayer player)) {
			return instance.getEyeHeight();
		}
		return moissanite$targetEyeHeight(player);
	}

	@Unique
	private static float moissanite$targetEyeHeight(LocalPlayer player) {
		if (!Boolean.TRUE.equals(UiDefinitions.OLD_SHIFT.get())) {
			return player.getEyeHeight();
		}
		if (player.getPose() != Pose.CROUCHING) {
			return player.getEyeHeight();
		}
		return player.getEyeHeight(Pose.STANDING) - OLD_SHIFT_DROP;
	}
}
