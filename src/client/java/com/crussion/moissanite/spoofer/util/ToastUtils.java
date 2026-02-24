package com.crussion.moissanite.spoofer.util;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class ToastUtils {
    private static final Set<String> SERVERS_ATTEMPTED_READING_MODS = new HashSet<>();

    private ToastUtils() {
    }

    public static void showServerAttemptedReadingModsToast() {
        ServerData serverData = Minecraft.getInstance().getCurrentServer();
        if (serverData != null) {
            if (SERVERS_ATTEMPTED_READING_MODS.contains(serverData.ip)) {
                return;
            }
            SERVERS_ATTEMPTED_READING_MODS.add(serverData.ip);
        }

        SystemToast.SystemToastId id = new SystemToast.SystemToastId(10000L);
        Component title = Component.literal("Client Spoofer");
        Component message = Component.translatable("moissanite.spoofer.toast.server_attempted_reading_mods");
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToastManager().addToast(SystemToast.multiline(minecraft, id, title, message));
    }
}
