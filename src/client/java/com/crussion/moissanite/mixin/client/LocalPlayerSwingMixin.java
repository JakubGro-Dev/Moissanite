package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.HandVisualTweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSwingMixin {
	@Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"))
	private void moissanite$restartSwingAnimation(InteractionHand hand, CallbackInfo ci) {
		if (!HandVisualTweaks.cancelSwing() || moissanite$isMiningSwing()) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		self.swinging = false;
		self.swingTime = -1;
	}

	@Unique
	private static boolean moissanite$isMiningSwing() {
		Minecraft client = Minecraft.getInstance();
		return client.gameMode != null && client.gameMode.isDestroying();
	}
}
