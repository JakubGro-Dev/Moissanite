package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class NoDebuffMixin {
	@Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
	private void moissanite$hideBlindnessEffect(Holder<MobEffect> effect,
			CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(UiDefinitions.NO_DEBUFF.get())) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof LocalPlayer)) {
			return;
		}
		if (effect == MobEffects.BLINDNESS) {
			cir.setReturnValue(false);
		}
	}
}
