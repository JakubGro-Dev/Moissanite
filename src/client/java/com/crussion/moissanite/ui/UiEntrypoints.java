package com.crussion.moissanite.ui;

import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.ui.navigation.ScreenRouter;
import com.crussion.moissanite.ui.screen.CommandReplacerScreen;
import com.crussion.moissanite.ui.screen.InventoryOverlayScreen;
import com.crussion.moissanite.ui.screen.SettingsScreen;

public final class UiEntrypoints {
	private static final ScreenRouter ROUTER = new ScreenRouter();

	private UiEntrypoints() {
	}

	public static void init() {
		ROUTER.register(ScreenIds.INVENTORY_OVERLAY, InventoryOverlayScreen::new);
		ROUTER.register(ScreenIds.SETTINGS, SettingsScreen::new);
		ROUTER.register(ScreenIds.COMMAND_REPLACER, CommandReplacerScreen::new);
	}

	public static void open(ScreenIds id) {
		ROUTER.open(id);
	}
}

