package com.crussion.moissanite.util.route;

import com.crussion.moissanite.util.inventory.HeldItemMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RouteTeleportItem(int slot, String skyblockId, int instantRange, int etherRange, boolean hasEtherTransmission) {
	private static final String ASPECT_OF_THE_VOID = "ASPECT_OF_THE_VOID";
	private static final String ASPECT_OF_THE_END = "ASPECT_OF_THE_END";
	private static final int MIN_INSTANT_RANGE = 8;
	private static final int MAX_INSTANT_RANGE = 12;
	private static final int MIN_ETHER_RANGE = 57;
	private static final int MAX_ETHER_RANGE = 61;
	private static final int ETHER_RANGE_OFFSET = 49;
	private static final Pattern INSTANT_RANGE_PATTERN = Pattern.compile("\\bteleport\\s+(\\d{1,2})\\s+blocks\\b");
	private static final Pattern ETHER_RANGE_PATTERN = Pattern.compile("\\bup\\s+to\\s+(\\d{1,2})\\s+blocks\\b");

	public static RouteTeleportItem findFor(RouteActionType actionType) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return null;
		}

		Inventory inventory = client.player.getInventory();
		int selectedSlot = inventory.getSelectedSlot();
		RouteTeleportItem selected = inspectSlot(inventory, selectedSlot);
		if (supports(selected, actionType)) {
			return selected;
		}

		RouteTeleportItem bestVoid = null;
		RouteTeleportItem bestEnd = null;
		int hotbarSize = Inventory.getSelectionSize();
		for (int slot = 0; slot < hotbarSize; slot++) {
			RouteTeleportItem item = inspectSlot(inventory, slot);
			if (!supports(item, actionType)) {
				continue;
			}
			if (ASPECT_OF_THE_VOID.equals(item.skyblockId())) {
				bestVoid = item;
				break;
			}
			if (bestEnd == null && ASPECT_OF_THE_END.equals(item.skyblockId())) {
				bestEnd = item;
			}
		}
		return bestVoid != null ? bestVoid : bestEnd;
	}

	private static boolean supports(RouteTeleportItem item, RouteActionType actionType) {
		if (item == null || actionType == null) {
			return false;
		}
		return actionType != RouteActionType.ETH || item.hasEtherTransmission();
	}

	private static RouteTeleportItem inspectSlot(Inventory inventory, int slot) {
		if (inventory == null || !Inventory.isHotbarSlot(slot)) {
			return null;
		}
		ItemStack stack = inventory.getItem(slot);
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		String skyblockId = skyblockId(stack);
		if (skyblockId == null) {
			return null;
		}

		String text = stackText(stack);
		int instantRange = parseInstantRange(text);
		int etherRange = parseEtherRange(text);
		boolean hasEther = text.contains("ether transmission") || etherRange != -1;

		if (instantRange == -1 && etherRange != -1) {
			instantRange = etherRange - ETHER_RANGE_OFFSET;
		}
		if (instantRange == -1) {
			instantRange = MIN_INSTANT_RANGE;
		}
		if (etherRange == -1) {
			etherRange = instantRange + ETHER_RANGE_OFFSET;
		}

		instantRange = Mth.clamp(instantRange, MIN_INSTANT_RANGE, MAX_INSTANT_RANGE);
		etherRange = Mth.clamp(etherRange, MIN_ETHER_RANGE, MAX_ETHER_RANGE);
		return new RouteTeleportItem(slot, skyblockId, instantRange, etherRange, hasEther);
	}

	private static String skyblockId(ItemStack stack) {
		if (HeldItemMatcher.stackMatchesSkyblockId(stack, ASPECT_OF_THE_VOID)) {
			return ASPECT_OF_THE_VOID;
		}
		if (HeldItemMatcher.stackMatchesSkyblockId(stack, ASPECT_OF_THE_END)) {
			return ASPECT_OF_THE_END;
		}
		return null;
	}

	private static String stackText(ItemStack stack) {
		StringBuilder builder = new StringBuilder(256);
		builder.append(stack.getHoverName().getString()).append(' ');
		builder.append(stack.getDisplayName().getString()).append(' ');
		builder.append(stack.getComponentsPatch()).append(' ');

		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				builder.append(line.getString()).append(' ');
			}
		}
		return TextNormalizer.normalize(builder.toString());
	}

	private static int parseInstantRange(String text) {
		Matcher matcher = INSTANT_RANGE_PATTERN.matcher(text == null ? "" : text);
		while (matcher.find()) {
			int value = parseRange(matcher.group(1));
			if (value >= MIN_INSTANT_RANGE && value <= MAX_INSTANT_RANGE) {
				return value;
			}
		}
		return -1;
	}

	private static int parseEtherRange(String text) {
		Matcher matcher = ETHER_RANGE_PATTERN.matcher(text == null ? "" : text);
		while (matcher.find()) {
			int value = parseRange(matcher.group(1));
			if (value >= MIN_ETHER_RANGE && value <= MAX_ETHER_RANGE) {
				return value;
			}
		}
		return -1;
	}

	private static int parseRange(String raw) {
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}
}
