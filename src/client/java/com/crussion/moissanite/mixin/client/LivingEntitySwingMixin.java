package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.HandVisualTweaks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {
	@Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
	private void moissanite$overrideSwingDuration(CallbackInfoReturnable<Integer> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof LocalPlayer)) {
			return;
		}
		if (!HandVisualTweaks.overridesSwingDuration()) {
			return;
		}

		ItemStack mainHand = self.getItemInHand(InteractionHand.MAIN_HAND);
		int swingDuration = mainHand.getSwingAnimation().duration();

		if (!HandVisualTweaks.ignoreHaste()) {
			if (MobEffectUtil.hasDigSpeed(self)) {
				swingDuration -= 1 + MobEffectUtil.getDigSpeedAmplification(self);
			} else if (self.hasEffect(MobEffects.MINING_FATIGUE)) {
				MobEffectInstance fatigue = self.getEffect(MobEffects.MINING_FATIGUE);
				if (fatigue != null) {
					swingDuration += (1 + fatigue.getAmplifier()) * 2;
				}
			}
		}

		cir.setReturnValue(HandVisualTweaks.adjustSwingDuration(swingDuration));
	}
}
