package com.crussion.moissanite.util.inventory;

import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class HotbarItemSearch {
	private HotbarItemSearch() {
	}

	public static boolean swapHeldItem(int hotbarSlot) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}
		if (!Inventory.isHotbarSlot(hotbarSlot)) {
			return false;
		}

		client.player.getInventory().setSelectedSlot(hotbarSlot);
		return true;
	}

	public static boolean swapHeldItem(String itemQuery) {
		int slot = findItem(itemQuery);
		return slot != -1 && swapHeldItem(slot);
	}

	public static int findItem(String itemQuery) {
		String needle = TextNormalizer.normalize(itemQuery);
		if (needle.isBlank()) {
			return -1;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return -1;
		}

		Inventory inventory = client.player.getInventory();
		int hotbarSize = Inventory.getSelectionSize();
		for (int slot = 0; slot < hotbarSize; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (matchesItem(stack, needle)) {
				return slot;
			}
		}
		return -1;
	}

	public static int findFirstHotbarSlotByNbt(String nbtSuffix) {
		String suffix = TextNormalizer.normalize(nbtSuffix);
		if (suffix.isBlank()) {
			return -1;
		}

		String[] targets = buildSkyblockIdTargets(suffix);
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return -1;
		}

		Inventory inventory = client.player.getInventory();
		int hotbarSize = Inventory.getSelectionSize();
		for (int slot = 0; slot < hotbarSize; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (matchesSkyblockNbtId(stack, targets)) {
				return slot;
			}
		}
		return -1;
	}

	public static ItemStack getHeldItem() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return ItemStack.EMPTY;
		}
		return client.player.getMainHandItem();
	}

	public static void setSliderFromSlot(UiSlider slider, int slot) {
		if (slider == null) {
			return;
		}
		slider.set((double) slot);
	}

	private static boolean matchesItem(ItemStack stack, String normalizedQuery) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		String displayName = TextNormalizer.normalize(stack.getDisplayName().getString());
		if (displayName.contains(normalizedQuery)) {
			return true;
		}

		String itemId = TextNormalizer.normalize(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		return itemId.contains(normalizedQuery);
	}

	private static boolean matchesSkyblockNbtId(ItemStack stack, String[] normalizedTargets) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null && !customData.isEmpty()) {
			String customDataTag = TextNormalizer.normalize(customData.copyTag().toString());
			if (containsAny(customDataTag, normalizedTargets)) {
				return true;
			}
		}

		String componentBlob = TextNormalizer.normalize(stack.getComponentsPatch().toString());
		if (containsAny(componentBlob, normalizedTargets)) {
			return true;
		}
		return containsAny(TextNormalizer.normalize(stack.toString()), normalizedTargets);
	}

	private static String[] buildSkyblockIdTargets(String normalizedSuffix) {
		String skyblockId = normalizedSuffix.startsWith("skyblock:") ? normalizedSuffix : "skyblock:" + normalizedSuffix;
		String plainId = normalizedSuffix.startsWith("skyblock:") ? normalizedSuffix.substring("skyblock:".length()) : normalizedSuffix;

		return new String[] {
				skyblockId,
				plainId,
				"id:\"" + plainId + "\"",
				"id:" + plainId,
				"id:\"" + skyblockId + "\"",
				"id:" + skyblockId
		};
	}

	private static boolean containsAny(String haystack, String[] normalizedTargets) {
		if (haystack == null || haystack.isBlank() || normalizedTargets == null || normalizedTargets.length == 0) {
			return false;
		}
		for (String target : normalizedTargets) {
			if (target == null || target.isBlank()) {
				continue;
			}
			if (haystack.contains(target)) {
				return true;
			}
		}
		return false;
	}
}
