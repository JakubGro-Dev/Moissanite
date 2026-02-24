package com.crussion.moissanite.features.visual.storage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ChestMenu;

public sealed interface StorageBackingHandle permits StorageBackingHandle.Overview, StorageBackingHandle.Page {

    sealed interface HasBackingScreen permits Overview, Page {
        ChestMenu handler();
    }

    record Overview(ChestMenu handler) implements StorageBackingHandle, HasBackingScreen {
    }

    record Page(ChestMenu handler, StoragePageSlot storagePageSlot) implements StorageBackingHandle, HasBackingScreen {
    }

    Pattern ENDER_CHEST_NAME = Pattern.compile("^Ender Chest (?:[\\u2726\\u2736] )?\\(([1-9])/[1-9]\\)$");
    Pattern BACK_PACK_NAME = Pattern.compile("^.+Backpack (?:[\\u2726\\u2736] )?\\(Slot #([0-9]+)\\)$");

    static StorageBackingHandle fromScreen(Screen screen) {
        if (screen == null)
            return null;
        if (!(screen instanceof ContainerScreen containerScreen))
            return null;
        if (!(containerScreen.getMenu() instanceof ChestMenu chestMenu))
            return null;

        String title = TextNormalizer.stripFormattingCodes(containerScreen.getTitle() == null ? "" : containerScreen.getTitle().getString()).trim();

        if ("Storage".equals(title))
            return new Overview(chestMenu);

        Matcher enderChestMatcher = ENDER_CHEST_NAME.matcher(title);
        if (enderChestMatcher.matches()) {
            return new Page(chestMenu, StoragePageSlot.ofEnderChestPage(Integer.parseInt(enderChestMatcher.group(1))));
        }

        Matcher backPackMatcher = BACK_PACK_NAME.matcher(title);
        if (backPackMatcher.matches()) {
            return new Page(chestMenu, StoragePageSlot.ofBackPackPage(Integer.parseInt(backPackMatcher.group(1))));
        }

        return null;
    }
}
