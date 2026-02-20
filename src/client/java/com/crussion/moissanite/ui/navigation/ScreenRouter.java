package com.crussion.moissanite.ui.navigation;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenRouter {
	private final Map<ScreenIds, Supplier<? extends Screen>> routes = new EnumMap<>(ScreenIds.class);

	public void register(ScreenIds id, Supplier<? extends Screen> factory) {
		this.routes.put(id, factory);
	}

	public void open(ScreenIds id) {
		Supplier<? extends Screen> factory = this.routes.get(id);
		if (factory == null) {
			return;
		}
		Minecraft.getInstance().setScreen(factory.get());
	}
}

