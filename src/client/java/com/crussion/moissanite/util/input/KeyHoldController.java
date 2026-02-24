package com.crussion.moissanite.util.input;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class KeyHoldController {
	private static final Map<KeyMapping, Integer> HELD_KEYS_RELEASE_TICK = new IdentityHashMap<>();
	private static boolean initialized;

	private KeyHoldController() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(KeyHoldController::onClientTick);
	}

	public static void clickBoundKey(KeyMapping keyMapping) {
		if (keyMapping == null) {
			return;
		}

		String keyName = keyMapping.saveString();
		if (keyName == null || keyName.isBlank()) {
			return;
		}

		InputConstants.Key key = InputConstants.getKey(keyName);
		if (key == null || key == InputConstants.UNKNOWN) {
			return;
		}
		KeyMapping.click(key);
	}

	public static void holdBoundKey(KeyMapping keyMapping, int ticks) {
		init();
		if (keyMapping == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		int holdTicks = Math.max(1, ticks);
		int releaseTick = client.player.tickCount + holdTicks;
		keyMapping.setDown(true);

		Integer existingReleaseTick = HELD_KEYS_RELEASE_TICK.get(keyMapping);
		if (existingReleaseTick == null || releaseTick > existingReleaseTick) {
			HELD_KEYS_RELEASE_TICK.put(keyMapping, releaseTick);
		}
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.player == null) {
			releaseAllKeys();
			HELD_KEYS_RELEASE_TICK.clear();
			return;
		}

		int now = client.player.tickCount;
		HELD_KEYS_RELEASE_TICK.entrySet().removeIf(entry -> {
			if (now < entry.getValue()) {
				return false;
			}
			KeyMapping key = entry.getKey();
			if (key != null) {
				key.setDown(false);
			}
			return true;
		});
	}

	private static void releaseAllKeys() {
		for (KeyMapping key : HELD_KEYS_RELEASE_TICK.keySet()) {
			if (key != null) {
				key.setDown(false);
			}
		}
	}
}
