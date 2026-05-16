package com.crussion.moissanite.util.rotation;

import java.util.Arrays;
import java.util.List;

public enum RotationMode {
	HUMANIZED_STRONG("Humanized Strong"),
	HUMANIZED("Humanized"),
	ADAPTIVE_SPRING("Adaptive Spring"),
	LINEAR("Linear"),
	SMOOTHSTEP("Smoothstep"),
	SINE("Sine"),
	MINIMUM_JERK("Minimum Jerk"),
	SIGMOID("Sigmoid"),
	EXPONENTIAL("Exponential");

	public static final RotationMode DEFAULT = HUMANIZED_STRONG;

	private static final List<String> DISPLAY_NAMES = Arrays.stream(values())
			.map(RotationMode::displayName)
			.toList();

	private final String displayName;

	RotationMode(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public static List<String> displayNames() {
		return DISPLAY_NAMES;
	}

	public static RotationMode fromDisplayName(String value) {
		if (value == null) {
			return DEFAULT;
		}

		String normalized = value.trim();
		for (RotationMode mode : values()) {
			if (mode.displayName.equalsIgnoreCase(normalized)) {
				return mode;
			}
		}
		return DEFAULT;
	}
}
