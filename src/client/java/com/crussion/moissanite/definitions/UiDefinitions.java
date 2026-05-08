package com.crussion.moissanite.definitions;

import com.crussion.moissanite.features.cheats.AutoPearl;
import com.crussion.moissanite.features.cheats.AutoRend_Reworked;
import com.crussion.moissanite.features.cheats.AutoExperimentMacro;
import com.crussion.moissanite.features.cheats.KuudraPosTracker;
import com.crussion.moissanite.features.kuudra.KuudraAutoOpenChest;
import com.crussion.moissanite.features.kuudra.KuudraAutoPickupSupply;
import com.crussion.moissanite.features.visual.RenderImageOnScreen;
import com.crussion.moissanite.features.visual.storage.StorageOverlayFeature;
import com.crussion.moissanite.features.mining.UniqueServerHopper;
import com.crussion.moissanite.update.ModUpdater;
import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.data.UiCatalog;
import com.crussion.moissanite.ui.data.UiCategory;
import com.crussion.moissanite.ui.data.UiColor;
import com.crussion.moissanite.ui.data.UiDropdown;
import com.crussion.moissanite.ui.data.UiInput;
import com.crussion.moissanite.ui.data.UiKeybind;
import com.crussion.moissanite.ui.data.UiNumber;
import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.data.UiSection;
import com.crussion.moissanite.ui.data.UiSwitch;
import com.crussion.moissanite.ui.data.UiButton;
import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;

public final class UiDefinitions {
	public static final UiCategory UISETTINGS = UiCatalog.category("Settings"); // Do not touch it or i'll touch you.

	// Categories
	public static final UiCategory GENERAL = UiCatalog.category("General");
	public static final UiCategory DUNGEONS = UiCatalog.category("Dungeons");
	public static final UiCategory KUUDRA = UiCatalog.category("Kuudra");
	public static final UiCategory MINING = UiCatalog.category("Mining");
	public static final UiCategory VISUAL = UiCatalog.category("Visual");
	public static final UiCategory CHAT = UiCatalog.category("Chat");
	public static final UiCategory MISC = UiCatalog.category("Misc");
	public static final UiCategory CHEATS = UiCatalog.category("Cheats");

	// General

	// Sections
	public static final UiSection GENERAL_GENERAL = GENERAL.section("General");

	// Bindables
	public static final UiSwitch ZOOM = GENERAL_GENERAL.toggle("Zoom", false);
	public static final UiKeybind ZOOM_KEYBIND = GENERAL_GENERAL.keybind("Hold Zoom",
			InputConstants.UNKNOWN.getValue());

	// Visibilities
	static {
		ZOOM_KEYBIND.visibleWhen(ZOOM);
	}

	// Mining

	// Sections
	public static final UiSection MINING_STRUCTURE_SCANNER = MINING.section("Structure Scanner");
	public static final UiSection MINING_UNIQUE_SERVER_HOPPER = MINING.section("Unique Server Hopper");

	// Bindables
	public static final UiSwitch STRUCTURE_SCANNER = MINING_STRUCTURE_SCANNER.toggle("Structure Scanner", false);
	public static final UiSwitch STRUCTURE_SCANNER_DRAGON_LAIR = MINING_STRUCTURE_SCANNER.toggle("Dragon Lair", false);
	public static final UiSwitch STRUCTURE_SCANNER_MINES_OF_DIVAN = MINING_STRUCTURE_SCANNER.toggle("Mines Of Divan", false);
	public static final UiSwitch STRUCTURE_SCANNER_BLUE_PRECURSOR_CITY = MINING_STRUCTURE_SCANNER.toggle("Precursor City", false);
	public static final UiSwitch STRUCTURE_SCANNER_GOBLIN_KING = MINING_STRUCTURE_SCANNER.toggle("Goblin King", false);
	public static final UiSwitch STRUCTURE_SCANNER_GOBLIN_QUEEN = MINING_STRUCTURE_SCANNER.toggle("Goblin Queen", false);
	public static final UiSwitch STRUCTURE_SCANNER_JUNGLE_TEMPLE = MINING_STRUCTURE_SCANNER.toggle("Jungle Temple", false);
	public static final UiSwitch STRUCTURE_SCANNER_GROTTO = MINING_STRUCTURE_SCANNER.toggle("Grotto", false);
	public static final UiSwitch STRUCTURE_SCANNER_CORLEONE = MINING_STRUCTURE_SCANNER.toggle("Corleone", false);
	public static final UiSwitch STRUCTURE_SCANNER_BAL = MINING_STRUCTURE_SCANNER.toggle("Bal", false);
	public static final UiSwitch UNIQUE_SERVER_HOPPER = MINING_UNIQUE_SERVER_HOPPER.toggle("Unique Server Hopper", false);
	public static final UiInput UNIQUE_SERVER_HOPPER_COMMAND_1 = MINING_UNIQUE_SERVER_HOPPER.input("Command 1", "warp hub").maxLength(120);
	public static final UiInput UNIQUE_SERVER_HOPPER_COMMAND_2 = MINING_UNIQUE_SERVER_HOPPER.input("Command 2", "warp crystal_hollows").maxLength(120);
	public static final UiSlider UNIQUE_SERVER_HOPPER_WORLD_ENTER_DELAY = MINING_UNIQUE_SERVER_HOPPER.slider("World Enter Delay", 0.5, 60.0, 4.5, 0.1);
	public static final UiSlider UNIQUE_SERVER_HOPPER_RETRY_DELAY = MINING_UNIQUE_SERVER_HOPPER.slider("Retry Delay", 0.5, 60.0, 2.0, 0.1);
	public static final UiSlider UNIQUE_SERVER_HOPPER_LOCRAW_DELAY = MINING_UNIQUE_SERVER_HOPPER.slider("Locraw Delay", 0.2, 10.0, 1.2, 0.1);
	public static final UiButton UNIQUE_SERVER_HOPPER_START = MINING_UNIQUE_SERVER_HOPPER.button("Start", "Start", button -> UniqueServerHopper.start());
	public static final UiButton UNIQUE_SERVER_HOPPER_STOP = MINING_UNIQUE_SERVER_HOPPER.button("Stop", "Stop", button -> UniqueServerHopper.stop());
	public static final UiButton UNIQUE_SERVER_HOPPER_CLEAN_CACHE = MINING_UNIQUE_SERVER_HOPPER.button("Clean Cache", "Clean", button -> UniqueServerHopper.cleanCache());

