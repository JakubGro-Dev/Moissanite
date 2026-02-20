package com.crussion.moissanite.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.ui.data.UiCatalog;
import net.fabricmc.loader.api.FabricLoader;

public final class MoissaniteConfig {
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
	private static final Path MOD_CONFIG_DIR = CONFIG_DIR.resolve("moissanite");
	private static final Path LEGACY_CONFIG_PATH = CONFIG_DIR.resolve("moissanite.json");
	private static final Path LEGACY_ASSETS_DIR = CONFIG_DIR.resolve("assets");
	private static final Path MOD_CONFIG_PATH = MOD_CONFIG_DIR.resolve("config.json");
	private static final Path MOD_ASSETS_DIR = MOD_CONFIG_DIR.resolve("assets");

	private static final ConfigManager CONFIG = new ConfigManager("moissanite/config.json");

	private MoissaniteConfig() {
	}

	public static void init() {
		migrateLegacyLayout();
		UiDefinitions.init();
		CONFIG.bindAll(UiCatalog.categories());
		CONFIG.load();
	}

	private static void migrateLegacyLayout() {
		try {
			Files.createDirectories(MOD_CONFIG_DIR);
		} catch (IOException ignored) {
		}

		if (Files.exists(LEGACY_CONFIG_PATH) && !Files.exists(MOD_CONFIG_PATH)) {
			try {
				Files.move(LEGACY_CONFIG_PATH, MOD_CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
			}
		}

		if (Files.exists(LEGACY_ASSETS_DIR) && !Files.exists(MOD_ASSETS_DIR)) {
			try {
				Files.move(LEGACY_ASSETS_DIR, MOD_ASSETS_DIR, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
			}
		}
	}
}
