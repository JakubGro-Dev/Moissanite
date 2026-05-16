package com.crussion.moissanite.features.cheats;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.input.KeyHoldController;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.HotbarItemSearch;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.route.RouteAction;
import com.crussion.moissanite.util.route.RouteActionType;
import com.crussion.moissanite.util.route.RouteDefinition;
import com.crussion.moissanite.util.route.RouteIsland;
import com.crussion.moissanite.util.route.RouteRepository;
import com.crussion.moissanite.util.route.RouteTeleportItem;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.tick.TickTaskScheduler;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RouteSystem {
	private static final String FEATURE_NAME = "Route";
	private static final long ACTION_TIMEOUT_MS = 12_000L;
	private static final long TELEPORT_ARRIVAL_GRACE_MS = 900L;
	private static final float ROUTE_LINE_WIDTH = 2.5F;
	private static final float ROUTE_MARKER_LINE_WIDTH = 2.2F;
	private static final double MARKER_EPSILON = 0.002D;
	private static final double[] MARKER_RINGS = { 0.0D, 0.25D, 0.50D, 0.75D, 1.0D };
	private static final double EDIT_RAYCAST_DISTANCE = 80.0D;
	private static final long SERVER_ROTATION_PACKET_MAX_AGE_MS = 750L;
	private static final double SERVER_ROTATION_TOLERANCE_DEGREES = 0.02D;
	private static final double TELEPORT_TARGET_TOLERANCE_BLOCKS = 1.75D;
	private static final double OLD_SHIFT_EYE_DROP = 0.08D;
	private static final double TELEPORT_AIM_FACE_EPSILON = 1.0E-4D;
	private static final double TELEPORT_AIM_MIN_FACE_MARGIN = 0.005D;
	private static final int TELEPORT_AIM_GRID_SIZE = 33;
	private static final Direction[] TELEPORT_ALL_FACES = Direction.values();

	private static boolean initialized;
	private static boolean editMode;
	private static String editModeKey = "";
	private static String editRouteName = "";
	private static RouteAction pendingWalkStart;
	private static boolean syncingUi;

	private static String runtimeRouteKey = "";
	private static int nextActionIndex;
	private static boolean routeStarted;
	private static long lastRouteAdvanceAtMs;
	private static RouteActionType lastAdvancedActionType;
	private static String lastTriggeredActionKey = "";
	private static BlockPos lastTriggeredBlock;
	private static String lastBlockedActionKey = "";
	private static PendingAction pendingAction;
	private static WaitingWalk waitingWalk;
	private static int inputToken;
	private static boolean routeHoldingEtherShift;
	private static int etherShiftToken;

	private static RenderType routeLinesThroughWallsRenderType;
	private static RenderType routeLinesDepthRenderType;

	private RouteSystem() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		RouteRepository.load();
		bindUi();
		syncRouteOptionsForSelectedIsland();
		ClientTickEvents.END_CLIENT_TICK.register(RouteSystem::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetRuntime(true));
		WorldRenderEvents.END_MAIN.register(RouteSystem::onWorldRender);
	}

	public static List<String> routeIslandDisplayNames() {
		return RouteIsland.displayNames();
	}

	public static boolean hasSelectedUiRoute() {
		RouteIsland island = selectedUiIsland();
		return RouteRepository.getRoute(island.modeKey(), selectedUiRouteName()) != null;
	}

	public static void createRouteFromUi() {
		RouteIsland island = selectedUiIsland();
		String name = RouteRepository.normalizeName(UiDefinitions.ROUTE_NEW_NAME.get());
		if (name.isBlank()) {
			send("Enter a route name first.");
			return;
		}
		if (!RouteRepository.createRoute(island.modeKey(), name)) {
			send("Route already exists: " + name);
			return;
		}
		RouteRepository.save();
		UiDefinitions.ROUTE_NEW_NAME.set("");
		setUiSelection(island.modeKey(), name);
		selectRouteForEdit(island.modeKey(), name);
		send("Created route " + name + " in " + island.displayName() + ".");
	}

	public static void selectSelectedRouteForEditingFromUi() {
		RouteIsland island = selectedUiIsland();
		String routeName = selectedUiRouteName();
		if (RouteRepository.getRoute(island.modeKey(), routeName) == null) {
			send("Select a route first.");
			return;
		}
		editMode = true;
		selectRouteForEdit(island.modeKey(), routeName);
		send("Editing route " + routeName + ".");
	}

	public static void removeSelectedRouteFromUi() {
		RouteIsland island = selectedUiIsland();
		String routeName = selectedUiRouteName();
		if (!RouteRepository.deleteRoute(island.modeKey(), routeName)) {
			send("Select a route first.");
			return;
		}
		if (routeName.equalsIgnoreCase(editRouteName) && island.modeKey().equals(editModeKey)) {
			clearEditSelection();
		}
		RouteRepository.save();
		syncRouteOptionsForSelectedIsland();
		resetRuntime(true);
		send("Removed route " + routeName + ".");
	}

	public static void reloadRoutesFromUi() {
		reloadRoutes();
	}

	public static void saveRoutesFromUi() {
		saveRoutes();
	}

	public static void editRouteFromManager(String modeKey, String routeName) {
		RouteDefinition route = RouteRepository.getRoute(modeKey, routeName);
		if (route == null) {
			send("Route not found: " + RouteRepository.normalizeName(routeName));
			return;
		}
		editMode = true;
		selectRouteForEdit(modeKey, route.name);
		setUiSelection(modeKey, route.name);
		send("Editing route " + route.name + ".");
	}

	public static void toggleEditModeCommand() {
		editMode = !editMode;
		pendingWalkStart = null;
		if (editMode) {
			RouteIsland island = selectedUiIsland();
			String routeName = selectedUiRouteName();
			RouteDefinition route = RouteRepository.getRoute(island.modeKey(), routeName);
			if (route != null) {
				selectRouteForEdit(island.modeKey(), route.name);
				send("Edit mode enabled. Editing route " + route.name + " in " + island.displayName() + ".");
				return;
			}
			editModeKey = "";
			editRouteName = "";
			send("Edit mode enabled. Select a route in the UI or use /route select <name>.");
			return;
		} else {
			pendingWalkStart = null;
		}
		send("Edit mode disabled.");
	}

	public static void selectRouteCommand(String routeName) {
		String name = RouteRepository.normalizeName(routeName);
		if (name.isBlank()) {
			send("Usage: /route select <name>");
			return;
		}
		RouteIsland island = currentIslandOrSelected();
		RouteDefinition route = RouteRepository.getRoute(island.modeKey(), name);
		String modeKey = island.modeKey();
		if (route == null) {
			RouteRepository.RouteHandle handle = RouteRepository.findRoute(name);
			if (handle != null) {
				modeKey = handle.modeKey();
				route = handle.route();
			}
		}
		if (route == null) {
			send("Route not found: " + name);
			return;
		}
		selectRouteForEdit(modeKey, route.name);
		setUiSelection(modeKey, route.name);
		send("Selected route " + route.name + ".");
	}

	public static void createRouteCommand(String routeName) {
		String name = RouteRepository.normalizeName(routeName);
		if (name.isBlank()) {
			send("Usage: /route create <name>");
			return;
		}
		RouteIsland island = currentIslandOrSelected();
		if (!RouteRepository.createRoute(island.modeKey(), name)) {
			send("Route already exists: " + name);
			return;
		}
		RouteRepository.save();
		editMode = true;
		selectRouteForEdit(island.modeKey(), name);
		setUiSelection(island.modeKey(), name);
		send("Created route " + name + " in " + island.displayName() + ".");
	}

	public static void deleteRouteCommand(String routeName) {
		String name = RouteRepository.normalizeName(routeName);
		if (name.isBlank()) {
			send("Usage: /route delete <name>");
			return;
		}
		RouteIsland island = currentIslandOrSelected();
		String modeKey = island.modeKey();
		if (RouteRepository.getRoute(modeKey, name) == null) {
			RouteRepository.RouteHandle handle = RouteRepository.findRoute(name);
			if (handle != null) {
				modeKey = handle.modeKey();
				name = handle.route().name;
			}
		}
		if (!RouteRepository.deleteRoute(modeKey, name)) {
			send("Route not found: " + name);
			return;
		}
		if (name.equalsIgnoreCase(editRouteName) && modeKey.equals(editModeKey)) {
			clearEditSelection();
		}
		RouteRepository.save();
		syncRouteOptionsForSelectedIsland();
		resetRuntime(true);
		send("Deleted route " + name + ".");
	}

	public static void clearRouteCommand(String routeName) {
		String name = RouteRepository.normalizeName(routeName);
		if (name.isBlank()) {
			send("Usage: /route clear <name>");
			return;
		}
		RouteIsland island = currentIslandOrSelected();
		String modeKey = island.modeKey();
		RouteDefinition route = RouteRepository.getRoute(modeKey, name);
		if (route == null) {
			RouteRepository.RouteHandle handle = RouteRepository.findRoute(name);
			if (handle != null) {
				modeKey = handle.modeKey();
				route = handle.route();
				name = route.name;
			}
		}
		if (route == null || !RouteRepository.clearActions(modeKey, name)) {
			send("Route not found: " + name);
			return;
		}
		if (name.equalsIgnoreCase(editRouteName) && modeKey.equals(editModeKey)) {
			pendingWalkStart = null;
		}
		RouteRepository.save();
		syncRouteOptionsForSelectedIsland();
		resetRuntime(true);
		send("Cleared route " + name + ".");
	}

	public static void addActionCommand(RouteActionType type) {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		RouteAction action = captureAction(type, client);
		if (action == null) {
			if (type == RouteActionType.WALK) {
				send("Look at a block to place the waypoint.");
			}
			return;
		}
		alignAppendedActionStart(client, editable.route(), action);
		if (!RouteRepository.addAction(editable.modeKey(), editable.route().name, action)) {
			send("Could not add action.");
			return;
		}
		RouteRepository.save();
		send("Added " + type.name().toLowerCase(Locale.ROOT) + " action at " + formatBlock(action.triggerBlock()) + ".");
	}

	public static void removeActionCommand(int actionNumber) {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		int actionIndex = actionNumber - 1;
		if (!RouteRepository.removeAction(editable.modeKey(), editable.route().name, actionIndex)) {
			send("Action #" + actionNumber + " does not exist.");
			return;
		}
		RouteRepository.save();
		resetRuntime(true);
		send("Removed action #" + actionNumber + " from " + editable.route().name + ".");
	}

	public static void removeNearestActionCommand() {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		RoutePointSelection selection = nearestRoutePoint(editable.route(), client.player.blockPosition());
		if (selection == null) {
			send("No waypoints in " + editable.route().name + ".");
			return;
		}
		boolean removed = selection.finalTarget()
				? RouteRepository.removeFinalTarget(editable.modeKey(), editable.route().name)
				: RouteRepository.removeAction(editable.modeKey(), editable.route().name, selection.actionIndex());
		if (!removed) {
			send("Could not remove nearest point from " + editable.route().name + ".");
			return;
		}
		RouteRepository.save();
		resetRuntime(true);
		send("Removed nearest point #" + selection.pointNumber() + " at " + formatBlock(selection.block()) + " from " + editable.route().name + ".");
	}

	public static void editActionCommand(int actionNumber) {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		RouteAction existing = actionAt(editable.route(), actionNumber - 1);
		if (existing == null) {
			send("Action #" + actionNumber + " does not exist.");
			return;
		}
		editActionCommand(actionNumber, existing.actionType());
	}

	public static void editNearestActionCommand() {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		RoutePointSelection selection = nearestRoutePoint(editable.route(), client.player.blockPosition());
		if (selection == null) {
			send("No waypoints in " + editable.route().name + ".");
			return;
		}
		BlockPos playerBlock = client.player.blockPosition();
		if (selection.finalTarget()) {
			RouteAction last = actionAt(editable.route(), selection.actionIndex());
			if (last == null) {
				send("Could not edit nearest point.");
				return;
			}
			last.setTarget(playerBlock);
		} else {
			RouteAction action = actionAt(editable.route(), selection.actionIndex());
			if (action == null) {
				send("Could not edit nearest point.");
				return;
			}
			BlockPos previous = action.triggerBlock();
			action.x = playerBlock.getX();
			action.y = playerBlock.getY();
			action.z = playerBlock.getZ();
			updatePreviousTargetAfterWaypointMove(editable.route(), selection.actionIndex(), previous, playerBlock);
		}
		RouteRepository.save();
		resetRuntime(true);
		send("Moved nearest point #" + selection.pointNumber() + " from " + formatBlock(selection.block()) + " to " + formatBlock(playerBlock) + ".");
	}

	public static void editActionCommand(int actionNumber, RouteActionType type) {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		int actionIndex = actionNumber - 1;
		RouteAction existing = actionAt(editable.route(), actionIndex);
		if (existing == null) {
			send("Action #" + actionNumber + " does not exist.");
			return;
		}
		BlockPos previousBlock = existing.triggerBlock();
		RouteAction action = captureAction(type, client);
		if (action == null) {
			send(type == RouteActionType.WALK ? "Look at a block to place the waypoint." : "Could not capture action #" + actionNumber + ".");
			return;
		}
		if (actionIndex == editable.route().actions.size() - 1 && action.explicitTargetBlock() == null) {
			action.setTarget(existing.explicitTargetBlock());
		}
		if (!RouteRepository.setAction(editable.modeKey(), editable.route().name, actionIndex, action)) {
			send("Could not edit action #" + actionNumber + ".");
			return;
		}
		updatePreviousTargetAfterWaypointMove(editable.route(), actionIndex, previousBlock, action.triggerBlock());
		RouteRepository.save();
		resetRuntime(true);
		send("Edited action #" + actionNumber + " in " + editable.route().name + ".");
	}

	public static void insertActionCommand(int actionNumber, RouteActionType type) {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		int actionIndex = actionNumber - 1;
		if (editable.route().actions == null || actionIndex < 0 || actionIndex > editable.route().actions.size()) {
			int max = editable.route().actions == null ? 1 : editable.route().actions.size() + 1;
			send("Insert position must be between 1 and " + max + ".");
			return;
		}
		RouteAction action = captureAction(type, client);
		if (action == null) {
			send(type == RouteActionType.WALK ? "Look at a block to place the waypoint." : "Could not capture action #" + actionNumber + ".");
			return;
		}
		if (actionIndex == editable.route().actions.size() && action.explicitTargetBlock() == null && !editable.route().actions.isEmpty()) {
			action.setTarget(editable.route().actions.getLast().explicitTargetBlock());
		}
		if (!RouteRepository.insertAction(editable.modeKey(), editable.route().name, actionIndex, action)) {
			send("Could not insert action #" + actionNumber + ".");
			return;
		}
		RouteRepository.save();
		resetRuntime(true);
		send("Inserted " + type.name().toLowerCase(Locale.ROOT) + " action as #" + actionNumber + " in " + editable.route().name + ".");
	}

	public static void walkCommand() {
		Minecraft client = Minecraft.getInstance();
		if (!canEdit(client)) {
			return;
		}
		EditableRoute editable = editableRoute();
		if (editable == null) {
			send("Select or create a route first.");
			return;
		}
		if (pendingWalkStart == null) {
			pendingWalkStart = captureEditAction(RouteActionType.WALK, client);
			if (pendingWalkStart == null) {
				send("Look at a block to place the walk start.");
				return;
			}
			send("Walk start saved at " + formatBlock(pendingWalkStart.triggerBlock()) + ".");
			return;
		}

		BlockPos target = editWaypointBlock(client);
		if (target == null) {
			send("Look at a block to place the walk target.");
			return;
		}
		pendingWalkStart.setTarget(target);
		if (!RouteRepository.addAction(editable.modeKey(), editable.route().name, pendingWalkStart)) {
			send("Could not add walk action.");
			pendingWalkStart = null;
			return;
		}
		RouteRepository.save();
		send("Added walk from " + formatBlock(pendingWalkStart.triggerBlock()) + " to " + formatBlock(target) + ".");
		pendingWalkStart = null;
	}

	public static void reloadRoutes() {
		RouteRepository.reload();
		syncRouteOptionsForSelectedIsland();
		resetRuntime(true);
		send("Routes reloaded.");
	}

	public static void saveRoutes() {
		RouteRepository.save();
		send("Routes saved.");
	}

	private static void bindUi() {
		UiDefinitions.ROUTE_ISLAND.setOptions(RouteIsland.displayNames());
		UiDefinitions.ROUTE_ISLAND.bind(RouteSystem::onUiIslandChanged);
		UiDefinitions.ROUTE_SELECT.bind(routeName -> syncActiveSwitchFromSelectedRoute());
		UiDefinitions.ROUTE_ACTIVE.bind(RouteSystem::onUiRouteActiveChanged);
	}

	private static void onUiIslandChanged(String ignored) {
		if (!syncingUi) {
			syncRouteOptionsForSelectedIsland();
		}
	}

	private static void onUiRouteActiveChanged(Boolean active) {
		if (syncingUi) {
			return;
		}
		RouteIsland island = selectedUiIsland();
		String routeName = selectedUiRouteName();
		if (RouteRepository.setRouteActive(island.modeKey(), routeName, Boolean.TRUE.equals(active))) {
			RouteRepository.save();
			resetRuntime(true);
			return;
		}
		syncActiveSwitchFromSelectedRoute();
		if (Boolean.TRUE.equals(active)) {
			send("Select a route first.");
		}
	}

	private static void syncRouteOptionsForSelectedIsland() {
		syncingUi = true;
		try {
			RouteIsland island = selectedUiIsland();
			String previous = selectedUiRouteName();
			List<String> names = RouteRepository.routeNames(island.modeKey());
			UiDefinitions.ROUTE_SELECT.setOptions(names);
			String matched = findCaseInsensitive(names, previous);
			if (matched != null) {
				UiDefinitions.ROUTE_SELECT.set(matched);
			}
			setActiveSwitchFromSelectedRoute();
		} finally {
			syncingUi = false;
		}
	}

	private static void syncActiveSwitchFromSelectedRoute() {
		if (syncingUi) {
			return;
		}
		syncingUi = true;
		try {
			setActiveSwitchFromSelectedRoute();
		} finally {
			syncingUi = false;
		}
	}

	private static void setActiveSwitchFromSelectedRoute() {
		RouteIsland island = selectedUiIsland();
		RouteDefinition route = RouteRepository.getRoute(island.modeKey(), selectedUiRouteName());
		UiDefinitions.ROUTE_ACTIVE.set(route != null && route.active);
	}

	private static void setUiSelection(String modeKey, String routeName) {
		syncingUi = true;
		try {
			RouteIsland island = RouteIsland.fromMode(modeKey);
			if (island == null) {
				island = RouteIsland.HUB;
			}
			UiDefinitions.ROUTE_ISLAND.set(island.displayName());
			UiDefinitions.ROUTE_SELECT.setOptions(RouteRepository.routeNames(island.modeKey()));
			UiDefinitions.ROUTE_SELECT.set(routeName);
			setActiveSwitchFromSelectedRoute();
		} finally {
			syncingUi = false;
		}
	}

	private static void selectRouteForEdit(String modeKey, String routeName) {
		editModeKey = RouteRepository.canonicalMode(modeKey);
		editRouteName = RouteRepository.normalizeName(routeName);
		pendingWalkStart = null;
	}

	private static void clearEditSelection() {
		editMode = false;
		editModeKey = "";
		editRouteName = "";
		pendingWalkStart = null;
	}

	private static EditableRoute editableRoute() {
		if (!editRouteName.isBlank()) {
			RouteDefinition route = RouteRepository.getRoute(editModeKey, editRouteName);
			if (route != null) {
				return new EditableRoute(editModeKey, route);
			}
		}

		RouteIsland island = selectedUiIsland();
		String routeName = selectedUiRouteName();
		RouteDefinition route = RouteRepository.getRoute(island.modeKey(), routeName);
		return route == null ? null : new EditableRoute(island.modeKey(), route);
	}

	private static boolean canEdit(Minecraft client) {
		if (!editMode) {
			send("Enable edit mode first with /route editmode.");
			return false;
		}
		if (client == null || client.player == null || client.level == null) {
			send("Waiting for world and player.");
			return false;
		}
		return true;
	}

	private static RouteAction captureAction(RouteActionType type, Minecraft client) {
		if (type == RouteActionType.TP || type == RouteActionType.ETH) {
			return captureTeleportAction(type, client);
		}
		return captureEditAction(type, client);
	}

	private static void alignAppendedActionStart(Minecraft client, RouteDefinition route, RouteAction action) {
		if (client == null || client.player == null || route == null || route.actions == null || route.actions.isEmpty() || action == null) {
			return;
		}
		RouteAction last = route.actions.get(route.actions.size() - 1);
		if (last == null) {
			return;
		}
		BlockPos routeEnd = last.hasTarget() ? last.targetBlock() : last.triggerBlock();
		if (!actionBlockMatchesPlayer(client, routeEnd, client.player.blockPosition())) {
			return;
		}
		action.x = routeEnd.getX();
		action.y = routeEnd.getY();
		action.z = routeEnd.getZ();
	}

	private static RouteAction captureTeleportAction(RouteActionType type, Minecraft client) {
		RouteTeleportItem item = RouteTeleportItem.findFor(type);
		if (item == null) {
			send(type == RouteActionType.ETH
					? "No Aspect with Ether Transmission in hotbar."
					: "No Aspect of the Void/End in hotbar.");
			return null;
		}
		TeleportTarget target = teleportTarget(client, type, item);
		if (target == null) {
			send("Look at a reachable block for " + type.name() + ".");
			return null;
		}
		RouteAction action = RouteAction.capture(type, client.player, client.player.blockPosition());
		action.setTarget(target.block());
		return action;
	}

	private static RouteAction captureEditAction(RouteActionType type, Minecraft client) {
		BlockPos waypoint = editWaypointBlock(client);
		return waypoint == null ? null : RouteAction.capture(type, client.player, waypoint);
	}

	private static BlockPos editWaypointBlock(Minecraft client) {
		FullBlockHit hit = raycastFullBlock(client);
		if (hit == null) {
			return null;
		}
		if (client.player.isShiftKeyDown()) {
			return hit.block();
		}
		return hit.block().relative(hit.face());
	}

	private static FullBlockHit raycastFullBlock(Minecraft client) {
		if (client == null || client.player == null) {
			return null;
		}
		return raycastFullBlock(client, client.player.getEyePosition(), client.player.getViewVector(1.0F), EDIT_RAYCAST_DISTANCE);
	}

	private static FullBlockHit raycastFullBlock(Minecraft client, Vec3 start, Vec3 direction, double maxDistance) {
		return raycastFullBlock(client, start, direction, maxDistance, false);
	}

	private static FullBlockHit raycastSnowBlock(Minecraft client, Vec3 start, Vec3 direction, double maxDistance) {
		return raycastFullBlock(client, start, direction, maxDistance, true);
	}

	private static FullBlockHit raycastFullBlock(Minecraft client, Vec3 start, Vec3 direction, double maxDistance, boolean snowOnly) {
		if (client == null || client.player == null || client.level == null) {
			return null;
		}
		if (start == null || direction == null || maxDistance <= 0.0D) {
			return null;
		}
		if (direction.lengthSqr() <= 1.0E-8D) {
			return null;
		}
		direction = direction.normalize();

		int x = Mth.floor(start.x);
		int y = Mth.floor(start.y);
		int z = Mth.floor(start.z);
		int stepX = sign(direction.x);
		int stepY = sign(direction.y);
		int stepZ = sign(direction.z);
		double tMaxX = initialRayStepDistance(start.x, x, direction.x, stepX);
		double tMaxY = initialRayStepDistance(start.y, y, direction.y, stepY);
		double tMaxZ = initialRayStepDistance(start.z, z, direction.z, stepZ);
		double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(direction.x);
		double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(direction.y);
		double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(direction.z);
		double traveled = 0.0D;
		Direction face = oppositeDominantDirection(direction);
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

		while (traveled <= maxDistance) {
			mutable.set(x, y, z);
			if (snowOnly ? isSnowBlocker(client.level.getBlockState(mutable)) : isRaycastSolidBlock(client, mutable)) {
				Vec3 hitPoint = start.add(direction.scale(Math.max(0.0D, traveled)));
				return new FullBlockHit(mutable.immutable(), face, hitPoint);
			}

			if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
				x += stepX;
				traveled = tMaxX;
				tMaxX += tDeltaX;
				face = stepX > 0 ? Direction.WEST : Direction.EAST;
			} else if (tMaxY <= tMaxZ) {
				y += stepY;
				traveled = tMaxY;
				tMaxY += tDeltaY;
				face = stepY > 0 ? Direction.DOWN : Direction.UP;
			} else {
				z += stepZ;
				traveled = tMaxZ;
				tMaxZ += tDeltaZ;
				face = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
			}
		}
		return null;
	}

	private static BlockPos teleportTargetBlock(Minecraft client, RouteActionType type, RouteTeleportItem item) {
		TeleportTarget target = teleportTarget(client, type, item);
		return target == null ? null : target.block();
	}

	private static TeleportTarget teleportTarget(Minecraft client, RouteActionType type, RouteTeleportItem item) {
		if (client == null || client.player == null || item == null) {
			return null;
		}
		Vec3 direction = client.player.getViewVector(1.0F);
		int range = type == RouteActionType.TP ? item.instantRange() : item.etherRange();
		return teleportTarget(client, type, teleportEyePosition(client, type), direction, range);
	}

	private static BlockPos teleportTargetBlock(Minecraft client, RouteActionType type, Vec3 eyePosition, Vec3 direction, int range) {
		TeleportTarget target = teleportTarget(client, type, eyePosition, direction, range);
		return target == null ? null : target.block();
	}

	private static TeleportTarget teleportTarget(Minecraft client, RouteActionType type, Vec3 eyePosition, Vec3 direction, int range) {
		if (client == null || client.player == null || eyePosition == null || direction == null || range <= 0) {
			return null;
		}
		if (type == RouteActionType.ETH) {
			FullBlockHit hit = raycastFullBlock(client, eyePosition, direction, range);
			return hit == null ? null : new TeleportTarget(hit.block().above(), hit.hitPoint());
		}
		if (type == RouteActionType.TP) {
			FullBlockHit hit = raycastFullBlock(client, eyePosition, direction, range);
			if (hit != null) {
				return new TeleportTarget(hit.block().above(), hit.hitPoint());
			}
		}
		return null;
	}

	private static Vec3 teleportEyePosition(Minecraft client, RouteActionType type) {
		if (type != RouteActionType.ETH) {
			return client.player.getEyePosition();
		}
		Pose pose = Boolean.TRUE.equals(UiDefinitions.ROUTE_USE_OLD_SHIFT_HEIGHT.get()) ? Pose.STANDING : Pose.CROUCHING;
		double eyeHeight = client.player.getEyeHeight(pose);
		if (pose == Pose.STANDING) {
			eyeHeight -= OLD_SHIFT_EYE_DROP;
		}
		return client.player.position().add(0.0D, eyeHeight, 0.0D);
	}

	private static boolean isRaycastSolidBlock(Minecraft client, BlockPos pos) {
		if (pos.getY() < client.level.getMinY() || pos.getY() >= client.level.getMaxY()) {
			return false;
		}
		BlockState state = client.level.getBlockState(pos);
		return isSnowBlocker(state) || (!state.isAir() && !state.getCollisionShape(client.level, pos).isEmpty());
	}

	private static boolean isSnowBlocker(BlockState state) {
		return state != null && (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW));
	}

	private static boolean actionBlockMatchesPlayer(Minecraft client, BlockPos actionBlock, BlockPos playerBlock) {
		if (actionBlock == null || playerBlock == null) {
			return false;
		}
		if (actionBlock.equals(playerBlock)) {
			return true;
		}
		if (client == null || client.level == null) {
			return false;
		}
		if (actionBlock.getX() != playerBlock.getX() || actionBlock.getZ() != playerBlock.getZ()) {
			return false;
		}
		if (actionBlock.getY() + 1 == playerBlock.getY() && isRaycastSolidBlock(client, actionBlock)) {
			return true;
		}
		return actionBlock.getY() == playerBlock.getY() + 1 && isRaycastSolidBlock(client, playerBlock);
	}

	private static double initialRayStepDistance(double start, int block, double direction, int step) {
		if (step > 0) {
			return (block + 1.0D - start) / direction;
		}
		if (step < 0) {
			return (start - block) / -direction;
		}
		return Double.POSITIVE_INFINITY;
	}

	private static int sign(double value) {
		if (value > 0.0D) {
			return 1;
		}
		return value < 0.0D ? -1 : 0;
	}

	private static Direction oppositeDominantDirection(Vec3 direction) {
		double absX = Math.abs(direction.x);
		double absY = Math.abs(direction.y);
		double absZ = Math.abs(direction.z);
		if (absX >= absY && absX >= absZ) {
			return direction.x > 0.0D ? Direction.WEST : Direction.EAST;
		}
		if (absY >= absZ) {
			return direction.y > 0.0D ? Direction.DOWN : Direction.UP;
		}
		return direction.z > 0.0D ? Direction.NORTH : Direction.SOUTH;
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			resetRuntime(true);
			return;
		}
		maintainEtherShift(client);

		if (!Boolean.TRUE.equals(UiDefinitions.ROUTE_SYSTEM.get()) || editMode) {
			resetRuntime(true);
			return;
		}

		SkyBlockLocationTracker.requestRefreshIfNeeded();
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		RouteIsland island = location.isSkyBlock() ? RouteIsland.fromMode(location.mode()) : null;
		if (island == null) {
			resetRuntime(true);
			return;
		}

		tickActiveRoutes(client, island.modeKey());
	}

	private static void tickActiveRoutes(Minecraft client, String modeKey) {
		BlockPos playerBlock = client.player.blockPosition();
		clearTriggeredBlockIfMoved(client, playerBlock);
		if (!hasRunnableActiveRoute(modeKey)) {
			resetRuntime(true);
			return;
		}

		RouteDefinition runtimeRoute = activeRuntimeRoute(modeKey);
		if (runtimeRoute != null) {
			if (tickRoute(client, modeKey, runtimeRoute)) {
				return;
			}
			tryStartActiveRouteAtBlock(client, modeKey, runtimeRouteKey);
			return;
		}

		if (actionBlockMatchesPlayer(client, lastTriggeredBlock, playerBlock)) {
			return;
		}
		if (tryStartActiveRouteAtBlock(client, modeKey, "")) {
			return;
		}
	}

	private static boolean tickRoute(Minecraft client, String modeKey, RouteDefinition route) {
		String routeKey = modeKey + "\u0000" + route.name;
		if (!routeKey.equals(runtimeRouteKey)) {
			resetRuntime(true);
			runtimeRouteKey = routeKey;
		}

		BlockPos playerBlock = client.player.blockPosition();

		if (pendingAction != null) {
			tickPendingAction(client);
			return true;
		}
		if (waitingWalk != null) {
			tickWaitingWalk(client);
			return true;
		}

		List<RouteAction> actions = route.actions;
		if (actions.isEmpty()) {
			return false;
		}
		nextActionIndex = Mth.clamp(nextActionIndex, 0, actions.size() - 1);
		RouteAction expected = actions.get(nextActionIndex);
		if (expected != null && actionBlockMatchesPlayer(client, expected.triggerBlock(), playerBlock)) {
			if (routeTargetBlock(route, nextActionIndex, expected) == null) {
				completeRouteAtTerminalWaypoint(routeKey, nextActionIndex, expected);
				return true;
			}
			beginAction(client, routeKey, route, nextActionIndex, expected);
			return true;
		}

		if (waitForTeleportArrivalOrStop(routeKey, playerBlock)) {
			return true;
		}

		if (actionBlockMatchesPlayer(client, lastTriggeredBlock, playerBlock)) {
			return true;
		}

		return false;
	}

	private static int findStartActionAtBlock(Minecraft client, List<RouteAction> actions, BlockPos block) {
		if (actions == null || actions.isEmpty()) {
			return -1;
		}
		RouteAction action = actions.getFirst();
		if (!actionHasRouteTarget(actions, 0, action)) {
			return -1;
		}
		return action != null && actionBlockMatchesPlayer(client, action.triggerBlock(), block) ? 0 : -1;
	}

	private static boolean tryStartActiveRouteAtBlock(Minecraft client, String modeKey, String excludedRouteKey) {
		BlockPos playerBlock = client.player.blockPosition();
		for (RouteDefinition route : RouteRepository.routes(modeKey)) {
			if (!isRunnableActiveRoute(route)) {
				continue;
			}
			String routeKey = modeKey + "\u0000" + route.name;
			if (routeKey.equals(excludedRouteKey)) {
				continue;
			}
			int actionIndex = findStartActionAtBlock(client, route.actions, playerBlock);
			if (actionIndex < 0) {
				continue;
			}
			String actionKey = actionKey(routeKey, actionIndex);
			if (actionKey.equals(lastTriggeredActionKey)) {
				return true;
			}
			resetRuntime(true);
			runtimeRouteKey = routeKey;
			beginAction(client, routeKey, route, actionIndex, route.actions.get(actionIndex));
			return true;
		}
		return false;
	}

	private static boolean hasRunnableActiveRoute(String modeKey) {
		for (RouteDefinition route : RouteRepository.routes(modeKey)) {
			if (isRunnableActiveRoute(route)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isRunnableActiveRoute(RouteDefinition route) {
		return route != null && route.active && route.actions != null && !route.actions.isEmpty();
	}

	private static RouteDefinition activeRuntimeRoute(String modeKey) {
		if (runtimeRouteKey.isBlank() || !runtimeRouteKeyMatchesMode(modeKey)) {
			return null;
		}
		RouteDefinition route = routeForRuntimeKey(runtimeRouteKey);
		return isRunnableActiveRoute(route) ? route : null;
	}

	private static boolean runtimeRouteKeyMatchesMode(String modeKey) {
		int separator = runtimeRouteKey.indexOf('\u0000');
		return separator > 0 && runtimeRouteKey.substring(0, separator).equals(RouteRepository.canonicalMode(modeKey));
	}

	private static void clearTriggeredBlockIfMoved(Minecraft client, BlockPos playerBlock) {
		if (lastTriggeredBlock != null && !actionBlockMatchesPlayer(client, lastTriggeredBlock, playerBlock)) {
			lastTriggeredActionKey = "";
			lastTriggeredBlock = null;
			lastBlockedActionKey = "";
		}
	}

	private static void beginAction(Minecraft client, String routeKey, RouteDefinition route, int actionIndex, RouteAction action) {
		String actionKey = actionKey(routeKey, actionIndex);
		RouteActionType type = action.actionType();
		BlockPos targetBlock = routeTargetBlock(route, actionIndex, action);
		if (type != RouteActionType.ETH) {
			releaseEtherShift();
		}
		if ((type == RouteActionType.TP || type == RouteActionType.ETH) && targetBlock == null) {
			reportBlocked(actionKey, type, "Saved " + type.name() + " action is missing its target. Re-edit it.");
			return;
		}
		RouteTeleportItem teleportItem = null;
		if (type == RouteActionType.TP || type == RouteActionType.ETH) {
			teleportItem = RouteTeleportItem.findFor(type);
			if (teleportItem == null) {
				reportBlocked(actionKey, type, type == RouteActionType.ETH
						? "No Aspect with Ether Transmission in hotbar."
						: "No Aspect of the Void/End in hotbar.");
				return;
			}
			if (!HotbarItemSearch.swapHeldItem(teleportItem.slot())) {
				reportBlocked(actionKey, type, "Could not swap to teleport item.");
				return;
			}
			if (!canReachTeleportTarget(action, type, teleportItem, targetBlock)) {
				int range = type == RouteActionType.TP ? teleportItem.instantRange() : teleportItem.etherRange();
				reportBlocked(actionKey, type, type.name() + " range " + range + " is too short for the saved target.");
				return;
			}
		}

		int teleportRange = teleportItem == null ? 0 : (type == RouteActionType.TP ? teleportItem.instantRange() : teleportItem.etherRange());
		AimAngles aim = routeAim(client, type, targetBlock, teleportRange);
		if (aim == null) {
			reportBlocked(actionKey, type, "Could not find an aim point for the next " + type.name() + " target.");
			return;
		}
		if (type != RouteActionType.WALK) {
			boolean rotated = RotationController.rotateYawPitch(aim.yaw(), aim.pitch(), configuredRotationMultiplier(), 0.0D, 0.0D);
			if (!rotated) {
				reportBlocked(actionKey, type, "Could not start rotation.");
				return;
			}
		}

		long now = System.currentTimeMillis();
		long delayMs = configuredActionDelayMs(type) + (routeStarted ? configuredBetweenActionDelayMs() : configuredStartDelayMs());
		pendingAction = new PendingAction(
				routeKey,
				actionKey,
				actionIndex,
				action,
				type,
				targetBlock,
				aim.yaw(),
				aim.pitch(),
				teleportRange,
				delayMs,
				now,
				configuredDelayAfterRotation());
	}

	private static void tickPendingAction(Minecraft client) {
		PendingAction pending = pendingAction;
		if (pending == null) {
			return;
		}
		if (pending.awaitingInput) {
			return;
		}
		if (!actionBlockMatchesPlayer(client, pending.action.triggerBlock(), client.player.blockPosition())) {
			RotationController.cancelRotation();
			if (pending.type == RouteActionType.ETH) {
				releaseEtherShift();
			}
			pendingAction = null;
			return;
		}

		long now = System.currentTimeMillis();
		if (now - pending.startedAtMs > ACTION_TIMEOUT_MS) {
			RotationController.cancelRotation();
			if (pending.type == RouteActionType.ETH) {
				releaseEtherShift();
			}
			reportBlocked(pending.actionKey, "Action timed out while rotating.");
			pendingAction = null;
			return;
		}

		if (pending.delayAfterRotation) {
			if (RotationController.isRotating()) {
				return;
			}
			if (pending.delayStartedAtMs < 0L) {
				pending.delayStartedAtMs = now;
			}
			if (now - pending.delayStartedAtMs < pending.delayMs) {
				return;
			}
			if (!serverRotationReady(pending)) {
				return;
			}
			dispatchPendingAction(client, pending);
			return;
		}

		if (RotationController.isRotating()) {
			return;
		}
		if (now - pending.startedAtMs < pending.delayMs) {
			return;
		}
		if (!serverRotationReady(pending)) {
			return;
		}
		dispatchPendingAction(client, pending);
	}

	private static void dispatchPendingAction(Minecraft client, PendingAction pending) {
		switch (pending.type) {
			case TP -> completeTeleportAction(client, pending);
			case ETH -> dispatchEtherTransmission(client, pending);
			case WALK -> startWalkAction(pending);
		}
	}

	private static boolean serverRotationReady(PendingAction pending) {
		if (pending.type == RouteActionType.WALK) {
			return true;
		}
		if (RotationController.hasRecentlySentRotation(
				pending.targetYaw,
				pending.targetPitch,
				SERVER_ROTATION_PACKET_MAX_AGE_MS,
				SERVER_ROTATION_TOLERANCE_DEGREES)) {
			return true;
		}
		if (!pending.sentRotationSyncPacket) {
			pending.sentRotationSyncPacket = RotationController.sendCurrentRotationPacket();
		}
		return false;
	}

	private static void completeTeleportAction(Minecraft client, PendingAction pending) {
		TeleportClickResult result = tryTeleportClickOrRefresh(client, pending);
		if (result == TeleportClickResult.CLICKED) {
			finishAction(true);
			return;
		}
		if (result == TeleportClickResult.FAILED) {
			failPendingAction(pending);
		}
	}

	private static TeleportClickResult tryTeleportClickOrRefresh(Minecraft client, PendingAction pending) {
		if (!teleportTargetStillMatches(client, pending)) {
			if (refreshTeleportAim(client, pending)) {
				return TeleportClickResult.RETRYING;
			}
			reportBlocked(pending.actionKey, "Could not find a visible aim point for saved " + pending.type.name() + " target.");
			return TeleportClickResult.FAILED;
		}
		if (!PlayerInputActions.rightClick()) {
			reportBlocked(pending.actionKey, "Action input failed.");
			return TeleportClickResult.FAILED;
		}
		return TeleportClickResult.CLICKED;
	}

	private static boolean refreshTeleportAim(Minecraft client, PendingAction pending) {
		if (pending.type != RouteActionType.TP && pending.type != RouteActionType.ETH) {
			return false;
		}
		AimAngles aim = routeAim(client, pending.type, pending.targetBlock, pending.teleportRange);
		if (aim == null) {
			return false;
		}
		if (!RotationController.rotateYawPitch(aim.yaw(), aim.pitch(), configuredRotationMultiplier(), 0.0D, 0.0D)) {
			return false;
		}
		pending.targetYaw = aim.yaw();
		pending.targetPitch = aim.pitch();
		pending.sentRotationSyncPacket = false;
		pending.delayStartedAtMs = -1L;
		return true;
	}

	private static void failPendingAction(PendingAction pending) {
		RotationController.cancelRotation();
		if (pending.type == RouteActionType.ETH) {
			releaseEtherShift();
		}
		pendingAction = null;
	}

	private static void dispatchEtherTransmission(Minecraft client, PendingAction pending) {
		if (client == null || client.options == null) {
			finishAction(false);
			return;
		}
		boolean skipShiftDelay = routeHoldingEtherShift;
		etherShiftToken++;
		holdEtherShift(client);
		int shiftDelayTicks = skipShiftDelay ? 0 : configuredEtherShiftDelayTicks();
		if (shiftDelayTicks <= 0) {
			completeTeleportAction(client, pending);
			return;
		}
		pending.awaitingInput = true;
		int token = ++inputToken;
		pending.inputToken = token;
		TickTaskScheduler.schedule(shiftDelayTicks, () -> completeDelayedEtherTransmission(token));
	}

	private static void completeDelayedEtherTransmission(int token) {
		PendingAction current = pendingAction;
		Minecraft scheduledClient = Minecraft.getInstance();
		if (current == null || current.inputToken != token || scheduledClient == null || scheduledClient.player == null) {
			return;
		}
		if (System.currentTimeMillis() - current.startedAtMs > ACTION_TIMEOUT_MS) {
			RotationController.cancelRotation();
			releaseEtherShift();
			pendingAction = null;
			return;
		}
		if (!actionBlockMatchesPlayer(scheduledClient, current.action.triggerBlock(), scheduledClient.player.blockPosition())) {
			releaseEtherShift();
			pendingAction = null;
			return;
		}
		holdEtherShift(scheduledClient);
		if (RotationController.isRotating()) {
			TickTaskScheduler.schedule(1, () -> completeDelayedEtherTransmission(token));
			return;
		}
		if (!serverRotationReady(current)) {
			TickTaskScheduler.schedule(1, () -> completeDelayedEtherTransmission(token));
			return;
		}
		TeleportClickResult result = tryTeleportClickOrRefresh(scheduledClient, current);
		if (result == TeleportClickResult.CLICKED) {
			finishAction(true);
			return;
		}
		if (result == TeleportClickResult.RETRYING) {
			TickTaskScheduler.schedule(1, () -> completeDelayedEtherTransmission(token));
			return;
		}
		failPendingAction(current);
	}

	private static void startWalkAction(PendingAction pending) {
		if (pending.targetBlock == null) {
			finishAction(true);
			return;
		}
		waitingWalk = new WaitingWalk(pending.routeKey, pending.actionKey, pending.actionIndex, pending.targetBlock);
		pendingAction = null;
		send("Walk to " + formatBlock(waitingWalk.target) + ".");
	}

	private static void tickWaitingWalk(Minecraft client) {
		if (waitingWalk == null) {
			return;
		}
		if (!actionBlockMatchesPlayer(client, waitingWalk.target, client.player.blockPosition())) {
			return;
		}
		advanceRoute(waitingWalk.routeKey, waitingWalk.actionKey, waitingWalk.actionIndex);
		waitingWalk = null;
	}

	private static void finishAction(boolean success) {
		PendingAction pending = pendingAction;
		if (pending == null) {
			return;
		}
		if (!success) {
			reportBlocked(pending.actionKey, "Action input failed.");
			if (pending.type == RouteActionType.ETH) {
				releaseEtherShift();
			}
			pendingAction = null;
			return;
		}
		boolean keepEtherShift = pending.type == RouteActionType.ETH && nextActionIsEther(pending.routeKey, pending.actionIndex);
		advanceRoute(pending.routeKey, pending.actionKey, pending.actionIndex);
		if (pending.type == RouteActionType.ETH && !keepEtherShift) {
			scheduleEtherShiftRelease();
		}
		pendingAction = null;
	}

	private static void advanceRoute(String routeKey, String actionKey, int actionIndex) {
		RouteDefinition route = routeForRuntimeKey(routeKey);
		int actionCount = route == null || route.actions == null ? 0 : route.actions.size();
		if (actionCount <= 0) {
			nextActionIndex = 0;
		} else {
			nextActionIndex = Math.min(actionIndex + 1, actionCount - 1);
		}
		routeStarted = true;
		lastTriggeredActionKey = actionKey;
		RouteAction action = route == null || actionIndex < 0 || actionIndex >= actionCount ? null : route.actions.get(actionIndex);
		lastRouteAdvanceAtMs = System.currentTimeMillis();
		lastAdvancedActionType = action == null ? null : action.actionType();
		lastTriggeredBlock = action == null ? null : action.triggerBlock();
		lastBlockedActionKey = "";
	}

	private static boolean nextActionIsEther(String routeKey, int actionIndex) {
		RouteDefinition route = routeForRuntimeKey(routeKey);
		int actionCount = route == null || route.actions == null ? 0 : route.actions.size();
		if (actionCount <= 0) {
			return false;
		}
		if (actionIndex + 1 >= actionCount) {
			return false;
		}
		int nextIndex = actionIndex + 1;
		RouteAction next = route.actions.get(nextIndex);
		return next != null && routeTargetBlock(route, nextIndex, next) != null && next.actionType() == RouteActionType.ETH;
	}

	private static boolean waitForTeleportArrivalOrStop(String routeKey, BlockPos playerBlock) {
		if (lastAdvancedActionType != RouteActionType.TP && lastAdvancedActionType != RouteActionType.ETH) {
			return false;
		}
		if (lastRouteAdvanceAtMs <= 0L || System.currentTimeMillis() - lastRouteAdvanceAtMs < TELEPORT_ARRIVAL_GRACE_MS) {
			return true;
		}
		RouteActionType missedType = lastAdvancedActionType;
		reportBlocked(actionKey(routeKey, nextActionIndex), missedType.name() + " landed at " + formatBlock(playerBlock) + " instead of the expected waypoint. Route stopped.");
		resetRuntime(false);
		return true;
	}

	private static void completeRouteAtTerminalWaypoint(String routeKey, int actionIndex, RouteAction action) {
		lastAdvancedActionType = null;
		lastRouteAdvanceAtMs = 0L;
		lastTriggeredActionKey = actionKey(routeKey, actionIndex);
		lastTriggeredBlock = action == null ? null : action.triggerBlock();
		lastBlockedActionKey = "";
		runtimeRouteKey = "";
		nextActionIndex = 0;
		routeStarted = false;
		releaseEtherShift();
	}

	private static boolean teleportTargetStillMatches(Minecraft client, PendingAction pending) {
		if (pending.type != RouteActionType.TP && pending.type != RouteActionType.ETH) {
			return true;
		}
		if (pending.teleportRange <= 0 || pending.targetBlock == null) {
			return true;
		}
		FullBlockHit hit = raycastMinecraftBlock(
				client,
				teleportEyePosition(client, pending.type),
				client.player.getViewVector(1.0F),
				pending.teleportRange);
		return teleportHitSafelyMatchesTarget(hit, teleportAimBlock(pending.type, pending.targetBlock));
	}

	private static void holdEtherShift(Minecraft client) {
		if (client == null || client.options == null || client.options.keyShift == null) {
			return;
		}
		client.options.keyShift.setDown(true);
		routeHoldingEtherShift = true;
	}

	private static void maintainEtherShift(Minecraft client) {
		if (routeHoldingEtherShift) {
			holdEtherShift(client);
		}
	}

	private static void scheduleEtherShiftRelease() {
		int token = ++etherShiftToken;
		TickTaskScheduler.schedule(1, () -> {
			if (etherShiftToken == token) {
				releaseEtherShift();
			}
		});
	}

	private static void releaseEtherShift() {
		if (!routeHoldingEtherShift) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null && client.options.keyShift != null) {
			KeyHoldController.releaseBoundKey(client.options.keyShift);
		}
		routeHoldingEtherShift = false;
		etherShiftToken++;
	}

	private static RouteDefinition routeForRuntimeKey(String routeKey) {
		if (routeKey == null || routeKey.isBlank()) {
			return null;
		}
		int separator = routeKey.indexOf('\u0000');
		if (separator < 0) {
			return null;
		}
		String modeKey = routeKey.substring(0, separator);
		String routeName = routeKey.substring(separator + 1);
		return RouteRepository.getRoute(modeKey, routeName);
	}

	private static boolean canReachTeleportTarget(RouteAction action, RouteActionType type, RouteTeleportItem item, BlockPos targetBlock) {
		if (action == null || item == null || targetBlock == null) {
			return true;
		}
		BlockPos aimBlock = teleportAimBlock(type, targetBlock);
		double distance = centerOf(action.triggerBlock()).distanceTo(centerOf(aimBlock));
		int range = type == RouteActionType.TP ? item.instantRange() : item.etherRange();
		return distance <= range + TELEPORT_TARGET_TOLERANCE_BLOCKS;
	}

	private static BlockPos routeTargetBlock(RouteDefinition route, int actionIndex, RouteAction action) {
		if (action == null) {
			return null;
		}
		if (action.hasTarget()) {
			return action.targetBlock();
		}
		return nextRouteActionBlock(route, actionIndex);
	}

	private static BlockPos nextRouteActionBlock(RouteDefinition route, int actionIndex) {
		if (route == null || route.actions == null || actionIndex < 0 || actionIndex + 1 >= route.actions.size()) {
			return null;
		}
		RouteAction next = route.actions.get(actionIndex + 1);
		return next == null ? null : next.triggerBlock();
	}

	private static boolean actionHasRouteTarget(List<RouteAction> actions, int actionIndex, RouteAction action) {
		if (action == null) {
			return false;
		}
		return action.hasTarget() || (actions != null && actionIndex >= 0 && actionIndex + 1 < actions.size() && actions.get(actionIndex + 1) != null);
	}

	private static AimAngles routeAim(Minecraft client, RouteActionType type, BlockPos targetBlock, int teleportRange) {
		if ((type == RouteActionType.TP || type == RouteActionType.ETH) && targetBlock != null) {
			Vec3 start = teleportEyePosition(client, type);
			Vec3 target = dynamicTeleportAimPoint(client, type, start, targetBlock, teleportRange);
			if (target == null) {
				return null;
			}
			return aimAngles(start, target);
		}
		return new AimAngles(Mth.wrapDegrees(client.player.getYRot()), Mth.clamp(client.player.getXRot(), -90.0F, 90.0F));
	}

	private static Vec3 dynamicTeleportAimPoint(Minecraft client, RouteActionType type, Vec3 start, BlockPos targetBlock, int range) {
		if (client == null || client.player == null || targetBlock == null || start == null || range <= 0) {
			return null;
		}

		BlockPos aimBlock = teleportAimBlock(type, targetBlock);
		return aimPointForTeleportTarget(client, start, aimBlock, range, visibleAimFaces(start, aimBlock));
	}

	private static BlockPos teleportAimBlock(RouteActionType type, BlockPos targetBlock) {
		if (targetBlock == null) {
			return null;
		}
		return type == RouteActionType.TP || type == RouteActionType.ETH ? targetBlock.below() : targetBlock;
	}

	private static Vec3 aimPointForTeleportTarget(Minecraft client, Vec3 start, BlockPos aimBlock, int range, Direction[] faces) {
		if (aimBlock == null || !isRaycastSolidBlock(client, aimBlock) || faces == null || faces.length == 0) {
			return null;
		}
		TeleportAimCandidate best = null;
		for (Direction face : faces) {
			TeleportAimCandidate candidate = aimCandidateForTeleportFace(client, start, aimBlock, range, face);
			if (betterTeleportAimCandidate(candidate, best)) {
				best = candidate;
			}
		}
		return best == null ? null : best.point();
	}

	private static TeleportAimCandidate aimCandidateForTeleportFace(Minecraft client, Vec3 start, BlockPos aimBlock, int range, Direction face) {
		if (face == null) {
			return null;
		}
		int gridSize = TELEPORT_AIM_GRID_SIZE;
		boolean[][] visible = new boolean[gridSize][gridSize];
		FullBlockHit[][] hits = new FullBlockHit[gridSize][gridSize];
		for (int firstIndex = 0; firstIndex < gridSize; firstIndex++) {
			double firstOffset = teleportAimOffset(firstIndex, gridSize);
			for (int secondIndex = 0; secondIndex < gridSize; secondIndex++) {
				double secondOffset = teleportAimOffset(secondIndex, gridSize);
				Vec3 point = aimPointOnFace(aimBlock, face, firstOffset, secondOffset);
				if (point.distanceTo(start) > range + TELEPORT_TARGET_TOLERANCE_BLOCKS) {
					continue;
				}
				Vec3 direction = point.subtract(start);
				if (direction.lengthSqr() <= 1.0E-8D) {
					continue;
				}
				FullBlockHit hit = raycastMinecraftBlock(client, start, direction, range);
				if (!teleportHitSafelyMatchesTarget(hit, aimBlock) || hit.face() != face) {
					continue;
				}
				visible[firstIndex][secondIndex] = true;
				hits[firstIndex][secondIndex] = hit;
			}
		}

		VisibleComponents components = visibleComponents(visible);
		TeleportAimCandidate best = null;
		for (int firstIndex = 0; firstIndex < gridSize; firstIndex++) {
			for (int secondIndex = 0; secondIndex < gridSize; secondIndex++) {
				FullBlockHit hit = hits[firstIndex][secondIndex];
				if (hit == null) {
					continue;
				}
				int componentId = components.ids()[firstIndex][secondIndex];
				int visibleCells = componentId <= 0 ? 0 : components.sizes()[componentId];
				TeleportAimCandidate candidate = new TeleportAimCandidate(
						visibleAimPoint(hit),
						visibleCells,
						visibleClearance(visible, firstIndex, secondIndex),
						hitFaceMargin(hit),
						hitFaceCenterDistanceSqr(hit),
						hit.hitPoint().distanceToSqr(start));
				if (betterTeleportAimCandidate(candidate, best)) {
					best = candidate;
				}
			}
		}
		return best;
	}

	private static boolean betterTeleportAimCandidate(TeleportAimCandidate candidate, TeleportAimCandidate best) {
		if (candidate == null || candidate.point() == null) {
			return false;
		}
		if (best == null) {
			return true;
		}
		if (candidate.clearance() != best.clearance()) {
			return candidate.clearance() > best.clearance();
		}
		if (candidate.visibleCells() != best.visibleCells()) {
			return candidate.visibleCells() > best.visibleCells();
		}
		if (Math.abs(candidate.faceMargin() - best.faceMargin()) > 1.0E-6D) {
			return candidate.faceMargin() > best.faceMargin();
		}
		if (Math.abs(candidate.faceCenterDistanceSqr() - best.faceCenterDistanceSqr()) > 1.0E-8D) {
			return candidate.faceCenterDistanceSqr() < best.faceCenterDistanceSqr();
		}
		return candidate.distanceSqr() < best.distanceSqr();
	}

	private static double teleportAimOffset(int index, int gridSize) {
		int safeGridSize = Math.max(1, gridSize);
		return (index + 0.5D) / safeGridSize;
	}

	private static VisibleComponents visibleComponents(boolean[][] visible) {
		int gridSize = visible.length;
		int[][] ids = new int[gridSize][gridSize];
		int[] sizes = new int[(gridSize * gridSize) + 1];
		int[] queueFirst = new int[gridSize * gridSize];
		int[] queueSecond = new int[gridSize * gridSize];
		int componentId = 0;
		for (int firstIndex = 0; firstIndex < gridSize; firstIndex++) {
			for (int secondIndex = 0; secondIndex < gridSize; secondIndex++) {
				if (!visible[firstIndex][secondIndex] || ids[firstIndex][secondIndex] != 0) {
					continue;
				}
				componentId++;
				int head = 0;
				int tail = 0;
				int componentSize = 0;
				ids[firstIndex][secondIndex] = componentId;
				queueFirst[tail] = firstIndex;
				queueSecond[tail] = secondIndex;
				tail++;
				while (head < tail) {
					int currentFirst = queueFirst[head];
					int currentSecond = queueSecond[head];
					head++;
					componentSize++;
					tail = enqueueVisibleNeighbor(visible, ids, componentId, queueFirst, queueSecond, tail, currentFirst - 1, currentSecond);
					tail = enqueueVisibleNeighbor(visible, ids, componentId, queueFirst, queueSecond, tail, currentFirst + 1, currentSecond);
					tail = enqueueVisibleNeighbor(visible, ids, componentId, queueFirst, queueSecond, tail, currentFirst, currentSecond - 1);
					tail = enqueueVisibleNeighbor(visible, ids, componentId, queueFirst, queueSecond, tail, currentFirst, currentSecond + 1);
				}
				sizes[componentId] = componentSize;
			}
		}
		return new VisibleComponents(ids, sizes);
	}

	private static int enqueueVisibleNeighbor(
			boolean[][] visible,
			int[][] ids,
			int componentId,
			int[] queueFirst,
			int[] queueSecond,
			int tail,
			int firstIndex,
			int secondIndex) {
		int gridSize = visible.length;
		if (firstIndex < 0 || firstIndex >= gridSize || secondIndex < 0 || secondIndex >= gridSize) {
			return tail;
		}
		if (!visible[firstIndex][secondIndex] || ids[firstIndex][secondIndex] != 0) {
			return tail;
		}
		ids[firstIndex][secondIndex] = componentId;
		queueFirst[tail] = firstIndex;
		queueSecond[tail] = secondIndex;
		return tail + 1;
	}

	private static int visibleClearance(boolean[][] visible, int firstIndex, int secondIndex) {
		int gridSize = visible.length;
		int maxRadius = Math.max(
				Math.max(firstIndex, gridSize - firstIndex - 1),
				Math.max(secondIndex, gridSize - secondIndex - 1)) + 1;
		for (int radius = 1; radius <= maxRadius; radius++) {
			for (int first = firstIndex - radius; first <= firstIndex + radius; first++) {
				if (!isVisibleCell(visible, first, secondIndex - radius) || !isVisibleCell(visible, first, secondIndex + radius)) {
					return radius - 1;
				}
			}
			for (int second = secondIndex - radius + 1; second <= secondIndex + radius - 1; second++) {
				if (!isVisibleCell(visible, firstIndex - radius, second) || !isVisibleCell(visible, firstIndex + radius, second)) {
					return radius - 1;
				}
			}
		}
		return maxRadius;
	}

	private static boolean isVisibleCell(boolean[][] visible, int firstIndex, int secondIndex) {
		int gridSize = visible.length;
		return firstIndex >= 0 && firstIndex < gridSize
				&& secondIndex >= 0 && secondIndex < gridSize
				&& visible[firstIndex][secondIndex];
	}

	private static Direction[] visibleAimFaces(Vec3 start, BlockPos block) {
		if (start == null || block == null) {
			return TELEPORT_ALL_FACES;
		}
		List<Direction> faces = new ArrayList<>(3);
		double minX = block.getX();
		double minY = block.getY();
		double minZ = block.getZ();
		double maxX = minX + 1.0D;
		double maxY = minY + 1.0D;
		double maxZ = minZ + 1.0D;
		if (start.x <= minX) {
			faces.add(Direction.WEST);
		} else if (start.x >= maxX) {
			faces.add(Direction.EAST);
		}
		if (start.y <= minY) {
			faces.add(Direction.DOWN);
		} else if (start.y >= maxY) {
			faces.add(Direction.UP);
		}
		if (start.z <= minZ) {
			faces.add(Direction.NORTH);
		} else if (start.z >= maxZ) {
			faces.add(Direction.SOUTH);
		}
		return faces.isEmpty() ? TELEPORT_ALL_FACES : faces.toArray(Direction[]::new);
	}

	private static Vec3 visibleAimPoint(FullBlockHit hit) {
		Vec3 point = hit.hitPoint();
		Direction face = hit.face();
		if (point == null || face == null) {
			return point;
		}
		return point.subtract(
				face.getStepX() * TELEPORT_AIM_FACE_EPSILON,
				face.getStepY() * TELEPORT_AIM_FACE_EPSILON,
				face.getStepZ() * TELEPORT_AIM_FACE_EPSILON);
	}

	private static Vec3 aimPointOnFace(BlockPos block, Direction face, double firstOffset, double secondOffset) {
		double x = block.getX();
		double y = block.getY();
		double z = block.getZ();
		return switch (face) {
			case WEST -> new Vec3(x + TELEPORT_AIM_FACE_EPSILON, y + firstOffset, z + secondOffset);
			case EAST -> new Vec3(x + 1.0D - TELEPORT_AIM_FACE_EPSILON, y + firstOffset, z + secondOffset);
			case DOWN -> new Vec3(x + firstOffset, y + TELEPORT_AIM_FACE_EPSILON, z + secondOffset);
			case UP -> new Vec3(x + firstOffset, y + 1.0D - TELEPORT_AIM_FACE_EPSILON, z + secondOffset);
			case NORTH -> new Vec3(x + firstOffset, y + secondOffset, z + TELEPORT_AIM_FACE_EPSILON);
			case SOUTH -> new Vec3(x + firstOffset, y + secondOffset, z + 1.0D - TELEPORT_AIM_FACE_EPSILON);
		};
	}

	private static FullBlockHit raycastMinecraftBlock(Minecraft client, Vec3 start, Vec3 direction, double maxDistance) {
		FullBlockHit blockHit = raycastVanillaBlock(client, start, direction, maxDistance);
		FullBlockHit snowHit = raycastSnowBlock(client, start, direction, maxDistance);
		return closerHit(start, blockHit, snowHit);
	}

	private static FullBlockHit raycastVanillaBlock(Minecraft client, Vec3 start, Vec3 direction, double maxDistance) {
		if (client == null || client.player == null || client.level == null || start == null || direction == null || maxDistance <= 0.0D) {
			return null;
		}
		if (direction.lengthSqr() <= 1.0E-8D) {
			return null;
		}
		Vec3 end = start.add(direction.normalize().scale(maxDistance));
		HitResult hit = client.level.clip(new ClipContext(
				start,
				end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				client.player));
		if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			return null;
		}
		return new FullBlockHit(blockHit.getBlockPos(), blockHit.getDirection(), blockHit.getLocation());
	}

	private static FullBlockHit closerHit(Vec3 start, FullBlockHit first, FullBlockHit second) {
		if (first == null) {
			return second;
		}
		if (second == null || start == null) {
			return first;
		}
		return first.hitPoint().distanceToSqr(start) <= second.hitPoint().distanceToSqr(start) ? first : second;
	}

	private static boolean teleportHitMatchesTarget(FullBlockHit hit, BlockPos expectedHitBlock) {
		if (hit == null || expectedHitBlock == null) {
			return false;
		}
		return hit.block().equals(expectedHitBlock);
	}

	private static boolean teleportHitSafelyMatchesTarget(FullBlockHit hit, BlockPos expectedHitBlock) {
		return teleportHitMatchesTarget(hit, expectedHitBlock) && hitFaceMargin(hit) >= TELEPORT_AIM_MIN_FACE_MARGIN;
	}

	private static double hitFaceMargin(FullBlockHit hit) {
		FaceCoordinates coordinates = hitFaceCoordinates(hit);
		if (coordinates == null) {
			return 0.0D;
		}
		double first = Mth.clamp(coordinates.first(), 0.0D, 1.0D);
		double second = Mth.clamp(coordinates.second(), 0.0D, 1.0D);
		return Math.min(Math.min(first, 1.0D - first), Math.min(second, 1.0D - second));
	}

	private static double hitFaceCenterDistanceSqr(FullBlockHit hit) {
		FaceCoordinates coordinates = hitFaceCoordinates(hit);
		if (coordinates == null) {
			return Double.POSITIVE_INFINITY;
		}
		double first = Mth.clamp(coordinates.first(), 0.0D, 1.0D) - 0.5D;
		double second = Mth.clamp(coordinates.second(), 0.0D, 1.0D) - 0.5D;
		return first * first + second * second;
	}

	private static FaceCoordinates hitFaceCoordinates(FullBlockHit hit) {
		if (hit == null || hit.hitPoint() == null || hit.face() == null) {
			return null;
		}
		double x = hit.hitPoint().x - hit.block().getX();
		double y = hit.hitPoint().y - hit.block().getY();
		double z = hit.hitPoint().z - hit.block().getZ();
		return switch (hit.face().getAxis()) {
			case X -> new FaceCoordinates(y, z);
			case Y -> new FaceCoordinates(x, z);
			case Z -> new FaceCoordinates(x, y);
		};
	}

	private static AimAngles aimAngles(Vec3 start, Vec3 target) {
		double dx = target.x - start.x;
		double dy = target.y - start.y;
		double dz = target.z - start.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D));
		float pitch = Mth.clamp((float) (-Math.toDegrees(Math.atan2(dy, horizontal))), -90.0F, 90.0F);
		return new AimAngles(yaw, pitch);
	}

	private static String actionKey(String routeKey, int actionIndex) {
		return routeKey + "\u0000" + actionIndex;
	}

	private static void reportBlocked(String actionKey, String reason) {
		if (actionKey.equals(lastBlockedActionKey)) {
			return;
		}
		lastBlockedActionKey = actionKey;
		send(reason);
	}

	private static void reportBlocked(String actionKey, RouteActionType type, String reason) {
		if (type == RouteActionType.ETH) {
			releaseEtherShift();
		}
		reportBlocked(actionKey, reason);
	}

	private static void resetRuntime(boolean cancelRotation) {
		if (cancelRotation && pendingAction != null) {
			RotationController.cancelRotation();
		}
		runtimeRouteKey = "";
		nextActionIndex = 0;
		routeStarted = false;
		lastRouteAdvanceAtMs = 0L;
		lastAdvancedActionType = null;
		lastTriggeredActionKey = "";
		lastTriggeredBlock = null;
		lastBlockedActionKey = "";
		pendingAction = null;
		waitingWalk = null;
		releaseEtherShift();
	}

	private static void onWorldRender(WorldRenderContext context) {
		if (context == null || context.matrices() == null || context.consumers() == null) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.ROUTE_SYSTEM.get()) || !Boolean.TRUE.equals(UiDefinitions.ROUTE_RENDER.get())) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null || client.gameRenderer == null) {
			return;
		}

		List<DisplayRoute> displayRoutes = routesToRender();
		if (displayRoutes.isEmpty()) {
			return;
		}

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		boolean throughWalls = Boolean.TRUE.equals(UiDefinitions.ROUTE_RENDER_THROUGH_WALLS.get());
		RenderType renderType = getRouteLinesRenderType(throughWalls);
		VertexConsumer buffer = context.consumers().getBuffer(renderType);
		PoseStack.Pose pose = context.matrices().last();

		for (DisplayRoute displayRoute : displayRoutes) {
			renderRoute(context, buffer, pose, cameraPos, displayRoute.route().actions, throughWalls);
		}
	}

	private static void renderRoute(WorldRenderContext context, VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, List<RouteAction> actions, boolean throughWalls) {
		if (actions == null || actions.isEmpty()) {
			return;
		}
		int waypointCount = renderedWaypointCount(actions);
		for (int i = 0; i < actions.size(); i++) {
			RouteAction action = actions.get(i);
			if (action == null) {
				continue;
			}
			int color = markerColor(i, waypointCount);
			renderMarker(context, cameraPos, action.triggerBlock(), color, throughWalls);

			Vec3 from = centerOf(action.triggerBlock());
			if (i + 1 < actions.size() && actions.get(i + 1) != null) {
				drawLine(buffer, pose, from, centerOf(actions.get(i + 1).triggerBlock()), cameraPos, color, ROUTE_LINE_WIDTH);
				continue;
			}
			if (action.hasTarget()) {
				BlockPos target = action.targetBlock();
				renderMarker(context, cameraPos, target, markerColor(i + 1, waypointCount), throughWalls);
				drawLine(buffer, pose, from, centerOf(target), cameraPos, color, ROUTE_LINE_WIDTH);
			}
		}
	}

	private static int renderedWaypointCount(List<RouteAction> actions) {
		if (actions == null || actions.isEmpty()) {
			return 0;
		}
		RouteAction last = actions.get(actions.size() - 1);
		if (last != null && last.hasTarget()) {
			return actions.size() + 1;
		}
		return actions.size();
	}

	private static List<DisplayRoute> routesToRender() {
		List<DisplayRoute> routes = new ArrayList<>();
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		RouteIsland currentIsland = location.isSkyBlock() ? RouteIsland.fromMode(location.mode()) : null;
		if (currentIsland == null) {
			return routes;
		}

		RouteDefinition editRoute = null;
		if (editMode && currentIsland.modeKey().equals(editModeKey) && !editRouteName.isBlank()) {
			editRoute = RouteRepository.getRoute(editModeKey, editRouteName);
			if (editRoute != null && editRoute.actions != null && !editRoute.actions.isEmpty()) {
				routes.add(new DisplayRoute(editModeKey, editRoute));
			}
		}
		for (RouteDefinition route : RouteRepository.routes(currentIsland.modeKey())) {
			if (!isRunnableActiveRoute(route) || route == editRoute) {
				continue;
			}
			routes.add(new DisplayRoute(currentIsland.modeKey(), route));
		}
		return routes;
	}

	private static void renderMarker(WorldRenderContext context, Vec3 cameraPos, BlockPos block, int color, boolean throughWalls) {
		VertexConsumer buffer = context.consumers().getBuffer(getRouteLinesRenderType(throughWalls));
		PoseStack.Pose pose = context.matrices().last();
		for (double ringHeight : MARKER_RINGS) {
			double y = block.getY() + Mth.clamp(ringHeight, 0.0D, 1.0D);
			Vec3 p1 = new Vec3(block.getX(), y + MARKER_EPSILON, block.getZ());
			Vec3 p2 = new Vec3(block.getX() + 1.0D, y + MARKER_EPSILON, block.getZ());
			Vec3 p3 = new Vec3(block.getX() + 1.0D, y + MARKER_EPSILON, block.getZ() + 1.0D);
			Vec3 p4 = new Vec3(block.getX(), y + MARKER_EPSILON, block.getZ() + 1.0D);
			drawLine(buffer, pose, p1, p2, cameraPos, color, ROUTE_MARKER_LINE_WIDTH);
			drawLine(buffer, pose, p2, p3, cameraPos, color, ROUTE_MARKER_LINE_WIDTH);
			drawLine(buffer, pose, p3, p4, cameraPos, color, ROUTE_MARKER_LINE_WIDTH);
			drawLine(buffer, pose, p4, p1, cameraPos, color, ROUTE_MARKER_LINE_WIDTH);
		}
	}

	private static void drawLine(VertexConsumer buffer, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 cameraPos, int color, float lineWidth) {
		double x1 = from.x - cameraPos.x;
		double y1 = from.y - cameraPos.y;
		double z1 = from.z - cameraPos.z;
		double x2 = to.x - cameraPos.x;
		double y2 = to.y - cameraPos.y;
		double z2 = to.z - cameraPos.z;
		double dx = x2 - x1;
		double dy = y2 - y1;
		double dz = z2 - z1;
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length <= 1.0E-6D) {
			return;
		}
		float nx = (float) (dx / length);
		float ny = (float) (dy / length);
		float nz = (float) (dz / length);
		buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
				.setColor(color)
				.setNormal(pose, nx, ny, nz)
				.setLineWidth(lineWidth);
		buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
				.setColor(color)
				.setNormal(pose, nx, ny, nz)
				.setLineWidth(lineWidth);
	}

	private static Vec3 centerOf(BlockPos block) {
		return new Vec3(block.getX() + 0.5D, block.getY() + 0.5D, block.getZ() + 0.5D);
	}

	private static int markerColor(int index, int actionCount) {
		if (index <= 0) {
			return UiDefinitions.ROUTE_START_COLOR.argb();
		}
		if (index >= actionCount - 1) {
			return UiDefinitions.ROUTE_END_COLOR.argb();
		}
		return UiDefinitions.ROUTE_POINT_COLOR.argb();
	}

	private static RenderType getRouteLinesRenderType(boolean throughWalls) {
		if (throughWalls) {
			if (routeLinesThroughWallsRenderType == null) {
				routeLinesThroughWallsRenderType = createRouteLinesRenderType("moissanite_route_lines_through_walls", DepthTestFunction.NO_DEPTH_TEST);
			}
			return routeLinesThroughWallsRenderType;
		}
		if (routeLinesDepthRenderType == null) {
			routeLinesDepthRenderType = createRouteLinesRenderType("moissanite_route_lines_depth", DepthTestFunction.LEQUAL_DEPTH_TEST);
		}
		return routeLinesDepthRenderType;
	}

	private static RenderType createRouteLinesRenderType(String name, DepthTestFunction depthTestFunction) {
		RenderPipeline source = RenderPipelines.LINES;
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation(name)
				.withVertexShader(source.getVertexShader())
				.withFragmentShader(source.getFragmentShader())
				.withCull(source.isCull())
				.withDepthWrite(false)
				.withDepthTestFunction(depthTestFunction)
				.withVertexFormat(source.getVertexFormat(), source.getVertexFormatMode());

		source.getBlendFunction().ifPresent(builder::withBlend);
		for (String sampler : source.getSamplers()) {
			builder.withSampler(sampler);
		}
		for (RenderPipeline.UniformDescription uniform : source.getUniforms()) {
			if (uniform.type() == UniformType.TEXEL_BUFFER) {
				builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
				continue;
			}
			builder.withUniform(uniform.name(), uniform.type());
		}
		source.getShaderDefines().flags().forEach(builder::withShaderDefine);
		source.getShaderDefines().values().forEach((key, value) -> applyNumericShaderDefine(builder, key, value));

		RenderPipeline pipeline = RenderPipelines.register(builder.build());
		RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
		return RenderType.create(name, setup);
	}

	private static void applyNumericShaderDefine(RenderPipeline.Builder builder, String key, String value) {
		if (builder == null || key == null || key.isBlank() || value == null || value.isBlank()) {
			return;
		}
		try {
			builder.withShaderDefine(key, Integer.parseInt(value));
			return;
		} catch (NumberFormatException ignored) {
		}
		try {
			builder.withShaderDefine(key, Float.parseFloat(value));
		} catch (NumberFormatException ignored) {
		}
	}

	private static RouteIsland selectedUiIsland() {
		return RouteIsland.fromDisplayName(UiDefinitions.ROUTE_ISLAND.get());
	}

	private static String selectedUiRouteName() {
		return RouteRepository.normalizeName(UiDefinitions.ROUTE_SELECT.get());
	}

	private static RouteIsland currentIslandOrSelected() {
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		RouteIsland island = RouteIsland.fromMode(location.mode());
		return island == null ? selectedUiIsland() : island;
	}

	private static RouteAction actionAt(RouteDefinition route, int actionIndex) {
		if (route == null || route.actions == null || actionIndex < 0 || actionIndex >= route.actions.size()) {
			return null;
		}
		return route.actions.get(actionIndex);
	}

	private static RoutePointSelection nearestRoutePoint(RouteDefinition route, BlockPos block) {
		if (route == null || route.actions == null || route.actions.isEmpty() || block == null) {
			return null;
		}
		int bestIndex = -1;
		boolean bestFinalTarget = false;
		BlockPos bestBlock = null;
		int bestPointNumber = -1;
		long bestDistanceSqr = Long.MAX_VALUE;
		for (int i = 0; i < route.actions.size(); i++) {
			RouteAction action = route.actions.get(i);
			if (action == null) {
				continue;
			}
			BlockPos actionBlock = action.triggerBlock();
			long distanceSqr = distanceSqr(actionBlock, block);
			if (distanceSqr < bestDistanceSqr) {
				bestDistanceSqr = distanceSqr;
				bestIndex = i;
				bestFinalTarget = false;
				bestBlock = actionBlock;
				bestPointNumber = i + 1;
			}
		}
		RouteAction last = route.actions.get(route.actions.size() - 1);
		if (last != null && last.hasTarget()) {
			BlockPos targetBlock = last.targetBlock();
			long distanceSqr = distanceSqr(targetBlock, block);
			if (distanceSqr < bestDistanceSqr) {
				bestIndex = route.actions.size() - 1;
				bestFinalTarget = true;
				bestBlock = targetBlock;
				bestPointNumber = route.actions.size() + 1;
			}
		}
		return bestIndex < 0 ? null : new RoutePointSelection(bestIndex, bestFinalTarget, bestPointNumber, bestBlock);
	}

	private static long distanceSqr(BlockPos first, BlockPos second) {
		if (first == null || second == null) {
			return Long.MAX_VALUE;
		}
		long dx = (long) first.getX() - second.getX();
		long dy = (long) first.getY() - second.getY();
		long dz = (long) first.getZ() - second.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private static void updatePreviousTargetAfterWaypointMove(RouteDefinition route, int actionIndex, BlockPos previousBlock, BlockPos newBlock) {
		if (route == null || route.actions == null || actionIndex <= 0 || actionIndex > route.actions.size() - 1 || previousBlock == null || newBlock == null) {
			return;
		}
		RouteAction previous = route.actions.get(actionIndex - 1);
		if (previous == null || previous.actionType() == RouteActionType.WALK) {
			return;
		}
		BlockPos previousTarget = previous.explicitTargetBlock();
		if (previousTarget == null || previousTarget.equals(previousBlock)) {
			previous.setTarget(newBlock);
		}
	}

	private static String findCaseInsensitive(List<String> values, String target) {
		if (target == null || target.isBlank() || values == null) {
			return null;
		}
		for (String value : values) {
			if (value != null && value.equalsIgnoreCase(target)) {
				return value;
			}
		}
		return null;
	}

	private static int configuredActionDelayMs(RouteActionType type) {
		return switch (type) {
			case TP -> configuredInt(UiDefinitions.ROUTE_TP_DELAY.get(), 200, 0, 10_000);
			case ETH -> configuredInt(UiDefinitions.ROUTE_ETH_DELAY.get(), 200, 0, 10_000);
			case WALK -> configuredInt(UiDefinitions.ROUTE_WALK_DELAY.get(), 0, 0, 10_000);
		};
	}

	private static int configuredBetweenActionDelayMs() {
		return configuredInt(UiDefinitions.ROUTE_ACTION_DELAY.get(), 200, 0, 10_000);
	}

	private static int configuredStartDelayMs() {
		return configuredInt(UiDefinitions.ROUTE_START_DELAY.get(), 0, 0, 10_000);
	}

	private static int configuredEtherShiftDelayTicks() {
		return configuredInt(UiDefinitions.ROUTE_ETH_SHIFT_DELAY.get(), 2, 1, 5);
	}

	private static boolean configuredDelayAfterRotation() {
		return Boolean.TRUE.equals(UiDefinitions.ROUTE_DELAY_AFTER_ROTATION.get());
	}

	private static double configuredRotationMultiplier() {
		Double value = UiDefinitions.ROUTE_ROTATION_MULTIPLIER.get();
		double raw = value != null && Double.isFinite(value) ? value : 0.5D;
		return Mth.clamp(raw, 0.0D, 2.0D);
	}

	private static int configuredInt(Integer value, int fallback, int min, int max) {
		int raw = value == null ? fallback : value;
		return Mth.clamp(raw, min, max);
	}

	private static String formatBlock(BlockPos block) {
		if (block == null) {
			return "unknown";
		}
		return block.getX() + " " + block.getY() + " " + block.getZ();
	}

	private static void send(String text) {
		FeatureChat.sendPrefixed(FEATURE_NAME, text);
	}

	private record EditableRoute(String modeKey, RouteDefinition route) {
	}

	private record DisplayRoute(String modeKey, RouteDefinition route) {
	}

	private record FullBlockHit(BlockPos block, Direction face, Vec3 hitPoint) {
	}

	private record TeleportAimCandidate(Vec3 point, int visibleCells, int clearance, double faceMargin, double faceCenterDistanceSqr, double distanceSqr) {
	}

	private record VisibleComponents(int[][] ids, int[] sizes) {
	}

	private record TeleportTarget(BlockPos block, Vec3 aimPoint) {
	}

	private record AimAngles(float yaw, float pitch) {
	}

	private record FaceCoordinates(double first, double second) {
	}

	private record RoutePointSelection(int actionIndex, boolean finalTarget, int pointNumber, BlockPos block) {
	}

	private enum TeleportClickResult {
		CLICKED,
		RETRYING,
		FAILED
	}

	private static final class PendingAction {
		private final String routeKey;
		private final String actionKey;
		private final int actionIndex;
		private final RouteAction action;
		private final RouteActionType type;
		private final BlockPos targetBlock;
		private float targetYaw;
		private float targetPitch;
		private final int teleportRange;
		private final long delayMs;
		private final long startedAtMs;
		private final boolean delayAfterRotation;
		private long delayStartedAtMs = -1L;
		private boolean awaitingInput;
		private boolean sentRotationSyncPacket;
		private int inputToken;

		private PendingAction(
				String routeKey,
				String actionKey,
				int actionIndex,
				RouteAction action,
				RouteActionType type,
				BlockPos targetBlock,
				float targetYaw,
				float targetPitch,
				int teleportRange,
				long delayMs,
				long startedAtMs,
				boolean delayAfterRotation) {
			this.routeKey = routeKey;
			this.actionKey = actionKey;
			this.actionIndex = actionIndex;
			this.action = action;
			this.type = type;
			this.targetBlock = targetBlock;
			this.targetYaw = targetYaw;
			this.targetPitch = targetPitch;
			this.teleportRange = teleportRange;
			this.delayMs = Math.max(0L, delayMs);
			this.startedAtMs = startedAtMs;
			this.delayAfterRotation = delayAfterRotation;
		}
	}

	private record WaitingWalk(String routeKey, String actionKey, int actionIndex, BlockPos target) {
	}
}
