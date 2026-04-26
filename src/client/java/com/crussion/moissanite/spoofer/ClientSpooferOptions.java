package com.crussion.moissanite.spoofer;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ClientSpooferOptions {
    private static final String MOISSANITE_MOD_ID = "moissanite";
    private static final String HIDE_ONLY_MOISSANITE_LABEL = "Hide Only Moissanite";
    private static final List<String> COMPATIBILITY_PAYLOAD_CHANNEL_PREFIXES = List.of(
            "minecraft:",
            "hypixel:",
            "hmapi:");

    public static SpoofMode SPOOF_MODE = SpoofMode.HIDE_ONLY_MOISSANITE;
    public static String CUSTOM_CLIENT = "fabric";
    public static boolean HIDE_MODS = true;
    public static boolean DISABLE_CUSTOM_PAYLOADS = false;
    public static final Set<String> ALLOWED_MODS = new LinkedHashSet<>();
    public static final Set<String> BLACKLISTED_MODS = new LinkedHashSet<>(List.of(MOISSANITE_MOD_ID));
    public static final Set<String> ALLOWED_CUSTOM_PAYLOAD_CHANNELS = new LinkedHashSet<>();
    private static boolean initialized;

    private ClientSpooferOptions() {
    }

    public static void applyFirstRunDefaultsIfMissing(Path unifiedConfigPath) {
        if (hasSpooferModeKey(unifiedConfigPath)) {
            return;
        }
        UiDefinitions.SPOOFER_MODE.set(HIDE_ONLY_MOISSANITE_LABEL);
        UiDefinitions.SPOOFER_CUSTOM_CLIENT.set("fabric");
        UiDefinitions.SPOOFER_HIDE_MODS.set(true);
        UiDefinitions.SPOOFER_ALLOWED_MODS.set("");
        UiDefinitions.SPOOFER_BLACKLISTED_MODS.set(MOISSANITE_MOD_ID);
        UiDefinitions.SPOOFER_DISABLE_CUSTOM_PAYLOADS.set(false);
        UiDefinitions.SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS.set("");
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UiDefinitions.SPOOFER_MODE.bind(value -> {
            SPOOF_MODE = parseMode(value);
            DISABLE_CUSTOM_PAYLOADS = SPOOF_MODE != SpoofMode.OFF
                    && Boolean.TRUE.equals(UiDefinitions.SPOOFER_DISABLE_CUSTOM_PAYLOADS.get());
        });
        UiDefinitions.SPOOFER_CUSTOM_CLIENT.bind(value -> CUSTOM_CLIENT = normalizeClientBrand(value));
        UiDefinitions.SPOOFER_HIDE_MODS.bind(value -> HIDE_MODS = Boolean.TRUE.equals(value));
        UiDefinitions.SPOOFER_DISABLE_CUSTOM_PAYLOADS.bind(value -> DISABLE_CUSTOM_PAYLOADS =
                SPOOF_MODE != SpoofMode.OFF && Boolean.TRUE.equals(value));
        UiDefinitions.SPOOFER_ALLOWED_MODS.bind(value -> updateTokenSet(ALLOWED_MODS, value));
        UiDefinitions.SPOOFER_BLACKLISTED_MODS.bind(value -> updateTokenSet(BLACKLISTED_MODS, value));
        UiDefinitions.SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS
                .bind(value -> updateTokenSet(ALLOWED_CUSTOM_PAYLOAD_CHANNELS, value));
        syncFromUi();
    }

    public static void migrateLegacyConfig(Path legacyPath, Path unifiedConfigPath) {
        if (legacyPath == null || !Files.exists(legacyPath)) {
            return;
        }

        if (hasSpooferKeys(unifiedConfigPath)) {
            deleteLegacyFile(legacyPath);
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(Files.readString(legacyPath)).getAsJsonObject();
            if (json.has("spoof-mode")) {
                UiDefinitions.SPOOFER_MODE.set(normalizeModeLabel(json.get("spoof-mode").getAsString()));
            }
            if (json.has("custom-client")) {
                UiDefinitions.SPOOFER_CUSTOM_CLIENT.set(normalizeClientBrand(json.get("custom-client").getAsString()));
            }
            if (json.has("hide-mods")) {
                UiDefinitions.SPOOFER_HIDE_MODS.set(json.get("hide-mods").getAsBoolean());
            }
            if (json.has("disable-custom-payloads")) {
                UiDefinitions.SPOOFER_DISABLE_CUSTOM_PAYLOADS.set(json.get("disable-custom-payloads").getAsBoolean());
            }
            if (json.has("allowed-mods")) {
                UiDefinitions.SPOOFER_ALLOWED_MODS.set(String.join(", ", readTokenList(json.get("allowed-mods"))));
            }
            if (json.has("allowed-custom-payload-channels")) {
                UiDefinitions.SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS
                        .set(String.join(", ", readTokenList(json.get("allowed-custom-payload-channels"))));
            }
            deleteLegacyFile(legacyPath);
        } catch (IOException | JsonParseException ex) {
            ClientSpoofer.LOGGER.error("Failed to migrate legacy clientspoofer.json", ex);
        }
    }

    public static boolean hideMods() {
        return switch (SPOOF_MODE) {
            case VANILLA, MODDED -> true;
            case CUSTOM -> HIDE_MODS;
            case HIDE_ONLY_MOISSANITE -> true;
            case OFF -> false;
        };
    }

    public static boolean isBlacklistedMod(String identifier) {
        if (SPOOF_MODE == SpoofMode.VANILLA) {
            return true;
        }

        if (SPOOF_MODE == SpoofMode.HIDE_ONLY_MOISSANITE) {
            return matchesToken(identifier, MOISSANITE_MOD_ID);
        }

        for (String token : BLACKLISTED_MODS) {
            if (matchesToken(identifier, token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlacklistedChannel(String channelId) {
        if (SPOOF_MODE == SpoofMode.VANILLA) {
            return true;
        }

        if (SPOOF_MODE == SpoofMode.HIDE_ONLY_MOISSANITE) {
            return matchesToken(channelId, MOISSANITE_MOD_ID);
        }

        for (String token : BLACKLISTED_MODS) {
            if (matchesToken(channelId, token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCompatibilityPayloadChannel(String channelId) {
        String normalized = normalizeIdentifier(channelId);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String prefix : COMPATIBILITY_PAYLOAD_CHANNEL_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void syncFromUi() {
        String normalizedMode = normalizeModeLabel(UiDefinitions.SPOOFER_MODE.get());
        UiDefinitions.SPOOFER_MODE.set(normalizedMode);
        SPOOF_MODE = parseMode(normalizedMode);

        String normalizedClient = normalizeClientBrand(UiDefinitions.SPOOFER_CUSTOM_CLIENT.get());
        UiDefinitions.SPOOFER_CUSTOM_CLIENT.set(normalizedClient);
        CUSTOM_CLIENT = normalizedClient;

        HIDE_MODS = Boolean.TRUE.equals(UiDefinitions.SPOOFER_HIDE_MODS.get());
        DISABLE_CUSTOM_PAYLOADS = SPOOF_MODE != SpoofMode.OFF
                && Boolean.TRUE.equals(UiDefinitions.SPOOFER_DISABLE_CUSTOM_PAYLOADS.get());
        updateTokenSet(ALLOWED_MODS, UiDefinitions.SPOOFER_ALLOWED_MODS.get());
        updateTokenSet(BLACKLISTED_MODS, UiDefinitions.SPOOFER_BLACKLISTED_MODS.get());
        updateTokenSet(ALLOWED_CUSTOM_PAYLOAD_CHANNELS, UiDefinitions.SPOOFER_ALLOWED_CUSTOM_PAYLOAD_CHANNELS.get());
    }

    private static SpoofMode parseMode(String value) {
        return switch (normalizeModeLabel(value).toLowerCase(Locale.ROOT)) {
            case "modded" -> SpoofMode.MODDED;
            case "custom" -> SpoofMode.CUSTOM;
            case "hide only moissanite" -> SpoofMode.HIDE_ONLY_MOISSANITE;
            case "off" -> SpoofMode.OFF;
            default -> SpoofMode.VANILLA;
        };
    }

    private static String normalizeModeLabel(String value) {
        if (value == null) {
            return "Vanilla";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "modded" -> "Modded";
            case "custom" -> "Custom";
            case "hide only moissanite", "hide_only_moissanite", "hide-only-moissanite", "hideonlymoissanite" ->
                    HIDE_ONLY_MOISSANITE_LABEL;
            case "off" -> "Off";
            default -> "Vanilla";
        };
    }

    private static String normalizeClientBrand(String value) {
        if (value == null) {
            return "fabric";
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? "fabric" : normalized;
    }

    private static String normalizeIdentifier(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
    }

    private static boolean matchesToken(String identifier, String token) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        String normalizedToken = normalizeIdentifier(token);
        if (normalizedIdentifier.isEmpty() || normalizedToken.isEmpty()) {
            return false;
        }
        return normalizedIdentifier.equals(normalizedToken)
                || normalizedIdentifier.startsWith(normalizedToken + "/")
                || normalizedIdentifier.endsWith("/" + normalizedToken)
                || normalizedIdentifier.endsWith(":" + normalizedToken)
                || normalizedIdentifier.startsWith(normalizedToken + ":");
    }

    private static void updateTokenSet(Set<String> target, String raw) {
        target.clear();
        for (String token : tokenize(raw)) {
            target.add(token);
        }
    }

    private static List<String> tokenize(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] split = raw.split("[,\\n\\r]+");
        List<String> tokens = new ArrayList<>(split.length);
        for (String value : split) {
            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private static List<String> readTokenList(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || !item.isJsonPrimitive()) {
                continue;
            }
            String value = item.getAsString();
            for (String token : tokenize(value)) {
                if (!tokens.contains(token)) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private static boolean hasSpooferKeys(Path unifiedConfigPath) {
        if (unifiedConfigPath == null || !Files.exists(unifiedConfigPath)) {
            return false;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(unifiedConfigPath));
            if (parsed == null || !parsed.isJsonObject()) {
                return false;
            }
            for (String key : parsed.getAsJsonObject().keySet()) {
                if (key != null && (key.startsWith("spoofer.") || key.startsWith("settings.spoofer"))) {
                    return true;
                }
            }
            return false;
        } catch (IOException | JsonParseException ignored) {
            return false;
        }
    }

    private static boolean hasSpooferModeKey(Path unifiedConfigPath) {
        if (unifiedConfigPath == null || !Files.exists(unifiedConfigPath)) {
            return false;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(unifiedConfigPath));
            if (parsed == null || !parsed.isJsonObject()) {
                return false;
            }
            for (String key : parsed.getAsJsonObject().keySet()) {
                if (key == null) {
                    continue;
                }
                String normalizedKey = key.trim().toLowerCase(Locale.ROOT);
                if ((normalizedKey.startsWith("spoofer.") || normalizedKey.startsWith("settings.spoofer"))
                        && normalizedKey.endsWith("spoof_mode")) {
                    return true;
                }
            }
            return false;
        } catch (IOException | JsonParseException ignored) {
            return false;
        }
    }

    private static void deleteLegacyFile(Path legacyPath) {
        try {
            Files.deleteIfExists(legacyPath);
        } catch (IOException ex) {
            ClientSpoofer.LOGGER.warn("Failed to delete legacy clientspoofer config: {}", legacyPath, ex);
        }
    }
}
