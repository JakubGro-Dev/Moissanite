package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.kuudra.KuudraPhase;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.MagmaCube;

public final class KuudraRendDamage {
	private static boolean initialized;
	private static int kuudraLastHp = 24_999;

	private KuudraRendDamage() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(KuudraRendDamage::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> kuudraLastHp = 24_999);
	}

	private static void onClientTick(Minecraft client) {
		if (!Boolean.TRUE.equals(UiDefinitions.REND_DAMAGE.get())) {
			return;
		}
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")) {
			return;
		}
		if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_DPS || client.player.getY() > 30) {
			return;
		}

		MagmaCube boss = KuudraPhaseTracker.getKuudraEntity();
		if (boss == null) {
			return;
		}

		int kuudraHp = (int) boss.getHealth();
		if (kuudraHp > 25_000) {
			return;
		}

		int diff = kuudraLastHp - kuudraHp;
		if (diff > 1_666) {
			String damageColor = getDamageColor(diff);
			String formattedDamage = parseToShorthandNumber(diff * 9600);
			long phaseTime = KuudraPhase.KILL.getTime(System.currentTimeMillis());
			String formattedTime = formatElapsedTimeMs(phaseTime);
			FeatureChat.sendPrefixed(
					"Rend",
					ChatFormatting.WHITE + "Someone pulled for "
							+ damageColor + formattedDamage
							+ ChatFormatting.WHITE + " damage at "
							+ ChatFormatting.GREEN + formattedTime
							+ ChatFormatting.WHITE + ".");
		}

		kuudraLastHp = kuudraHp;
	}

	private static String getDamageColor(int damage) {
		if (damage <= 4_166) {
			return ChatFormatting.RED.toString();
		}
		if (damage <= 7_291) {
			return ChatFormatting.YELLOW.toString();
		}
		return ChatFormatting.GREEN.toString();
	}

	private static String parseToShorthandNumber(int number) {
		if (number >= 1_000_000_000) {
			return String.format("%.2fB", number / 1_000_000_000.0);
		}
		if (number >= 1_000_000) {
			return String.format("%.2fM", number / 1_000_000.0);
		}
		if (number >= 1_000) {
			return String.format("%.2fk", number / 1_000.0);
		}
		return String.valueOf(number);
	}

	private static String formatElapsedTimeMs(long timeMs) {
		long seconds = timeMs / 1000;
		long centiseconds = (timeMs % 1000) / 10;
		return String.format("%d.%02ds", seconds, centiseconds);
	}
}
