package com.crussion.moissanite.features.general;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.ui.data.UiInput;
import com.crussion.moissanite.ui.data.UiSwitch;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class WindowNameChanger {
	private static boolean initialized;
	private static boolean customTitleApplied;

	private WindowNameChanger() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		UiInput input = UiDefinitions.CUSTOM_TITLE;
		UiSwitch enabled = UiDefinitions.ENABLE_CUSTOM_TITLE;
		if (input == null || enabled == null) {
			return;
		}

		input.bind(value -> syncWindowTitle(Minecraft.getInstance()));
		enabled.bind(value -> syncWindowTitle(Minecraft.getInstance()));
		ClientTickEvents.END_CLIENT_TICK.register(WindowNameChanger::handleClientTick);
		syncWindowTitle(Minecraft.getInstance());
	}

	private static void handleClientTick(Minecraft client) {
		syncWindowTitle(client);
	}

	private static void syncWindowTitle(Minecraft client) {
		if (client == null || client.getWindow() == null) {
			return;
		}

		if (!Boolean.TRUE.equals(UiDefinitions.ENABLE_CUSTOM_TITLE.get())) {
			restoreDefaultTitle(client);
			return;
		}

		String title = UiDefinitions.CUSTOM_TITLE.get();
		String trimmed = title == null ? "" : title.trim();
		if (trimmed.isEmpty()) {
			restoreDefaultTitle(client);
			return;
		}

		client.getWindow().setTitle(trimmed);
		customTitleApplied = true;
	}

	private static void restoreDefaultTitle(Minecraft client) {
		if (!customTitleApplied) {
			return;
		}
		client.updateTitle();
		customTitleApplied = false;
	}
}
