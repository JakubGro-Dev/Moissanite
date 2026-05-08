package com.crussion.moissanite.util.chat;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.text.TextNormalizer;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;

public final class SystemChatFilter {
	private static final String BLOCKS_IN_THE_WAY = "There are blocks in the way!";
	private static final Pattern IMPLOSION_DAMAGE_PATTERN = Pattern
			.compile("^Your Implosion hit");
	private static final Pattern ABILITY_COOLDOWN_PATTERN = Pattern
			.compile("^This ability is on cooldown for [0-9]+(?:\\.[0-9]+)?s\\.$");

	private SystemChatFilter() {
	}

	public static boolean shouldHide(Component message) {
		String text = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString()).trim();
		if (text.isEmpty()) {
			return false;
		}
		if (Boolean.TRUE.equals(UiDefinitions.DISABLE_BLOCKS_IN_THE_WAY.get()) && BLOCKS_IN_THE_WAY.equals(text)) {
			return true;
		}
		if (Boolean.TRUE.equals(UiDefinitions.DISABLE_IMPLOSION_DAMAGE.get())
				&& IMPLOSION_DAMAGE_PATTERN.matcher(text).find()) {
			return true;
		}
		if (Boolean.TRUE.equals(UiDefinitions.DISABLE_ABILITY_COOLDOWN.get())
				&& ABILITY_COOLDOWN_PATTERN.matcher(text).matches()) {
			return true;
		}
		return SkyBlockLocationTracker.shouldHideSystemChat(message);
	}
}
