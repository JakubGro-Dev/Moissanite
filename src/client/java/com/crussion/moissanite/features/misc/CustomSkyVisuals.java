package com.crussion.moissanite.features.misc;

import com.crussion.moissanite.definitions.UiDefinitions;
import net.minecraft.client.renderer.state.SkyRenderState;

public final class CustomSkyVisuals {
	private static final long DAY_LENGTH_TICKS = 24000L;
	private static final double VANILLA_TICKS_PER_SECOND = 20.0;
	private static final int CLEAR_DAY_SKY_COLOR = 0x78A7FF;
	private static final int CLEAR_NIGHT_SKY_COLOR = 0x050812;
	private static final String PHASE_BY_TIME = "By Time";
	private static final String PHASE_FORCE_DAY = "Force Day";
	private static final String PHASE_FORCE_NIGHT = "Force Night";
	private static final String WEATHER_VANILLA = "Vanilla";
	private static final String WEATHER_CLEAR = "Clear";
	private static final String WEATHER_RAIN = "Rain";
	private static final String WEATHER_SNOW = "Snow";
	private static final String WEATHER_THUNDER = "Thunder";

	private static boolean wasEnabled;
	private static double anchorSkyTimeTicks;
	private static long anchorTimeMillis;
	private static double lastConfiguredSkyTime = Double.NaN;
	private static double lastConfiguredFlow = Double.NaN;

	public static boolean enabled() {
		return Boolean.TRUE.equals(UiDefinitions.CUSTOM_SKY_VISUALS.get());
	}

	public static long visualDayTime() {
		syncAnchor();
		return Math.floorMod((long)Math.floor(currentSkyTimeTicks()), DAY_LENGTH_TICKS);
	}

	public static void applyForcedSkyPhase(SkyRenderState skyRenderState) {
		if (!enabled() || skyRenderState == null) {
			return;
		}

		String phase = phaseMode();
		if (PHASE_FORCE_DAY.equalsIgnoreCase(phase)) {
			skyRenderState.skyColor = CLEAR_DAY_SKY_COLOR;
			skyRenderState.starBrightness = 0.0F;
			skyRenderState.sunriseAndSunsetColor = 0;
			return;
		}

		if (PHASE_FORCE_NIGHT.equalsIgnoreCase(phase)) {
			skyRenderState.skyColor = CLEAR_NIGHT_SKY_COLOR;
			skyRenderState.starBrightness = Math.max(skyRenderState.starBrightness, 1.0F);
			skyRenderState.sunriseAndSunsetColor = 0;
		}
	}

	public static Float skyRainLevel(boolean canHaveWeather) {
		if (!enabled()) {
			return null;
		}

		String mode = weatherMode();
		if (WEATHER_VANILLA.equalsIgnoreCase(mode)) {
			return null;
		}

		return 0.0F;
	}

	public static Float skyThunderLevel(boolean canHaveWeather) {
		if (!enabled()) {
			return null;
		}

		String mode = weatherMode();
		if (WEATHER_VANILLA.equalsIgnoreCase(mode)) {
			return null;
		}

		return 0.0F;
	}

	public static Float precipitationIntensity(boolean canHaveWeather) {
		if (!enabled()) {
			return null;
		}

		String mode = weatherMode();
		if (WEATHER_VANILLA.equalsIgnoreCase(mode)) {
			return null;
		}
		if (!canHaveWeather || WEATHER_CLEAR.equalsIgnoreCase(mode)) {
			return 0.0F;
		}
		if (WEATHER_RAIN.equalsIgnoreCase(mode) || WEATHER_SNOW.equalsIgnoreCase(mode) || WEATHER_THUNDER.equalsIgnoreCase(mode)) {
			return 1.0F;
		}

		return null;
	}

	public static Boolean rainingState(boolean canHaveWeather) {
		Float intensity = precipitationIntensity(canHaveWeather);
		return intensity == null ? null : intensity > 0.2F;
	}

	public static Boolean thunderingState(boolean canHaveWeather) {
		if (!enabled()) {
			return null;
		}

		String mode = weatherMode();
		if (WEATHER_VANILLA.equalsIgnoreCase(mode)) {
			return null;
		}

		return false;
	}

	public static boolean rainWeather(boolean canHaveWeather) {
		String mode = weatherMode();
		return enabled() && canHaveWeather && (WEATHER_RAIN.equalsIgnoreCase(mode) || WEATHER_THUNDER.equalsIgnoreCase(mode));
	}

	public static boolean snowWeather(boolean canHaveWeather) {
		return enabled() && canHaveWeather && WEATHER_SNOW.equalsIgnoreCase(weatherMode());
	}

	private static void syncAnchor() {
		if (!enabled()) {
			wasEnabled = false;
			return;
		}

		double configuredSkyTime = skyTimeSetting();
		double configuredFlow = skyTimeFlowSetting();
		if (!wasEnabled || configuredSkyTime != lastConfiguredSkyTime || configuredFlow != lastConfiguredFlow) {
			anchorSkyTimeTicks = configuredSkyTime;
			anchorTimeMillis = System.currentTimeMillis();
			lastConfiguredSkyTime = configuredSkyTime;
			lastConfiguredFlow = configuredFlow;
			wasEnabled = true;
		}
	}

	private static double currentSkyTimeTicks() {
		double elapsedSeconds = (System.currentTimeMillis() - anchorTimeMillis) / 1000.0;
		return anchorSkyTimeTicks + elapsedSeconds * VANILLA_TICKS_PER_SECOND * skyTimeFlowSetting();
	}

	private static double skyTimeSetting() {
		Double value = UiDefinitions.CUSTOM_SKY_TIME.get();
		if (value == null || !Double.isFinite(value)) {
			return 18000.0;
		}
		return Math.max(0.0, Math.min(DAY_LENGTH_TICKS - 1.0, value));
	}

	private static double skyTimeFlowSetting() {
		Double value = UiDefinitions.CUSTOM_SKY_TIME_FLOW.get();
		if (value == null || !Double.isFinite(value)) {
			return 0.0;
		}
		return Math.max(0.0, Math.min(20.0, value));
	}

	private static String weatherMode() {
		String value = UiDefinitions.CUSTOM_SKY_WEATHER.get();
		return value == null || value.isBlank() ? WEATHER_VANILLA : value;
	}

	private static String phaseMode() {
		String value = UiDefinitions.CUSTOM_SKY_PHASE.get();
		return value == null || value.isBlank() ? PHASE_BY_TIME : value;
	}

	private CustomSkyVisuals() {
	}
}
