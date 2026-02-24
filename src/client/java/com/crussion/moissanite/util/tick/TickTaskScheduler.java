package com.crussion.moissanite.util.tick;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class TickTaskScheduler {
	private static final List<ScheduledTickAction> SCHEDULED_TICK_ACTIONS = new ArrayList<>();
	private static boolean initialized;

	private TickTaskScheduler() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(TickTaskScheduler::onClientTick);
	}

	public static void schedule(int amountOfTicksToWaitBeforeRunning, Runnable action) {
		init();
		if (action == null) {
			return;
		}

		int waitTicks = Math.max(0, amountOfTicksToWaitBeforeRunning);
		if (waitTicks == 0) {
			action.run();
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		int runAtTick = client.player.tickCount + waitTicks;
		SCHEDULED_TICK_ACTIONS.add(new ScheduledTickAction(runAtTick, action));
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.player == null) {
			SCHEDULED_TICK_ACTIONS.clear();
			return;
		}

		int now = client.player.tickCount;
		List<Runnable> dueActions = new ArrayList<>();
		for (int i = SCHEDULED_TICK_ACTIONS.size() - 1; i >= 0; i--) {
			ScheduledTickAction scheduled = SCHEDULED_TICK_ACTIONS.get(i);
			if (now < scheduled.runAtTick()) {
				continue;
			}
			SCHEDULED_TICK_ACTIONS.remove(i);
			if (scheduled.action() != null) {
				dueActions.add(scheduled.action());
			}
		}

		for (Runnable dueAction : dueActions) {
			try {
				dueAction.run();
			} catch (Exception ignored) {
			}
		}
	}

	private record ScheduledTickAction(int runAtTick, Runnable action) {
	}
}
