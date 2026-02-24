package com.crussion.moissanite.spoofer;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientSpoofer {
    public static final Logger LOGGER = LoggerFactory.getLogger("moissanite-spoofer");
    public static final Path LEGACY_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("clientspoofer.json");
    public static final Path UNIFIED_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("moissanite/config.json");

    private ClientSpoofer() {
    }

    public static void init() {
        ClientSpooferOptions.applyFirstRunDefaultsIfMissing(UNIFIED_CONFIG_FILE);
        ClientSpooferOptions.init();
        ClientSpooferOptions.migrateLegacyConfig(LEGACY_CONFIG_FILE, UNIFIED_CONFIG_FILE);
    }
}
