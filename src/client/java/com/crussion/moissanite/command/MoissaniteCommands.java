package com.crussion.moissanite.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.navigation.ScreenIds;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

public final class MoissaniteCommands {
	private MoissaniteCommands() {
	}

	public static void init() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			registerRoot(dispatcher, "moissanite");
			registerRoot(dispatcher, "mois");
		});
	}

	private static void registerRoot(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
		dispatcher.register(literal(root)
				.executes(context -> openGui(context.getSource()))
				.then(literal("files")
						.executes(context -> openConfigFolder(context.getSource()))));
	}

	private static int openGui(FabricClientCommandSource source) {
		source.getClient().execute(() -> UiEntrypoints.open(ScreenIds.INVENTORY_OVERLAY));
		return 1;
	}

	private static int openConfigFolder(FabricClientCommandSource source) {
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("moissanite");
		try {
			Files.createDirectories(configDir);
		} catch (IOException ignored) {
		}

		source.getClient().execute(() -> Util.getPlatform().openPath(configDir));
		return 1;
	}
}
