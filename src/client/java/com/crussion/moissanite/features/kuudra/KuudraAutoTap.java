package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;
import com.crussion.moissanite.util.tick.TickTaskScheduler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class KuudraAutoTap {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String EATEN_TRIGGER = "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!";
	private static final String COMMAND_PREFIX = "getfromsacks toxic_arrow_poison ";
	private static final int DELAY_TICKS = 10;
	private static final int DEFAULT_AMOUNT = 16;
	private static final int MIN_AMOUNT = 1;
	private static final int MAX_AMOUNT = 64;

	private static boolean initialized;
	private static boolean firedThisRun;

	private KuudraAutoTap() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetState());
	}

	public static void onSystemChat(Component message) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_TAP.get())) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		if (firedThisRun) {
			return;
		}

		String msg = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString());
		if (!msg.contains(EATEN_TRIGGER)) {
			return;
		}

		firedThisRun = true;
		int amount = configuredAmount();
		TickTaskScheduler.schedule(DELAY_TICKS, () -> sendGetFromSack(amount));
	}

	private static void sendGetFromSack(int amount) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.player.connection == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		client.player.connection.sendCommand(COMMAND_PREFIX + amount);
	}

	private static int configuredAmount() {
		Double value = UiDefinitions.AUTO_TAP_AMOUNT.get();
		int configured = value != null && Double.isFinite(value) ? (int) Math.round(value) : DEFAULT_AMOUNT;
		return Mth.clamp(configured, MIN_AMOUNT, MAX_AMOUNT);
	}

	private static void resetState() {
		firedThisRun = false;
	}
}
