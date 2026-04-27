package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.inventory.GuiClickThrottle;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class KuudraAutoShop {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String PERK_MENU_TITLE = "perk menu";
	private static final String CONFIRM_TITLE = "are you sure?";
	private static final String SPECIALIST_ROUTE_NAME = "Specialist Route";
	private static final String BALLISTA_MECHANIC_PREFIX = "Ballista Mechanic";
	private static final String NOT_ENOUGH_TOKENS = "You do not have enough tokens to upgrade this perk!";
	private static final int DEFAULT_CLICK_DELAY_MS = 150;
	private static final int MIN_CLICK_DELAY_MS = 1;
	private static final int MAX_CLICK_DELAY_MS = 1000;
	private static final int DEFAULT_FIRST_CLICK_DELAY_MS = 300;
	private static final int MIN_FIRST_CLICK_DELAY_MS = 1;
	private static final int MAX_FIRST_CLICK_DELAY_MS = 5000;

	private enum State {
		IDLE,
		WAITING_CONFIRM,
		SPAM_CLICKING
	}

	private static boolean initialized;
	private static State state = State.IDLE;
	private static long lastClickTimeMs;
	private static boolean firstClickDone;

	private KuudraAutoShop() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(KuudraAutoShop::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetState());
	}

	public static void onSystemChat(net.minecraft.network.chat.Component message) {
		if (state != State.SPAM_CLICKING) {
			return;
		}
		String msg = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString());
		if (!msg.contains(NOT_ENOUGH_TOKENS)) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			client.player.closeContainer();
		}
		resetState();
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.gameMode == null) {
			resetState();
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_SHOP.get())) {
			resetState();
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			resetState();
			return;
		}
		if (KuudraPhaseTracker.getPhase() >= KuudraPhaseTracker.PHASE_EATEN) {
			resetState();
			return;
		}

		ChestContext context = resolveChestContext(client);
		if (context == null) {
			return;
		}

		switch (state) {
			case IDLE -> handleIdle(client, context);
			case WAITING_CONFIRM -> handleWaitingConfirm(client, context);
			case SPAM_CLICKING -> handleSpamClicking(client, context);
		}
	}

	private static void handleIdle(Minecraft client, ChestContext context) {
		if (!context.titleNormalized().contains(PERK_MENU_TITLE)) {
			return;
		}

		int routeSlot = findSlotByDisplayNameContaining(context.menu().slots, SPECIALIST_ROUTE_NAME);
		if (routeSlot != -1) {
			clickSlot(client, context.menu(), routeSlot);
			state = State.WAITING_CONFIRM;
			return;
		}

		int ballistaSlot = findSlotByDisplayNameStartingWith(context.menu().slots, BALLISTA_MECHANIC_PREFIX);
		if (ballistaSlot != -1) {
			state = State.SPAM_CLICKING;
			lastClickTimeMs = System.currentTimeMillis();
			firstClickDone = false;
		}
	}

	private static void handleWaitingConfirm(Minecraft client, ChestContext context) {
		if (context.titleNormalized().contains(CONFIRM_TITLE)) {
			int confirmSlot = findSlotByItem(context.menu().slots, Items.GREEN_TERRACOTTA);
			if (confirmSlot != -1) {
				clickSlot(client, context.menu(), confirmSlot);
				state = State.SPAM_CLICKING;
				lastClickTimeMs = System.currentTimeMillis();
				firstClickDone = false;
			}
			return;
		}

		if (context.titleNormalized().contains(PERK_MENU_TITLE)) {
			int ballistaSlot = findSlotByDisplayNameStartingWith(context.menu().slots, BALLISTA_MECHANIC_PREFIX);
			if (ballistaSlot != -1) {
				state = State.SPAM_CLICKING;
				lastClickTimeMs = System.currentTimeMillis();
				firstClickDone = false;
			}
		}
	}

	private static void handleSpamClicking(Minecraft client, ChestContext context) {
		if (!context.titleNormalized().contains(PERK_MENU_TITLE)) {
			return;
		}

		int ballistaSlot = findSlotByDisplayNameStartingWith(context.menu().slots, BALLISTA_MECHANIC_PREFIX);
		if (ballistaSlot == -1) {
			return;
		}

		long now = System.currentTimeMillis();
		long delay = firstClickDone ? configuredClickDelayMs() : configuredFirstClickDelayMs();
		if (now - lastClickTimeMs < delay) {
			return;
		}

		clickSlot(client, context.menu(), ballistaSlot);
		lastClickTimeMs = now;
		firstClickDone = true;
	}

	private static ChestContext resolveChestContext(Minecraft client) {
		if (!(client.screen instanceof ContainerScreen containerScreen)) {
			return null;
		}
		if (!(client.player.containerMenu instanceof ChestMenu chestMenu)) {
			return null;
		}

		String title = TextNormalizer.normalize(containerScreen.getTitle().getString());
		return new ChestContext(title, chestMenu);
	}

	private static int findSlotByDisplayNameContaining(List<Slot> slots, String text) {
		if (slots == null || text == null) {
			return -1;
		}
		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			if (slot == null) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			String name = TextNormalizer.stripFormattingCodes(stack.getHoverName().getString());
			if (name.contains(text)) {
				return i;
			}
		}
		return -1;
	}

	private static int findSlotByDisplayNameStartingWith(List<Slot> slots, String prefix) {
		if (slots == null || prefix == null) {
			return -1;
		}
		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			if (slot == null) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			String name = TextNormalizer.stripFormattingCodes(stack.getHoverName().getString());
			if (name.startsWith(prefix)) {
				return i;
			}
		}
		return -1;
	}

	private static int findSlotByItem(List<Slot> slots, net.minecraft.world.item.Item item) {
		if (slots == null || item == null) {
			return -1;
		}
		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			if (slot == null) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (stack != null && !stack.isEmpty() && stack.is(item)) {
				return i;
			}
		}
		return -1;
	}

	private static boolean clickSlot(Minecraft client, ChestMenu menu, int slot) {
		return GuiClickThrottle.clickSlotLikeUser(client, menu, slot);
	}

	private static int configuredClickDelayMs() {
		Double value = UiDefinitions.AUTO_SHOP_CLICK_DELAY.get();
		int configured = value != null && Double.isFinite(value) ? (int) Math.round(value) : DEFAULT_CLICK_DELAY_MS;
		return Mth.clamp(configured, MIN_CLICK_DELAY_MS, MAX_CLICK_DELAY_MS);
	}

	private static int configuredFirstClickDelayMs() {
		Double value = UiDefinitions.AUTO_SHOP_FIRST_CLICK_DELAY.get();
		int configured = value != null && Double.isFinite(value) ? (int) Math.round(value) : DEFAULT_FIRST_CLICK_DELAY_MS;
		return Mth.clamp(configured, MIN_FIRST_CLICK_DELAY_MS, MAX_FIRST_CLICK_DELAY_MS);
	}

	private static void resetState() {
		state = State.IDLE;
		lastClickTimeMs = 0;
		firstClickDone = false;
	}

	private record ChestContext(String titleNormalized, ChestMenu menu) {
	}
}
