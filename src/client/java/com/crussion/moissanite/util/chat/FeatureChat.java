package com.crussion.moissanite.util.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class FeatureChat {
	private static final String PREFIX_TEXT = "Moissanite";
	private static final String PLAIN_PREFIX = "[" + PREFIX_TEXT + "] ";
	private static final float GRADIENT_HUE_START = 0.83f;
	private static final float GRADIENT_HUE_END = 0.53f;
	private static final float GRADIENT_SATURATION = 0.75f;
	private static final float GRADIENT_BRIGHTNESS = 1.0f;
	private static final long GRADIENT_CYCLE_MS = 3000L;
	private static final int BODY_TEXT_COLOR = 0xAAAAAA;
	private static final int PREFIX_BODY_SIBLING_INDEX = PREFIX_TEXT.length() + 1;

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

		MutableComponent message = createGradientPrefix(System.currentTimeMillis());
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
		if (messages == null || messages.isEmpty()) {
			return Map.of();
		}

		Map<GuiMessage, GuiMessage> replacements = new IdentityHashMap<>();
		ListIterator<GuiMessage> iterator = messages.listIterator();
		while (iterator.hasNext()) {
			GuiMessage message = iterator.next();
			Component refreshedComponent = refreshAnimatedComponent(message == null ? null : message.content(), timeMs);
			if (message == null || refreshedComponent == message.content()) {
				continue;
			}

			GuiMessage refreshedMessage = new GuiMessage(
					message.addedTime(),
					refreshedComponent,
					message.signature(),
					message.tag());
			iterator.set(refreshedMessage);
			replacements.put(message, refreshedMessage);
		}
		return replacements;
	}

	private static Component refreshAnimatedComponent(Component component, long timeMs) {
		if (component == null) {
			return null;
		}

		String plainText = component.getString();
		if (!plainText.startsWith(PLAIN_PREFIX)) {
			return component;
		}

		MutableComponent refreshed = createGradientPrefix(timeMs);
		if (hasStructuredPrefix(component)) {
			List<Component> siblings = component.getSiblings();
			for (int i = PREFIX_BODY_SIBLING_INDEX; i < siblings.size(); i++) {
				refreshed.append(siblings.get(i).copy());
			}
			return refreshed;
		}

		String remainingText = plainText.length() > PLAIN_PREFIX.length() ? plainText.substring(PLAIN_PREFIX.length()) : "";
		if (!remainingText.isEmpty()) {
			refreshed.append(Component.literal(remainingText)
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY_TEXT_COLOR))));
		}
		return refreshed;
	}

	private static boolean hasStructuredPrefix(Component component) {
		List<Component> siblings = component.getSiblings();
		if (siblings.size() < PREFIX_BODY_SIBLING_INDEX) {
			return false;
		}
		for (int i = 0; i < PREFIX_TEXT.length(); i++) {
			if (!String.valueOf(PREFIX_TEXT.charAt(i)).equals(siblings.get(i).getString())) {
				return false;
			}
		}
		return "] ".equals(siblings.get(PREFIX_BODY_SIBLING_INDEX - 1).getString());
	}

	private static MutableComponent createGradientPrefix(long timeMs) {
		float shift = (timeMs % GRADIENT_CYCLE_MS) / (float) GRADIENT_CYCLE_MS;

		MutableComponent prefix = Component.literal("[").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x888888)));

		int textLen = PREFIX_TEXT.length();
		for (int i = 0; i < textLen; i++) {
			float position = textLen > 1 ? (float) i / (textLen - 1) : 0.0f;
			float hue = GRADIENT_HUE_START + (GRADIENT_HUE_END - GRADIENT_HUE_START) * position;
			hue = ((hue + shift) % 1.0f + 1.0f) % 1.0f;
			int rgb = hsbToRgb(hue, GRADIENT_SATURATION, GRADIENT_BRIGHTNESS) & 0xFFFFFF;
			prefix.append(Component.literal(String.valueOf(PREFIX_TEXT.charAt(i)))
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
		}

		prefix.append(Component.literal("] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x888888))));
		return prefix;
	}

	private static int hsbToRgb(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float) Math.floor(hue)) * 6.0f;
			float f = h - (float) Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - saturation * (1.0f - f));
			switch ((int) h) {
				case 0 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (t * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
				case 1 -> { r = (int) (q * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
				case 2 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (t * 255.0f + 0.5f); }
				case 3 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (q * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
				case 4 -> { r = (int) (t * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
				case 5 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (q * 255.0f + 0.5f); }
			}
		}
		return (r << 16) | (g << 8) | b;
	}
}
