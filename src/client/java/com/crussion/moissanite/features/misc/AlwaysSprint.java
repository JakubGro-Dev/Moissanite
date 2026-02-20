package com.crussion.moissanite.features.misc;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class AlwaysSprint {
	private static boolean initialized;

	private AlwaysSprint() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(AlwaysSprint::handleClientTick);
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.options == null) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.ALWAYS_SPRINT.get())) {
			return;
		}
		boolean shouldSprint = client.options.keyUp.isDown() && !client.player.isCrouching();
		client.player.setSprinting(shouldSprint);
	}
}
