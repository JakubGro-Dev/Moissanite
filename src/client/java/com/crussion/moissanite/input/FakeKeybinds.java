package com.crussion.moissanite.input;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.crussion.moissanite.ui.data.UiKeybind;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class FakeKeybinds {
	private static final List<Binding> BINDINGS = new ArrayList<>();
	private static final Set<Integer> DOWN_KEYS = new HashSet<>();
	private static boolean initialized;

	private FakeKeybinds() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(FakeKeybinds::handleClientTick);
	}

	public static void onKeyPress(UiKeybind keybind, Runnable action) {
		if (keybind == null || action == null) {
			return;
		}
		init();
		BINDINGS.add(new Binding(keybind, action));
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.getWindow() == null || client.screen != null) {
			DOWN_KEYS.clear();
			return;
		}

		Map<Integer, List<Runnable>> actionsByKey = new LinkedHashMap<>();
		for (Binding binding : BINDINGS) {
			Integer keyCode = binding.keybind().get();
			if (keyCode == null || keyCode == InputConstants.UNKNOWN.getValue()) {
				continue;
			}
			actionsByKey.computeIfAbsent(keyCode, ignored -> new ArrayList<>()).add(binding.action());
		}

		Set<Integer> nextDownKeys = new HashSet<>();
		for (Map.Entry<Integer, List<Runnable>> entry : actionsByKey.entrySet()) {
			int keyCode = entry.getKey();
			boolean down = KeybindKeys.isPressed(client.getWindow(), keyCode);
			if (!down) {
				continue;
			}
			nextDownKeys.add(keyCode);
			if (DOWN_KEYS.contains(keyCode)) {
				continue;
			}
			for (Runnable action : entry.getValue()) {
				action.run();
			}
		}

		DOWN_KEYS.clear();
		DOWN_KEYS.addAll(nextDownKeys);
	}

	private record Binding(UiKeybind keybind, Runnable action) {
	}
}
