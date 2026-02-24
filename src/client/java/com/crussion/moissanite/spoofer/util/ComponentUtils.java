package com.crussion.moissanite.spoofer.util;

import com.crussion.moissanite.mixin.client.SpooferLanguageAccessor;
import com.crussion.moissanite.mixin.client.SpooferLanguageManagerAccessor;
import com.crussion.moissanite.spoofer.ClientSpooferOptions;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ComponentUtils {
    private static final Language LANGUAGE = SpooferLanguageAccessor.invokeLoadDefault();
    private static final Map<ClientPacketListener, Language> SERVER_LANGUAGES = new IdentityHashMap<>();

    private ComponentUtils() {
    }

    public static String getString(Component component) {
        if (!(component instanceof MutableComponent)) {
            return component.getString();
        }

        StringBuilder stringBuilder = new StringBuilder();
        visit(component.getContents()).ifPresent(stringBuilder::append);
        for (Component sibling : component.getSiblings()) {
            stringBuilder.append(getString(sibling));
        }
        return stringBuilder.toString();
    }

    public static Optional<String> visit(ComponentContents contents) {
        if (contents instanceof KeybindContents keybind) {
            String key = keybind.getName();
            if (!canTranslate(key)) {
                return Optional.of(key);
            }
        } else if (contents instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (!canTranslate(key)) {
                return Optional.of(Objects.requireNonNullElseGet(translatable.getFallback(), translatable::getKey));
            }
        }

        return contents.visit(Optional::of);
    }

    public static boolean canTranslate(String key) {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData serverData = minecraft.getCurrentServer();

        if (serverData != null && serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            ClientPacketListener connection = minecraft.getConnection();
            if (connection == null) {
                return LANGUAGE.has(key);
            }

            if (!SERVER_LANGUAGES.containsKey(connection)) {
                if (!SERVER_LANGUAGES.isEmpty()) {
                    SERVER_LANGUAGES.clear();
                }
                SERVER_LANGUAGES.put(connection, createServerLanguage());
            }

            Language language = SERVER_LANGUAGES.get(connection);
            return language != null ? language.has(key) : LANGUAGE.has(key);
        }

        return LANGUAGE.has(key);
    }

    private static Language createServerLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        List<PackResources> allPackResources = minecraft.getResourceManager().listPacks().toList();
        List<PackResources> packResources = new ArrayList<>();

        if (allPackResources.isEmpty()) {
            return LANGUAGE;
        }

        packResources.add(allPackResources.getFirst());

        for (int i = 1; i < allPackResources.size(); i++) {
            PackResources packResource = allPackResources.get(i);
            PackSource source = packResource.location().source();

            if (!ClientSpooferOptions.hideMods()
                    || !ClientSpooferOptions.isBlacklistedMod(packResource.packId())
                    || source == PackSource.FEATURE
                    || source == PackSource.WORLD
                    || source == PackSource.SERVER) {
                packResources.add(packResource);
            }
        }

        ResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, packResources);
        String currentLanguageCode = minecraft.getLanguageManager().getSelected();
        Map<String, LanguageInfo> languages = SpooferLanguageManagerAccessor.invokeExtractLanguages(resourceManager.listPacks());
        List<String> list = new ArrayList<>(2);
        list.add("en_us");

        boolean bidirectional = SpooferLanguageManagerAccessor.getDefaultLanguage().bidirectional();
        if (!"en_us".equals(currentLanguageCode)) {
            LanguageInfo languageInfo = languages.get(currentLanguageCode);
            if (languageInfo != null) {
                list.add(currentLanguageCode);
                bidirectional = languageInfo.bidirectional();
            }
        }

        return ClientLanguage.loadFrom(resourceManager, list, bidirectional);
    }
}
