package com.crussion.moissanite.util.text;

import java.util.Locale;

import net.minecraft.ChatFormatting;

public final class TextNormalizer {
	private TextNormalizer() {
	}

	public static String stripFormattingCodes(String input) {
		if (input == null) {
			return "";
		}
		String stripped = ChatFormatting.stripFormatting(input);
		String base = stripped == null ? input : stripped;
		return stripSectionCodes(base);
	}

	public static String normalize(String input) {
		String withoutSectionCodes = stripFormattingCodes(input);
		return withoutSectionCodes
				.replace('\u00A0', ' ')
				.replace('\u2019', '\'')
				.replace('\u2018', '\'')
				.replace('\u02BC', '\'')
				.replace('\u2032', '\'')
				.replace('\uFF07', '\'')
				.trim()
				.toLowerCase(Locale.ROOT);
	}

	public static String stripSectionCodes(String input) {
		if (input == null || input.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '\u00A7') {
				if (i + 1 < input.length()) {
					i++;
				}
				continue;
			}
			builder.append(c);
		}
		return builder.toString();
	}
}
