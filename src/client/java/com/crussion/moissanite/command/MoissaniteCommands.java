package com.crussion.moissanite.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.crussion.moissanite.features.cheats.AutoRendHelper;
import com.crussion.moissanite.features.cheats.RouteSystem;
import com.crussion.moissanite.update.ModUpdater;
import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.util.route.RouteActionType;
import com.crussion.moissanite.util.rotation.RotationController;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

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
			registerRotate(dispatcher);
			registerRoute(dispatcher);
		});
	}

	private static void registerRoot(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
		dispatcher.register(literal(root)
				.executes(context -> openGui(context.getSource()))
				.then(literal("files")
						.executes(context -> openConfigFolder(context.getSource())))
				.then(literal("update")
						.then(literal("check")
								.executes(context -> checkForUpdate(context.getSource())))
						.then(literal("download")
								.executes(context -> downloadUpdate(context.getSource())))
						.then(literal("manual")
								.executes(context -> openUpdatePage(context.getSource())))
						.then(literal("open")
								.executes(context -> openUpdatePage(context.getSource())))
						.executes(context -> checkForUpdate(context.getSource())))
				.then(literal("lc")
						.executes(context -> leftClick(context.getSource())))
				.then(literal("rc")
						.executes(context -> rightClick(context.getSource()))));
	}

	private static void registerRotate(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("rotate")
				.then(argument("a", DoubleArgumentType.doubleArg())
						.then(argument("b", DoubleArgumentType.doubleArg())
								.executes(context -> rotateYawPitch(
										context.getSource(),
										DoubleArgumentType.getDouble(context, "a"),
										DoubleArgumentType.getDouble(context, "b")))
								.then(argument("c", DoubleArgumentType.doubleArg())
										.executes(context -> rotateYawPitchWithMultiplier(
												context.getSource(),
												DoubleArgumentType.getDouble(context, "a"),
												DoubleArgumentType.getDouble(context, "b"),
												DoubleArgumentType.getDouble(context, "c")))
										.then(argument("d", DoubleArgumentType.doubleArg())
												.executes(context -> rotateXYZWithMultiplier(
														context.getSource(),
														DoubleArgumentType.getDouble(context, "a"),
														DoubleArgumentType.getDouble(context, "b"),
														DoubleArgumentType.getDouble(context, "c"),
														DoubleArgumentType.getDouble(context, "d"))))))));
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

	private static int rotateXYZ(FabricClientCommandSource source, double x, double y, double z) {
		source.getClient().execute(() -> RotationController.rotateTo(x, y, z));
		return 1;
	}

	private static void registerRoute(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("route")
				.then(literal("editmode")
						.executes(context -> routeEditMode(context.getSource())))
				.then(literal("select")
						.then(argument("name", StringArgumentType.greedyString())
								.executes(context -> routeSelect(
										context.getSource(),
										StringArgumentType.getString(context, "name")))))
				.then(literal("add")
						.then(literal("tp")
								.executes(context -> routeAdd(context.getSource(), RouteActionType.TP)))
						.then(literal("eth")
								.executes(context -> routeAdd(context.getSource(), RouteActionType.ETH))))
				.then(literal("remove")
						.executes(context -> routeRemoveNearest(context.getSource()))
						.then(argument("index", IntegerArgumentType.integer(1))
								.executes(context -> routeRemove(
										context.getSource(),
										IntegerArgumentType.getInteger(context, "index")))))
				.then(literal("edit")
						.executes(context -> routeEditNearest(context.getSource()))
						.then(argument("index", IntegerArgumentType.integer(1))
								.executes(context -> routeEdit(
										context.getSource(),
										IntegerArgumentType.getInteger(context, "index")))
								.then(literal("tp")
										.executes(context -> routeEdit(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "index"),
												RouteActionType.TP)))
								.then(literal("eth")
										.executes(context -> routeEdit(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "index"),
												RouteActionType.ETH)))
								.then(literal("walk")
										.executes(context -> routeEdit(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "index"),
												RouteActionType.WALK)))))
				.then(literal("insert")
						.then(argument("index", IntegerArgumentType.integer(1))
								.then(literal("tp")
										.executes(context -> routeInsert(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "index"),
												RouteActionType.TP)))
								.then(literal("eth")
										.executes(context -> routeInsert(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "index"),
												RouteActionType.ETH)))
								.then(literal("walk")
										.executes(context -> routeInsert(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "index"),
												RouteActionType.WALK)))))
				.then(literal("walk")
						.executes(context -> routeWalk(context.getSource())))
				.then(literal("delete")
						.then(argument("name", StringArgumentType.greedyString())
								.executes(context -> routeDelete(
										context.getSource(),
										StringArgumentType.getString(context, "name")))))
				.then(literal("clear")
						.then(argument("name", StringArgumentType.greedyString())
								.executes(context -> routeClear(
										context.getSource(),
										StringArgumentType.getString(context, "name")))))
				.then(literal("reload")
						.executes(context -> routeReload(context.getSource())))
				.then(literal("create")
						.then(argument("name", StringArgumentType.greedyString())
								.executes(context -> routeCreate(
										context.getSource(),
										StringArgumentType.getString(context, "name")))))
				.then(literal("save")
						.executes(context -> routeSave(context.getSource()))));
	}

	private static int rotateXYZWithMultiplier(FabricClientCommandSource source, double x, double y, double z, double multiplier) {
		source.getClient().execute(() -> RotationController.rotateTo(x, y, z, multiplier));
		return 1;
	}

	private static int rotateYawPitch(FabricClientCommandSource source, double yaw, double pitch) {
		source.getClient().execute(() -> RotationController.rotateYawPitch(yaw, pitch));
		return 1;
	}

	private static int rotateYawPitchWithMultiplier(FabricClientCommandSource source, double yaw, double pitch, double multiplier) {
		source.getClient().execute(() -> RotationController.rotateYawPitch(yaw, pitch, multiplier));
		return 1;
	}

	private static int leftClick(FabricClientCommandSource source) {
		source.getClient().execute(AutoRendHelper::LC);
		return 1;
	}

	private static int rightClick(FabricClientCommandSource source) {
		source.getClient().execute(AutoRendHelper::RC);
		return 1;
	}

	private static int routeEditMode(FabricClientCommandSource source) {
		source.getClient().execute(RouteSystem::toggleEditModeCommand);
		return 1;
	}

	private static int routeSelect(FabricClientCommandSource source, String routeName) {
		source.getClient().execute(() -> RouteSystem.selectRouteCommand(routeName));
		return 1;
	}

	private static int routeAdd(FabricClientCommandSource source, RouteActionType type) {
		source.getClient().execute(() -> RouteSystem.addActionCommand(type));
		return 1;
	}

	private static int routeRemove(FabricClientCommandSource source, int actionIndex) {
		source.getClient().execute(() -> RouteSystem.removeActionCommand(actionIndex));
		return 1;
	}

	private static int routeRemoveNearest(FabricClientCommandSource source) {
		source.getClient().execute(RouteSystem::removeNearestActionCommand);
		return 1;
	}

	private static int routeEdit(FabricClientCommandSource source, int actionIndex) {
		source.getClient().execute(() -> RouteSystem.editActionCommand(actionIndex));
		return 1;
	}

	private static int routeEdit(FabricClientCommandSource source, int actionIndex, RouteActionType type) {
		source.getClient().execute(() -> RouteSystem.editActionCommand(actionIndex, type));
		return 1;
	}

	private static int routeEditNearest(FabricClientCommandSource source) {
		source.getClient().execute(RouteSystem::editNearestActionCommand);
		return 1;
	}

	private static int routeInsert(FabricClientCommandSource source, int actionIndex, RouteActionType type) {
		source.getClient().execute(() -> RouteSystem.insertActionCommand(actionIndex, type));
		return 1;
	}

	private static int routeWalk(FabricClientCommandSource source) {
		source.getClient().execute(RouteSystem::walkCommand);
		return 1;
	}

	private static int routeDelete(FabricClientCommandSource source, String routeName) {
		source.getClient().execute(() -> RouteSystem.deleteRouteCommand(routeName));
		return 1;
	}

	private static int routeClear(FabricClientCommandSource source, String routeName) {
		source.getClient().execute(() -> RouteSystem.clearRouteCommand(routeName));
		return 1;
	}

	private static int routeReload(FabricClientCommandSource source) {
		source.getClient().execute(RouteSystem::reloadRoutes);
		return 1;
	}

	private static int routeCreate(FabricClientCommandSource source, String routeName) {
		source.getClient().execute(() -> RouteSystem.createRouteCommand(routeName));
		return 1;
	}

	private static int routeSave(FabricClientCommandSource source) {
		source.getClient().execute(RouteSystem::saveRoutes);
		return 1;
	}

	private static int checkForUpdate(FabricClientCommandSource source) {
		source.getClient().execute(() -> ModUpdater.checkForUpdatesAsync(ModUpdater.CheckTrigger.COMMAND));
		return 1;
	}

	private static int downloadUpdate(FabricClientCommandSource source) {
		source.getClient().execute(() -> ModUpdater.downloadLatestAsync(ModUpdater.CheckTrigger.COMMAND));
		return 1;
	}

	private static int openUpdatePage(FabricClientCommandSource source) {
		source.getClient().execute(ModUpdater::openLatestReleasePage);
		return 1;
	}
}
