package com.crussion.moissanite.features.general;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.input.KeybindKeys;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;

public final class Zoom {
	private static final double MIN_ZOOM_FACTOR = 1.0D;
	private static final double MAX_ZOOM_FACTOR = 120.0D;
	private static final double DEFAULT_ZOOM_FACTOR = 6.0D;
	private static final double ZOOM_SCROLL_MULTIPLIER = 1.1D;
	private static boolean initialized;
	private static boolean zoomHeldLastCheck;
	private static double currentZoomFactor = DEFAULT_ZOOM_FACTOR;

	private Zoom() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
	}

	public static boolean handleMouseScroll(double horizontalAmount, double verticalAmount) {
		if (!isZoomActive()) {
			return false;
		}

		double scrollAmount = verticalAmount != 0.0D ? verticalAmount : -horizontalAmount;
		if (scrollAmount != 0.0D) {
			adjustZoomFactor(scrollAmount);
		}
		return true;
	}

	public static float applyZoomFov(float originalFov) {
		if (!isZoomActive()) {
			return originalFov;
		}
		return (float) Math.max(1.0D, originalFov / effectiveZoomFactor());
	}

	public static double sensitivityScale() {
		if (!isZoomActive()) {
			return 1.0D;
		}
		return 1.0D / effectiveZoomFactor();
	}

	public static boolean isZoomActive() {
		boolean active = isZoomKeyHeld();
		if (active && !zoomHeldLastCheck) {
			currentZoomFactor = DEFAULT_ZOOM_FACTOR;
		} else if (!active) {
			currentZoomFactor = DEFAULT_ZOOM_FACTOR;
		}
		zoomHeldLastCheck = active;
		return active;
	}

	private static boolean isZoomKeyHeld() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null || client.options == null
				|| client.getWindow() == null) {
			return false;
		}
		if (client.screen != null || client.getOverlay() != null) {
			return false;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.ZOOM.get())) {
			return false;
		}

		Integer keyCode = UiDefinitions.ZOOM_KEYBIND.get();
		if (keyCode == null || keyCode == InputConstants.UNKNOWN.getValue()) {
			return false;
		}
		return KeybindKeys.isPressed(client.getWindow(), keyCode);
	}

	private static void adjustZoomFactor(double scrollAmount) {
		currentZoomFactor = clampZoomFactor(currentZoomFactor * Math.pow(ZOOM_SCROLL_MULTIPLIER, scrollAmount));
	}

	private static double effectiveZoomFactor() {
		return currentZoomFactor;
	}

	private static double clampZoomFactor(double zoomFactor) {
		if (!Double.isFinite(zoomFactor)) {
			return DEFAULT_ZOOM_FACTOR;
		}
		return Math.max(MIN_ZOOM_FACTOR, Math.min(MAX_ZOOM_FACTOR, zoomFactor));
	}
}
