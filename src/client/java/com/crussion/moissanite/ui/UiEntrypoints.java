package com.crussion.moissanite.ui;

import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.ui.navigation.ScreenRouter;
import com.crussion.moissanite.ui.screen.ImGuiCommandReplacerScreen;
import com.crussion.moissanite.ui.screen.ImGuiInventoryOverlayScreen;
import com.crussion.moissanite.ui.screen.ImGuiRouteManagerScreen;

public final class UiEntrypoints {
	private static final ScreenRouter ROUTER = new ScreenRouter();

	private UiEntrypoints() {
	}

	public static void init() {
		ROUTER.register(ScreenIds.INVENTORY_OVERLAY, ImGuiInventoryOverlayScreen::new);
		ROUTER.register(ScreenIds.SETTINGS, () -> new ImGuiInventoryOverlayScreen(true));
		ROUTER.register(ScreenIds.COMMAND_REPLACER, ImGuiCommandReplacerScreen::new);
		ROUTER.register(ScreenIds.ROUTES, ImGuiRouteManagerScreen::new);
	}

	public static void open(ScreenIds id) {
		ROUTER.open(id);
	}
}

