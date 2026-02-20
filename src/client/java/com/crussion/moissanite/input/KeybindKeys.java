package com.crussion.moissanite.input;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

public final class KeybindKeys {
	private KeybindKeys() {
	}

	public static int normalizeCapturedKey(int keyCode, int scanCode) {
		if (keyCode != InputConstants.UNKNOWN.getValue()) {
			return keyCode;
		}
		int numpadKey = numpadKeyFromScanCode(scanCode);
		return numpadKey != InputConstants.UNKNOWN.getValue() ? numpadKey : keyCode;
	}

	public static boolean isPressed(Window window, int keyCode) {
		if (window == null) {
			return false;
		}
		return InputConstants.isKeyDown(window, keyCode);
	}

	public static String displayName(int keyCode) {
		return switch (keyCode) {
			case GLFW.GLFW_KEY_KP_0 -> "NUM LOCK 0";
			case GLFW.GLFW_KEY_KP_1 -> "NUM LOCK 1";
			case GLFW.GLFW_KEY_KP_2 -> "NUM LOCK 2";
			case GLFW.GLFW_KEY_KP_3 -> "NUM LOCK 3";
			case GLFW.GLFW_KEY_KP_4 -> "NUM LOCK 4";
			case GLFW.GLFW_KEY_KP_5 -> "NUM LOCK 5";
			case GLFW.GLFW_KEY_KP_6 -> "NUM LOCK 6";
			case GLFW.GLFW_KEY_KP_7 -> "NUM LOCK 7";
			case GLFW.GLFW_KEY_KP_8 -> "NUM LOCK 8";
			case GLFW.GLFW_KEY_KP_9 -> "NUM LOCK 9";
			case GLFW.GLFW_KEY_KP_DECIMAL -> "NUM LOCK .";
			default -> InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase(Locale.ROOT);
		};
	}

	private static int numpadKeyFromScanCode(int scanCode) {
		if (scanCode <= 0) {
			return InputConstants.UNKNOWN.getValue();
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_0)) {
			return GLFW.GLFW_KEY_KP_0;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_1)) {
			return GLFW.GLFW_KEY_KP_1;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_2)) {
			return GLFW.GLFW_KEY_KP_2;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_3)) {
			return GLFW.GLFW_KEY_KP_3;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_4)) {
			return GLFW.GLFW_KEY_KP_4;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_5)) {
			return GLFW.GLFW_KEY_KP_5;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_6)) {
			return GLFW.GLFW_KEY_KP_6;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_7)) {
			return GLFW.GLFW_KEY_KP_7;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_8)) {
			return GLFW.GLFW_KEY_KP_8;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_9)) {
			return GLFW.GLFW_KEY_KP_9;
		}
		if (scanCode == GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_KP_DECIMAL)) {
			return GLFW.GLFW_KEY_KP_DECIMAL;
		}
		return InputConstants.UNKNOWN.getValue();
	}
}
