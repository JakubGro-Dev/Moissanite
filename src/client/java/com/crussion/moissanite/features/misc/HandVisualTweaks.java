package com.crussion.moissanite.features.misc;

import com.crussion.moissanite.definitions.UiDefinitions;

public final class HandVisualTweaks {
	private static final double EPSILON = 1.0e-4;

	private HandVisualTweaks() {
	}

	public static float handX() {
		return (float) value(UiDefinitions.HAND_VISUAL_X.get());
	}

	public static float handY() {
		return (float) value(UiDefinitions.HAND_VISUAL_Y.get());
	}

	public static float handZ() {
		return (float) value(UiDefinitions.HAND_VISUAL_Z.get());
	}

	public static double handSizeOffset() {
		return value(UiDefinitions.HAND_VISUAL_SIZE.get());
	}

	public static float handScale() {
		return (float) (1.0 + handSizeOffset());
	}

	public static double handSpeedOffset() {
		return value(UiDefinitions.HAND_VISUAL_SPEED.get());
	}

	public static boolean hasHandTransform() {
		return Math.abs(handX()) > EPSILON
			|| Math.abs(handY()) > EPSILON
			|| Math.abs(handZ()) > EPSILON
			|| Math.abs(handSizeOffset()) > EPSILON;
	}

	public static boolean scaleSwingWithHandSize() {
		return Boolean.TRUE.equals(UiDefinitions.HAND_VISUAL_SCALE_SWING.get());
	}

	public static boolean ignoreHaste() {
		return Boolean.TRUE.equals(UiDefinitions.HAND_VISUAL_IGNORE_HASTE.get());
	}

	public static boolean noEquipReset() {
		return Boolean.TRUE.equals(UiDefinitions.HAND_VISUAL_NO_EQUIP_RESET.get());
	}

	public static boolean cancelSwing() {
		return Boolean.TRUE.equals(UiDefinitions.HAND_VISUAL_CANCEL_SWING.get());
	}

	public static boolean overridesSwingDuration() {
		return ignoreHaste() || Math.abs(handSpeedOffset()) > EPSILON;
	}

	public static int adjustSwingDuration(int baseDuration) {
		int safeDuration = Math.max(1, baseDuration);
		double speed = handSpeedOffset();
		double rate = speed >= 0.0 ? (1.0 + speed) : (1.0 / (1.0 - speed));
		int adjustedDuration = (int) Math.round(safeDuration / rate);
		return Math.max(1, adjustedDuration);
	}

	private static double value(Double value) {
		return value == null ? 0.0 : value;
	}
}
