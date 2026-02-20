package com.crussion.moissanite;

import net.fabricmc.api.ClientModInitializer;

import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.input.FakeKeybinds;
import com.crussion.moissanite.input.UiKeybinds;
import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.command.MoissaniteCommands;
import com.crussion.moissanite.features.cheats.WardrobeKeybinds;
import com.crussion.moissanite.features.misc.AlwaysSprint;
import com.crussion.moissanite.features.misc.WindowNameChanger;
import com.crussion.moissanite.features.visual.RenderImageOnScreen;
import com.crussion.moissanite.config.MoissaniteConfig;

public class MoissaniteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		UiDefinitions.init();
		WindowNameChanger.init();
		MoissaniteConfig.init();
		UiEntrypoints.init();
		MoissaniteCommands.init();
		UiKeybinds.init();
		FakeKeybinds.init();
		WardrobeKeybinds.init();
		AlwaysSprint.init();
		RenderImageOnScreen.init();
	}
}
