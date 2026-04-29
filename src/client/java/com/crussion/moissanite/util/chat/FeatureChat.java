package com.crussion.moissanite.util.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Map;

public final class FeatureChat {
	private static final String PREFIX_TEXT = "Moissanite";
	private static final int BODY_TEXT_COLOR = 0xAAAAAA;
	private static final int BRACKET_COLOR = 0x777A85;

	private static final int[] PREFIX_COLORS = {
			0xD66BFF,
			0xC46FFF,
			0xB174FF,
			0x9A7BFF,
			0x8285FF,
			0x6A91FF,
			0x579FFF,
			0x4AAEFF,
			0x45C0FF,
			0x4EEBFF
	};

	private FeatureChat() {
	}

	public static void send(String text) {
		if (text == null || text.isBlank()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		if (!client.isSameThread()) {
			String scheduledText = text;
			client.execute(() -> send(scheduledText));
			return;
		}

		if (client.player == null) {
			return;
		}

		MutableComponent message = createStaticPrefix();
		message.append(Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY_TEXT_COLOR))));
		client.player.displayClientMessage(message, false);
	}

	public static void sendPrefixed(String featureName, String text) {
		if (featureName == null || featureName.isBlank()) {
			send(text);
			return;
		}

		send(featureName + ": " + (text == null ? "" : text));
	}

	public static Map<GuiMessage, GuiMessage> refreshAnimatedMessages(List<GuiMessage> messages, long timeMs) {
		return Map.of();
	}

	private static MutableComponent createStaticPrefix() {
		MutableComponent prefix = Component.literal("[")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BRACKET_COLOR)));

		for (int i = 0; i < PREFIX_TEXT.length(); i++) {
			int color = PREFIX_COLORS[Math.min(i, PREFIX_COLORS.length - 1)];
			prefix.append(Component.literal(String.valueOf(PREFIX_TEXT.charAt(i)))
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
		}

		prefix.append(Component.literal("] ")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BRACKET_COLOR))));

		return prefix;
	}
}