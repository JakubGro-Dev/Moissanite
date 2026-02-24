package com.crussion.moissanite.util;

import com.crussion.moissanite.util.input.KeyHoldController;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.tick.TickTaskScheduler;

public final class CheatAutomationRuntime {
	private static boolean initialized;

	private CheatAutomationRuntime() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		TickTaskScheduler.init();
		KeyHoldController.init();
		RotationController.init();
	}
}
