package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.GuiClickThrottle;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class KuudraAutoOpenChest {
	private static final String FEATURE_NAME = "Kuudra Chest";
	private static final String CROESUS_TITLE = "croesus";
	private static final String KUUDRA_CHEST_TITLE = "kuudra";
	private static final String PAID_CHEST_TITLE = "paid chest";
	private static final String CROESUS_NPC = "croesus";
	private static final String KUUDRA_HOLLOW = "kuudra's hollow";
	private static final String CHESTS_EXPIRE = "chests expire";
	private static final String NEXT_PAGE = "next page";
	private static final String PAID_CHEST = "paid chest";
	private static final String OPEN_REWARD_CHEST = "open reward chest";
	private static final String CLICK_TO_OPEN = "click to open";
	private static final String INFERNAL_KUUDRA_KEY = "infernal kuudra key";
	private static final String INFERNAL_KUUDRA_KEY_ID = "infernal_kuudra_key";
	private static final int MAX_CROESUS_SCAN_PAGES = 3;
	private static final int MENU_TIMEOUT_TICKS = 100;
	private static final int OPEN_CROESUS_TIMEOUT_TICKS = 120;
	private static final int NPC_INTERACT_INTERVAL_TICKS = 1;
	private static final int DEFAULT_FIRST_CLICK_DELAY_MS = 1000;
	private static final int MIN_FIRST_CLICK_DELAY_MS = 1;
	private static final int MAX_FIRST_CLICK_DELAY_MS = 5000;
	private static final int DEFAULT_CLICK_DELAY_MS = 600;
	private static final int MIN_CLICK_DELAY_MS = 1;
	private static final int MAX_CLICK_DELAY_MS = 2000;
	private static final int POST_PURCHASE_NPC_CLICK_DELAY_TICKS = 2;
	private static final double DEFAULT_ROTATION_MULTIPLIER = 0.45D;
	private static final double MIN_ROTATION_MULTIPLIER = 0.0D;
	private static final double MAX_ROTATION_MULTIPLIER = 2.0D;
	private static final double NPC_SEARCH_RADIUS = 12.0D;

	private enum State {
		IDLE,
		OPEN_CROESUS,
		SCAN_CROESUS,
		WAIT_SCAN_PAGE,
		NAVIGATE_TO_TARGET_PAGE,
		WAIT_TARGET_PAGE,
		OPEN_TARGET_CHEST,
		WAIT_KUUDRA_CHEST,
		SELECT_PAID_CHEST,
		WAIT_PAID_CHEST,
		OPEN_REWARD_CHEST,
		WAIT_AFTER_PURCHASE,
		WAIT_CROESUS_AFTER_PURCHASE
	}

	private static boolean initialized;
	private static boolean running;
	private static State state = State.IDLE;
	private static int stateTicks;
	private static int lastInteractTick;
	private static int lastSeenGuiContainerId = -1;
	private static String lastSeenGuiTitle = "";
	private static int lastSeenGuiSignature;
	private static int guiStableTicks;
	private static boolean firstClickInCurrentGui = true;
	private static final List<ChestTarget> cachedTargets = new ArrayList<>();
	private static boolean scanComplete;
	private static int scanPage = 1;
	private static int lastCachedScanPage;
	private static int currentCroesusPage = 1;
	private static int pendingPageSourceSignature;
	private static int pendingPageContainerId = -1;
	private static long containerContentUpdateVersion;
	private static long pendingPageContentUpdateVersion;
	private static int postPurchaseGuiClosedTicks = -1;
	private static ChestTarget activeTarget;
	private static Entity croesusTarget;
	private static Vec3 croesusAimPoint;

	private KuudraAutoOpenChest() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(KuudraAutoOpenChest::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetRuntime());
	}

	public static void start() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_OPEN_KUUDRA_CHEST.get())) {
			UiDefinitions.AUTO_OPEN_KUUDRA_CHEST.set(true);
		}
		resetRuntime();
		if (!hasInfernalKuudraKey(client)) {
			sendMessage("No Infernal Kuudra Keys found.");
			return;
		}
		if (client.screen != null) {
			client.setScreen(null);
		}
		running = true;
		enterState(State.OPEN_CROESUS);
		sendMessage("Started.");
	}

	public static boolean isRunning() {
		return running;
	}

	public static void onContainerContentUpdate(int containerId) {
		Minecraft client = Minecraft.getInstance();
		ChestMenu menu = currentMenu(client);
		if (containerId == 0 || menu == null || menu.containerId != containerId) {
			return;
		}
		markContainerContentUpdated();
	}

	public static void onContainerSlotUpdate(int containerId, int slotIndex) {
		Minecraft client = Minecraft.getInstance();
		ChestMenu menu = currentMenu(client);
		if (containerId == 0 || menu == null || menu.containerId != containerId
				|| slotIndex < 0 || slotIndex >= containerSlotCount(menu)) {
			return;
		}
		markContainerContentUpdated();
	}

	private static void markContainerContentUpdated() {
		containerContentUpdateVersion++;
		guiStableTicks = 0;
	}

	public static void stop() {
		if (running) {
			sendMessage("Stopped.");
		}
		resetRuntime();
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.gameMode == null) {
			resetRuntime();
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_OPEN_KUUDRA_CHEST.get())) {
			resetRuntime();
			return;
		}

		trackGuiForClickDelay(client);
		if (!running) {
			return;
		}
		if (!hasInfernalKuudraKey(client)) {
			stopInternal("No Infernal Kuudra Keys found.");
			return;
		}

		stateTicks++;
		switch (state) {
			case OPEN_CROESUS -> handleOpenCroesus(client);
			case SCAN_CROESUS -> handleScanCroesus(client);
			case WAIT_SCAN_PAGE -> handleWaitScanPage(client);
			case NAVIGATE_TO_TARGET_PAGE -> handleNavigateToTargetPage(client);
			case WAIT_TARGET_PAGE -> handleWaitTargetPage(client);
			case OPEN_TARGET_CHEST -> handleOpenTargetChest(client);
			case WAIT_KUUDRA_CHEST -> handleWaitKuudraChest(client);
			case SELECT_PAID_CHEST -> handleSelectPaidChest(client);
			case WAIT_PAID_CHEST -> handleWaitPaidChest(client);
			case OPEN_REWARD_CHEST -> handleOpenRewardChest(client);
			case WAIT_AFTER_PURCHASE -> handleWaitAfterPurchase(client);
			case WAIT_CROESUS_AFTER_PURCHASE -> handleWaitCroesusAfterPurchase(client);
			case IDLE -> enterState(State.OPEN_CROESUS);
		}
	}

	private static void handleOpenCroesus(Minecraft client) {
		if (isCroesusMenu(client)) {
			currentCroesusPage = 1;
			enterState(scanComplete ? State.NAVIGATE_TO_TARGET_PAGE : State.SCAN_CROESUS);
			return;
		}
		if (stateTicks > OPEN_CROESUS_TIMEOUT_TICKS) {
			stopInternal("Could not open Croesus.");
			return;
		}

		if (!prepareCroesusAim(client)) {
			return;
		}

		if (RotationController.isRotating()) {
			return;
		}
		if (stateTicks - lastInteractTick < NPC_INTERACT_INTERVAL_TICKS) {
			return;
		}
		lastInteractTick = stateTicks;
		interactWithCroesus(client);
	}

	private static void handleScanCroesus(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		if (!isCroesusMenu(client) || menu == null) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				enterState(State.OPEN_CROESUS);
			}
			return;
		}
		if (!hasAnyContainerItem(menu)) {
			return;
		}
		if (!isCurrentGuiContentSettled()) {
			return;
		}

		if (lastCachedScanPage != scanPage) {
			int cachedThisPage = cacheCurrentCroesusPage(menu, scanPage);
			lastCachedScanPage = scanPage;
			sendMessage("Scanned Croesus page " + scanPage + ": " + cachedThisPage + " Kuudra chests.");
		}
		int nextPageSlot = findSlotContaining(menu, NEXT_PAGE);
		if (scanPage < MAX_CROESUS_SCAN_PAGES && nextPageSlot != -1) {
			if (clickSlot(client, menu, nextPageSlot)) {
				beginPendingPageTransition(menu);
				scanPage++;
				enterState(State.WAIT_SCAN_PAGE);
			}
			return;
		}

		finishScan();
	}

	private static void finishScan() {
		scanComplete = true;
		activeTarget = null;
		if (cachedTargets.isEmpty()) {
			stopInternal("No Kuudra chests found in Croesus.");
			return;
		}
		sendMessage("Cached " + cachedTargets.size() + " Kuudra chests.");
		enterState(State.NAVIGATE_TO_TARGET_PAGE);
	}

	private static void handleWaitScanPage(Minecraft client) {
		if (isPageTransitionReady(client)) {
			currentCroesusPage = scanPage;
			clearPendingPageTransition();
			enterState(State.SCAN_CROESUS);
		}
		if (stateTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Croesus scan page did not load.");
		}
	}

	private static void handleNavigateToTargetPage(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		if (!isCroesusMenu(client) || menu == null) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				enterState(State.OPEN_CROESUS);
			}
			return;
		}
		if (!hasAnyContainerItem(menu)) {
			return;
		}

		if (activeTarget == null) {
			activeTarget = nextCachedTarget();
		}
		if (activeTarget == null) {
			stopInternal("No cached Kuudra chests left.");
			return;
		}
		if (currentCroesusPage > activeTarget.page()) {
			client.setScreen(null);
			currentCroesusPage = 1;
			enterState(State.OPEN_CROESUS);
			return;
		}
		if (currentCroesusPage < activeTarget.page()) {
			int nextPageSlot = findSlotContaining(menu, NEXT_PAGE);
			if (nextPageSlot == -1) {
				if (!isCurrentGuiContentSettled() && stateTicks <= MENU_TIMEOUT_TICKS) {
					return;
				}
				cachedTargets.remove(activeTarget);
				activeTarget = null;
				enterState(State.NAVIGATE_TO_TARGET_PAGE);
				return;
			}
			if (clickSlot(client, menu, nextPageSlot)) {
				beginPendingPageTransition(menu);
				currentCroesusPage++;
				enterState(State.WAIT_TARGET_PAGE);
			}
			return;
		}

		enterState(State.OPEN_TARGET_CHEST);
	}

	private static void handleWaitTargetPage(Minecraft client) {
		if (isPageTransitionReady(client)) {
			clearPendingPageTransition();
			enterState(State.NAVIGATE_TO_TARGET_PAGE);
		}
		if (stateTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Target Croesus page did not load.");
		}
	}

	private static void handleOpenTargetChest(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		if (!isCroesusMenu(client) || menu == null) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				enterState(State.OPEN_CROESUS);
			}
			return;
		}
		if (!hasAnyContainerItem(menu)) {
			return;
		}
		if (activeTarget == null) {
			enterState(State.NAVIGATE_TO_TARGET_PAGE);
			return;
		}
		if (currentCroesusPage != activeTarget.page()) {
			enterState(State.NAVIGATE_TO_TARGET_PAGE);
			return;
		}
		if (!isValidKuudraChestSlot(menu, activeTarget.slot())) {
			if (!hasContainerItem(menu, activeTarget.slot()) && stateTicks <= MENU_TIMEOUT_TICKS) {
				return;
			}
			cachedTargets.remove(activeTarget);
			activeTarget = null;
			enterState(State.NAVIGATE_TO_TARGET_PAGE);
			return;
		}
		if (clickSlot(client, menu, activeTarget.slot())) {
			enterState(State.WAIT_KUUDRA_CHEST);
		}
	}

	private static void handleWaitKuudraChest(Minecraft client) {
		if (isKuudraChestMenu(client)) {
			enterState(State.SELECT_PAID_CHEST);
			return;
		}
		if (stateTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Kuudra chest menu did not open.");
		}
	}

	private static void handleSelectPaidChest(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		if (!isKuudraChestMenu(client) || menu == null) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Kuudra chest menu.");
			}
			return;
		}

		int paidChestSlot = findSlotContaining(menu, PAID_CHEST);
		if (paidChestSlot == -1) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Could not find Paid Chest.");
			}
			return;
		}
		if (clickSlot(client, menu, paidChestSlot)) {
			enterState(State.WAIT_PAID_CHEST);
		}
	}

	private static void handleWaitPaidChest(Minecraft client) {
		if (isPaidChestMenu(client)) {
			enterState(State.OPEN_REWARD_CHEST);
			return;
		}
		if (stateTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Paid Chest menu did not open.");
		}
	}

	private static void handleOpenRewardChest(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		if (!isPaidChestMenu(client) || menu == null) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Paid Chest menu.");
			}
			return;
		}

		int openSlot = findOpenRewardChestSlot(menu);
		if (openSlot == -1) {
			if (stateTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Could not find Open Reward Chest.");
			}
			return;
		}
		if (clickSlot(client, menu, openSlot)) {
			if (activeTarget != null) {
				cachedTargets.remove(activeTarget);
				activeTarget = null;
			}
			enterState(State.WAIT_AFTER_PURCHASE);
		}
	}

	private static void handleWaitAfterPurchase(Minecraft client) {
		if (!hasInfernalKuudraKey(client)) {
			stopInternal("No Infernal Kuudra Keys found.");
			return;
		}
		if (client.screen != null) {
			if (stateTicks <= MENU_TIMEOUT_TICKS) {
				return;
			}
			client.setScreen(null);
		}
		if (postPurchaseGuiClosedTicks < 0) {
			postPurchaseGuiClosedTicks = 0;
			croesusTarget = null;
			croesusAimPoint = null;
		}
		if (!prepareCroesusAim(client)) {
			if (stateTicks > OPEN_CROESUS_TIMEOUT_TICKS) {
				stopInternal("Could not find Croesus after purchase.");
			}
			return;
		}
		if (postPurchaseGuiClosedTicks++ < POST_PURCHASE_NPC_CLICK_DELAY_TICKS) {
			return;
		}
		interactWithCroesus(client);
		enterState(State.WAIT_CROESUS_AFTER_PURCHASE);
	}

	private static void handleWaitCroesusAfterPurchase(Minecraft client) {
		if (isCroesusMenu(client)) {
			currentCroesusPage = 1;
			enterState(scanComplete ? State.NAVIGATE_TO_TARGET_PAGE : State.SCAN_CROESUS);
			return;
		}
		if (stateTicks > OPEN_CROESUS_TIMEOUT_TICKS) {
			stopInternal("Croesus did not open after purchase.");
		}
	}

	private static Entity findCroesusEntity(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return null;
		}
		double maxDistSq = NPC_SEARCH_RADIUS * NPC_SEARCH_RADIUS;
		Entity best = null;
		double bestDistSq = Double.POSITIVE_INFINITY;
		Vec3 playerPos = client.player.position();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity == null || !entity.isAlive()) {
				continue;
			}
			String name = TextNormalizer.normalize(entity.getName().getString());
			if (!name.contains(CROESUS_NPC)) {
				continue;
			}
			double distSq = entity.position().distanceToSqr(playerPos);
			if (distSq > maxDistSq || distSq >= bestDistSq) {
				continue;
			}
			best = entity;
			bestDistSq = distSq;
		}
		return best;
	}

	private static Vec3 aimPoint(Entity entity) {
		if (entity == null) {
			return null;
		}
		return entity.position().add(0.0D, Math.max(1.0D, entity.getBbHeight() * 0.6D), 0.0D);
	}

	private static boolean prepareCroesusAim(Minecraft client) {
		if (croesusAimPoint == null || croesusTarget == null || !croesusTarget.isAlive()) {
			croesusTarget = findCroesusEntity(client);
			croesusAimPoint = aimPoint(croesusTarget);
			if (croesusAimPoint == null) {
				return false;
			}
			RotationController.rotateTo(croesusAimPoint.x, croesusAimPoint.y, croesusAimPoint.z,
					configuredRotationMultiplier());
		}
		return true;
	}

	private static void interactWithCroesus(Minecraft client) {
		if (client == null || client.player == null) {
			return;
		}
		PlayerInputActions.leftClick();
	}

	private static boolean hasInfernalKuudraKey(Minecraft client) {
		if (client == null || client.player == null) {
			return false;
		}
		Inventory inventory = client.player.getInventory();
		int size = inventory.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (matchesInfernalKuudraKey(stack)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesInfernalKuudraKey(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		String name = normalizedStackName(stack);
		if (name.contains(INFERNAL_KUUDRA_KEY)) {
			return true;
		}
		String text = normalizedStackText(stack);
		return text.contains(INFERNAL_KUUDRA_KEY) || text.contains(INFERNAL_KUUDRA_KEY_ID);
	}

	private static boolean isCroesusMenu(Minecraft client) {
		return currentTitle(client).contains(CROESUS_TITLE);
	}

	private static boolean isKuudraChestMenu(Minecraft client) {
		String title = currentTitle(client);
		return title.contains(KUUDRA_CHEST_TITLE) && !title.contains(CROESUS_TITLE);
	}

	private static boolean isPaidChestMenu(Minecraft client) {
		return currentTitle(client).contains(PAID_CHEST_TITLE);
	}

	private static String currentTitle(Minecraft client) {
		if (client == null || !(client.screen instanceof ContainerScreen containerScreen)) {
			return "";
		}
		return TextNormalizer.normalize(containerScreen.getTitle().getString());
	}

	private static ChestMenu currentMenu(Minecraft client) {
		if (client == null || client.player == null || !(client.player.containerMenu instanceof ChestMenu menu)) {
			return null;
		}
		return menu;
	}

	private static void trackGuiForClickDelay(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		String title = currentTitle(client);
		if (menu == null || title.isBlank()) {
			lastSeenGuiContainerId = -1;
			lastSeenGuiTitle = "";
			lastSeenGuiSignature = 0;
			guiStableTicks = 0;
			firstClickInCurrentGui = true;
			return;
		}
		int signature = menuSignature(menu);
		if (menu.containerId != lastSeenGuiContainerId || !title.equals(lastSeenGuiTitle)
				|| signature != lastSeenGuiSignature) {
			lastSeenGuiContainerId = menu.containerId;
			lastSeenGuiTitle = title;
			lastSeenGuiSignature = signature;
			guiStableTicks = 0;
			firstClickInCurrentGui = true;
			GuiClickThrottle.reset();
			return;
		}
		guiStableTicks++;
	}

	private static int currentGuiSignature() {
		ChestMenu menu = currentMenu(Minecraft.getInstance());
		return menu == null ? 0 : menuSignature(menu);
	}

	private static boolean isCurrentGuiContentSettled() {
		return guiStableTicks > 0;
	}

	private static boolean isPageTransitionReady(Minecraft client) {
		if (!isCroesusMenu(client)) {
			return false;
		}
		ChestMenu menu = currentMenu(client);
		if (menu == null || !hasAnyContainerItem(menu)) {
			return false;
		}
		if (!hasPendingPageContentUpdate(menu)) {
			return false;
		}
		if (!isCurrentGuiContentSettled()) {
			return false;
		}
		return menu.containerId != pendingPageContainerId || currentGuiSignature() != pendingPageSourceSignature
				|| containerContentUpdateVersion > pendingPageContentUpdateVersion;
	}

	private static void beginPendingPageTransition(ChestMenu menu) {
		pendingPageSourceSignature = menuSignature(menu);
		pendingPageContainerId = menu.containerId;
		pendingPageContentUpdateVersion = containerContentUpdateVersion;
	}

	private static void clearPendingPageTransition() {
		pendingPageSourceSignature = 0;
		pendingPageContainerId = -1;
		pendingPageContentUpdateVersion = 0L;
	}

	private static boolean hasPendingPageContentUpdate(ChestMenu menu) {
		if (menu == null) {
			return false;
		}
		return menu.containerId != pendingPageContainerId
				|| containerContentUpdateVersion > pendingPageContentUpdateVersion;
	}

	private static int menuSignature(ChestMenu menu) {
		if (menu == null) {
			return 0;
		}
		int maxSlot = containerSlotCount(menu);
		int result = 1;
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
			result = (31 * result) + slotIndex;
			result = (31 * result) + normalizedStackText(stack).hashCode();
		}
		return result;
	}

	private static int cacheCurrentCroesusPage(ChestMenu menu, int page) {
		if (menu == null || page < 1 || page > MAX_CROESUS_SCAN_PAGES) {
			return 0;
		}
		cachedTargets.removeIf(target -> target.page() == page);
		int count = 0;
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot != null && slot.hasItem() && isKuudraChestEntry(slot.getItem())) {
				cachedTargets.add(new ChestTarget(page, slotIndex));
				count++;
			}
		}
		return count;
	}

	private static ChestTarget nextCachedTarget() {
		ChestTarget best = null;
		for (ChestTarget target : cachedTargets) {
			if (target == null) {
				continue;
			}
			if (best == null
					|| target.page() > best.page()
					|| (target.page() == best.page() && target.slot() > best.slot())) {
				best = target;
			}
		}
		return best;
	}

	private static boolean isValidKuudraChestSlot(ChestMenu menu, int slotIndex) {
		if (menu == null || slotIndex < 0 || slotIndex >= containerSlotCount(menu)) {
			return false;
		}
		Slot slot = menu.slots.get(slotIndex);
		return slot != null && slot.hasItem() && isKuudraChestEntry(slot.getItem());
	}

	private static boolean isKuudraChestEntry(ItemStack stack) {
		String text = normalizedStackText(stack);
		return text.contains(KUUDRA_HOLLOW) && text.contains(CHESTS_EXPIRE);
	}

	private static int findSlotContaining(ChestMenu menu, String needle) {
		if (menu == null || needle == null || needle.isBlank()) {
			return -1;
		}
		String normalizedNeedle = TextNormalizer.normalize(needle);
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			if (normalizedStackText(slot.getItem()).contains(normalizedNeedle)) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static int findOpenRewardChestSlot(ChestMenu menu) {
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			String text = normalizedStackText(slot.getItem());
			if (text.contains(OPEN_REWARD_CHEST) && text.contains(CLICK_TO_OPEN)) {
				return slotIndex;
			}
		}
		return findSlotContaining(menu, OPEN_REWARD_CHEST);
	}

	private static int containerSlotCount(ChestMenu menu) {
		if (menu == null || menu.slots == null) {
			return 0;
		}
		return Mth.clamp(menu.slots.size() - 36, 0, menu.slots.size());
	}

	private static boolean hasAnyContainerItem(ChestMenu menu) {
		if (menu == null) {
			return false;
		}
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			if (hasContainerItem(menu, slotIndex)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasContainerItem(ChestMenu menu, int slotIndex) {
		if (menu == null || slotIndex < 0 || slotIndex >= containerSlotCount(menu)) {
			return false;
		}
		Slot slot = menu.slots.get(slotIndex);
		return slot != null && slot.hasItem();
	}

	private static String normalizedStackName(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		return TextNormalizer.normalize(stack.getHoverName().getString() + " " + stack.getDisplayName().getString());
	}

	private static String normalizedStackText(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}

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

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null && !customData.isEmpty()) {
			builder.append(customData.copyTag());
		}
		return TextNormalizer.normalize(builder.toString());
	}

	private static boolean clickSlot(Minecraft client, ChestMenu menu, int slot) {
		int delayMs = firstClickInCurrentGui ? configuredFirstClickDelayMs() : configuredClickDelayMs();
		boolean clicked = GuiClickThrottle.clickSlot(client, menu, slot, delayMs, delayMs);
		if (clicked) {
			firstClickInCurrentGui = false;
		}
		return clicked;
	}

	private static int configuredFirstClickDelayMs() {
		Double value = UiDefinitions.AUTO_OPEN_KUUDRA_CHEST_FIRST_CLICK_DELAY.get();
		int configured = value != null && Double.isFinite(value) ? (int) Math.round(value) : DEFAULT_FIRST_CLICK_DELAY_MS;
		return Mth.clamp(configured, MIN_FIRST_CLICK_DELAY_MS, MAX_FIRST_CLICK_DELAY_MS);
	}

	private static int configuredClickDelayMs() {
		Double value = UiDefinitions.AUTO_OPEN_KUUDRA_CHEST_CLICK_DELAY.get();
		int configured = value != null && Double.isFinite(value) ? (int) Math.round(value) : DEFAULT_CLICK_DELAY_MS;
		return Mth.clamp(configured, MIN_CLICK_DELAY_MS, MAX_CLICK_DELAY_MS);
	}

	private static double configuredRotationMultiplier() {
		Double value = UiDefinitions.AUTO_OPEN_KUUDRA_CHEST_ROTATION_MULTIPLIER.get();
		double configured = value != null && Double.isFinite(value) ? value : DEFAULT_ROTATION_MULTIPLIER;
		return Mth.clamp(configured, MIN_ROTATION_MULTIPLIER, MAX_ROTATION_MULTIPLIER);
	}

	private static void enterState(State nextState) {
		state = nextState == null ? State.IDLE : nextState;
		stateTicks = 0;
		if (state == State.WAIT_AFTER_PURCHASE) {
			postPurchaseGuiClosedTicks = -1;
		}
		if (state == State.OPEN_CROESUS) {
			lastInteractTick = -NPC_INTERACT_INTERVAL_TICKS;
			currentCroesusPage = 1;
			croesusTarget = null;
			croesusAimPoint = null;
		}
	}

	private static void stopInternal(String reason) {
		if (reason != null && !reason.isBlank()) {
			sendMessage(reason);
		}
		resetRuntime();
	}

	private static void resetRuntime() {
		running = false;
		state = State.IDLE;
		stateTicks = 0;
		lastInteractTick = 0;
		lastSeenGuiContainerId = -1;
		lastSeenGuiTitle = "";
		lastSeenGuiSignature = 0;
		guiStableTicks = 0;
		firstClickInCurrentGui = true;
		clearChestPlan();
		croesusTarget = null;
		croesusAimPoint = null;
		GuiClickThrottle.reset();
	}

	private static void clearChestPlan() {
		cachedTargets.clear();
		scanComplete = false;
		scanPage = 1;
		lastCachedScanPage = 0;
		currentCroesusPage = 1;
		clearPendingPageTransition();
		postPurchaseGuiClosedTicks = -1;
		activeTarget = null;
	}

	private static void sendMessage(String text) {
		FeatureChat.sendPrefixed(FEATURE_NAME, text);
	}

	private record ChestTarget(int page, int slot) {
	}
}
