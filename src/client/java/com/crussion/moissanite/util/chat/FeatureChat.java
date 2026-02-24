package com.crussion.moissanite.util.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class FeatureChat {
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
		client.player.displayClientMessage(Component.literal(text), false);
	}

	public static void sendPrefixed(String featureName, String text) {
		if (featureName == null || featureName.isBlank()) {
			send(text);
			return;
		}
		send(featureName + ": " + (text == null ? "" : text));
	}
}
