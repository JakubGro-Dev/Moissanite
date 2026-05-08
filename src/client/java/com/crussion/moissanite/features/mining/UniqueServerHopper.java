package com.crussion.moissanite.features.mining;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.text.TextNormalizer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class UniqueServerHopper {
	private static final long WORLD_CHANGE_TIMEOUT_MS = 20_000L;
	private static final long LOCRAW_TIMEOUT_MS = 8_000L;
	private static final long MIN_ANY_COMMAND_INTERVAL_MS = 3_500L;
	private static final long SERVER_THROTTLE_BACKOFF_MS = 6_000L;
	private static final Set<String> SEEN_SERVERS = new HashSet<>();

	private static boolean initialized;
	private static State state = State.STOPPED;
	private static CommandKind lastCommand = CommandKind.NONE;
	private static long actionAtMs;
	private static long commandSentAtMs;
	private static long locrawRequestedAtMs;
	private static long lastAnyCommandSentAtMs;
	private static long nextWorldCheckLocrawAtMs;
	private static long worldCheckLocrawRequestedAtMs;
	private static boolean worldCheckLocrawPending;
	private static String lastKnownServer = "";
	private static String commandStartServer = "";
	private static int uniqueSoundToken;

	private UniqueServerHopper() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(UniqueServerHopper::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> onWorldChange());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stopRuntime());
	}

	public static void start() {
		if (!Boolean.TRUE.equals(UiDefinitions.UNIQUE_SERVER_HOPPER.get())) {
			FeatureChat.sendPrefixed("Unique Server Hopper", "Enable the feature first.");
			return;
		}
		if (command1().isBlank() || command2().isBlank()) {
			FeatureChat.sendPrefixed("Unique Server Hopper", "Command 1 and Command 2 cannot be empty.");
			return;
		}

		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		lastKnownServer = location == null ? "" : safeServer(location.server());

		state = State.WAITING_INITIAL_LOCRAW_READY;
		lastCommand = CommandKind.NONE;
		actionAtMs = 0L;
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		lastAnyCommandSentAtMs = 0L;
		nextWorldCheckLocrawAtMs = 0L;
		worldCheckLocrawRequestedAtMs = 0L;
		worldCheckLocrawPending = false;
		commandStartServer = "";
		FeatureChat.sendPrefixed("Unique Server Hopper", "Started. Cached servers: " + SEEN_SERVERS.size());
	}

	public static void stop() {
		stopRuntime();
		FeatureChat.sendPrefixed("Unique Server Hopper", "Stopped. Cache was not cleared.");
	}

	public static void cleanCache() {
		SEEN_SERVERS.clear();
		FeatureChat.sendPrefixed("Unique Server Hopper", "Cache cleared.");
	}

	public static boolean isRunning() {
		return state != State.STOPPED;
	}

	public static void onToggleChanged(Boolean enabled) {
		if (!Boolean.TRUE.equals(enabled)) {
			stopRuntime();
		}
	}

	public static void onSystemChat(Component message) {
		String text = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString()).trim();
		if (text.isBlank()) {
			return;
		}

		Locraw locraw = parseLocraw(message);
		if (locraw != null) {
			handleLocrawResponse(locraw.server());
			return;
		}

		if (isKickMessage(text)) {
			if (isRunning()) {
				stopRuntime();
				FeatureChat.sendPrefixed("Unique Server Hopper", "Stopped because a kick occurred. Cache was not cleared.");
			}
			return;
		}

		if (isTemporaryFailureMessage(text)) {
			handleTemporaryFailure(text);
		}
	}

	private static void onClientTick(Minecraft client) {
		if (!Boolean.TRUE.equals(UiDefinitions.UNIQUE_SERVER_HOPPER.get())) {
			stopRuntime();
			return;
		}
		if (!isRunning()) {
			return;
		}
		if (client == null || client.player == null || client.level == null || client.player.connection == null) {
			return;
		}

		long now = System.currentTimeMillis();

		if (isWaitingForCommandWorldChange()) {
			handleWorldChangeWaitTick(now);
			return;
		}

		switch (state) {
			case WAITING_INITIAL_LOCRAW_READY -> {
				if (now >= actionAtMs) {
					requestInitialLocraw();
				}
			}
			case WAITING_INITIAL_LOCRAW_RESPONSE -> {
				if (locrawRequestedAtMs > 0L && now - locrawRequestedAtMs >= LOCRAW_TIMEOUT_MS) {
					state = State.WAITING_INITIAL_LOCRAW_READY;
					actionAtMs = now + retryDelayMs();
					locrawRequestedAtMs = 0L;
					FeatureChat.sendPrefixed("Unique Server Hopper", "Initial locraw timed out. Retrying locraw.");
				}
			}
			case WAITING_COMMAND_1_READY -> {
				if (now >= actionAtMs) {
					sendCommand(client, CommandKind.COMMAND_1);
				}
			}
			case WAITING_COMMAND_2_READY, WAITING_COMMAND_2_AFTER_UNIQUE_LEAVE -> {
				if (now >= actionAtMs) {
					sendCommand(client, CommandKind.COMMAND_2);
				}
			}
			case WAITING_LOCRAW_READY -> {
				if (now >= actionAtMs) {
					requestTrackedLocraw();
				}
			}
			case WAITING_LOCRAW_RESPONSE -> {
				if (locrawRequestedAtMs > 0L && now - locrawRequestedAtMs >= LOCRAW_TIMEOUT_MS) {
					state = State.WAITING_LOCRAW_READY;
					actionAtMs = now + retryDelayMs();
					locrawRequestedAtMs = 0L;
					FeatureChat.sendPrefixed("Unique Server Hopper", "Locraw timed out. Retrying locraw.");
				}
			}
			case WAITING_RETRY -> {
				if (now >= actionAtMs) {
					retryLastCommand(client);
				}
			}
			default -> {
			}
		}
	}

	private static void handleWorldChangeWaitTick(long now) {
		if (commandSentAtMs > 0L && now - commandSentAtMs >= WORLD_CHANGE_TIMEOUT_MS) {
			state = State.WAITING_RETRY;
			actionAtMs = now + retryDelayMs();
			clearWorldCheckLocraw();
			FeatureChat.sendPrefixed("Unique Server Hopper", "World did not change. Retrying " + lastCommandName() + ".");
			return;
		}

		if (worldCheckLocrawPending) {
			if (worldCheckLocrawRequestedAtMs > 0L && now - worldCheckLocrawRequestedAtMs >= LOCRAW_TIMEOUT_MS) {
				worldCheckLocrawPending = false;
				worldCheckLocrawRequestedAtMs = 0L;
				nextWorldCheckLocrawAtMs = now + retryDelayMs();
				FeatureChat.sendPrefixed("Unique Server Hopper", "World check locraw timed out.");
			}
			return;
		}

		if (now >= nextWorldCheckLocrawAtMs) {
			requestWorldCheckLocraw();
		}
	}

	private static void onWorldChange() {
		if (!isRunning()) {
			return;
		}

		long now = System.currentTimeMillis();

		if (state == State.UNIQUE_WAITING_USER_LEAVE) {
			state = State.WAITING_COMMAND_2_AFTER_UNIQUE_LEAVE;
			actionAtMs = now + worldEnterDelayMs();
			commandSentAtMs = 0L;
			locrawRequestedAtMs = 0L;
			lastCommand = CommandKind.NONE;
			clearWorldCheckLocraw();
			FeatureChat.sendPrefixed("Unique Server Hopper", "Left unique world. Command 2 in " + configuredWorldEnterDelayText() + "s.");
			return;
		}

		if (state == State.WAITING_COMMAND_1_WORLD_CHANGE) {
			completeCommand1WorldChange(now);
			return;
		}

		if (state == State.WAITING_COMMAND_2_WORLD_CHANGE) {
			completeCommand2WorldChange(now, "");
		}
	}

	private static void sendCommand(Minecraft client, CommandKind kind) {
		long now = System.currentTimeMillis();
		long nextAllowed = lastAnyCommandSentAtMs + MIN_ANY_COMMAND_INTERVAL_MS;
		if (now < nextAllowed) {
			actionAtMs = nextAllowed;
			return;
		}

		String command = kind == CommandKind.COMMAND_1 ? command1() : command2();
		if (command.isBlank()) {
			stopRuntime();
			FeatureChat.sendPrefixed("Unique Server Hopper", "Stopped because a command is empty.");
			return;
		}

		client.player.connection.sendCommand(command);
		lastAnyCommandSentAtMs = now;
		lastCommand = kind;
		commandSentAtMs = now;
		commandStartServer = lastKnownServer;
		worldCheckLocrawPending = false;
		worldCheckLocrawRequestedAtMs = 0L;
		nextWorldCheckLocrawAtMs = now + Math.max(locrawDelayMs(), MIN_ANY_COMMAND_INTERVAL_MS);

		if (kind == CommandKind.COMMAND_1) {
			state = State.WAITING_COMMAND_1_WORLD_CHANGE;
		} else {
			state = State.WAITING_COMMAND_2_WORLD_CHANGE;
		}
	}

	private static void retryLastCommand(Minecraft client) {
		if (lastCommand == CommandKind.NONE) {
			stopRuntime();
			return;
		}
		sendCommand(client, lastCommand);
	}

	private static void requestInitialLocraw() {
		if (!canSendAnotherCommand()) {
			state = State.WAITING_INITIAL_LOCRAW_READY;
			actionAtMs = nextAllowedCommandAt();
			return;
		}

		long now = System.currentTimeMillis();
		state = State.WAITING_INITIAL_LOCRAW_RESPONSE;
		locrawRequestedAtMs = now;
		lastAnyCommandSentAtMs = now;
		SkyBlockLocationTracker.requestRefreshNow(true);
	}

	private static void requestTrackedLocraw() {
		if (!canSendAnotherCommand()) {
			state = State.WAITING_LOCRAW_READY;
			actionAtMs = nextAllowedCommandAt();
			return;
		}

		long now = System.currentTimeMillis();
		state = State.WAITING_LOCRAW_RESPONSE;
		locrawRequestedAtMs = now;
		lastAnyCommandSentAtMs = now;
		SkyBlockLocationTracker.requestRefreshNow(true);
	}

	private static void requestWorldCheckLocraw() {
		if (!canSendAnotherCommand()) {
			nextWorldCheckLocrawAtMs = nextAllowedCommandAt();
			return;
		}

		long now = System.currentTimeMillis();
		worldCheckLocrawPending = true;
		worldCheckLocrawRequestedAtMs = now;
		nextWorldCheckLocrawAtMs = now + LOCRAW_TIMEOUT_MS;
		lastAnyCommandSentAtMs = now;
		SkyBlockLocationTracker.requestRefreshNow(true);
	}

	private static void handleLocrawResponse(String server) {
		String normalizedServer = safeServer(server);
		if (!normalizedServer.isBlank()) {
			lastKnownServer = normalizedServer;
		}

		long now = System.currentTimeMillis();

		if (state == State.WAITING_INITIAL_LOCRAW_RESPONSE) {
			state = State.WAITING_COMMAND_1_READY;
			actionAtMs = nextAllowedCommandAt();
			locrawRequestedAtMs = 0L;
			return;
		}

		if (state == State.WAITING_LOCRAW_RESPONSE) {
			locrawRequestedAtMs = 0L;
			handleTrackedServer(normalizedServer);
			return;
		}

		if (state == State.WAITING_COMMAND_1_WORLD_CHANGE) {
			worldCheckLocrawPending = false;
			worldCheckLocrawRequestedAtMs = 0L;

			if (hasServerChanged(normalizedServer)) {
				completeCommand1WorldChange(now);
			} else {
				nextWorldCheckLocrawAtMs = now + retryDelayMs();
			}
			return;
		}

		if (state == State.WAITING_COMMAND_2_WORLD_CHANGE) {
			worldCheckLocrawPending = false;
			worldCheckLocrawRequestedAtMs = 0L;

			if (hasServerChanged(normalizedServer)) {
				completeCommand2WorldChange(now, normalizedServer);
			} else {
				nextWorldCheckLocrawAtMs = now + retryDelayMs();
			}
		}
	}

	private static void completeCommand1WorldChange(long now) {
		state = State.WAITING_COMMAND_2_READY;
		actionAtMs = now + retryDelayMs();
		lastCommand = CommandKind.NONE;
		commandSentAtMs = 0L;
		commandStartServer = "";
		clearWorldCheckLocraw();
		FeatureChat.sendPrefixed("Unique Server Hopper", "Command 1 changed world. Sending Command 2.");
	}

	private static void completeCommand2WorldChange(long now, String serverFromLocraw) {
		lastCommand = CommandKind.NONE;
		commandSentAtMs = 0L;
		commandStartServer = "";
		clearWorldCheckLocraw();

		if (serverFromLocraw != null && !serverFromLocraw.isBlank()) {
			handleTrackedServer(serverFromLocraw);
			return;
		}

		state = State.WAITING_LOCRAW_READY;
		actionAtMs = now + locrawDelayMs();
	}

	private static void handleTemporaryFailure(String text) {
		if (!isRunning()) {
			return;
		}

		long now = System.currentTimeMillis();
		boolean commandThrottle = isCommandThrottleMessage(text);
		long delayMs = commandThrottle ? throttleBackoffMs() : retryDelayMs();
		if (commandThrottle) {
			lastAnyCommandSentAtMs = Math.max(lastAnyCommandSentAtMs, now);
		}

		if (commandThrottle && worldCheckLocrawPending && isWaitingForCommandWorldChange()) {
			worldCheckLocrawPending = false;
			worldCheckLocrawRequestedAtMs = 0L;
			nextWorldCheckLocrawAtMs = now + Math.max(delayMs, locrawDelayMs());
			FeatureChat.sendPrefixed("Unique Server Hopper", "World check locraw was throttled. Retrying locraw.");
			return;
		}

		if (state == State.WAITING_INITIAL_LOCRAW_READY || state == State.WAITING_INITIAL_LOCRAW_RESPONSE) {
			state = State.WAITING_INITIAL_LOCRAW_READY;
			actionAtMs = now + delayMs;
			locrawRequestedAtMs = 0L;
			FeatureChat.sendPrefixed("Unique Server Hopper", "Initial locraw was throttled. Retrying locraw.");
			return;
		}

		if (state == State.WAITING_LOCRAW_READY || state == State.WAITING_LOCRAW_RESPONSE) {
			state = State.WAITING_LOCRAW_READY;
			actionAtMs = now + Math.max(delayMs, locrawDelayMs());
			locrawRequestedAtMs = 0L;
			FeatureChat.sendPrefixed("Unique Server Hopper", "Locraw was throttled. Retrying locraw.");
			return;
		}

		CommandKind failedCommand = waitingWorldChangeCommand();
		if (failedCommand == CommandKind.NONE) {
			delayCurrentReadyState(now, delayMs);
			return;
		}

		clearWorldCheckLocraw();
		commandSentAtMs = 0L;
		lastCommand = failedCommand;

		if (failedCommand == CommandKind.COMMAND_1) {
			state = State.WAITING_COMMAND_1_READY;
			actionAtMs = now + delayMs;
			FeatureChat.sendPrefixed("Unique Server Hopper", "Warp failed. Retrying Command 1.");
			return;
		}

		state = State.WAITING_COMMAND_2_READY;
		actionAtMs = now + delayMs;
		FeatureChat.sendPrefixed("Unique Server Hopper", "Warp failed. Retrying Command 2.");
	}

	private static void handleTrackedServer(String server) {
		if (server == null || server.isBlank()) {
			state = State.WAITING_LOCRAW_READY;
			actionAtMs = System.currentTimeMillis() + retryDelayMs();
			return;
		}

		String key = server.toLowerCase(Locale.ROOT);
		boolean unique = SEEN_SERVERS.add(key);

		if (unique) {
			state = State.UNIQUE_WAITING_USER_LEAVE;
			lastCommand = CommandKind.NONE;
			commandSentAtMs = 0L;
			locrawRequestedAtMs = 0L;
			commandStartServer = "";
			clearWorldCheckLocraw();
			queueUniqueSound();
			FeatureChat.sendPrefixed("Unique Server Hopper", "UNIQUE " + server + ". Waiting until you leave this world.");
			return;
		}

		state = State.WAITING_COMMAND_1_READY;
		actionAtMs = System.currentTimeMillis() + retryDelayMs();
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		commandStartServer = "";
		clearWorldCheckLocraw();
		FeatureChat.sendPrefixed("Unique Server Hopper", "NOT UNIQUE " + server + ". Hopping again.");
	}

	private static void queueUniqueSound() {
		int token = ++uniqueSoundToken;
		Thread thread = new Thread(() -> playUniqueSoundSequence(token), "Moissanite Unique Server Sound");
		thread.setDaemon(true);
		thread.start();
	}

	private static void playUniqueSoundSequence(int token) {
		playToneIfCurrent(token, 880.0D, 180);
		sleepSound(90L);
		playToneIfCurrent(token, 1174.66D, 180);
		sleepSound(90L);
		playToneIfCurrent(token, 1567.98D, 280);
	}

	private static void playToneIfCurrent(int token, double frequency, int durationMs) {
		if (token != uniqueSoundToken) {
			return;
		}
		playTone(frequency, durationMs);
	}

	private static void playTone(double frequency, int durationMs) {
		float sampleRate = 44100.0F;
		AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
		int sampleCount = Math.max(1, (int) (durationMs * sampleRate / 1000.0F));
		byte[] buffer = new byte[sampleCount * 2];

		for (int i = 0; i < sampleCount; i++) {
			double progress = i / (double) sampleCount;
			double fade = Math.min(1.0D, Math.min(progress / 0.08D, (1.0D - progress) / 0.12D));
			double value = Math.sin(2.0D * Math.PI * frequency * i / sampleRate);
			short sample = (short) Mth.clamp((int) Math.round(value * fade * 32767.0D), -32767, 32767);
			buffer[i * 2] = (byte) (sample & 255);
			buffer[i * 2 + 1] = (byte) ((sample >> 8) & 255);
		}

		try {
			SourceDataLine line = AudioSystem.getSourceDataLine(format);
			line.open(format);
			line.start();
			line.write(buffer, 0, buffer.length);
			line.drain();
			line.stop();
			line.close();
		} catch (LineUnavailableException | RuntimeException ignored) {
		}
	}

	private static void sleepSound(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}

	private static void stopRuntime() {
		state = State.STOPPED;
		lastCommand = CommandKind.NONE;
		actionAtMs = 0L;
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		nextWorldCheckLocrawAtMs = 0L;
		worldCheckLocrawRequestedAtMs = 0L;
		worldCheckLocrawPending = false;
		commandStartServer = "";
		uniqueSoundToken++;
	}

	private static boolean isWaitingForCommandWorldChange() {
		return state == State.WAITING_COMMAND_1_WORLD_CHANGE || state == State.WAITING_COMMAND_2_WORLD_CHANGE;
	}

	private static CommandKind waitingWorldChangeCommand() {
		return switch (state) {
			case WAITING_COMMAND_1_WORLD_CHANGE -> CommandKind.COMMAND_1;
			case WAITING_COMMAND_2_WORLD_CHANGE -> CommandKind.COMMAND_2;
			default -> CommandKind.NONE;
		};
	}

	private static boolean hasServerChanged(String server) {
		if (server == null || server.isBlank()) {
			return false;
		}
		if (commandStartServer == null || commandStartServer.isBlank()) {
			return false;
		}
		return !server.equalsIgnoreCase(commandStartServer);
	}

	private static boolean canSendAnotherCommand() {
		return System.currentTimeMillis() >= nextAllowedCommandAt();
	}

	private static long nextAllowedCommandAt() {
		return lastAnyCommandSentAtMs + MIN_ANY_COMMAND_INTERVAL_MS;
	}

	private static void clearWorldCheckLocraw() {
		worldCheckLocrawPending = false;
		worldCheckLocrawRequestedAtMs = 0L;
		nextWorldCheckLocrawAtMs = 0L;
	}

	private static void delayCurrentReadyState(long now, long delayMs) {
		long delayedAtMs = now + delayMs;
		switch (state) {
			case WAITING_COMMAND_1_READY,
					WAITING_COMMAND_2_READY,
					WAITING_COMMAND_2_AFTER_UNIQUE_LEAVE,
					WAITING_RETRY -> actionAtMs = Math.max(actionAtMs, delayedAtMs);
			default -> {
			}
		}
	}

	private static boolean isKickMessage(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		return lower.contains("kick occurred in your connection")
				|| lower.contains("you were put in the skyblock lobby");
	}

	private static boolean isTemporaryFailureMessage(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		return lower.contains("dynamic_pool_error")
				|| lower.contains("player_transfer_cooldown")
				|| lower.contains("couldn't warp you")
				|| lower.contains("couldnt warp you")
				|| lower.contains("try again later")
				|| lower.contains("you are sending commands too fast")
				|| lower.contains("please slow down");
	}

	private static boolean isCommandThrottleMessage(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		return lower.contains("player_transfer_cooldown")
				|| lower.contains("you are sending commands too fast")
				|| lower.contains("please slow down");
	}

	private static String command1() {
		return normalizeCommand(UiDefinitions.UNIQUE_SERVER_HOPPER_COMMAND_1.get());
	}

	private static String command2() {
		return normalizeCommand(UiDefinitions.UNIQUE_SERVER_HOPPER_COMMAND_2.get());
	}

	private static String normalizeCommand(String command) {
		if (command == null) {
			return "";
		}
		String value = command.trim();
		while (value.startsWith("/")) {
			value = value.substring(1).trim();
		}
		return value;
	}

	private static String safeServer(String server) {
		return server == null ? "" : server.trim();
	}

	private static long worldEnterDelayMs() {
		Double value = UiDefinitions.UNIQUE_SERVER_HOPPER_WORLD_ENTER_DELAY.get();
		double seconds = value != null && Double.isFinite(value) ? value : 4.5D;
		return Math.round(Mth.clamp(seconds, 0.5D, 60.0D) * 1000.0D);
	}

	private static long retryDelayMs() {
		Double value = UiDefinitions.UNIQUE_SERVER_HOPPER_RETRY_DELAY.get();
		double seconds = value != null && Double.isFinite(value) ? value : 3.0D;
		return Math.round(Mth.clamp(seconds, 0.5D, 60.0D) * 1000.0D);
	}

	private static long throttleBackoffMs() {
		return Math.max(retryDelayMs(), SERVER_THROTTLE_BACKOFF_MS);
	}

	private static long locrawDelayMs() {
		Double value = UiDefinitions.UNIQUE_SERVER_HOPPER_LOCRAW_DELAY.get();
		double seconds = value != null && Double.isFinite(value) ? value : 2.0D;
		return Math.round(Mth.clamp(seconds, 0.5D, 10.0D) * 1000.0D);
	}

	private static String configuredWorldEnterDelayText() {
		Double value = UiDefinitions.UNIQUE_SERVER_HOPPER_WORLD_ENTER_DELAY.get();
		double seconds = value != null && Double.isFinite(value) ? value : 4.5D;
		double clamped = Mth.clamp(seconds, 0.5D, 60.0D);
		return String.format(Locale.ROOT, "%.1f", clamped);
	}

	private static String lastCommandName() {
		return switch (lastCommand) {
			case COMMAND_1 -> "Command 1";
			case COMMAND_2 -> "Command 2";
			default -> "command";
		};
	}

	private static Locraw parseLocraw(Component message) {
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
			if (!root.has("server")) {
				return null;
			}

			return new Locraw(stringValue(root, "server"));
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

	private enum State {
		STOPPED,
		WAITING_INITIAL_LOCRAW_READY,
		WAITING_INITIAL_LOCRAW_RESPONSE,
		WAITING_COMMAND_1_READY,
		WAITING_COMMAND_1_WORLD_CHANGE,
		WAITING_COMMAND_2_READY,
		WAITING_COMMAND_2_AFTER_UNIQUE_LEAVE,
		WAITING_COMMAND_2_WORLD_CHANGE,
		WAITING_LOCRAW_READY,
		WAITING_LOCRAW_RESPONSE,
		WAITING_RETRY,
		UNIQUE_WAITING_USER_LEAVE
	}

	private enum CommandKind {
		NONE,
		COMMAND_1,
		COMMAND_2
	}

	private record Locraw(String server) {
	}
}
