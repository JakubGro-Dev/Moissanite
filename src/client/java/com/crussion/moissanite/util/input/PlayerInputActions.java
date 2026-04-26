package com.crussion.moissanite.util.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class PlayerInputActions {
	private static final int MIN_INPUT_HOLD_TICKS = 2;

	private PlayerInputActions() {
	}

	public static boolean jump() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null) {
			return false;
		}
		if (!client.player.onGround()) {
			return false;
		}
		KeyHoldController.holdBoundKey(client.options.keyJump, MIN_INPUT_HOLD_TICKS);
		client.player.jumpFromGround();
		return true;
	}

	public static boolean leftClick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null) {
			return false;
		}
		KeyMapping attackKey = client.options.keyAttack;
		KeyHoldController.clickBoundKey(attackKey);
		KeyHoldController.holdBoundKey(attackKey, MIN_INPUT_HOLD_TICKS);
		client.player.swing(InteractionHand.MAIN_HAND);
		return true;
	}

	public static boolean rightClick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return false;
		}
		KeyMapping useKey = client.options.keyUse;
		KeyHoldController.clickBoundKey(useKey);
		KeyHoldController.holdBoundKey(useKey, MIN_INPUT_HOLD_TICKS);
		return true;
	}

	public static boolean shiftRightClick() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null) {
			return false;
		}
		KeyHoldController.holdBoundKey(client.options.keyShift, MIN_INPUT_HOLD_TICKS);
		return rightClick();
	}
}
