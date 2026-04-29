package com.crussion.moissanite.util.inventory;

import com.crussion.moissanite.mixin.client.ContainerScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.concurrent.ThreadLocalRandom;

public final class GuiClickThrottle {
	public static final int SAFE_MIN_DELAY_MS = 250;
	public static final int SAFE_MAX_DELAY_MS = 270;

	private static long nextAllowedClickMs;

	private GuiClickThrottle() {
	}

	public static void reset() {
		nextAllowedClickMs = 0L;
	}

	public static boolean clickSlot(Minecraft client, ChestMenu menu, int slot) {
		return clickSlot(client, menu, slot, SAFE_MIN_DELAY_MS, SAFE_MAX_DELAY_MS);
	}

	public static boolean clickSlot(Minecraft client, ChestMenu menu, int slot, int minDelayMs, int maxDelayMs) {
		if (!isClickable(client, menu, slot)) {
			return false;
		}

		long now = System.currentTimeMillis();
		if (nextAllowedClickMs == 0L) {
			clickSlotDirect(client, menu, slot);
			nextAllowedClickMs = now + randomDelayMs(minDelayMs, maxDelayMs);
			return true;
		}
		if (now < nextAllowedClickMs) {
			return false;
		}

		clickSlotDirect(client, menu, slot);
		nextAllowedClickMs = now + randomDelayMs(minDelayMs, maxDelayMs);
		return true;
	}

	public static boolean clickSlotLikeUser(Minecraft client, ChestMenu menu, int slot) {
		if (!isClickable(client, menu, slot)) {
			return false;
		}
		performClickSlotLikeUser(client, menu, slot);
		return true;
	}

	public static boolean clickSlotLikeUser(Minecraft client, ChestMenu menu, int slot, int minDelayMs, int maxDelayMs) {
		if (!isClickable(client, menu, slot)) {
			return false;
		}

		long now = System.currentTimeMillis();
		if (nextAllowedClickMs != 0L && now < nextAllowedClickMs) {
			return false;
		}

		performClickSlotLikeUser(client, menu, slot);
		nextAllowedClickMs = now + randomDelayMs(minDelayMs, maxDelayMs);
		return true;
	}

	private static void performClickSlotLikeUser(Minecraft client, ChestMenu menu, int slot) {
		if (client.screen instanceof ContainerScreen containerScreen && containerScreen.getMenu() == menu) {
			Slot targetSlot = menu.slots.get(slot);
			double mouseX = ((ContainerScreenAccessor) containerScreen).moissanite$getLeftPos() + targetSlot.x + 8.0D;
			double mouseY = ((ContainerScreenAccessor) containerScreen).moissanite$getTopPos() + targetSlot.y + 8.0D;
			MouseButtonEvent click = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(0, 0));
			containerScreen.mouseClicked(click, false);
			containerScreen.mouseReleased(click);
			return;
		}

		client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.PICKUP, client.player);
	}

	private static void clickSlotDirect(Minecraft client, ChestMenu menu, int slot) {
		client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.PICKUP, client.player);
	}

	private static boolean isClickable(Minecraft client, ChestMenu menu, int slot) {
		if (client == null || client.player == null || client.gameMode == null || menu == null) {
			return false;
		}
		return slot >= 0 && slot < menu.slots.size();
	}

	private static int randomDelayMs(int minDelayMs, int maxDelayMs) {
		int min = Math.max(0, minDelayMs);
		int max = Math.max(min, maxDelayMs);
		if (min == max) {
			return min;
		}
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}
}
