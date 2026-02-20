package com.crussion.moissanite.input;

import org.lwjgl.glfw.GLFW;

import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.ui.screen.InventoryOverlayScreen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

public final class UiKeybinds {
	private static final String OPEN_UI_KEY = "Open UI";
	private static final String OPEN_UI_NUMPAD_KEY = "Open UI 2";
	private static final KeyMapping.Category MOISSANITE_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("moissanite", "moissanite"));
	private static final Map<KeyMapping, Runnable> ACTIONS = new LinkedHashMap<>();
	private static KeyMapping openUi;
	private static KeyMapping openUiNumpad;

	private UiKeybinds() {
	}

	public static void init() {
		openUi = bind(OPEN_UI_KEY, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_INSERT,
				() -> toggleOverlay(Minecraft.getInstance()));
		openUiNumpad = bind(OPEN_UI_NUMPAD_KEY, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_0,
				() -> toggleOverlay(Minecraft.getInstance()));

		ClientTickEvents.END_CLIENT_TICK.register(UiKeybinds::handleClientTick);
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null) {
			return;
		}

		for (Map.Entry<KeyMapping, Runnable> entry : ACTIONS.entrySet()) {
			KeyMapping mapping = entry.getKey();
			Runnable action = entry.getValue();
			while (mapping != null && mapping.consumeClick()) {
				action.run();
			}
		}
	}

	private static void toggleOverlay(Minecraft client) {
		if (client == null) {
			return;
		}
		if (client.screen instanceof InventoryOverlayScreen) {
			client.setScreen(null);
		} else {
			UiEntrypoints.open(ScreenIds.INVENTORY_OVERLAY);
		}
	}

	public static boolean isToggleKey(KeyEvent event) {
		return (openUi != null && openUi.matches(event)) || (openUiNumpad != null && openUiNumpad.matches(event));
	}

	public static KeyMapping bind(String name, int defaultKey, Runnable action) {
		return bind(name, InputConstants.Type.KEYSYM, defaultKey, action);
	}

	public static KeyMapping bind(String name, InputConstants.Type type, int defaultKey, Runnable action) {
		KeyMapping mapping = KeyBindingHelper.registerKeyBinding(
				new KeyMapping(name, type, defaultKey, MOISSANITE_CATEGORY));
		ACTIONS.put(mapping, action);
		return mapping;
	}
}
