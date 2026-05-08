package com.crussion.moissanite.util.hypixel;

import com.crussion.moissanite.util.text.TextNormalizer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SkyBlockLocationTracker {
	private static final String CRYSTAL_HOLLOWS_MODE = "crystal_hollows";
	private static final long MIN_REFRESH_INTERVAL_MS = 8_000L;
	private static final long SILENT_RESPONSE_TIMEOUT_MS = 8_000L;

	private static boolean initialized;
	private static Location currentLocation = Location.unknown();
	private static long lastRefreshRequestAtMs;
	private static int pendingSilentResponses;
	private static long silentResponseExpiresAtMs;

	private SkyBlockLocationTracker() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset());
	}

	public static void requestRefreshIfNeeded() {
		if (!currentLocation.isUnknown()) {
			return;
		}

		requestLocraw(true, false);
	}

	public static void requestRefreshNow(boolean silent) {
		requestLocraw(silent, true);
	}

	public static boolean isInCrystalHollows() {
		Location location = currentLocation;
		return location.isSkyBlock() && CRYSTAL_HOLLOWS_MODE.equalsIgnoreCase(location.mode());
	}

	public static Location currentLocation() {
		return currentLocation;
	}

	public static void onSystemChat(Component message) {
		Location parsed = parseLocraw(message);
		if (parsed == null) {
			return;
		}

		currentLocation = parsed;
	}

	public static boolean shouldHideSystemChat(Component message) {
		if (parseLocraw(message) == null) {
			return false;
		}

		long nowMs = System.currentTimeMillis();
		if (pendingSilentResponses <= 0 || nowMs > silentResponseExpiresAtMs) {
			pendingSilentResponses = 0;
			return false;
		}

		pendingSilentResponses--;
		return true;
	}

	private static void requestLocraw(boolean silent, boolean force) {
		Minecraft client = Minecraft.getInstance();
		if (!canUseLocraw(client)) {
			return;
		}

		long nowMs = System.currentTimeMillis();
		if (!force && nowMs - lastRefreshRequestAtMs < MIN_REFRESH_INTERVAL_MS) {
			return;
		}

		lastRefreshRequestAtMs = nowMs;
		if (silent) {
			pendingSilentResponses++;
			silentResponseExpiresAtMs = nowMs + SILENT_RESPONSE_TIMEOUT_MS;
		}

		client.player.connection.sendCommand("locraw");
	}

	private static boolean canUseLocraw(Minecraft client) {
		return client != null
				&& client.player != null
				&& client.player.connection != null
				&& client.level != null
				&& isHypixelServer(client);
	}

	private static boolean isHypixelServer(Minecraft client) {
		ServerData server = client.getCurrentServer();
		if (server == null || server.ip == null) {
			return false;
		}

		return server.ip.toLowerCase(Locale.ROOT).contains("hypixel");
	}

	private static Location parseLocraw(Component message) {
		String text = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString()).trim();
		if (!text.startsWith("{") || !text.endsWith("}")) {
			return null;
		}

		try {
			JsonElement parsed = JsonParser.parseString(text);
			if (parsed == null || !parsed.isJsonObject()) {
				return null;
			}

			JsonObject root = parsed.getAsJsonObject();
			if (!root.has("server") && !root.has("gametype") && !root.has("mode") && !root.has("map")) {
				return null;
			}

			return new Location(
					stringValue(root, "server"),
					stringValue(root, "gametype"),
					stringValue(root, "mode"),
					stringValue(root, "map"));
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private static String stringValue(JsonObject root, String key) {
		JsonElement value = root.get(key);
		if (value == null || value.isJsonNull()) {
			return "";
		}
		try {
			return value.getAsString();
		} catch (RuntimeException ignored) {
			return "";
		}
	}

	private static void reset() {
		currentLocation = Location.unknown();
		lastRefreshRequestAtMs = 0L;
		pendingSilentResponses = 0;
		silentResponseExpiresAtMs = 0L;
	}

	public record Location(String server, String gametype, String mode, String map) {
		private static Location unknown() {
			return new Location("", "", "", "");
		}

		public boolean isSkyBlock() {
			return "SKYBLOCK".equalsIgnoreCase(gametype);
		}

		private boolean isUnknown() {
			return server.isBlank() && gametype.isBlank() && mode.isBlank() && map.isBlank();
		}
	}
}
