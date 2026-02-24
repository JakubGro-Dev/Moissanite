package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EntitySprintGuardMixin {
	@Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
	private void moissanite$blockSprintWhileShifting(boolean sprinting, CallbackInfo ci) {
		if (!sprinting) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.ALWAYS_SPRINT.get())) {
			return;
		}
		if (!((Object) this instanceof LocalPlayer player)) {
			return;
		}

		boolean shiftHeld = player.input != null
			? player.input.keyPresses.shift()
			: player.isShiftKeyDown();
		if (shiftHeld || player.isMovingSlowly()) {
			ci.cancel();
		}
	}
}