	// Visibilities
	static {
		STRUCTURE_SCANNER_DRAGON_LAIR.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_MINES_OF_DIVAN.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_BLUE_PRECURSOR_CITY.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_GOBLIN_KING.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_GOBLIN_QUEEN.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_JUNGLE_TEMPLE.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_GROTTO.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_CORLEONE.visibleWhen(STRUCTURE_SCANNER);
		STRUCTURE_SCANNER_BAL.visibleWhen(STRUCTURE_SCANNER);
		UNIQUE_SERVER_HOPPER_COMMAND_1.visibleWhen(UNIQUE_SERVER_HOPPER);
		UNIQUE_SERVER_HOPPER_COMMAND_2.visibleWhen(UNIQUE_SERVER_HOPPER);
		UNIQUE_SERVER_HOPPER_WORLD_ENTER_DELAY.visibleWhen(UNIQUE_SERVER_HOPPER);
		UNIQUE_SERVER_HOPPER_RETRY_DELAY.visibleWhen(UNIQUE_SERVER_HOPPER);
		UNIQUE_SERVER_HOPPER_LOCRAW_DELAY.visibleWhen(UNIQUE_SERVER_HOPPER);
		UNIQUE_SERVER_HOPPER_START.visibleWhen(() -> Boolean.TRUE.equals(UNIQUE_SERVER_HOPPER.get()) && !UniqueServerHopper.isRunning());
		UNIQUE_SERVER_HOPPER_STOP.visibleWhen(() -> Boolean.TRUE.equals(UNIQUE_SERVER_HOPPER.get()) && UniqueServerHopper.isRunning());
		UNIQUE_SERVER_HOPPER_CLEAN_CACHE.visibleWhen(UNIQUE_SERVER_HOPPER);
		UNIQUE_SERVER_HOPPER.bind(UniqueServerHopper::onToggleChanged);
	}

	// Dungeons

	// Sections
	public static final UiSection DUNGEONS_GENERAL = DUNGEONS.section("General");
	public static final UiSection DUNGEONS_AUTO_TERMS_SECTION = DUNGEONS.section("Auto Terminals");
	public static final UiSection DUNGEONS_INVWALK_SECTION = DUNGEONS.section("Invwalk");
	public static final UiSection DUNGEONS_INVWALK_VISUALIZER_SECTION = DUNGEONS.section("Invwalk Visualizer");

	// Bindables
	public static final UiSwitch DUNGEONS_TERMINAL_AURA = DUNGEONS_AUTO_TERMS_SECTION.toggle("Terminal Aura", false);
	public static final UiSwitch DUNGEONS_AUTO_TERMS = DUNGEONS_AUTO_TERMS_SECTION.toggle("Auto Terms", false);
	public static final UiSlider DUNGEONS_AUTO_TERMS_CLICK_DELAY = DUNGEONS_AUTO_TERMS_SECTION.slider("Click Delay",
			50.0, 300.0, 100.0, 1.0);
	public static final UiSlider DUNGEONS_AUTO_TERMS_FIRST_CLICK_DELAY = DUNGEONS_AUTO_TERMS_SECTION
			.slider("First Click Delay", 300.0, 500.0, 350.0, 1.0);
	public static final UiSlider DUNGEONS_AUTO_TERMS_BREAK_THRESHOLD = DUNGEONS_AUTO_TERMS_SECTION
			.slider("Break Threshold", 500.0, 1500.0, 500.0, 1.0);
	public static final UiSwitch DUNGEONS_AUTO_TERMS_MELODY_ENABLED = DUNGEONS_AUTO_TERMS_SECTION.toggle("Melody Toggle",
			false);
	public static final UiDropdown DUNGEONS_AUTO_TERMS_MELODY_SKIP = DUNGEONS_AUTO_TERMS_SECTION
			.dropdown("Melody Skip On", "Edges");
	public static final UiSlider DUNGEONS_AUTO_TERMS_MELODY_SKIP_DELAY = DUNGEONS_AUTO_TERMS_SECTION.slider(
			"Melody Skip Delay", 0.0, 100.0, 50.0, 1.0);
	public static final UiSlider DUNGEONS_AUTO_TERMS_MELODY_FIRST_DELAY = DUNGEONS_AUTO_TERMS_SECTION.slider(
			"Melody First Click Delay", 0.0, 400.0, 0.0, 1.0);

