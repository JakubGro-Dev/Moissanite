package com.crussion.moissanite.features.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class StarItemstack {
	private static final int MAX_STAR_STACK = 15;
	private static final String STAR_GLYPHS = "\\u272A\\u2605\\u2B50\\u272F\\u2736";
	private static final String MASTER_STAR_GLYPHS = "\\u278A-\\u278E\\u2776-\\u277A\\u2460-\\u2464";
	private static final Pattern STAR_COUNT_DIGITS =
			Pattern.compile("(?<!\\d)(1[0-5]|[1-9])\\s*[" + STAR_GLYPHS + "]");
	private static final Pattern BASIC_STAR_RUN =
			Pattern.compile("([" + STAR_GLYPHS + "]{1,64})");
	private static final Pattern MASTER_STAR_RUN =
			Pattern.compile("([" + MASTER_STAR_GLYPHS + "]{1,16})");
	private static final String[] STAR_LEVEL_KEYS = {
			"upgrade_level",
			"dungeon_item_level",
			"master_stars",
			"kuudra_stars",
			"stars",
			"star_level"
	};

	private StarItemstack() {
	}

	public static boolean isEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STAR_ITEMSTACK.get());
	}

	public static String resolveCountText(ItemStack stack) {
		if (!isEnabled() || stack == null || stack.isEmpty()) {
			return null;
		}
		int stars = extractStarCount(stack);
		if (stars <= 0) {
			return null;
		}
		return Integer.toString(Math.min(stars, MAX_STAR_STACK));
	}

	private static int extractStarCount(ItemStack stack) {
		int nbtStars = extractStarCountFromNbt(stack);
		if (nbtStars > 0) {
			return nbtStars;
		}

		String name = TextNormalizer.stripFormattingCodes(stack.getHoverName() == null ? "" : stack.getHoverName().getString());
		if (name == null || name.isBlank()) {
			return -1;
		}

		Matcher digitMatcher = STAR_COUNT_DIGITS.matcher(name);
		if (digitMatcher.find()) {
			try {
				return Integer.parseInt(digitMatcher.group(1));
			} catch (NumberFormatException ignored) {
			}
		}

		int bestBasicRun = longestRun(name, BASIC_STAR_RUN);
		int bestMasterRun = longestRun(name, MASTER_STAR_RUN);
		int bestMasterValue = highestMasterStarValue(name);

		int best = Math.max(bestBasicRun, bestMasterValue);
		if (bestBasicRun >= 5) {
			if (bestMasterRun > 0) {
				best = Math.max(best, bestBasicRun + bestMasterRun);
			}
			if (bestMasterValue > 0) {
				best = Math.max(best, bestBasicRun + bestMasterValue);
			}
		}
		return best;
	}

	private static int extractStarCountFromNbt(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null || customData.isEmpty()) {
			return -1;
		}

		CompoundTag root = customData.copyTag();
		if (root == null || root.isEmpty()) {
			return -1;
		}

		int best = -1;
		best = Math.max(best, extractStarCountFromTag(root));
		best = Math.max(best,
				root.getCompound("ExtraAttributes").map(StarItemstack::extractStarCountFromTag).orElse(-1));
		best = Math.max(best,
				root.getCompound("extra_attributes").map(StarItemstack::extractStarCountFromTag).orElse(-1));
		return best;
	}

	private static int extractStarCountFromTag(CompoundTag tag) {
		if (tag == null || tag.isEmpty()) {
			return -1;
		}

		int best = -1;
		for (String key : STAR_LEVEL_KEYS) {
			int value = tag.getInt(key).orElse(0);
			if (value <= 0) {
				continue;
			}
			best = Math.max(best, Math.min(value, MAX_STAR_STACK));
		}
		return best;
	}

	private static int longestRun(String name, Pattern pattern) {
		Matcher matcher = pattern.matcher(name);
		int bestRun = -1;
		while (matcher.find()) {
			bestRun = Math.max(bestRun, matcher.group(1).length());
		}
		return bestRun;
	}

	private static int highestMasterStarValue(String text) {
		if (text == null || text.isBlank()) {
			return -1;
		}
		int best = -1;
		for (int i = 0; i < text.length(); i++) {
			best = Math.max(best, toMasterStarValue(text.charAt(i)));
		}
		return best;
	}

	private static int toMasterStarValue(char symbol) {
		return switch (symbol) {
			case '\u278A', '\u2776', '\u2460' -> 1;
			case '\u278B', '\u2777', '\u2461' -> 2;
			case '\u278C', '\u2778', '\u2462' -> 3;
			case '\u278D', '\u2779', '\u2463' -> 4;
			case '\u278E', '\u277A', '\u2464' -> 5;
			default -> -1;
		};
	}
}
