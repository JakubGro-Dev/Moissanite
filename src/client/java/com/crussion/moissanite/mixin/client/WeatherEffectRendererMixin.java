package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.CustomSkyVisuals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome.Precipitation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	@Redirect(
			method = "extractRenderState",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"))
	private float moissanite$useVisualWeatherRenderIntensity(Level level, float partialTick) {
		Float customIntensity = CustomSkyVisuals.precipitationIntensity(level.canHaveWeather());
		return customIntensity != null ? customIntensity : level.getRainLevel(partialTick);
	}

	@Redirect(
			method = "tickRainParticles",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"),
			require = 0)
	private float moissanite$useVisualWeatherParticleIntensity(ClientLevel level, float partialTick) {
		Float customIntensity = CustomSkyVisuals.precipitationIntensity(level.canHaveWeather());
		return customIntensity != null ? customIntensity : level.getRainLevel(partialTick);
	}

	@Redirect(
			method = "tickRainParticles",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"),
			require = 0)
	private float moissanite$useVisualWeatherParticleIntensityFallback(Level level, float partialTick) {
		Float customIntensity = CustomSkyVisuals.precipitationIntensity(level.canHaveWeather());
		return customIntensity != null ? customIntensity : level.getRainLevel(partialTick);
	}

	@Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
	private void moissanite$useVisualSnowWeather(Level level, BlockPos blockPos, CallbackInfoReturnable<Precipitation> cir) {
		boolean canHaveWeather = level.canHaveWeather();
		if (!CustomSkyVisuals.snowWeather(canHaveWeather) && !CustomSkyVisuals.rainWeather(canHaveWeather)) {
			return;
		}

		if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
			return;
		}

		cir.setReturnValue(CustomSkyVisuals.snowWeather(canHaveWeather) ? Precipitation.SNOW : Precipitation.RAIN);
	}
}
