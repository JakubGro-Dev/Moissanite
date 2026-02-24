package com.crussion.moissanite.util.scoreboard;

import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class ScoreboardAreaMatcher {
	private static final long FALSE_DEBUG_COOLDOWN_MS = 3_000L;
	private static final int FALSE_DEBUG_MAX_LENGTH = 280;
	private static long lastFalseDebugAtMs;

	private ScoreboardAreaMatcher() {
	}

	public static boolean isInArea(String areaNameInScoreboard) {
		String needle = TextNormalizer.normalize(areaNameInScoreboard);
		if (needle.isBlank()) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return false;
		}

		Scoreboard scoreboard = client.level.getScoreboard();
		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (sidebar == null) {
			return false;
		}

		String haystack = normalizedSidebarText(scoreboard, sidebar);
		boolean matched = haystack.contains(needle);
		// if (!matched) {
		// 	sendFalseDebug(areaNameInScoreboard);
		// }
		return matched;
	}

	public static String sidebarContentsForDebug() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return "<no client world>";
		}

		Scoreboard scoreboard = client.level.getScoreboard();
		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (sidebar == null) {
			return "<no sidebar objective>";
		}

		StringBuilder builder = new StringBuilder();
		appendRaw(builder, sidebar.getDisplayName().getString());
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
			if (entry == null || entry.isHidden()) {
				continue;
			}
			appendRaw(builder, sidebarLine(scoreboard, entry));
		}
		return builder.length() == 0 ? "<sidebar empty>" : builder.toString();
	}

	private static String normalizedSidebarText(Scoreboard scoreboard, Objective sidebar) {
		StringBuilder builder = new StringBuilder();
		appendNormalized(builder, sidebar.getDisplayName().getString());
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
			if (entry == null || entry.isHidden()) {
				continue;
			}
			appendNormalized(builder, sidebarLine(scoreboard, entry));
		}
		return builder.toString();
	}

	private static void appendNormalized(StringBuilder builder, String rawText) {
		String normalized = TextNormalizer.normalize(rawText);
		if (normalized.isBlank()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(' ');
		}
		builder.append(normalized);
	}

	private static void appendRaw(StringBuilder builder, String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(" | ");
		}
		builder.append(rawText);
	}

	private static void sendFalseDebug(String areaNameInScoreboard) {
		long now = System.currentTimeMillis();
		if (now - lastFalseDebugAtMs < FALSE_DEBUG_COOLDOWN_MS) {
			return;
		}
		lastFalseDebugAtMs = now;

		String sidebar = sidebarContentsForDebug();
		if (sidebar.length() > FALSE_DEBUG_MAX_LENGTH) {
			sidebar = sidebar.substring(0, FALSE_DEBUG_MAX_LENGTH) + "...";
		}
		FeatureChat.sendPrefixed(
				"ScoreboardAreaMatcher",
				"isInArea false for '" + (areaNameInScoreboard == null ? "" : areaNameInScoreboard) + "'. Sidebar: " + sidebar);
	}

	private static String sidebarLine(Scoreboard scoreboard, PlayerScoreEntry entry) {
		if (entry.display() != null) {
			return entry.display().getString();
		}
		String owner = entry.owner();
		PlayerTeam team = scoreboard.getPlayersTeam(owner);
		return PlayerTeam.formatNameForTeam(team, Component.literal(owner)).getString();
	}
}
