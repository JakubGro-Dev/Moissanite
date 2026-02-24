package com.crussion.moissanite.util.customgui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class CustomGuiAccess {
	private CustomGuiAccess() {
	}

	@Nullable
	public static CustomGui get(AbstractContainerScreen<?> screen) {
		if (!(screen instanceof HasCustomGui hasCustomGui)) {
			return null;
		}
		return hasCustomGui.moissanite$getCustomGui();
	}

	public static void set(AbstractContainerScreen<?> screen, @Nullable CustomGui customGui) {
		if (!(screen instanceof HasCustomGui hasCustomGui)) {
			return;
		}
		hasCustomGui.moissanite$setCustomGui(customGui);
	}
}