	public static final UiSwitch DUNGEONS_AUTO_TERMS_INVWALK = DUNGEONS_INVWALK_SECTION.toggle("Invwalk", false);
	public static final UiSwitch DUNGEONS_AUTO_TERMS_INVWALK_MENTAL = DUNGEONS_INVWALK_SECTION.toggle("Mental", false);
	public static final UiSwitch DUNGEONS_AUTO_TERMS_INVWALK_MELODY = DUNGEONS_INVWALK_SECTION.toggle("Melody", false);
	public static final UiDropdown DUNGEONS_AUTO_TERMS_INVWALK_MELODY_METHOD = DUNGEONS_INVWALK_SECTION
			.dropdown("Melody InvWalk Method", "Blink");
	public static final UiSwitch DUNGEONS_AUTO_TERMS_VISUALIZE_MELODY = DUNGEONS_INVWALK_SECTION
			.toggle("Visualize Melody", false);
	public static final UiSlider DUNGEONS_AUTO_TERMS_INVWALK_MELODY_MOVE_DELAY = DUNGEONS_INVWALK_SECTION.slider(
			"Melody Move Delay", 0.0, 500.0, 350.0, 1.0);

	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Visualizer Toggle", true);
	public static final UiSlider DUNGEONS_INVWALK_VISUALIZER_SCALE = DUNGEONS_INVWALK_VISUALIZER_SECTION.slider("Scale",
			0.25, 4.0, 1.0, 0.01);
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Color", UiColor.argb(0, 255, 0, 255));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_BACKGROUND_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Background Color", UiColor.argb(0, 0, 0, 127));
	public static final UiInput DUNGEONS_INVWALK_VISUALIZER_OFFSET_X = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.input("X Offset", "0");
	public static final UiInput DUNGEONS_INVWALK_VISUALIZER_OFFSET_Y = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.input("Y Offset", "0");

	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Numbers Toggle", true);
	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_NUMBERS_SHOW_NUMBERS = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Numbers Show Numbers", true);
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR1 = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Numbers Color 1", UiColor.argb(0, 255, 0, 255));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR2 = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Numbers Color 2", UiColor.argb(0, 255, 0, 155));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR3 = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Numbers Color 3", UiColor.argb(0, 255, 0, 55));

	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_COLORS_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Colors Toggle", true);
	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_STARTSWITH_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Starts With Toggle", true);
	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_RUBIX_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Rubix Toggle", true);
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_RUBIX_LEFT_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Rubix Left Color", UiColor.argb(0, 255, 0, 255));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_RUBIX_RIGHT_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Rubix Right Color", UiColor.argb(255, 0, 0, 255));

	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_REDGREEN_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Red Green Toggle", true);
	public static final UiSwitch DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.toggle("Melody Toggle", true);
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_MELODY_SLOT_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Melody Slot Color", UiColor.argb(0, 255, 0, 255));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_MELODY_CORRECT_BUTTON_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Melody Correct Button Color", UiColor.argb(0, 255, 0, 255));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_MELODY_INCORRECT_BUTTON_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Melody Incorrect Button Color", UiColor.argb(255, 0, 0, 255));
	public static final UiColor DUNGEONS_INVWALK_VISUALIZER_MELODY_COLUMN_COLOR = DUNGEONS_INVWALK_VISUALIZER_SECTION
			.color("Melody Column Color", UiColor.argb(255, 0, 255, 127));

	// Visibilities
	static {
		DUNGEONS_AUTO_TERMS_INVWALK_MELODY_METHOD.setOptions(List.of("Keybind", "Blink"));
		DUNGEONS_AUTO_TERMS_MELODY_SKIP.setOptions(List.of("None", "Edges", "All"));

		DUNGEONS_AUTO_TERMS_CLICK_DELAY.visibleWhen(DUNGEONS_AUTO_TERMS);
		DUNGEONS_AUTO_TERMS_FIRST_CLICK_DELAY.visibleWhen(DUNGEONS_AUTO_TERMS);
		DUNGEONS_AUTO_TERMS_BREAK_THRESHOLD.visibleWhen(DUNGEONS_AUTO_TERMS);
		DUNGEONS_AUTO_TERMS_MELODY_ENABLED.visibleWhen(DUNGEONS_AUTO_TERMS);
		DUNGEONS_AUTO_TERMS_MELODY_SKIP.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_MELODY_ENABLED.get()));
		DUNGEONS_AUTO_TERMS_MELODY_SKIP_DELAY.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_MELODY_ENABLED.get()));
		DUNGEONS_AUTO_TERMS_MELODY_FIRST_DELAY.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_MELODY_ENABLED.get()));

		DUNGEONS_AUTO_TERMS_INVWALK.visibleWhen(DUNGEONS_AUTO_TERMS);
		DUNGEONS_AUTO_TERMS_INVWALK_MENTAL.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK.get()));
		DUNGEONS_AUTO_TERMS_INVWALK_MELODY.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK.get()));
		DUNGEONS_AUTO_TERMS_INVWALK_MELODY_METHOD.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK.get())
						&& Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK_MELODY.get()));
		DUNGEONS_AUTO_TERMS_VISUALIZE_MELODY.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK.get())
						&& Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK_MELODY.get()));
		DUNGEONS_AUTO_TERMS_INVWALK_MELODY_MOVE_DELAY.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK.get())
						&& Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK_MELODY.get()));

		DUNGEONS_INVWALK_VISUALIZER_ENABLED.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS.get()) && Boolean.TRUE.equals(DUNGEONS_AUTO_TERMS_INVWALK.get()));
		DUNGEONS_INVWALK_VISUALIZER_SCALE.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_COLOR.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_BACKGROUND_COLOR.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_OFFSET_X.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_OFFSET_Y.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_NUMBERS_SHOW_NUMBERS.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR1.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR2.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_NUMBERS_COLOR3.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_NUMBERS_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_COLORS_ENABLED.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_STARTSWITH_ENABLED.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_RUBIX_ENABLED.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_RUBIX_LEFT_COLOR.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_RUBIX_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_RUBIX_RIGHT_COLOR.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_RUBIX_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_REDGREEN_ENABLED.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED.visibleWhen(DUNGEONS_INVWALK_VISUALIZER_ENABLED);
		DUNGEONS_INVWALK_VISUALIZER_MELODY_SLOT_COLOR.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_MELODY_CORRECT_BUTTON_COLOR.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_MELODY_INCORRECT_BUTTON_COLOR.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED.get()));
		DUNGEONS_INVWALK_VISUALIZER_MELODY_COLUMN_COLOR.visibleWhen(
				() -> Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_ENABLED.get()) && Boolean.TRUE.equals(DUNGEONS_INVWALK_VISUALIZER_MELODY_ENABLED.get()));
	}

	// Chat

	// Sections
	public static final UiSection CHAT_GENERAL = CHAT.section("General");
	public static final UiSection CHAT_COMMAND_REPLACER = CHAT.section("Command Replacer");

	// Bindables
	public static final UiSwitch DISABLE_BLOCKS_IN_THE_WAY = CHAT_GENERAL.toggle("Disable blocks in the way!", false);
	public static final UiSwitch DISABLE_IMPLOSION_DAMAGE = CHAT_GENERAL.toggle("Disable Implosion damage", false);
	public static final UiSwitch DISABLE_ABILITY_COOLDOWN = CHAT_GENERAL.toggle("Disable ability cooldown", false);
	public static final UiSwitch COMPACT_CHAT = CHAT_GENERAL.toggle("Compact Chat", false);
	public static final UiButton COMMAND_REPLACER_OPEN = CHAT_COMMAND_REPLACER.button("Command Replacer Editor", "Open",
			button -> UiEntrypoints.open(ScreenIds.COMMAND_REPLACER));

	// Visibilities
	static {
	}

	// Visual

	// Sections
	public static final UiSection VISUAL_GENERAL = VISUAL.section("General");
	public static final UiSection VISUAL_IMAGE_SECTION = VISUAL.section("Image on screen");
	public static final UiSection KUUDRA_VISUAL = KUUDRA.section("Visual");

	// Bindables
	public static final UiSwitch IMAGE_ON_SCREEN = VISUAL_GENERAL.toggle("Image On Screen", false);
	public static final UiSwitch NO_TILT = VISUAL_GENERAL.toggle("No Tilt", false);
	public static final UiSwitch NO_DEBUFF = VISUAL_GENERAL.toggle("No Debuff", false);
	public static final UiDropdown IMAGE_ON_SCREEN_SELECT = VISUAL_IMAGE_SECTION.dropdown("Select Image", "");
	public static final UiSlider IMAGE_ON_SCREEN_SCALE = VISUAL_IMAGE_SECTION.slider("Scale", 0.1, 2.0, 1.0);
	public static final UiNumber IMAGE_ON_SCREEN_X = VISUAL_IMAGE_SECTION.number("Image X", -10000, 10000, 8);
	public static final UiNumber IMAGE_ON_SCREEN_Y = VISUAL_IMAGE_SECTION.number("Image Y", -10000, 10000, 8);
	public static final UiButton IMAGE_ON_SCREEN_MOVE = VISUAL_IMAGE_SECTION.button("Move", "Move",
			button -> RenderImageOnScreen.openMoveScreen());
	public static final UiButton IMAGE_ON_SCREEN_REFRESH = VISUAL_IMAGE_SECTION.button("Refresh", "Refresh",
			button -> RenderImageOnScreen.refreshImageOptions());
	public static final UiSwitch KUUDRA_POS_TRACKER = KUUDRA_VISUAL.toggle("Kuudra Pos Tracker", false);
	public static final UiSlider KUUDRA_POS_TRACKER_X = KUUDRA_VISUAL.slider("Kuudra Pos Tracker X", -10000.0, 10000.0,
			0.0, 1.0);
	public static final UiSlider KUUDRA_POS_TRACKER_Y = KUUDRA_VISUAL.slider("Kuudra Pos Tracker Y", -10000.0, 10000.0,
			20.0, 1.0);
	public static final UiButton KUUDRA_POS_TRACKER_MOVE = KUUDRA_VISUAL.button("Kuudra Pos Tracker Move", "Move",
			button -> KuudraPosTracker.openMoveScreen());
	public static final UiSwitch KUUDRA_ESP = KUUDRA_VISUAL.toggle("Kuudra ESP", false);
	public static final UiColor KUUDRA_ESP_COLOR = KUUDRA_VISUAL.color("Kuudra ESP Color", UiColor.argb(255, 0, 0, 255));
	public static final UiSlider KUUDRA_ESP_LINE_WIDTH = KUUDRA_VISUAL.slider("Kuudra ESP Line Width", 1.0, 10.0, 3.0,
			0.5);
	public static final UiSwitch KUUDRA_CRATE_WAYPOINTS = KUUDRA_VISUAL.toggle("Kuudra Crate Waypoints", false);
	public static final UiSwitch KUUDRA_BALLISTA_BUILD_WAYPOINTS = KUUDRA_VISUAL
			.toggle("Kuudra Ballista Build Waypoints", false);

	public static final UiSwitch KUUDRA_SPLITS = KUUDRA_VISUAL.toggle("Kuudra Splits", false);
	public static final UiSlider KUUDRA_SPLITS_X = KUUDRA_VISUAL.slider("Kuudra Splits X", -10000.0, 10000.0, 50.0,
			1.0);
	public static final UiSlider KUUDRA_SPLITS_Y = KUUDRA_VISUAL.slider("Kuudra Splits Y", -10000.0, 10000.0, 50.0,
			1.0);
	public static final UiButton KUUDRA_SPLITS_MOVE = KUUDRA_VISUAL.button("Move Kuudra Splits", "Move",
			button -> com.crussion.moissanite.features.kuudra.KuudraSplits.openMoveScreen());

	public static final UiSwitch KUUDRA_HP_BOSSBAR = KUUDRA_VISUAL.toggle("Kuudra HP Bossbar", false);
	public static final UiSwitch KUUDRA_HP_TAG = KUUDRA_VISUAL.toggle("Kuudra HP Tag", false);
	public static final UiSection MISC_STORAGE = MISC.section("Storage");
	public static final UiSwitch STORAGE_OVERLAY = MISC_STORAGE.toggle("Storage Overlay", false);
	public static final UiButton STORAGE_OVERLAY_OPEN = MISC_STORAGE.button("Storage Overlay Open", "Open",
			button -> StorageOverlayFeature.openOverlayScreen());
	public static final UiButton STORAGE_OVERLAY_REFRESH = MISC_STORAGE.button("Storage Overlay Refresh",
			"Run /storage", button -> StorageOverlayFeature.requestStorageRefresh());
	public static final UiButton STORAGE_OVERLAY_CLEAR = MISC_STORAGE.button("Storage Overlay Clear Cache", "Clear",
			button -> StorageOverlayFeature.clearCache());
	public static final UiSlider STORAGE_OVERLAY_COLUMNS = MISC_STORAGE.slider("Storage Overlay Columns", 1.0, 10.0,
			3.0, 1.0);
	public static final UiSlider STORAGE_OVERLAY_HEIGHT = MISC_STORAGE.slider("Storage Overlay Height", 80.0, 3000.0,
			324.0, 1.0);
	public static final UiSlider STORAGE_OVERLAY_SCROLL_SPEED = MISC_STORAGE.slider("Storage Overlay Scroll Speed", 1.0,
			50.0, 10.0, 1.0);
	public static final UiSwitch STORAGE_OVERLAY_RETAIN_SCROLL = MISC_STORAGE.toggle("Storage Overlay Retain Scroll",
			true);
	public static final UiSwitch STORAGE_OVERLAY_INVERSE_SCROLL = MISC_STORAGE.toggle("Storage Overlay Inverse Scroll",
			false);
	public static final UiSlider STORAGE_OVERLAY_PADDING = MISC_STORAGE.slider("Storage Overlay Padding", 1.0, 20.0,
			5.0, 1.0);
	public static final UiSlider STORAGE_OVERLAY_MARGIN = MISC_STORAGE.slider("Storage Overlay Margin", 1.0, 60.0, 20.0,
			1.0);
	public static final UiSwitch STORAGE_OVERLAY_SHOW_TOOLTIPS = MISC_STORAGE
			.toggle("Storage Overlay Show Inactive Tooltips", false);
	public static final UiSwitch STORAGE_OVERLAY_ALWAYS_REPLACE = MISC_STORAGE.toggle("Storage Overlay Always Replace",
			true);
	public static final UiSwitch STORAGE_OVERLAY_ITEMS_BLOCK_SCROLLING = MISC_STORAGE
			.toggle("Storage Overlay Items Block Scrolling", true);

	// Visibilities
	static {
		IMAGE_ON_SCREEN_SELECT.visibleWhen(IMAGE_ON_SCREEN);
		IMAGE_ON_SCREEN_SCALE.visibleWhen(IMAGE_ON_SCREEN);
		IMAGE_ON_SCREEN_X.visibleWhen(() -> false);
		IMAGE_ON_SCREEN_Y.visibleWhen(() -> false);
		IMAGE_ON_SCREEN_MOVE.visibleWhen(
				() -> Boolean.TRUE.equals(IMAGE_ON_SCREEN.get()) && !IMAGE_ON_SCREEN_SELECT.get().isBlank());
		IMAGE_ON_SCREEN_REFRESH.visibleWhen(IMAGE_ON_SCREEN);
		KUUDRA_POS_TRACKER_X.visibleWhen(() -> false);
		KUUDRA_POS_TRACKER_Y.visibleWhen(() -> false);
		KUUDRA_POS_TRACKER_MOVE.visibleWhen(KUUDRA_POS_TRACKER);
		KUUDRA_ESP_COLOR.visibleWhen(KUUDRA_ESP);
		KUUDRA_ESP_LINE_WIDTH.visibleWhen(KUUDRA_ESP);

		KUUDRA_SPLITS_X.visibleWhen(KUUDRA_SPLITS);
		KUUDRA_SPLITS_Y.visibleWhen(KUUDRA_SPLITS);
		KUUDRA_SPLITS_MOVE.visibleWhen(KUUDRA_SPLITS);

		STORAGE_OVERLAY_OPEN.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_REFRESH.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_CLEAR.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_COLUMNS.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_HEIGHT.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_SCROLL_SPEED.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_RETAIN_SCROLL.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_INVERSE_SCROLL.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_PADDING.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_MARGIN.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_SHOW_TOOLTIPS.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_ALWAYS_REPLACE.visibleWhen(STORAGE_OVERLAY);
		STORAGE_OVERLAY_ITEMS_BLOCK_SCROLLING.visibleWhen(STORAGE_OVERLAY);
	}

	// Misc

	// Sections
	public static final UiSection MISC_GENERAL = MISC.section("General");
	public static final UiSection MISC_SKY_VISUALS = MISC.section("Sky Visuals");
	public static final UiSection MISC_HAND_VISUALS = MISC.section("Animations");

	// Bindables
	public static final UiSwitch ALWAYS_SPRINT = MISC_GENERAL.toggle("Always Sprint", false);

	public static final UiSwitch ENABLE_CUSTOM_TITLE = MISC_GENERAL.toggle("Enable Custom Title", false);
	public static final UiInput CUSTOM_TITLE = MISC_GENERAL.input("Custom Title", null);
	public static final UiSwitch NO_SHIFT_ANIMATION = MISC_GENERAL.toggle("No Shift Animation", false);
	public static final UiSwitch OLD_SHIFT = MISC_GENERAL.toggle("Old Shift", false);
	public static final UiSwitch STAR_ITEMSTACK = MISC_GENERAL.toggle("Star Itemstack", true);

	public static final UiSwitch CUSTOM_SKY_VISUALS = MISC_SKY_VISUALS.toggle("Custom Sky Visuals", false);
	public static final UiSlider CUSTOM_SKY_TIME = MISC_SKY_VISUALS.slider("Sky Time", 0.0, 23999.0, 18000.0, 1.0);
	public static final UiSlider CUSTOM_SKY_TIME_FLOW = MISC_SKY_VISUALS.slider("Sky Time Flow", 0.0, 20.0, 0.0, 0.1);
	public static final UiDropdown CUSTOM_SKY_PHASE = MISC_SKY_VISUALS.dropdown("Sky Phase", "By Time");
	public static final UiDropdown CUSTOM_SKY_WEATHER = MISC_SKY_VISUALS.dropdown("Sky Weather", "Vanilla");

	public static final UiSlider HAND_VISUAL_X = MISC_HAND_VISUALS.slider("Hand X", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_Y = MISC_HAND_VISUALS.slider("Hand Y", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_Z = MISC_HAND_VISUALS.slider("Hand Z", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_SIZE = MISC_HAND_VISUALS.slider("Hand Size", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_SPEED = MISC_HAND_VISUALS.slider("Hand Speed", -5.0, 5.0, 0.0, 0.01);

	public static final UiSwitch HAND_VISUAL_CUSTOM_SWING = MISC_HAND_VISUALS.toggle("Custom Swing Animation", false);
	public static final UiSwitch HAND_VISUAL_IGNORE_HASTE = MISC_HAND_VISUALS.toggle("Ignore Haste", false);
	public static final UiSwitch HAND_VISUAL_NO_EQUIP_RESET = MISC_HAND_VISUALS.toggle("No Equip Reset", false);
	public static final UiSwitch HAND_VISUAL_CANCEL_SWING = MISC_HAND_VISUALS.toggle("Cancel Swing", false);

	// Visibilities
	static {
		CUSTOM_SKY_PHASE.setOptions(List.of("By Time", "Force Day", "Force Night"));
		CUSTOM_SKY_WEATHER.setOptions(List.of("Vanilla", "Clear", "Rain", "Snow", "Thunder"));
		CUSTOM_TITLE.visibleWhen(ENABLE_CUSTOM_TITLE);
		CUSTOM_SKY_TIME.visibleWhen(CUSTOM_SKY_VISUALS);
		CUSTOM_SKY_TIME_FLOW.visibleWhen(CUSTOM_SKY_VISUALS);
		CUSTOM_SKY_PHASE.visibleWhen(CUSTOM_SKY_VISUALS);
		CUSTOM_SKY_WEATHER.visibleWhen(CUSTOM_SKY_VISUALS);
	}

	// Cheats
	// Sections
	public static final UiSection CHEATS_GENERAL = CHEATS.section("General");
	public static final UiSection WARDROBE_KEYBINDS = CHEATS.section("Wardrobe Keybinds");
	public static final UiSection CHEATS_AUTO_EXPERIMENTATION = CHEATS.section("Auto Experimentation");
	public static final UiSection KUUDRA_NORMAL = KUUDRA.section("Normal");
	public static final UiSection KUUDRA_CHEATS = KUUDRA.section("Cheats");

	// Bindables
	public static final UiSwitch ENABLE_GHOST_WARDROBE = CHEATS_GENERAL.toggle("Enable Ghost Wardrobe", false);
	public static final UiSwitch NO_INTERACT = CHEATS_GENERAL.toggle("No Interact", false);
	public static final UiKeybind WD_ONE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 1",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_TWO_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 2",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_THREE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 3",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_FOUR_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 4",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_FIVE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 5",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_SIX_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 6",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_SEVEN_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 7",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_EIGHT_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 8",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind WD_NINE_KEYBIND = WARDROBE_KEYBINDS.keybind("Wardrobe 9",
			InputConstants.UNKNOWN.getValue());

	public static final UiSwitch AUTO_EXPERIMENTS = CHEATS_AUTO_EXPERIMENTATION.toggle("Auto Experiments", false);
	public static final UiNumber AUTO_EXPERIMENTS_CLICK_DELAY = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Experiments Click Delay", 0, 1000, 200);
	public static final UiSwitch AUTO_EXPERIMENTS_AUTO_CLOSE = CHEATS_AUTO_EXPERIMENTATION
			.toggle("Auto Experiments Auto Close", true);
	public static final UiNumber AUTO_EXPERIMENTS_SERUM_COUNT = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Experiments Serum Count", 0, 3, 0);
	public static final UiSwitch AUTO_EXPERIMENTS_GET_MAX_XP = CHEATS_AUTO_EXPERIMENTATION
			.toggle("Auto Experiments Get Max XP", false);
	public static final UiNumber AUTO_EXPERIMENTS_ENCHANTING_LEVEL = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Enchanting Level", 1, 60, 60);
	public static final UiNumber AUTO_EXPERIMENTS_MACRO_UI_CLICK_DELAY = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Enchanting UI Click Delay MS", 1, 1000, 250);
	public static final UiNumber AUTO_EXPERIMENTS_SUPERPAIRS_CLICK_DELAY = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Enchanting Superpairs Click Delay MS", 0, 10000, 1500);
	public static final UiNumber AUTO_EXPERIMENTS_SUPERPAIRS_AFTER_PAIR_DELAY = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Enchanting Superpairs After Pair Delay MS", 0, 10000, 3500);
	public static final UiNumber AUTO_EXPERIMENTS_SUPERPAIRS_PAIR_CLAIMS = CHEATS_AUTO_EXPERIMENTATION
			.number("Auto Enchanting Superpairs Pair Claims", 1, 2, 1);
	public static final UiButton AUTO_EXPERIMENTS_MACRO_START = CHEATS_AUTO_EXPERIMENTATION
			.button("Auto Enchanting Start", "Start", button -> AutoExperimentMacro.start());
	public static final UiButton AUTO_EXPERIMENTS_MACRO_STOP = CHEATS_AUTO_EXPERIMENTATION
			.button("Auto Enchanting Stop", "Stop", button -> AutoExperimentMacro.stop());

	public static final UiSwitch AUTO_DIRECTION = KUUDRA_CHEATS.toggle("Auto Direction", false);
	public static final UiSlider AUTO_DIRECTION_ROTATION_MULTIPLIER = KUUDRA_CHEATS.slider(
			"Auto Direction Rotation Multiplier", 0.1, 2.0, 0.5, 0.1);
	public static final UiSwitch AUTO_DIRECTION_AIM_AT_KUUDRA = KUUDRA_CHEATS.toggle("Aim at Kuudra", false);
	public static final UiSlider AUTO_DIRECTION_AIM_AT_KUUDRA_TICKS = KUUDRA_CHEATS.slider(
			"Aim at Kuudra Ticks", 1.0, 100.0, 30.0, 1.0);
	public static final UiSlider AUTO_DIRECTION_AIM_AT_KUUDRA_ROTATION_MULTIPLIER = KUUDRA_CHEATS.slider(
			"Aim at Kuudra Rotation Multiplier", 0.1, 2.0, 0.5, 0.1);
	public static final UiSwitch AUTO_DIRECTION_AUTO_HYPERION = KUUDRA_CHEATS.toggle("Auto Hyperion", false);
	public static final UiSwitch AUTO_DIRECTION_AUTO_REAPER = KUUDRA_CHEATS.toggle("Auto Reaper", false);
	public static final UiSwitch AUTO_SWAP_ARMOR_RAG = KUUDRA_CHEATS.toggle("Auto Swap Armor After Rag", false);
	public static final UiSlider AUTO_SWAP_ARMOR_RAG_SLOT = KUUDRA_CHEATS.slider("Armor Slot", -1.0, 8.0, -1.0, 1.0);
	public static final UiSwitch AUTO_PEARL = KUUDRA_CHEATS.toggle("Auto Pearl", false);
	public static final UiKeybind AUTO_PEARL_KEYBIND = KUUDRA_CHEATS.keybind("Toggle Auto Pearl",
			InputConstants.UNKNOWN.getValue());
	public static final UiSlider AUTO_PEARL_TALISMAN_TIER = KUUDRA_CHEATS.slider("Auto Pearl Talisman Tier", 0.0, 3.0,
			0.0, 1.0);
	public static final UiSlider AUTO_PEARL_KUUDRA_TIER = KUUDRA_CHEATS.slider("Auto Pearl Kuudra Tier", 1.0, 5.0, 5.0,
			1.0);
	public static final UiSlider AUTO_PEARL_ROTATION_MULTIPLIER = KUUDRA_CHEATS.slider(
			"Auto Pearl Rotation Multiplier", 0.0, 2.0, 0.4, 0.1);
	public static final UiSlider AUTO_PEARL_INACCURACY = KUUDRA_CHEATS.slider("Auto Pearl Inaccuracy", 0.0, 1.0, 0.0,
			0.01);
	public static final UiSwitch AUTO_PEARL_USE_ONLY_SKY = KUUDRA_CHEATS.toggle("Use only sky", false);
	public static final UiSlider AUTO_PEARL_X = KUUDRA_CHEATS.slider("Auto Pearl X", -10000.0, 10000.0, 6.0, 1.0);
	public static final UiSlider AUTO_PEARL_Y = KUUDRA_CHEATS.slider("Auto Pearl Y", -10000.0, 10000.0, 6.0, 1.0);
	public static final UiButton AUTO_PEARL_MOVE = KUUDRA_CHEATS.button("Auto Pearl Move", "Move",
			button -> AutoPearl.openMoveScreen());
	// public static final UiSwitch AUTO_PICKUP_SUPPLY = KUUDRA_CHEATS.toggle("Auto Pickup Supply", false);
	// public static final UiDropdown AUTO_PICKUP_SUPPLY_MODE = KUUDRA_CHEATS.dropdown("Auto Pickup Supply Mode", "Auto");
	// public static final UiKeybind AUTO_PICKUP_SUPPLY_KEYBIND = KUUDRA_CHEATS.keybind("Manual Pickup Supply",
	// 		InputConstants.UNKNOWN.getValue());
	// public static final UiButton AUTO_PICKUP_SUPPLY_MANUAL = KUUDRA_CHEATS.button("Manual Pickup Supply Button", "Pickup",
	// 		button -> KuudraAutoPickupSupply.requestManualPickup());
	// public static final UiSwitch AUTO_PICKUP_SUPPLY_USE_DIRECTION = KUUDRA_CHEATS.toggle("Auto Pickup Supply Direction",
	// 		true);
	// public static final UiSlider AUTO_PICKUP_SUPPLY_DELAY = KUUDRA_CHEATS.slider("Auto Pickup Supply Delay MS", 0.0,
	// 		2000.0, 0.0, 50.0);
	// public static final UiSlider AUTO_PICKUP_SUPPLY_RANGE = KUUDRA_CHEATS.slider("Auto Pickup Supply Range", 1.0, 6.0,
	// 		3.0, 0.1);
	public static final UiSwitch AUTO_SHOP = KUUDRA_CHEATS.toggle("Auto Shop", false);
	public static final UiSlider AUTO_SHOP_FIRST_CLICK_DELAY = KUUDRA_CHEATS.slider("Auto Shop First Click Delay", 1.0, 1500.0, 850.0, 1.0);
	public static final UiSlider AUTO_SHOP_CLICK_DELAY = KUUDRA_CHEATS.slider("Auto Shop Click Delay", 1.0, 1000.0, 250.0, 1.0);
	public static final UiSwitch AUTO_OPEN_KUUDRA_CHEST = KUUDRA_CHEATS.toggle("Auto Open Kuudra Chest", false);
	public static final UiSlider AUTO_OPEN_KUUDRA_CHEST_FIRST_CLICK_DELAY = KUUDRA_CHEATS
			.slider("Auto Open Kuudra Chest First Click Delay", 1.0, 5000.0, 1000.0, 1.0);
	public static final UiSlider AUTO_OPEN_KUUDRA_CHEST_CLICK_DELAY = KUUDRA_CHEATS
			.slider("Auto Open Kuudra Chest Click Delay", 1.0, 2000.0, 600.0, 1.0);
	public static final UiButton AUTO_OPEN_KUUDRA_CHEST_START = KUUDRA_CHEATS
			.button("Auto Open Kuudra Chest Start", "Start", button -> KuudraAutoOpenChest.start());
	public static final UiButton AUTO_OPEN_KUUDRA_CHEST_STOP = KUUDRA_CHEATS
			.button("Auto Open Kuudra Chest Stop", "Stop", button -> KuudraAutoOpenChest.stop());
	public static final UiSwitch AUTO_TAP = KUUDRA_CHEATS.toggle("Auto Tap", false);
	public static final UiSlider AUTO_TAP_AMOUNT = KUUDRA_CHEATS.slider("Auto Tap Amount", 1.0, 64.0, 32.0, 1.0);
	public static final UiSwitch AUTO_PEARL_REFILL = KUUDRA_CHEATS.toggle("Auto Pearl Refill Kuudra", false);
	public static final UiSlider AUTO_PEARL_REFILL_EVERY_TICKS = KUUDRA_CHEATS.slider("Auto Pearl Refill Every X Ticks",
			1.0, 20.0, 5.0, 1.0);
	public static final UiSwitch AUTO_REND = KUUDRA_CHEATS.toggle("Auto Rend", false);
	public static final UiSwitch AUTO_REND_AUTO_BACK_PEARL = KUUDRA_CHEATS.toggle("Auto Back Pearl", false);
	public static final UiSwitch AUTO_REND_TERMINATOR_PULL = KUUDRA_CHEATS.toggle("Terminator Pull", false);
	public static final UiSwitch AUTO_REND_AUTO_BACKBONE_DETECTION = KUUDRA_CHEATS.toggle("Auto Backbone Detection", true);
	public static final UiSlider AUTO_REND_BONEMERANG_AIR_TICKS = KUUDRA_CHEATS.slider("Bonemerang In Air Ticks",
			0.0, 80.0, 21.0, 1.0);
	public static final UiSlider AUTO_REND_ROTATION_MULTIPLIER = KUUDRA_CHEATS.slider("Rotation Multiplier", 0.1, 2.0,
			1.0, 0.1);
	public static final UiSlider AUTO_REND_HYPERION = KUUDRA_CHEATS.slider("Hyperion", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_BONEMERANG = KUUDRA_CHEATS.slider("Bonemerang", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_TERMINATOR = KUUDRA_CHEATS.slider("Terminator", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_ATOMSPLIT = KUUDRA_CHEATS.slider("Atomsplit", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_ENDSTONE = KUUDRA_CHEATS.slider("Endstone", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_PEARLS = KUUDRA_CHEATS.slider("Pearls", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_SWAP_ARMOR = KUUDRA_CHEATS.slider("Swap Armor", -1.0, 8.0, -1.0, 1.0);
	public static final UiButton AUTO_REND_SCAN_ITEMS = KUUDRA_CHEATS.button("Scan Items", "Scan Items",
			button -> AutoRend_Reworked.scanItemSlots());
			


	public static final UiSwitch NO_PRE = KUUDRA_NORMAL.toggle("No Pre", false);
	public static final UiSwitch REND_DAMAGE = KUUDRA_NORMAL.toggle("Rend Damage", false);
	public static final UiSwitch REND_DAMAGE_AUTO_REND_SCREEN = KUUDRA_NORMAL.toggle("Auto Rend Screen Damage", true);

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
		AUTO_EXPERIMENTS_CLICK_DELAY.visibleWhen(AUTO_EXPERIMENTS);
		AUTO_EXPERIMENTS_AUTO_CLOSE.visibleWhen(AUTO_EXPERIMENTS);
		AUTO_EXPERIMENTS_SERUM_COUNT.visibleWhen(AUTO_EXPERIMENTS);
		AUTO_EXPERIMENTS_GET_MAX_XP.visibleWhen(AUTO_EXPERIMENTS);
		AUTO_EXPERIMENTS_MACRO_STOP.visibleWhen(AutoExperimentMacro::isRunning);
		AUTO_DIRECTION_ROTATION_MULTIPLIER.visibleWhen(AUTO_DIRECTION);
		AUTO_DIRECTION_AIM_AT_KUUDRA.visibleWhen(AUTO_DIRECTION);
		AUTO_DIRECTION_AIM_AT_KUUDRA_TICKS.visibleWhen(() -> Boolean.TRUE.equals(AUTO_DIRECTION.get())
				&& Boolean.TRUE.equals(AUTO_DIRECTION_AIM_AT_KUUDRA.get()));
		AUTO_DIRECTION_AUTO_HYPERION.visibleWhen(AUTO_DIRECTION);
		AUTO_DIRECTION_AUTO_REAPER.visibleWhen(AUTO_DIRECTION);
		AUTO_REND_AUTO_BACK_PEARL.visibleWhen(AUTO_REND);
		AUTO_REND_TERMINATOR_PULL.visibleWhen(AUTO_REND);
		AUTO_REND_AUTO_BACKBONE_DETECTION.visibleWhen(AUTO_REND);
		AUTO_REND_BONEMERANG_AIR_TICKS.visibleWhen(
				() -> Boolean.TRUE.equals(AUTO_REND.get()) && !Boolean.TRUE.equals(AUTO_REND_AUTO_BACKBONE_DETECTION.get()));
		AUTO_PEARL_TALISMAN_TIER.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_KUUDRA_TIER.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_ROTATION_MULTIPLIER.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_INACCURACY.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_USE_ONLY_SKY.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_X.visibleWhen(() -> false);
		AUTO_PEARL_Y.visibleWhen(() -> false);
		AUTO_PEARL_MOVE.visibleWhen(AUTO_PEARL);
		// AUTO_PICKUP_SUPPLY_MODE.setOptions(List.of("Auto", "Manual"));
		// AUTO_PICKUP_SUPPLY_MODE.visibleWhen(AUTO_PICKUP_SUPPLY);
		// AUTO_PICKUP_SUPPLY_KEYBIND.visibleWhen(
		// 		() -> Boolean.TRUE.equals(AUTO_PICKUP_SUPPLY.get()) && isAutoPickupSupplyManualMode());
		// AUTO_PICKUP_SUPPLY_MANUAL.visibleWhen(
		// 		() -> Boolean.TRUE.equals(AUTO_PICKUP_SUPPLY.get()) && isAutoPickupSupplyManualMode());
		// AUTO_PICKUP_SUPPLY_USE_DIRECTION.visibleWhen(AUTO_PICKUP_SUPPLY);
		// AUTO_PICKUP_SUPPLY_DELAY.visibleWhen(
		// 		() -> Boolean.TRUE.equals(AUTO_PICKUP_SUPPLY.get()) && !isAutoPickupSupplyManualMode());
		// AUTO_PICKUP_SUPPLY_RANGE.visibleWhen(AUTO_PICKUP_SUPPLY);
		AUTO_PEARL_REFILL_EVERY_TICKS.visibleWhen(AUTO_PEARL_REFILL);
		AUTO_SHOP_FIRST_CLICK_DELAY.visibleWhen(AUTO_SHOP);
		AUTO_SHOP_CLICK_DELAY.visibleWhen(AUTO_SHOP);
		AUTO_OPEN_KUUDRA_CHEST_FIRST_CLICK_DELAY.visibleWhen(AUTO_OPEN_KUUDRA_CHEST);
		AUTO_OPEN_KUUDRA_CHEST_CLICK_DELAY.visibleWhen(AUTO_OPEN_KUUDRA_CHEST);
		AUTO_OPEN_KUUDRA_CHEST_START.visibleWhen(AUTO_OPEN_KUUDRA_CHEST);
		AUTO_OPEN_KUUDRA_CHEST_STOP.visibleWhen(KuudraAutoOpenChest::isRunning);
		AUTO_TAP_AMOUNT.visibleWhen(AUTO_TAP);
		AUTO_REND_ROTATION_MULTIPLIER.visibleWhen(AUTO_REND);
		AUTO_REND_HYPERION.visibleWhen(AUTO_REND);
		AUTO_REND_BONEMERANG.visibleWhen(AUTO_REND);
		AUTO_REND_TERMINATOR.visibleWhen(
				() -> Boolean.TRUE.equals(AUTO_REND.get()) && Boolean.TRUE.equals(AUTO_REND_TERMINATOR_PULL.get()));
		AUTO_REND_ATOMSPLIT.visibleWhen(AUTO_REND);
		AUTO_REND_ENDSTONE.visibleWhen(AUTO_REND);
		AUTO_REND_PEARLS.visibleWhen(AUTO_REND);
		AUTO_REND_SWAP_ARMOR.visibleWhen(AUTO_REND);
		AUTO_REND_SCAN_ITEMS.visibleWhen(AUTO_REND);
		AUTO_SWAP_ARMOR_RAG_SLOT.visibleWhen(AUTO_SWAP_ARMOR_RAG);
		REND_DAMAGE_AUTO_REND_SCREEN.visibleWhen(REND_DAMAGE);
	}

	// Spoofer

	// Sections
	public static final UiSection UPDATE_SECTION = UISETTINGS.section("Update");
	public static final UiSection DEBUG_SECTION = UISETTINGS.section("Debug");

	// Bindables
	public static final UiSwitch UPDATE_AUTO_CHECK_ON_JOIN = UPDATE_SECTION.toggle("Auto Check On Server Join", true);
	public static final UiSwitch UPDATE_AUTO_DOWNLOAD_LATEST = UPDATE_SECTION.toggle("Auto Download Latest Update", true);
	public static final UiButton UPDATE_CHECK = UPDATE_SECTION.button("Check For Updates", "Check",
			button -> ModUpdater.checkForUpdatesAsync(ModUpdater.CheckTrigger.MANUAL));
	public static final UiButton UPDATE_DOWNLOAD = UPDATE_SECTION.button("Download Update", "Click to Download",
			button -> ModUpdater.downloadLatestAsync(ModUpdater.CheckTrigger.MANUAL));
	public static final UiButton UPDATE_MANUAL = UPDATE_SECTION.button("Manual Download", "Open GitHub",
			button -> ModUpdater.openLatestReleasePage());
			
	public static final UiSwitch DEBUG = DEBUG_SECTION.toggle("Debug", false);
	public static final UiSwitch AUTO_PEARL_DEBUG = DEBUG_SECTION.toggle("Auto Pearl", false);
	public static final UiSwitch AUTO_PICKUP_SUPPLY_DEBUG = DEBUG_SECTION.toggle("Auto Pickup Supply", false);
	public static final UiSwitch AUTO_REND_DEBUG = DEBUG_SECTION.toggle("Auto Rend", false);
	public static final UiKeybind AUTO_REND_DEBUG_TRIGGER_KEYBIND = DEBUG_SECTION.keybind("Trigger Auto Rend",
			InputConstants.UNKNOWN.getValue());
	public static final UiSwitch ENCHANTING_MACRO_DEBUG = DEBUG_SECTION.toggle("Enchanting Macro", false);

	// Visibilities
	static {
		AUTO_PEARL_DEBUG.visibleWhen(DEBUG);
		// AUTO_PICKUP_SUPPLY_DEBUG.visibleWhen(DEBUG);
		AUTO_REND_DEBUG.visibleWhen(DEBUG);
		AUTO_REND_DEBUG_TRIGGER_KEYBIND.visibleWhen(
				() -> Boolean.TRUE.equals(DEBUG.get()) && Boolean.TRUE.equals(AUTO_REND_DEBUG.get()));
		ENCHANTING_MACRO_DEBUG.visibleWhen(DEBUG);
	}

	// Spoofer

	// Sections
	public static final UiSection SPOOFER_GENERAL = UISETTINGS.section("Spoofer");
	public static final UiSection SPOOFER_FILTERS = UISETTINGS.section("Spoofer Filters");

	// Bindables
	public static final UiDropdown SPOOFER_MODE = SPOOFER_GENERAL.dropdown("Spoof Mode", "Hide Only Moissanite");
	public static final UiInput SPOOFER_CUSTOM_CLIENT = SPOOFER_GENERAL.input("Custom Client Brand", "fabric")
			.maxLength(128);
	public static final UiSwitch SPOOFER_HIDE_MODS = SPOOFER_GENERAL.toggle("Hide Mod Resources", true);
	public static final UiInput SPOOFER_ALLOWED_MODS = SPOOFER_FILTERS.input("Allowed Mods", "").maxLength(4096);
	public static final UiInput SPOOFER_BLACKLISTED_MODS = SPOOFER_FILTERS.input("Blacklisted Mods", "moissanite")
			.maxLength(4096);
	public static final UiSwitch SPOOFER_DISABLE_CUSTOM_PAYLOADS = SPOOFER_FILTERS.toggle("Disable Custom Payloads",
			false);
	public static final UiInput SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS = SPOOFER_FILTERS
			.input("Allowed Payload Channels", "").maxLength(4096);

	// Visibilities
	static {
		SPOOFER_MODE.setOptions(List.of("Hide Only Moissanite", "Vanilla", "Modded", "Custom", "Off"));
		SPOOFER_CUSTOM_CLIENT.visibleWhen(() -> isSpooferMode("Custom"));
		SPOOFER_HIDE_MODS.visibleWhen(() -> isSpooferMode("Custom"));
		SPOOFER_ALLOWED_MODS.visibleWhen(() -> isSpooferMode("Modded") || isSpooferMode("Custom"));
		SPOOFER_BLACKLISTED_MODS.visibleWhen(
				() -> isSpooferHideActive() && !isSpooferMode("Hide Only Moissanite") && !isSpooferMode("Vanilla"));
		SPOOFER_DISABLE_CUSTOM_PAYLOADS.visibleWhen(() -> !isSpooferMode("Off"));
		SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS.visibleWhen(
				() -> isSpooferMode("Custom") && Boolean.TRUE.equals(SPOOFER_DISABLE_CUSTOM_PAYLOADS.get()));
	}

	public static void init() {
	}

	private static boolean isSpooferMode(String mode) {
		String current = SPOOFER_MODE.get();
		return current != null && current.equalsIgnoreCase(mode);
	}

	private static boolean isSpooferHideActive() {
		if (isSpooferMode("Off")) {
			return false;
		}
		if (isSpooferMode("Custom")) {
			return Boolean.TRUE.equals(SPOOFER_HIDE_MODS.get());
		}
		return true;
	}

	// private static boolean isAutoPickupSupplyManualMode() {
		// String current = AUTO_PICKUP_SUPPLY_MODE.get();
		// return current != null && current.equalsIgnoreCase("Manual");
	// }

	private UiDefinitions() {
	}
}
