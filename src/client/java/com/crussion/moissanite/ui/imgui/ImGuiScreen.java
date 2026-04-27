package com.crussion.moissanite.ui.imgui;

import imgui.ImGuiIO;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ImGuiScreen extends Screen {
	protected ImGuiScreen(Component title) {
		super(title);
	}

	public final void open() {
		ImGuiHandler.open(this);
	}

	@Override
	protected void init() {
		ImGuiHandler.prepareForScreen();
	}

	@Override
	public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public abstract void renderImGui(ImGuiIO io);
}
