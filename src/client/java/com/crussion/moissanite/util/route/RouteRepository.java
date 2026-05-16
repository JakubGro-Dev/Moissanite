package com.crussion.moissanite.util.route;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RouteRepository {
	private static final Path ROUTE_PATH = FabricLoader.getInstance().getConfigDir().resolve("moissanite").resolve("routes.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static RouteFile file = new RouteFile();
	private static boolean loaded;

	private RouteRepository() {
	}

	public static void load() {
		loaded = true;
		if (!Files.exists(ROUTE_PATH)) {
			file = new RouteFile();
			return;
		}

		try {
			String content = Files.readString(ROUTE_PATH, StandardCharsets.UTF_8);
			RouteFile loadedFile = GSON.fromJson(content, RouteFile.class);
			file = loadedFile == null ? new RouteFile() : loadedFile;
			sanitizeFile();
		} catch (Exception ignored) {
			file = new RouteFile();
		}
	}

	public static void reload() {
		loaded = false;
		load();
	}

	public static void save() {
		ensureLoaded();
		sanitizeFile();
		try {
			Files.createDirectories(ROUTE_PATH.getParent());
			Files.writeString(ROUTE_PATH, GSON.toJson(file), StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}

	public static List<RouteDefinition> routes(String modeKey) {
		ensureLoaded();
		return file.routes.computeIfAbsent(canonicalMode(modeKey), ignored -> new ArrayList<>());
	}

	public static List<String> routeNames(String modeKey) {
		List<String> names = new ArrayList<>();
		for (RouteDefinition route : routes(modeKey)) {
			if (route != null && route.name != null && !route.name.isBlank()) {
				names.add(route.name);
			}
		}
		return names;
	}

	public static RouteDefinition getRoute(String modeKey, String routeName) {
		String normalizedName = normalizeName(routeName);
		if (normalizedName.isBlank()) {
			return null;
		}
		for (RouteDefinition route : routes(modeKey)) {
			if (normalizeName(route.name).equalsIgnoreCase(normalizedName)) {
				return route;
			}
		}
		return null;
	}

	public static RouteHandle findRoute(String routeName) {
		ensureLoaded();
		String normalizedName = normalizeName(routeName);
		if (normalizedName.isBlank()) {
			return null;
		}
		for (Map.Entry<String, List<RouteDefinition>> entry : file.routes.entrySet()) {
			for (RouteDefinition route : entry.getValue()) {
				if (normalizeName(route.name).equalsIgnoreCase(normalizedName)) {
					return new RouteHandle(entry.getKey(), route);
				}
			}
		}
		return null;
	}

	public static RouteDefinition activeRoute(String modeKey) {
		for (RouteDefinition route : routes(modeKey)) {
			if (route != null && route.active && route.actions != null && !route.actions.isEmpty()) {
				return route;
			}
		}
		return null;
	}

	public static boolean createRoute(String modeKey, String routeName) {
		String name = normalizeName(routeName);
		if (name.isBlank() || getRoute(modeKey, name) != null) {
			return false;
		}
		routes(modeKey).add(new RouteDefinition(name));
		return true;
	}

	public static boolean deleteRoute(String modeKey, String routeName) {
		List<RouteDefinition> routes = routes(modeKey);
		String normalizedName = normalizeName(routeName);
		Iterator<RouteDefinition> iterator = routes.iterator();
		while (iterator.hasNext()) {
			RouteDefinition route = iterator.next();
			if (normalizeName(route.name).equalsIgnoreCase(normalizedName)) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}

	public static boolean setRouteActive(String modeKey, String routeName, boolean active) {
		RouteDefinition selected = getRoute(modeKey, routeName);
		if (selected == null) {
			return false;
		}
		selected.active = active;
		return true;
	}

	public static boolean clearActions(String modeKey, String routeName) {
		RouteDefinition route = getRoute(modeKey, routeName);
		if (route == null) {
			return false;
		}
		if (route.actions == null) {
			route.actions = new ArrayList<>();
		} else {
			route.actions.clear();
		}
		return true;
	}

	public static boolean addAction(String modeKey, String routeName, RouteAction action) {
		RouteDefinition route = getRoute(modeKey, routeName);
		if (route == null || action == null) {
			return false;
		}
		if (route.actions == null) {
			route.actions = new ArrayList<>();
		}
		appendRouteAction(route, action);
		normalizeRouteTargets(route);
		return true;
	}

	public static boolean insertAction(String modeKey, String routeName, int actionIndex, RouteAction action) {
		RouteDefinition route = getRoute(modeKey, routeName);
		if (route == null || action == null) {
			return false;
		}
		if (route.actions == null) {
			route.actions = new ArrayList<>();
		}
		if (actionIndex < 0 || actionIndex > route.actions.size()) {
			return false;
		}
		if (actionIndex == route.actions.size()) {
			appendRouteAction(route, action);
		} else {
			action.clearTarget();
			route.actions.add(actionIndex, action);
		}
		normalizeRouteTargets(route);
		return true;
	}

	public static boolean setAction(String modeKey, String routeName, int actionIndex, RouteAction action) {
		RouteDefinition route = getRoute(modeKey, routeName);
		if (route == null || action == null || route.actions == null || actionIndex < 0 || actionIndex >= route.actions.size()) {
			return false;
		}
		if (actionIndex + 1 < route.actions.size()) {
			action.clearTarget();
		}
		route.actions.set(actionIndex, action);
		normalizeRouteTargets(route);
		return true;
	}

	public static boolean removeAction(String modeKey, String routeName, int actionIndex) {
		RouteDefinition route = getRoute(modeKey, routeName);
		if (route == null || route.actions == null || actionIndex < 0 || actionIndex >= route.actions.size()) {
			return false;
		}
		route.actions.remove(actionIndex);
		normalizeRouteTargets(route);
		return true;
	}

	public static boolean removeFinalTarget(String modeKey, String routeName) {
		RouteDefinition route = getRoute(modeKey, routeName);
		if (route == null || route.actions == null || route.actions.isEmpty()) {
			return false;
		}
		RouteAction last = route.actions.get(route.actions.size() - 1);
		if (last == null || !last.hasTarget()) {
			return false;
		}
		last.clearTarget();
		normalizeRouteTargets(route);
		return true;
	}

	public static String normalizeName(String routeName) {
		if (routeName == null) {
			return "";
		}
		String normalized = routeName.trim().replaceAll("\\s+", " ");
		return normalized.length() > 80 ? normalized.substring(0, 80).trim() : normalized;
	}

	public static String canonicalMode(String modeKey) {
		RouteIsland island = RouteIsland.fromMode(modeKey);
		return island == null ? normalizeMode(modeKey) : island.modeKey();
	}

	private static void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	private static void sanitizeFile() {
		if (file == null) {
			file = new RouteFile();
		}
		if (file.routes == null) {
			file.routes = new LinkedHashMap<>();
		}

		Map<String, List<RouteDefinition>> sanitized = new LinkedHashMap<>();
		for (Map.Entry<String, List<RouteDefinition>> entry : file.routes.entrySet()) {
			String mode = canonicalMode(entry.getKey());
			List<RouteDefinition> routes = entry.getValue();
			if (mode.isBlank() || routes == null) {
				continue;
			}
			sanitizeRoutes(routes);
			if (!routes.isEmpty()) {
				sanitized.computeIfAbsent(mode, ignored -> new ArrayList<>()).addAll(routes);
			}
		}
		file.routes = sanitized;
	}

	private static void sanitizeRoutes(List<RouteDefinition> routes) {
		for (Iterator<RouteDefinition> iterator = routes.iterator(); iterator.hasNext();) {
			RouteDefinition route = iterator.next();
			if (route == null) {
				iterator.remove();
				continue;
			}
			route.name = normalizeName(route.name);
			if (route.name.isBlank()) {
				iterator.remove();
				continue;
			}
			if (route.actions == null) {
				route.actions = new ArrayList<>();
				continue;
			}
			normalizeRouteTargets(route);
		}
	}

	private static void normalizeRouteTargets(RouteDefinition route) {
		if (route == null || route.actions == null) {
			return;
		}
		route.actions.removeIf(action -> action == null);
		BlockPos finalTarget = null;
		RouteActionType finalTargetType = RouteActionType.TP;
		for (int i = 0; i < route.actions.size(); i++) {
			RouteAction action = route.actions.get(i);
			action.type = action.actionType().name();
			if (action.hasTarget()) {
				finalTarget = action.targetBlock();
				finalTargetType = action.actionType();
			}
			action.clearTarget();
		}
		if (finalTarget == null) {
			return;
		}
		RouteAction last = route.actions.isEmpty() ? null : route.actions.get(route.actions.size() - 1);
		if (last == null || !finalTarget.equals(last.triggerBlock())) {
			route.actions.add(actionAt(finalTargetType, finalTarget));
		}
	}

	private static void appendRouteAction(RouteDefinition route, RouteAction action) {
		if (route.actions.isEmpty()) {
			route.actions.add(action);
			return;
		}
		RouteAction last = route.actions.get(route.actions.size() - 1);
		if (last != null && last.triggerBlock().equals(action.triggerBlock())) {
			last.type = action.actionType().name();
			last.setTarget(action.explicitTargetBlock());
			return;
		}
		route.actions.add(action);
	}

	private static RouteAction actionAt(RouteActionType type, BlockPos block) {
		RouteAction action = new RouteAction();
		action.type = (type == null ? RouteActionType.TP : type).name();
		action.x = block.getX();
		action.y = block.getY();
		action.z = block.getZ();
		return action;
	}

	private static String normalizeMode(String modeKey) {
		return modeKey == null ? "" : modeKey.trim().toLowerCase(Locale.ROOT);
	}

	public record RouteHandle(String modeKey, RouteDefinition route) {
	}

	private static final class RouteFile {
		int version = 1;
		Map<String, List<RouteDefinition>> routes = new LinkedHashMap<>();
	}
}
