package com.crussion.moissanite.util.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum RouteIsland {
	FISHING_1("fishing_1", "Backwater Bayou"),
	FARMING_1("farming_1", "The Farming Islands"),
	CRYSTAL_HOLLOWS("crystal_hollows", "Crystal Hollows"),
	FORAGING_2("foraging_2", "Galatea"),
	WINTER("winter", "Jerry's Workshop"),
	FORAGING_1("foraging_1", "The Park"),
	DARK_AUCTION("dark_auction", "Dark Auction"),
	DUNGEON("dungeon", "Dungeons"),
	COMBAT_3("combat_3", "The End"),
	CRIMSON_ISLE("crismon_isle", "Crimson Isle", "crimson_isle"),
	HUB("hub", "Hub"),
	INSTANCED("kuudra", "Kuudra's Hollow"),
	DYNAMIC("dynamic", "Private Island"),
	MINING_3("mining_3", "Dwarven Mines"),
	GARDEN("garden", "The Garden"),
	MINING_1("mining_1", "Gold Mine"),
	COMBAT_2("combat_2", "Blazing Fortress"),
	MINING_2("mining_2", "Deep Caverns"),
	COMBAT_1("combat_1", "Spider's Den");

	private final String modeKey;
	private final String displayName;
	private final List<String> aliases;

	RouteIsland(String modeKey, String displayName, String... aliases) {
		this.modeKey = modeKey;
		this.displayName = displayName;
		this.aliases = List.of(aliases);
	}

	public String modeKey() {
		return modeKey;
	}

	public String displayName() {
		return displayName;
	}

	public boolean matchesMode(String mode) {
		String normalized = normalize(mode);
		if (normalized.isBlank()) {
			return false;
		}
		if (normalize(modeKey).equals(normalized)) {
			return true;
		}
		for (String alias : aliases) {
			if (normalize(alias).equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	public static RouteIsland fromMode(String mode) {
		for (RouteIsland island : values()) {
			if (island.matchesMode(mode)) {
				return island;
			}
		}
		return null;
	}

	public static RouteIsland fromDisplayName(String displayName) {
		String normalized = normalize(displayName);
		if (normalized.isBlank()) {
			return HUB;
		}
		for (RouteIsland island : values()) {
			if (normalize(island.displayName).equals(normalized) || normalize(island.modeKey).equals(normalized)) {
				return island;
			}
		}
		return HUB;
	}

	public static List<String> displayNames() {
		List<String> names = new ArrayList<>();
		for (RouteIsland island : values()) {
			names.add(island.displayName);
		}
		return names;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
