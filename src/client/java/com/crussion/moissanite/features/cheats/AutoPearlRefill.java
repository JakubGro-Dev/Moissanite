package com.crussion.moissanite.features.cheats;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoPearlRefill {
	private static final String KUUDRA_MODE = "kuudra";
	private static final String KUUDRA_MAP = "Kuudra's Hollow";
	private static final String DUNGEON_MODE = "dungeon";
	private static final String HAUNT_NAME = "Haunt";
	private static final int TARGET_PEARL_COUNT = 16;
	private static final int MIN_REFILL_AMOUNT = 4;
	private static final int CHECK_INTERVAL_TICKS = 20;
	private static final long COMMAND_COOLDOWN_MS = 1_500L;
	private static final long REQUEST_IN_FLIGHT_MS = 1_750L;
	private static final String REFILL_COMMAND_PREFIX = "gfs ender_pearl ";
	private static final String SACKS_TEXT = "Sacks";
	private static final String ENDER_PEARL_TEXT = "Ender Pearl";

	private static boolean initialized;
	private static int ticksUntilNextCheck;
	private static boolean refillRequestInFlight;
	private static long refillRequestExpiresAtMs;
	private static long nextRefillCommandAtMs;

	private AutoPearlRefill() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.START_CLIENT_TICK.register(AutoPearlRefill::handleClientTick);
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.player.connection == null) {
			resetRuntimeState();
			return;
		}

		SkyBlockLocationTracker.requestRefreshIfNeeded();
		boolean inDungeons = isInDungeons();
		if (!isEnabledForCurrentLocation(inDungeons)) {
			resetRuntimeState();
			return;
		}
		if (isDead(client.player)) {
			return;
		}
		if (client.screen != null) {
			return;
		}
		if (client.player == null) {
			return;
		}

		if (ticksUntilNextCheck > 0) {
			ticksUntilNextCheck--;
			return;
		}

		ticksUntilNextCheck = CHECK_INTERVAL_TICKS - 1;
		checkAndRefill(client);
	}

	public static void onUseItemAttempt(Player player, InteractionHand hand) {
		if (player == null || hand == null) {
			return;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack == null || stack.isEmpty() || !stack.is(Items.ENDER_PEARL)) {
			return;
		}
		ticksUntilNextCheck = 0;
	}

	public static void onSystemChat(Component message) {
		if (!refillRequestInFlight) {
			return;
		}

		String text = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString()).trim();
		if (text.contains(SACKS_TEXT) && text.contains(ENDER_PEARL_TEXT)) {
			refillRequestInFlight = false;
			ticksUntilNextCheck = 0;
		}
	}

	private static void checkAndRefill(Minecraft client) {
		long now = System.currentTimeMillis();
		if (refillRequestInFlight) {
			if (now < refillRequestExpiresAtMs) {
				return;
			}
			refillRequestInFlight = false;
		}
		if (now < nextRefillCommandAtMs) {
			return;
		}

		int pearlCount = countPearls(client.player.getInventory());
		if (pearlCount >= TARGET_PEARL_COUNT) {
			return;
		}

		int needed = TARGET_PEARL_COUNT - pearlCount;
		if (needed < MIN_REFILL_AMOUNT) {
			return;
		}

		client.player.connection.sendCommand(REFILL_COMMAND_PREFIX + needed);
		refillRequestInFlight = true;
		refillRequestExpiresAtMs = now + REQUEST_IN_FLIGHT_MS;
		nextRefillCommandAtMs = now + COMMAND_COOLDOWN_MS;
	}

	private static boolean isEnabledForCurrentLocation(boolean inDungeons) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL_REFILL.get())) {
			return false;
		}
		return (inDungeons && Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL_REFILL_DUNGEONS.get()))
				|| (isInKuudra() && Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL_REFILL_KUUDRA.get()));
	}

	private static boolean isInKuudra() {
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		return location.isSkyBlock()
				&& (KUUDRA_MODE.equalsIgnoreCase(location.mode()) || KUUDRA_MAP.equalsIgnoreCase(location.map()));
	}

	private static boolean isInDungeons() {
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		return location.isSkyBlock() && DUNGEON_MODE.equalsIgnoreCase(location.mode());
	}

	private static int countPearls(Inventory inventory) {
		if (inventory == null) {
			return 0;
		}

		int totalPearls = 0;
		int size = inventory.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack == null || stack.isEmpty() || !stack.is(Items.ENDER_PEARL)) {
				continue;
			}
			totalPearls += stack.getCount();
		}
		return totalPearls;
	}

	private static void resetRuntimeState() {
		ticksUntilNextCheck = 0;
		clearPendingRequest();
	}

	private static void clearPendingRequest() {
		refillRequestInFlight = false;
		refillRequestExpiresAtMs = 0L;
		nextRefillCommandAtMs = 0L;
	}

	private static boolean isDead(Player player) {
		if (player == null) {
			return false;
		}
		ItemStack stack = player.getInventory().getItem(0);
		return stack != null && stack.getCustomName() != null && HAUNT_NAME.equals(stack.getCustomName().getString());
	}
}
