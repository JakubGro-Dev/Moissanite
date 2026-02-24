package com.crussion.moissanite.util.inventory;

import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.world.item.ItemStack;

public final class HeldItemMatcher {
	private HeldItemMatcher() {
	}

	public static boolean heldMatchesSkyblockId(String normalizedId) {
		if (normalizedId == null || normalizedId.isBlank()) {
			return false;
		}
		return stackMatchesSkyblockId(HotbarItemSearch.getHeldItem(), normalizedId);
	}

	public static boolean stackMatchesSkyblockId(ItemStack stack, String normalizedId) {
		if (stack == null || stack.isEmpty() || normalizedId == null || normalizedId.isBlank()) {
			return false;
		}

		String heldItemData = TextNormalizer.normalize(stack.getComponentsPatch().toString());
		String id = TextNormalizer.normalize(normalizedId);
		return heldItemData.contains("skyblock:" + id)
				|| heldItemData.contains("id:\"" + id + "\"")
				|| heldItemData.contains("id:" + id);
	}
}
