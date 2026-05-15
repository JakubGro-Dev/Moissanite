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
	private static final Set<String> SEEN_SERVERS = new HashSet<>();

	private static boolean initialized;
	private static boolean startedByUser;
	private static State state = State.STOPPED;
	private static CommandKind pendingCommand = CommandKind.NONE;
	private static long actionAtMs;
	private static long commandSentAtMs;
	private static long locrawRequestedAtMs;
	private static String lastKnownServer = "";
	private static String commandStartServer = "";
	private static String uniqueWaitServer = "";
	private static int uniqueSoundToken;
	private static Object observedLevel;

	private UniqueServerHopper() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(UniqueServerHopper::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) ->
				onWorldChangeDetected(client != null && client.level != null ? client.level : world, true));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stopRuntime());
	}

	public static void start() {
		stopRuntime();

		if (!Boolean.TRUE.equals(UiDefinitions.UNIQUE_SERVER_HOPPER.get())) {
			return;
		}
		if (command1().isBlank() || command2().isBlank()) {
			return;
		}

		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		lastKnownServer = location == null ? "" : safeServer(location.server());

		startedByUser = true;
		state = State.WAITING_INITIAL_LOCRAW_READY;
		pendingCommand = CommandKind.NONE;
		actionAtMs = 0L;
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		commandStartServer = "";
		uniqueWaitServer = "";
		observedLevel = currentClientLevel();
	}

	public static void stop() {
		stopRuntime();
	}

	public static void cleanCache() {
		SEEN_SERVERS.clear();
	}

	public static boolean isRunning() {
		return startedByUser && state != State.STOPPED;
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
		if (!isRunning()) {
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
			if (state != State.STOPPED) {
				stopRuntime();
			}
			return;
		}
		if (client == null || client.player == null || client.level == null || client.player.connection == null) {
			return;
		}

		detectMissedWorldChange(client);

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
				if (locrawRequestedAtMs > 0L && now - locrawRequestedAtMs >= retryDelayMs()) {
					state = State.WAITING_INITIAL_LOCRAW_READY;
					actionAtMs = now;
					locrawRequestedAtMs = 0L;
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
				if (locrawRequestedAtMs > 0L && now - locrawRequestedAtMs >= retryDelayMs()) {
					state = State.WAITING_LOCRAW_READY;
					actionAtMs = now;
					locrawRequestedAtMs = 0L;
				}
			}
			case WAITING_UNIQUE_LEAVE_LOCRAW_READY -> {
				if (now >= actionAtMs) {
					requestUniqueLeaveLocraw();
				}
			}
			case WAITING_UNIQUE_LEAVE_LOCRAW_RESPONSE -> {
				if (locrawRequestedAtMs > 0L && now - locrawRequestedAtMs >= retryDelayMs()) {
					state = State.UNIQUE_WAITING_USER_LEAVE;
					actionAtMs = 0L;
					locrawRequestedAtMs = 0L;
				}
			}
			default -> {
			}
		}
	}

	private static void handleWorldChangeWaitTick(long now) {
		if (commandSentAtMs <= 0L || now - commandSentAtMs < retryDelayMs()) {
			return;
		}

		CommandKind command = pendingCommand != CommandKind.NONE ? pendingCommand : waitingWorldChangeCommand();
		if (command == CommandKind.NONE) {
			stopRuntime();
			return;
		}

		scheduleCommandRetry(now, 0L, command);
	}

	private static void detectMissedWorldChange(Minecraft client) {
		Object level = client.level;
		if (observedLevel == null) {
			observedLevel = level;
			return;
		}

		if (observedLevel != level) {
			onWorldChangeDetected(level, false);
		}
	}

	private static void onWorldChangeDetected(Object level, boolean force) {
		if (!isRunning()) {
			observedLevel = level;
			return;
		}

		if (!force && level != null && observedLevel == level) {
			return;
		}

		observedLevel = level;
		long now = System.currentTimeMillis();

		if (state == State.UNIQUE_WAITING_USER_LEAVE) {
			scheduleUniqueLeaveCheck(now);
			return;
		}

		if (state == State.WAITING_COMMAND_1_WORLD_CHANGE) {
			completeCommand1WorldChange(now);
			return;
		}

		if (state == State.WAITING_COMMAND_2_WORLD_CHANGE) {
			completeCommand2WorldChange(now, "");
			return;
		}

	}

	private static void sendCommand(Minecraft client, CommandKind kind) {
		if (!canRunAutomation()) {
			stopRuntime();
			return;
		}

		String command = kind == CommandKind.COMMAND_1 ? command1() : command2();
		if (command.isBlank()) {
			stopRuntime();
			return;
		}

		client.player.connection.sendCommand(command);
		pendingCommand = kind;
		commandSentAtMs = System.currentTimeMillis();
		commandStartServer = lastKnownServer;

		if (kind == CommandKind.COMMAND_1) {
			state = State.WAITING_COMMAND_1_WORLD_CHANGE;
		} else {
			state = State.WAITING_COMMAND_2_WORLD_CHANGE;
		}
	}

	private static void requestInitialLocraw() {
		if (!canRunAutomation()) {
			stopRuntime();
			return;
		}

		long now = System.currentTimeMillis();
		state = State.WAITING_INITIAL_LOCRAW_RESPONSE;
		locrawRequestedAtMs = now;
		SkyBlockLocationTracker.requestRefreshNow(true);
	}

	private static void requestTrackedLocraw() {
		if (!canRunAutomation()) {
			stopRuntime();
			return;
		}

		long now = System.currentTimeMillis();
		state = State.WAITING_LOCRAW_RESPONSE;
		locrawRequestedAtMs = now;
		SkyBlockLocationTracker.requestRefreshNow(true);
	}

	private static void requestUniqueLeaveLocraw() {
		if (!canRunAutomation()) {
			stopRuntime();
			return;
		}

		long now = System.currentTimeMillis();
		state = State.WAITING_UNIQUE_LEAVE_LOCRAW_RESPONSE;
		locrawRequestedAtMs = now;
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
			actionAtMs = now;
			locrawRequestedAtMs = 0L;
			return;
		}

		if (state == State.WAITING_LOCRAW_RESPONSE) {
			locrawRequestedAtMs = 0L;
			handleTrackedServer(normalizedServer);
			return;
		}

		if (state == State.WAITING_UNIQUE_LEAVE_LOCRAW_RESPONSE) {
			locrawRequestedAtMs = 0L;
			if (hasLeftUniqueServer(normalizedServer)) {
				completeUniqueLeave(now);
			} else {
				state = State.UNIQUE_WAITING_USER_LEAVE;
				actionAtMs = 0L;
			}
			return;
		}

		if (state == State.WAITING_COMMAND_1_WORLD_CHANGE) {
			if (hasServerChanged(normalizedServer)) {
				completeCommand1WorldChange(now);
			}
			return;
		}

		if (state == State.WAITING_COMMAND_2_WORLD_CHANGE) {
			if (hasServerChanged(normalizedServer)) {
				completeCommand2WorldChange(now, normalizedServer);
			}
		}
	}

	private static void completeUniqueLeave(long now) {
		state = State.WAITING_COMMAND_2_AFTER_UNIQUE_LEAVE;
		pendingCommand = CommandKind.NONE;
		actionAtMs = now + worldEnterDelayMs();
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		commandStartServer = "";
		uniqueWaitServer = "";
	}

	private static void completeCommand1WorldChange(long now) {
		state = State.WAITING_COMMAND_2_READY;
		pendingCommand = CommandKind.NONE;
		actionAtMs = now + worldEnterDelayMs();
		commandSentAtMs = 0L;
		commandStartServer = "";
		uniqueWaitServer = "";
	}

	private static void completeCommand2WorldChange(long now, String serverFromLocraw) {
		pendingCommand = CommandKind.NONE;
		commandSentAtMs = 0L;
		commandStartServer = "";
		uniqueWaitServer = "";

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
		long delayMs = retryDelayMs();

		if (state == State.WAITING_INITIAL_LOCRAW_READY || state == State.WAITING_INITIAL_LOCRAW_RESPONSE) {
			state = State.WAITING_INITIAL_LOCRAW_READY;
			actionAtMs = now + delayMs;
			locrawRequestedAtMs = 0L;
			return;
		}

		if (state == State.WAITING_LOCRAW_READY || state == State.WAITING_LOCRAW_RESPONSE) {
			state = State.WAITING_LOCRAW_READY;
			actionAtMs = now + delayMs;
			locrawRequestedAtMs = 0L;
			return;
		}

		CommandKind failedCommand = waitingWorldChangeCommand();
		if (failedCommand == CommandKind.NONE) {
			delayCurrentReadyState(now, delayMs);
			return;
		}

		scheduleCommandRetry(now, delayMs, failedCommand);
	}

	private static void scheduleCommandRetry(long now, long delayMs, CommandKind command) {
		pendingCommand = CommandKind.NONE;
		commandSentAtMs = 0L;
		uniqueWaitServer = "";
		actionAtMs = now + delayMs;

		if (command == CommandKind.COMMAND_1) {
			state = State.WAITING_COMMAND_1_READY;
			return;
		}

		if (command == CommandKind.COMMAND_2) {
			state = State.WAITING_COMMAND_2_READY;
		}
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
			pendingCommand = CommandKind.NONE;
			commandSentAtMs = 0L;
			locrawRequestedAtMs = 0L;
			commandStartServer = "";
			uniqueWaitServer = server;
			queueUniqueSound();
			FeatureChat.sendPrefixed("Unique Server Hopper", "UNIQUE FOUND " + server);
			return;
		}

		state = State.WAITING_COMMAND_1_READY;
		pendingCommand = CommandKind.NONE;
		actionAtMs = System.currentTimeMillis();
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		commandStartServer = "";
		uniqueWaitServer = "";
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
		startedByUser = false;
		state = State.STOPPED;
		pendingCommand = CommandKind.NONE;
		actionAtMs = 0L;
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		commandStartServer = "";
		uniqueWaitServer = "";
		observedLevel = currentClientLevel();
		uniqueSoundToken++;
	}

	private static boolean isWaitingForCommandWorldChange() {
		return state == State.WAITING_COMMAND_1_WORLD_CHANGE || state == State.WAITING_COMMAND_2_WORLD_CHANGE;
	}

	private static boolean canRunAutomation() {
		return isRunning() && Boolean.TRUE.equals(UiDefinitions.UNIQUE_SERVER_HOPPER.get());
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

	private static boolean hasLeftUniqueServer(String server) {
		if (server == null || server.isBlank()) {
			return false;
		}
		if (uniqueWaitServer == null || uniqueWaitServer.isBlank()) {
			return false;
		}
		return !server.equalsIgnoreCase(uniqueWaitServer);
	}

	private static void scheduleUniqueLeaveCheck(long now) {
		state = State.WAITING_UNIQUE_LEAVE_LOCRAW_READY;
		pendingCommand = CommandKind.NONE;
		actionAtMs = now + locrawDelayMs();
		commandSentAtMs = 0L;
		locrawRequestedAtMs = 0L;
		commandStartServer = "";
	}

	private static void delayCurrentReadyState(long now, long delayMs) {
		switch (state) {
			case WAITING_COMMAND_1_READY,
					WAITING_COMMAND_2_READY,
					WAITING_COMMAND_2_AFTER_UNIQUE_LEAVE -> actionAtMs = now + delayMs;
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
		double seconds = value != null && Double.isFinite(value) ? value : 2.0D;
		return Math.round(Mth.clamp(seconds, 0.5D, 60.0D) * 1000.0D);
	}

	private static long locrawDelayMs() {
		Double value = UiDefinitions.UNIQUE_SERVER_HOPPER_LOCRAW_DELAY.get();
		double seconds = value != null && Double.isFinite(value) ? value : 1.2D;
		return Math.round(Mth.clamp(seconds, 0.2D, 10.0D) * 1000.0D);
	}

	private static Object currentClientLevel() {
		Minecraft client = Minecraft.getInstance();
		return client == null ? null : client.level;
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
		WAITING_UNIQUE_LEAVE_LOCRAW_READY,
		WAITING_UNIQUE_LEAVE_LOCRAW_RESPONSE,
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
