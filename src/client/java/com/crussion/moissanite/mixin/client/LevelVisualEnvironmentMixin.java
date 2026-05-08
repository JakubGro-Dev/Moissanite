package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.CustomSkyVisuals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelVisualEnvironmentMixin {
	@Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
	private void moissanite$useVisualSkyTime(CallbackInfoReturnable<Long> cir) {
		if ((Object)this instanceof ClientLevel && CustomSkyVisuals.enabled()) {
			cir.setReturnValue(CustomSkyVisuals.visualDayTime());
		}
	}

	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	private void moissanite$useVisualRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (!((Object)this instanceof ClientLevel clientLevel)) {
			return;
		}

		Float customRainLevel = CustomSkyVisuals.skyRainLevel(clientLevel.canHaveWeather());
		if (customRainLevel != null) {
			cir.setReturnValue(customRainLevel);
		}
	}

	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
	private void moissanite$useVisualThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (!((Object)this instanceof ClientLevel clientLevel)) {
			return;
		}

		Float customThunderLevel = CustomSkyVisuals.skyThunderLevel(clientLevel.canHaveWeather());
		if (customThunderLevel != null) {
			cir.setReturnValue(customThunderLevel);
		}
	}

	@Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
	private void moissanite$useVisualRainingState(CallbackInfoReturnable<Boolean> cir) {
		if (!((Object)this instanceof ClientLevel clientLevel)) {
			return;
		}

		Boolean rainingState = CustomSkyVisuals.rainingState(clientLevel.canHaveWeather());
		if (rainingState != null) {
			cir.setReturnValue(rainingState);
		}
	}

	@Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
	private void moissanite$useVisualThunderingState(CallbackInfoReturnable<Boolean> cir) {
		if (!((Object)this instanceof ClientLevel clientLevel)) {
			return;
		}

		Boolean thunderingState = CustomSkyVisuals.thunderingState(clientLevel.canHaveWeather());
		if (thunderingState != null) {
			cir.setReturnValue(thunderingState);
		}
	}
}
