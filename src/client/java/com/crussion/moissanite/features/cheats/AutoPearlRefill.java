package com.crussion.moissanite.features.cheats;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoPearlRefill {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final int TARGET_PEARL_COUNT = 16;
	private static final int REFILL_TRIGGER_PEARL_COUNT = 2;
	private static final int MIN_INTERVAL_TICKS = 1;
	private static final int MAX_INTERVAL_TICKS = 120;
	private static final int DEFAULT_INTERVAL_TICKS = 20;
	private static final int REQUEST_TIMEOUT_TICKS = 60;
	private static final String REFILL_COMMAND_PREFIX = "gfs ender_pearl ";

	private static boolean initialized;
	private static int ticksUntilNextCheck;
	private static boolean refillRequestPending;
	private static int pendingRequestStartPearlCount;
	private static int pendingRequestTimeoutTicks;

	private AutoPearlRefill() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(AutoPearlRefill::handleClientTick);
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.player.connection == null) {
			resetRuntimeState();
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL_REFILL.get())) {
			resetRuntimeState();
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			resetRuntimeState();
			return;
		}

		int pearlCount = countPearls(client.player.getInventory());
		if (shouldWaitForPendingRequest(pearlCount)) {
			return;
		}

		if (ticksUntilNextCheck > 0) {
			ticksUntilNextCheck--;
			return;
		}

		ticksUntilNextCheck = configuredIntervalTicks() - 1;
		if (pearlCount > REFILL_TRIGGER_PEARL_COUNT || pearlCount >= TARGET_PEARL_COUNT) {
			return;
		}

		int missingPearls = TARGET_PEARL_COUNT - pearlCount;
		if (missingPearls <= 0) {
			return;
		}
		client.player.connection.sendCommand(REFILL_COMMAND_PREFIX + missingPearls);
		refillRequestPending = true;
		pendingRequestStartPearlCount = pearlCount;
		pendingRequestTimeoutTicks = REQUEST_TIMEOUT_TICKS;
	}

	private static boolean shouldWaitForPendingRequest(int pearlCount) {
		if (!refillRequestPending) {
			return false;
		}
		if (pearlCount >= TARGET_PEARL_COUNT || pearlCount > pendingRequestStartPearlCount) {
			clearPendingRequest();
			return false;
		}
		if (pendingRequestTimeoutTicks > 0) {
			pendingRequestTimeoutTicks--;
			return true;
		}
		clearPendingRequest();
		return false;
	}

	private static int configuredIntervalTicks() {
		Double configured = UiDefinitions.AUTO_PEARL_REFILL_EVERY_TICKS.get();
		double raw = configured != null && Double.isFinite(configured) ? configured : DEFAULT_INTERVAL_TICKS;
		return Mth.clamp((int) Math.round(raw), MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
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
		refillRequestPending = false;
		pendingRequestStartPearlCount = 0;
		pendingRequestTimeoutTicks = 0;
	}
}
