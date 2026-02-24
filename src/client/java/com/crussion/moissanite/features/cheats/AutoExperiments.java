package com.crussion.moissanite.features.cheats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoExperiments {
	private static final int CONTROL_SLOT = 49;
	private static final int CHRONOMATRON_MIN_SLOT = 10;
	private static final int CHRONOMATRON_MAX_SLOT = 43;
	private static final int ULTRASEQUENCER_MIN_SLOT = 9;
	private static final int ULTRASEQUENCER_MAX_SLOT = 44;
	private static final String CHRONOMATRON_TITLE = "chronomatron (";
	private static final String ULTRASEQUENCER_TITLE = "ultrasequencer (";
	private static final String PRIVATE_ISLAND = "private island";
	private static final String YOUR_ISLAND = "your island";
	private static final int DEFAULT_CLICK_DELAY_MS = 200;
	private static final int MIN_CLICK_DELAY_MS = 0;
	private static final int MAX_CLICK_DELAY_MS = 1000;
	private static final int DEFAULT_SERUM_COUNT = 0;
	private static final int MIN_SERUM_COUNT = 0;
	private static final int MAX_SERUM_COUNT = 3;

	private static final Map<Integer, Integer> ultrasequencerOrder = new HashMap<>();
	private static final List<Integer> chronomatronOrder = new ArrayList<>(28);
	private static boolean initialized;
	private static long lastClickTimeMs;
	private static boolean hasAdded;
	private static int lastAddedSlot = -1;
	private static int clicks;
	private static ExperimentType activeExperiment = ExperimentType.NONE;

	private AutoExperiments() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(AutoExperiments::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetAllState());
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			resetAllState();
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_EXPERIMENTS.get())) {
			resetAllState();
			return;
		}
		if (!isInPrivateIsland()) {
			resetAllState();
			return;
		}

		ExperimentContext context = resolveExperimentContext(client);
		if (context == null) {
			resetAllState();
			return;
		}
		if (context.type() != activeExperiment) {
			resetProgress();
			activeExperiment = context.type();
		}

		switch (context.type()) {
			case CHRONOMATRON -> solveChronomatron(client, context.menu().slots, context.menu());
			case ULTRASEQUENCER -> solveUltraSequencer(client, context.menu().slots, context.menu());
			case NONE -> resetAllState();
		}
	}

	private static void solveChronomatron(Minecraft client, List<Slot> invSlots, ChestMenu menu) {
		int maxChronomatron = shouldGetMaxXp() ? 15 : 11 - configuredSerumCount();
		ItemStack controlStack = stackAt(invSlots, CONTROL_SLOT);
		if (isItem(controlStack, Items.GLOWSTONE) && !isEnchantedSlot(invSlots, lastAddedSlot)) {
			if (shouldAutoClose() && chronomatronOrder.size() > maxChronomatron) {
				closeScreen(client);
				return;
			}
			hasAdded = false;
		}

		if (!hasAdded && isItem(controlStack, Items.CLOCK)) {
			int highlighted = findChronomatronHighlightedSlot(invSlots);
			if (highlighted != -1) {
				chronomatronOrder.add(highlighted);
				lastAddedSlot = highlighted;
				hasAdded = true;
				clicks = 0;
			}
		}

		long now = System.currentTimeMillis();
		if (hasAdded
				&& isItem(controlStack, Items.CLOCK)
				&& chronomatronOrder.size() > clicks
				&& now - lastClickTimeMs >= configuredClickDelayMs()) {
			int slotToClick = chronomatronOrder.get(clicks);
			if (clickSlot(client, menu, slotToClick)) {
				lastClickTimeMs = now;
				clicks++;
			}
		}
	}

	private static void solveUltraSequencer(Minecraft client, List<Slot> invSlots, ChestMenu menu) {
		int maxUltraSequencer = shouldGetMaxXp() ? 20 : 9 - configuredSerumCount();
		ItemStack controlStack = stackAt(invSlots, CONTROL_SLOT);
		if (isItem(controlStack, Items.CLOCK)) {
			hasAdded = false;
		}

		if (!hasAdded && isItem(controlStack, Items.GLOWSTONE)) {
			if (stackAt(invSlots, ULTRASEQUENCER_MAX_SLOT).isEmpty()) {
				return;
			}

			ultrasequencerOrder.clear();
			int maxSlot = Math.min(ULTRASEQUENCER_MAX_SLOT, invSlots.size() - 1);
			for (int slotIndex = ULTRASEQUENCER_MIN_SLOT; slotIndex <= maxSlot; slotIndex++) {
				ItemStack stack = stackAt(invSlots, slotIndex);
				if (!isUltraSequencerSequenceItem(stack)) {
					continue;
				}
				int orderIndex = stack.getCount() - 1;
				ultrasequencerOrder.put(orderIndex, slotIndex);
			}

			hasAdded = true;
			clicks = 0;
			if (ultrasequencerOrder.size() > maxUltraSequencer && shouldAutoClose()) {
				closeScreen(client);
				return;
			}
		}

		long now = System.currentTimeMillis();
		if (isItem(controlStack, Items.CLOCK)
				&& ultrasequencerOrder.containsKey(clicks)
				&& now - lastClickTimeMs >= configuredClickDelayMs()) {
			Integer slotToClick = ultrasequencerOrder.get(clicks);
			if (slotToClick != null && clickSlot(client, menu, slotToClick)) {
				lastClickTimeMs = now;
				clicks++;
			}
		}
	}

	private static ExperimentContext resolveExperimentContext(Minecraft client) {
		if (client == null || client.player == null || !(client.screen instanceof ContainerScreen containerScreen)) {
			return null;
		}
		if (!(client.player.containerMenu instanceof ChestMenu chestMenu)) {
			return null;
		}

		String title = TextNormalizer.normalize(containerScreen.getTitle().getString());
		if (title.startsWith(CHRONOMATRON_TITLE)) {
			return new ExperimentContext(ExperimentType.CHRONOMATRON, chestMenu);
		}
		if (title.startsWith(ULTRASEQUENCER_TITLE)) {
			return new ExperimentContext(ExperimentType.ULTRASEQUENCER, chestMenu);
		}
		return null;
	}

	private static boolean isInPrivateIsland() {
		return ScoreboardAreaMatcher.isInArea(PRIVATE_ISLAND) || ScoreboardAreaMatcher.isInArea(YOUR_ISLAND);
	}

	private static int findChronomatronHighlightedSlot(List<Slot> invSlots) {
		int maxSlot = Math.min(CHRONOMATRON_MAX_SLOT, invSlots.size() - 1);
		for (int slotIndex = CHRONOMATRON_MIN_SLOT; slotIndex <= maxSlot; slotIndex++) {
			if (isEnchantedSlot(invSlots, slotIndex)) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static ItemStack stackAt(List<Slot> slots, int index) {
		if (slots == null || index < 0 || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(index);
		if (slot == null) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		return stack == null ? ItemStack.EMPTY : stack;
	}

	private static boolean isEnchantedSlot(List<Slot> slots, int index) {
		ItemStack stack = stackAt(slots, index);
		return !stack.isEmpty() && stack.hasFoil();
	}

	private static boolean isItem(ItemStack stack, Item item) {
		return stack != null && !stack.isEmpty() && stack.is(item);
	}

	private static boolean isUltraSequencerSequenceItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		String itemPath = BuiltInRegistries.ITEM.getKey(item).getPath();
		if (itemPath != null && itemPath.contains("dye")) {
			return true;
		}

		return item == Items.INK_SAC
				|| item == Items.COCOA_BEANS
				|| item == Items.LAPIS_LAZULI
				|| item == Items.BONE_MEAL;
	}

	private static boolean clickSlot(Minecraft client, ChestMenu menu, int slot) {
		if (client == null || client.player == null || client.gameMode == null || menu == null) {
			return false;
		}
		if (slot < 0 || slot >= menu.slots.size()) {
			return false;
		}
		client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.PICKUP, client.player);
		return true;
	}

	private static void closeScreen(Minecraft client) {
		if (client == null || client.player == null) {
			return;
		}
		client.player.closeContainer();
	}

	private static boolean shouldAutoClose() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_EXPERIMENTS_AUTO_CLOSE.get());
	}

	private static boolean shouldGetMaxXp() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_EXPERIMENTS_GET_MAX_XP.get());
	}

	private static int configuredClickDelayMs() {
		Integer value = UiDefinitions.AUTO_EXPERIMENTS_CLICK_DELAY.get();
		int configured = value == null ? DEFAULT_CLICK_DELAY_MS : value;
		return Mth.clamp(configured, MIN_CLICK_DELAY_MS, MAX_CLICK_DELAY_MS);
	}

	private static int configuredSerumCount() {
		Integer value = UiDefinitions.AUTO_EXPERIMENTS_SERUM_COUNT.get();
		int configured = value == null ? DEFAULT_SERUM_COUNT : value;
		return Mth.clamp(configured, MIN_SERUM_COUNT, MAX_SERUM_COUNT);
	}

	private static void resetProgress() {
		ultrasequencerOrder.clear();
		chronomatronOrder.clear();
		hasAdded = false;
		lastAddedSlot = -1;
		clicks = 0;
		lastClickTimeMs = 0L;
	}

	private static void resetAllState() {
		resetProgress();
		activeExperiment = ExperimentType.NONE;
	}

	private enum ExperimentType {
		NONE,
		CHRONOMATRON,
		ULTRASEQUENCER
	}

	private record ExperimentContext(ExperimentType type, ChestMenu menu) {
	}
}
