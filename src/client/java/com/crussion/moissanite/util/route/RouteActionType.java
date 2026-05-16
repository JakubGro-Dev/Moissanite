package com.crussion.moissanite.util.route;

import java.util.Locale;

public enum RouteActionType {
	TP,
	ETH,
	WALK;

	public static RouteActionType fromSerialized(String raw) {
		if (raw == null || raw.isBlank()) {
			return TP;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return TP;
		}
	}
}
