package com.crussion.moissanite.features.dungeons.terminals;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.text.TextNormalizer;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class TerminalSupport {
	static final int[] COLOR_ORDER = new int[] {14, 1, 4, 13, 11};

	static final int[] NUMBERS_ALLOWED_SLOTS = new int[] {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
	static final int[] COLORS_ALLOWED_SLOTS = new int[] {
			10, 11, 12, 13, 14, 15, 16,
			19, 20, 21, 22, 23, 24, 25,
			28, 29, 30, 31, 32, 33, 34,
			37, 38, 39, 40, 41, 42, 43};
	static final int[] RUBIX_ALLOWED_SLOTS = new int[] {12, 13, 14, 21, 22, 23, 30, 31, 32};
	static final int[] REDGREEN_ALLOWED_SLOTS = new int[] {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
	static final int[] MELODY_WRONG_BUTTON_SLOTS = new int[] {16, 25, 34, 43};

	static final Map<String, String> COLOR_REPLACEMENTS = Map.ofEntries(
			Map.entry("light gray", "silver"),
			Map.entry("wool", "white"),
			Map.entry("bone", "white"),
			Map.entry("ink", "black"),
			Map.entry("lapis", "blue"),
			Map.entry("cocoa", "brown"),
			Map.entry("dandelion", "yellow"),
			Map.entry("rose", "red"),
			Map.entry("cactus", "green"));

	private TerminalSupport() {
	}

	static void sendWindowClick(int windowId, int slot, int clickType) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getConnection() == null || windowId < 0 || slot < 0) {
			return;
		}
		client.getConnection().send(new ServerboundContainerClickPacket(
				windowId,
				0,
				(short) slot,
				(byte) clickType,
				ClickType.PICKUP,
				new Int2ObjectOpenHashMap<>(),
				HashedStack.EMPTY));
	}

	static int calcIndex(int index) {
		return Math.floorMod(index, COLOR_ORDER.length);
	}

	static int colorIndex(int color) {
		for (int i = 0; i < COLOR_ORDER.length; i++) {
			if (COLOR_ORDER[i] == color) {
				return i;
			}
		}
		return -1;
	}

	static boolean contains(int[] values, int value) {
		for (int candidate : values) {
			if (candidate == value) {
				return true;
			}
		}
		return false;
	}

	static String fixColorItemName(String itemName) {
		if (itemName == null) {
			return "";
		}
		String result = itemName;
		for (Map.Entry<String, String> replacement : COLOR_REPLACEMENTS.entrySet()) {
			String from = replacement.getKey();
			String to = replacement.getValue();
			if (result.startsWith(from)) {
				result = to + result.substring(from.length());
			}
		}
		return result;
	}

	static String stripFormatting(Component component) {
		return component == null ? "" : stripFormatting(component.getString());
	}

	static String stripFormatting(String text) {
		return TextNormalizer.stripFormattingCodes(text == null ? "" : text);
	}

	static int intValue(Double value, int defaultValue) {
		if (value == null || !Double.isFinite(value)) {
			return defaultValue;
		}
		return (int) Math.round(value);
	}

	static double doubleValue(Double value, double defaultValue) {
		if (value == null || !Double.isFinite(value)) {
			return defaultValue;
		}
		return value;
	}

	static double clampDouble(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	static int parseInt(String value, int defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	static int rgbaToArgb(double r, double g, double b, double a) {
		int rr = (int) clampDouble(Math.round(r), 0, 255);
		int gg = (int) clampDouble(Math.round(g), 0, 255);
		int bb = (int) clampDouble(Math.round(b), 0, 255);
		int aa = (int) clampDouble(Math.round(a), 0, 255);
		return (aa << 24) | (rr << 16) | (gg << 8) | bb;
	}

	static int legacyItemId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return -1;
		}
		String path = itemPath(stack);
		if (path.endsWith("_stained_glass_pane")) {
			return 160;
		}
		return -1;
	}

	static int legacyMetadata(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return -1;
		}
		String path = itemPath(stack);
		if (!path.endsWith("_stained_glass_pane")) {
			return -1;
		}
		String color = path.substring(0, path.length() - "_stained_glass_pane".length());
		return switch (color) {
			case "white" -> 0;
			case "orange" -> 1;
			case "magenta" -> 2;
			case "light_blue" -> 3;
			case "yellow" -> 4;
			case "lime" -> 5;
			case "pink" -> 6;
			case "gray" -> 7;
			case "light_gray" -> 8;
			case "cyan" -> 9;
			case "purple" -> 10;
			case "blue" -> 11;
			case "brown" -> 12;
			case "green" -> 13;
			case "red" -> 14;
			case "black" -> 15;
			default -> -1;
		};
	}

	private static String itemPath(ItemStack stack) {
		if (stack == null || stack.isEmpty() || stack.getItem() == null) {
			return "";
		}
		var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id == null ? "" : id.getPath();
	}

	static String hypixelItemId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null || customData.isEmpty()) {
			return "";
		}
		CompoundTag root = customData.copyTag();
		if (root == null || root.isEmpty()) {
			return "";
		}

		String id = extraAttributesId(root);
		if (!id.isBlank()) {
			return id;
		}

		Optional<CompoundTag> tagTag = root.getCompound("tag");
		if (tagTag.isPresent()) {
			id = extraAttributesId(tagTag.get());
			if (!id.isBlank()) {
				return id;
			}
		}

		return "";
	}

	private static String extraAttributesId(CompoundTag tag) {
		if (tag == null || tag.isEmpty()) {
			return "";
		}

		Optional<CompoundTag> extra = tag.getCompound("ExtraAttributes");
		if (extra.isPresent()) {
			String id = extra.get().getString("id").orElse("");
			if (!id.isBlank()) {
				return id;
			}
		}

		Optional<CompoundTag> extraLower = tag.getCompound("extra_attributes");
		if (extraLower.isPresent()) {
			String id = extraLower.get().getString("id").orElse("");
			if (!id.isBlank()) {
				return id;
			}
		}

		return "";
	}

	static void restoreKeyState(Minecraft client, KeyMapping mapping) {
		if (client == null || mapping == null || client.getWindow() == null) {
			return;
		}
		String keyName = mapping.saveString();
		if (keyName == null || keyName.isBlank()) {
			return;
		}
		InputConstants.Key key = InputConstants.getKey(keyName);
		if (key == null || key == InputConstants.UNKNOWN) {
			return;
		}
		mapping.setDown(InputConstants.isKeyDown(client.getWindow(), key.getValue()));
	}

	static boolean terminalAuraEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_TERMINAL_AURA.get());
	}

	static boolean autoTermsEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_AUTO_TERMS.get());
	}

	static boolean autoTermsInvwalkEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK.get());
	}

	static boolean autoTermsInvwalkMentalEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK_MENTAL.get());
	}

	static boolean autoTermsMelodyEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_AUTO_TERMS_MELODY_ENABLED.get());
	}

	static boolean autoTermsInvwalkMelodyEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK_MELODY.get());
	}

	static int autoTermsInvwalkMelodyMethod() {
		String selected = UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK_MELODY_METHOD.get();
		if (selected == null) {
			return 1;
		}
		return selected.equalsIgnoreCase("Keybind") ? 0 : 1;
	}

	static int autoTermsMelodySkipMode() {
		String selected = UiDefinitions.DUNGEONS_AUTO_TERMS_MELODY_SKIP.get();
		if (selected == null) {
			return 1;
		}
		if (selected.equalsIgnoreCase("None")) {
			return 0;
		}
		if (selected.equalsIgnoreCase("All")) {
			return 2;
		}
		return 1;
	}

	static boolean visualizerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_ENABLED.get());
	}

	static boolean visualizerNumbersEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED.get());
	}

	static boolean visualizerColorsEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_COLORS_ENABLED.get());
	}

	static boolean visualizerStartsWithEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_STARTSWITH_ENABLED.get());
	}

	static boolean visualizerRubixEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_RUBIX_ENABLED.get());
	}

	static boolean visualizerRedGreenEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_REDGREEN_ENABLED.get());
	}

	static boolean visualizerMelodyEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED.get());
	}

	static float visualizerScale() {
		return (float) clampDouble(doubleValue(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_SCALE.get(), 1.0), 0.25, 4.0);
	}

	static int visualizerMainColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_COLOR.argb();
	}

	static int visualizerBackgroundColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_BACKGROUND_COLOR.argb();
	}

	static int visualizerNumbersColor(int index) {
		return switch (index) {
			case 1 -> UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR1.argb();
			case 2 -> UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR2.argb();
			default -> UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR3.argb();
		};
	}

	static int visualizerRubixLeftColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_RUBIX_LEFT_COLOR.argb();
	}

	static int visualizerRubixRightColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_RUBIX_RIGHT_COLOR.argb();
	}

	static int visualizerMelodySlotColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_MELODY_SLOT_COLOR.argb();
	}

	static int visualizerMelodyCorrectButtonColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_MELODY_CORRECT_BUTTON_COLOR.argb();
	}

	static int visualizerMelodyIncorrectButtonColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_MELODY_INCORRECT_BUTTON_COLOR.argb();
	}

	static int visualizerMelodyColumnColor() {
		return UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_MELODY_COLUMN_COLOR.argb();
	}
}
