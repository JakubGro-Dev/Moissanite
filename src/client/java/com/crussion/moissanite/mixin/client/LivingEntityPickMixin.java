package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.general.GeneralTweaks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPickMixin {
	@Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
	private void moissanite$ignoreMobLeftoverPick(CallbackInfoReturnable<Boolean> cir) {
		if (GeneralTweaks.shouldIgnorePickableEntity((LivingEntity)(Object)this)) {
			cir.setReturnValue(false);
		}
	}
}
