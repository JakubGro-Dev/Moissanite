package com.crussion.moissanite.definitions;

import com.crussion.moissanite.features.cheats.AutoPearl;
import com.crussion.moissanite.features.cheats.AutoRend;
import com.crussion.moissanite.features.cheats.AutoRend_Reworked;
import com.crussion.moissanite.features.cheats.KuudraPosTracker;
import com.crussion.moissanite.features.visual.RenderImageOnScreen;
import com.crussion.moissanite.features.visual.storage.StorageOverlayFeature;
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
import java.util.List;

public final class UiDefinitions {
	public static final UiCategory UISETTINGS = UiCatalog.category("Settings"); // Do not touch it or i'll touch you.

	// Categories
	public static final UiCategory GENERAL = UiCatalog.category("General");
	public static final UiCategory DUNGEONS = UiCatalog.category("Dungeons");
	public static final UiCategory KUUDRA = UiCatalog.category("Kuudra");
	public static final UiCategory VISUAL = UiCatalog.category("Visual");
	public static final UiCategory CHAT = UiCatalog.category("Chat");
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

	// Chat

	// Sections
	public static final UiSection CHAT_GENERAL = CHAT.section("General");

	// Bindables
	public static final UiSwitch DISABLE_BLOCKS_IN_THE_WAY = CHAT_GENERAL.toggle("Disable blocks in the way!", false);
	public static final UiSwitch DISABLE_IMPLOSION_DAMAGE = CHAT_GENERAL.toggle("Disable Implosion damage", false);
	public static final UiSwitch DISABLE_ABILITY_COOLDOWN = CHAT_GENERAL.toggle("Disable ability cooldown", false);

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
	public static final UiSlider KUUDRA_ESP_COLOR_R = KUUDRA_VISUAL.slider("Kuudra ESP Color R", 0.0, 255.0, 255.0,
			1.0);
	public static final UiSlider KUUDRA_ESP_COLOR_G = KUUDRA_VISUAL.slider("Kuudra ESP Color G", 0.0, 255.0, 0.0, 1.0);
	public static final UiSlider KUUDRA_ESP_COLOR_B = KUUDRA_VISUAL.slider("Kuudra ESP Color B", 0.0, 255.0, 0.0, 1.0);
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
		KUUDRA_ESP_COLOR_R.visibleWhen(KUUDRA_ESP);
		KUUDRA_ESP_COLOR_G.visibleWhen(KUUDRA_ESP);
		KUUDRA_ESP_COLOR_B.visibleWhen(KUUDRA_ESP);
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
	public static final UiSection MISC_HAND_VISUALS = MISC.section("Animations");

	// Bindables
	public static final UiSwitch ALWAYS_SPRINT = MISC_GENERAL.toggle("Always Sprint", false);

	public static final UiSwitch ENABLE_CUSTOM_TITLE = MISC_GENERAL.toggle("Enable Custom Title", false);
	public static final UiInput CUSTOM_TITLE = MISC_GENERAL.input("Custom Title", null);
	public static final UiSwitch NO_SHIFT_ANIMATION = MISC_GENERAL.toggle("No Shift Animation", false);
	public static final UiSwitch OLD_SHIFT = MISC_GENERAL.toggle("Old Shift", false);
	public static final UiSwitch STAR_ITEMSTACK = MISC_GENERAL.toggle("Star Itemstack", true);

