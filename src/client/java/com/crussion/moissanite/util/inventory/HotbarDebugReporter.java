package com.crussion.moissanite.util.inventory;

import com.crussion.moissanite.util.chat.FeatureChat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class HotbarDebugReporter {
	private HotbarDebugReporter() {
	}

	public static void dumpHotbarItemsToChat() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		Inventory inventory = client.player.getInventory();
		int hotbarSize = Inventory.getSelectionSize();
		FeatureChat.send("Auto Rend scan: hotbar details");

		for (int slot = 0; slot < hotbarSize; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack == null || stack.isEmpty()) {
				FeatureChat.send("[Slot " + slot + "] empty");
				continue;
			}

			String name = stack.getDisplayName().getString();
			String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			String description = limitForChat(stack.toString(), 240);
			String metadata = buildMetadataString(stack);

			FeatureChat.send("[Slot " + slot + "] name=" + name + " id=" + itemId + " count=" + stack.getCount());
			FeatureChat.send("[Slot " + slot + "] description=" + description);
			FeatureChat.send("[Slot " + slot + "] metadata=" + metadata);
		}
	}

	private static String buildMetadataString(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		String customDataText = "";
		if (customData != null && !customData.isEmpty()) {
			customDataText = "customData=" + customData.copyTag();
		}

		String components = "components=" + stack.getComponentsPatch();
		if (customDataText.isBlank()) {
			return limitForChat(components, 320);
		}
		return limitForChat(customDataText + " " + components, 320);
	}

	private static String limitForChat(String text, int maxLen) {
		if (text == null || text.isBlank()) {
			return "";
		}
		int boundedMax = Math.max(16, maxLen);
		if (text.length() <= boundedMax) {
			return text;
		}
		return text.substring(0, boundedMax - 3) + "...";
	}
}
