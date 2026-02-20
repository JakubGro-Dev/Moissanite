package com.crussion.moissanite.definitions;

import com.crussion.moissanite.features.visual.RenderImageOnScreen;
import com.crussion.moissanite.ui.data.UiCatalog;
import com.crussion.moissanite.ui.data.UiCategory;
import com.crussion.moissanite.ui.data.UiDropdown;
import com.crussion.moissanite.ui.data.UiInput;
import com.crussion.moissanite.ui.data.UiKeybind;
import com.crussion.moissanite.ui.data.UiNumber;
import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.data.UiSection;
import com.crussion.moissanite.ui.data.UiSwitch;
import com.crussion.moissanite.ui.data.UiButton;
import com.mojang.blaze3d.platform.InputConstants;

public final class UiDefinitions {
	public static final UiCategory UISETTINGS = UiCatalog.category("Settings"); // Do not touch it or i'll touch you.

	// Categories
	public static final UiCategory GENERAL = UiCatalog.category("General");
	public static final UiCategory DUNGEONS = UiCatalog.category("Dungeons");
	public static final UiCategory VISUAL = UiCatalog.category("Visual");
	public static final UiCategory MISC = UiCatalog.category("Misc");
	public static final UiCategory CHEATS = UiCatalog.category("Cheats");

	// General

		// Sections
		public static final UiSection GENERAL_GENERAL = GENERAL.section("General");

		// Bindables


		// Visibilities
		static {
		}



	// Dungeons

		// Sections
		public static final UiSection DUNGEONS_GENERAL = DUNGEONS.section("General");

		// Bindables


		// Visibilities
		static {
		}



	// Visual

		// Sections
		public static final UiSection VISUAL_GENERAL = VISUAL.section("General");
		public static final UiSection VISUAL_IMAGE_SECTION = VISUAL.section("Image on screen");

		// Bindables
		public static final UiSwitch IMAGE_ON_SCREEN = VISUAL_GENERAL.toggle("Image On Screen", false);
		public static final UiDropdown IMAGE_ON_SCREEN_SELECT = VISUAL_IMAGE_SECTION.dropdown("Select Image", "");
		public static final UiSlider IMAGE_ON_SCREEN_SCALE = VISUAL_IMAGE_SECTION.slider("Scale", 0.1, 2.0, 1.0);
		public static final UiNumber IMAGE_ON_SCREEN_X = VISUAL_IMAGE_SECTION.number("Image X", -10000, 10000, 8);
		public static final UiNumber IMAGE_ON_SCREEN_Y = VISUAL_IMAGE_SECTION.number("Image Y", -10000, 10000, 8);
		public static final UiButton IMAGE_ON_SCREEN_MOVE = VISUAL_IMAGE_SECTION.button("Move", "Move", button -> RenderImageOnScreen.openMoveScreen());
		public static final UiButton IMAGE_ON_SCREEN_REFRESH = VISUAL_IMAGE_SECTION.button("Refresh", "Refresh", button -> RenderImageOnScreen.refreshImageOptions());


		// Visibilities
		static {
			IMAGE_ON_SCREEN_SELECT.visibleWhen(IMAGE_ON_SCREEN);
			IMAGE_ON_SCREEN_SCALE.visibleWhen(IMAGE_ON_SCREEN);
			IMAGE_ON_SCREEN_X.visibleWhen(() -> false);
			IMAGE_ON_SCREEN_Y.visibleWhen(() -> false);
			IMAGE_ON_SCREEN_MOVE.visibleWhen(() -> Boolean.TRUE.equals(IMAGE_ON_SCREEN.get()) && !IMAGE_ON_SCREEN_SELECT.get().isBlank());
			IMAGE_ON_SCREEN_REFRESH.visibleWhen(IMAGE_ON_SCREEN);
		}



	// Misc

		// Sections
		public static final UiSection MISC_GENERAL = MISC.section("General");
		

		// Bindables
		public static final UiSwitch ALWAYS_SPRINT = MISC_GENERAL.toggle("Always Sprint", false);

		public static final UiSwitch ENABLE_CUSTOM_TITLE = MISC_GENERAL.toggle("Enable Custom Title", false);
		public static final UiInput CUSTOM_TITLE = MISC_GENERAL.input("Custom Title", null);

		// Visibilities
		static {
			CUSTOM_TITLE.visibleWhen(ENABLE_CUSTOM_TITLE);
		}
	
	

	// Cheats
		// Sections
		public static final UiSection CHEATS_GENERAL = CHEATS.section("General");
		public static final UiSection WARDROBE_KEYBINDS = CHEATS.section("Wardrobe Keybinds");

		// Bindables
		public static final UiSwitch ENABLE_GHOST_WARDROBE = CHEATS_GENERAL.toggle("Enable Ghost Wardrobe", false);
		public static final UiKeybind WD_ONE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 1", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_TWO_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 2", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_THREE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 3", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_FOUR_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 4", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_FIVE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 5", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_SIX_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 6", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_SEVEN_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 7", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_EIGHT_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 8", InputConstants.UNKNOWN.getValue());
		public static final UiKeybind WD_NINE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 9", InputConstants.UNKNOWN.getValue());

		// Visibilities
		static {
			WD_ONE_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_TWO_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_THREE_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_FOUR_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_FIVE_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_SIX_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_SEVEN_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_EIGHT_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
			WD_NINE_KEYBIND.visibleWhen(ENABLE_GHOST_WARDROBE);
		}



	public static void init() {
	}

	private UiDefinitions() {
	}
}