	public static final UiSlider HAND_VISUAL_X = MISC_HAND_VISUALS.slider("Hand X", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_Y = MISC_HAND_VISUALS.slider("Hand Y", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_Z = MISC_HAND_VISUALS.slider("Hand Z", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_SIZE = MISC_HAND_VISUALS.slider("Hand Size", -5.0, 5.0, 0.0, 0.01);
	public static final UiSlider HAND_VISUAL_SPEED = MISC_HAND_VISUALS.slider("Hand Speed", -5.0, 5.0, 0.0, 0.01);

	public static final UiSwitch HAND_VISUAL_SCALE_SWING = MISC_HAND_VISUALS.toggle("Scale Swing", false);
	public static final UiSwitch HAND_VISUAL_IGNORE_HASTE = MISC_HAND_VISUALS.toggle("Ignore Haste", false);
	public static final UiSwitch HAND_VISUAL_NO_EQUIP_RESET = MISC_HAND_VISUALS.toggle("No Equip Reset", false);
	public static final UiSwitch HAND_VISUAL_CANCEL_SWING = MISC_HAND_VISUALS.toggle("Cancel Swing", false);

	// Visibilities
	static {
		CUSTOM_TITLE.visibleWhen(ENABLE_CUSTOM_TITLE);
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

	public static final UiSwitch AUTO_DIRECTION = KUUDRA_CHEATS.toggle("Auto Direction", false);
	public static final UiSwitch AUTO_PEARL = KUUDRA_CHEATS.toggle("Auto Pearl", false);
	public static final UiSwitch AUTO_PEARL_DEBUG = KUUDRA_CHEATS.toggle("Auto Pearl Debug", false);
	public static final UiKeybind AUTO_PEARL_KEYBIND = KUUDRA_CHEATS.keybind("Toggle Auto Pearl",
			InputConstants.UNKNOWN.getValue());
	public static final UiSlider AUTO_PEARL_TALISMAN_TIER = KUUDRA_CHEATS.slider("Auto Pearl Talisman Tier", 0.0, 3.0,
			0.0, 1.0);
	public static final UiSlider AUTO_PEARL_KUUDRA_TIER = KUUDRA_CHEATS.slider("Auto Pearl Kuudra Tier", 1.0, 5.0, 5.0,
			1.0);
	public static final UiSlider AUTO_PEARL_X = KUUDRA_CHEATS.slider("Auto Pearl X", -10000.0, 10000.0, 6.0, 1.0);
	public static final UiSlider AUTO_PEARL_Y = KUUDRA_CHEATS.slider("Auto Pearl Y", -10000.0, 10000.0, 6.0, 1.0);
	public static final UiButton AUTO_PEARL_MOVE = KUUDRA_CHEATS.button("Auto Pearl Move", "Move",
			button -> AutoPearl.openMoveScreen());
	public static final UiSwitch AUTO_PEARL_REFILL = KUUDRA_CHEATS.toggle("Auto Pearl Refill Kuudra", false);
	public static final UiSlider AUTO_PEARL_REFILL_EVERY_TICKS = KUUDRA_CHEATS.slider("Auto Pearl Refill Every X Ticks",
			1.0, 120.0, 20.0, 1.0);
	public static final UiSwitch AUTO_REND = KUUDRA_CHEATS.toggle("Auto Rend", false);
	public static final UiSwitch AUTO_REND_HARDCODE = KUUDRA_CHEATS.toggle("Auto Rend Hardcode", true);
	public static final UiSwitch AUTO_REND_DEBUG = KUUDRA_CHEATS.toggle("Auto Rend Debug", false);
	public static final UiKeybind AUTO_REND_TRIGGER_KEYBIND = KUUDRA_CHEATS.keybind("Trigger Auto Rend DEBUG",
			InputConstants.UNKNOWN.getValue());
	public static final UiKeybind AUTO_REND_OLD_TRIGGER_KEYBIND = KUUDRA_CHEATS.keybind("Trigger Auto Rend OLD DEBUG",
			InputConstants.UNKNOWN.getValue());
	public static final UiSlider AUTO_REND_ROTATION_MULTIPLIER = KUUDRA_CHEATS.slider("Rotation Multiplier", 0.1, 2.0,
			1.0, 0.1);
	public static final UiSlider AUTO_REND_HYPERION = KUUDRA_CHEATS.slider("Hyperion", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_BONEMERANG = KUUDRA_CHEATS.slider("Bonemerang", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_ATOMSPLIT = KUUDRA_CHEATS.slider("Atomsplit", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_ENDSTONE = KUUDRA_CHEATS.slider("Endstone", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_PEARLS = KUUDRA_CHEATS.slider("Pearls", -1.0, 9.0, -1.0, 1.0);
	public static final UiSlider AUTO_REND_SWAP_ARMOR = KUUDRA_CHEATS.slider("Swap Armor", -1.0, 9.0, -1.0, 1.0);
	public static final UiButton AUTO_REND_SCAN_ITEMS = KUUDRA_CHEATS.button("Scan Items", "Scan Items",
			button -> {
				if (Boolean.TRUE.equals(AUTO_REND_HARDCODE.get())) {
					AutoRend_Reworked.scanItemSlots();
				} else {
					AutoRend.scanItemSlots();
				}
			});

	public static final UiSwitch NO_PRE = KUUDRA_NORMAL.toggle("No Pre", false);
	public static final UiSwitch REND_DAMAGE = KUUDRA_NORMAL.toggle("Rend Damage", false);

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
		AUTO_REND_HARDCODE.visibleWhen(AUTO_REND);
		AUTO_REND_DEBUG.visibleWhen(AUTO_REND);
		AUTO_PEARL_TALISMAN_TIER.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_KUUDRA_TIER.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_X.visibleWhen(() -> false);
		AUTO_PEARL_Y.visibleWhen(() -> false);
		AUTO_PEARL_MOVE.visibleWhen(AUTO_PEARL);
		AUTO_PEARL_REFILL_EVERY_TICKS.visibleWhen(AUTO_PEARL_REFILL);
		AUTO_REND_TRIGGER_KEYBIND.visibleWhen(
				() -> Boolean.TRUE.equals(AUTO_REND.get()) && Boolean.TRUE.equals(AUTO_REND_DEBUG.get()));
		AUTO_REND_OLD_TRIGGER_KEYBIND.visibleWhen(
				() -> Boolean.TRUE.equals(AUTO_REND.get()) && Boolean.TRUE.equals(AUTO_REND_DEBUG.get()));
		AUTO_REND_ROTATION_MULTIPLIER.visibleWhen(AUTO_REND);
		AUTO_REND_HYPERION.visibleWhen(AUTO_REND);
		AUTO_REND_BONEMERANG.visibleWhen(AUTO_REND);
		AUTO_REND_ATOMSPLIT.visibleWhen(AUTO_REND);
		AUTO_REND_ENDSTONE.visibleWhen(AUTO_REND);
		AUTO_REND_PEARLS.visibleWhen(AUTO_REND);
		AUTO_REND_SWAP_ARMOR.visibleWhen(AUTO_REND);
		AUTO_REND_SCAN_ITEMS.visibleWhen(AUTO_REND);
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
			true);
	public static final UiInput SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS = SPOOFER_FILTERS
			.input("Allowed Payload Channels", "").maxLength(4096);

	// Visibilities
	static {
		SPOOFER_MODE.setOptions(List.of("Hide Only Moissanite", "Vanilla", "Modded", "Custom", "Off"));
		SPOOFER_CUSTOM_CLIENT.visibleWhen(() -> isSpooferMode("Custom"));
		SPOOFER_HIDE_MODS.visibleWhen(() -> isSpooferMode("Custom"));
		SPOOFER_ALLOWED_MODS.visibleWhen(() -> isSpooferMode("Modded") || isSpooferMode("Custom"));
		SPOOFER_BLACKLISTED_MODS.visibleWhen(
				() -> isSpooferHideActive() && !isSpooferMode("Hide Only Moissanite"));
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

	private UiDefinitions() {
	}
}
