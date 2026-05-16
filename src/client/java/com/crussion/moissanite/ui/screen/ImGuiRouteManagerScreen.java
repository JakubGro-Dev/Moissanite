package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.features.cheats.RouteSystem;
import com.crussion.moissanite.input.UiKeybinds;
import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.imgui.ImGuiScreen;
import com.crussion.moissanite.ui.imgui.MoissaniteImGui;
import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.route.RouteAction;
import com.crussion.moissanite.util.route.RouteActionType;
import com.crussion.moissanite.util.route.RouteDefinition;
import com.crussion.moissanite.util.route.RouteIsland;
import com.crussion.moissanite.util.route.RouteRepository;
import com.mojang.blaze3d.platform.InputConstants;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiComboFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class ImGuiRouteManagerScreen extends ImGuiScreen {
	private static final int WINDOW_FLAGS = ImGuiWindowFlags.NoTitleBar
			| ImGuiWindowFlags.NoResize
			| ImGuiWindowFlags.NoMove
			| ImGuiWindowFlags.NoCollapse
			| ImGuiWindowFlags.NoBringToFrontOnFocus
			| ImGuiWindowFlags.NoSavedSettings
			| ImGuiWindowFlags.NoBackground;
	private static final float OUTER_PADDING = 14.0f;
	private static final float TOP_HEIGHT = 40.0f;
	private static final float TOP_GAP = 14.0f;
	private static final float ACTION_BUTTON_SIZE = 36.0f;
	private static final float ACTION_BUTTON_GAP = 8.0f;
	private static final float AUTO_SELECT_WIDTH = 92.0f;
	private static final float LIST_PADDING = 10.0f;
	private static final float ROW_HEIGHT = 58.0f;
	private static final float ROW_GAP = 7.0f;
	private static final float ACTION_EDITOR_HEADER_HEIGHT = 46.0f;
	private static final float ACTION_ROW_HEIGHT = 112.0f;
	private static final float ACTION_ROW_GAP = 8.0f;
	private static final float FOOTER_HEIGHT = 34.0f;
	private static final int STATUS_SUCCESS = Colors.TEXT_ACCENT;
	private static final int STATUS_ERROR = 0xFFFF7878;

	private final ImString routeNameBuffer = new ImString(81);
	private RouteIsland selectedIsland = RouteIsland.HUB;
	private String selectedRouteName = "";
	private String statusMessage = "Routes are grouped by SkyBlock /locraw mode.";
	private int statusColor = Colors.TEXT_MUTED;
	private float listScroll;
	private float actionScroll;
	private boolean restoreListScroll = true;
	private boolean restoreActionScroll;
	private String editingRouteName = "";

	public ImGuiRouteManagerScreen() {
		super(Component.literal("Route Manager"));
	}

	@Override
	protected void init() {
		super.init();
		this.selectedIsland = RouteIsland.fromDisplayName(UiDefinitions.ROUTE_ISLAND.get());
		this.selectedRouteName = RouteRepository.normalizeName(UiDefinitions.ROUTE_SELECT.get());
		if (RouteRepository.getRoute(this.selectedIsland.modeKey(), this.selectedRouteName) == null) {
			this.selectedRouteName = firstRouteName();
		}
		this.restoreListScroll = true;
	}

	@Override
	public void renderImGui(ImGuiIO io) {
		renderMainWindow();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (UiKeybinds.isToggleKey(event)) {
			this.minecraft.setScreen(null);
			return true;
		}
		if (event.key() == InputConstants.KEY_ESCAPE) {
			openOverlay();
			return true;
		}
		return super.keyPressed(event);
	}

	private void renderMainWindow() {
		ImGuiViewport viewport = ImGui.getMainViewport();
		ImGui.setNextWindowPos(viewport.getPosX(), viewport.getPosY(), ImGuiCond.Always);
		ImGui.setNextWindowSize(viewport.getSizeX(), viewport.getSizeY(), ImGuiCond.Always);
		ImGui.setNextWindowViewport(viewport.getID());

		if (ImGui.begin("Moissanite Routes##overlay", WINDOW_FLAGS)) {
			RouteLayout layout = computeLayout(viewport.getSizeX(), viewport.getSizeY());
			ImDrawList draw = ImGui.getWindowDrawList();
			draw.addRectFilled(
					viewport.getPosX(),
					viewport.getPosY(),
					viewport.getPosX() + viewport.getSizeX(),
					viewport.getPosY() + viewport.getSizeY(),
					MoissaniteImGui.color(Colors.BACKDROP));
			renderChrome(draw, layout);
			renderTopControls(draw, layout);
			renderRoutesList(draw, layout);
			renderFooter(draw, layout);
		}
		ImGui.end();
	}

	private void renderChrome(ImDrawList draw, RouteLayout layout) {
		MoissaniteImGui.panel(draw, layout.outer.x(), layout.outer.y(), layout.outer.width(), layout.outer.height(), Theme.PANEL_RADIUS);
		MoissaniteImGui.section(draw, layout.list.x(), layout.list.y(), layout.list.width(), layout.list.height(), Theme.SECTION_RADIUS, Colors.RIGHT_BG, Colors.SECTION_OUTLINE_NESTED);
		MoissaniteImGui.text(draw, "ROUTES", layout.outer.x() + OUTER_PADDING, layout.outer.y() - 31.0f, Colors.TEXT_PRIMARY, MoissaniteImGui.SECTION_TITLE_SIZE);
	}

	private void renderTopControls(ImDrawList draw, RouteLayout layout) {
		float inputWidth = Math.max(180.0f, layout.top.width() * 0.42f);
		float comboWidth = Math.max(190.0f, layout.top.width() * 0.25f);
		float actionsWidth = (ACTION_BUTTON_SIZE * 2.0f) + ACTION_BUTTON_GAP;
		float rightControlsWidth = AUTO_SELECT_WIDTH + ACTION_BUTTON_GAP + comboWidth;
		if (inputWidth + rightControlsWidth + actionsWidth + (TOP_GAP * 2.0f) > layout.top.width()) {
			comboWidth = Math.max(150.0f, layout.top.width() * 0.30f);
			rightControlsWidth = AUTO_SELECT_WIDTH + ACTION_BUTTON_GAP + comboWidth;
			inputWidth = Math.max(120.0f, layout.top.width() - rightControlsWidth - actionsWidth - (TOP_GAP * 2.0f));
		}

		drawInput(layout.top.x(), layout.top.y(), inputWidth);
		float buttonX = layout.top.x() + inputWidth + TOP_GAP;
		if (MoissaniteImGui.textButton(draw, "##route_add", "+", buttonX, layout.top.y(), ACTION_BUTTON_SIZE, TOP_HEIGHT, true)) {
			createRoute();
		}
		buttonX += ACTION_BUTTON_SIZE + ACTION_BUTTON_GAP;
		boolean canRemove = selectedRoute() != null;
		if (MoissaniteImGui.textButton(draw, "##route_remove", "-", buttonX, layout.top.y(), ACTION_BUTTON_SIZE, TOP_HEIGHT, canRemove)) {
			removeSelectedRoute();
		}

		float comboX = layout.top.right() - comboWidth;
		float autoSelectX = comboX - ACTION_BUTTON_GAP - AUTO_SELECT_WIDTH;
		if (MoissaniteImGui.textButton(draw, "##route_auto_select", "AutoSelect", autoSelectX, layout.top.y(), AUTO_SELECT_WIDTH, TOP_HEIGHT, true)) {
			autoSelectIsland();
		}
		renderIslandDropdown(comboX, layout.top.y(), comboWidth);
	}

	private void drawInput(float x, float y, float width) {
		ImGui.setCursorScreenPos(x, y);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 8.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10.0f, 8.0f);
		ImGui.inputTextWithHint("##route_name_input", "Route name", this.routeNameBuffer, ImGuiInputTextFlags.EscapeClearsAll);
		ImGui.popStyleVar(2);
		MoissaniteImGui.popFrameColors();
	}

	private void renderIslandDropdown(float x, float y, float width) {
		List<String> options = RouteIsland.displayNames();
		ImGui.setCursorScreenPos(x, y);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 8.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, 8.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, 1.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10.0f, 8.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8.0f, 8.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 8.0f, 7.0f);
		if (ImGui.beginCombo("##route_island_dropdown", selectedIsland.displayName(), ImGuiComboFlags.HeightRegular)) {
			ImGui.dummy(1.0f, 2.0f);
			for (String option : options) {
				boolean selected = option.equals(selectedIsland.displayName());
				if (ImGui.selectable(option, selected)) {
					selectIsland(RouteIsland.fromDisplayName(option));
				}
				if (selected) {
					ImGui.setItemDefaultFocus();
				}
			}
			ImGui.dummy(1.0f, 2.0f);
			ImGui.endCombo();
		}
		ImGui.popStyleVar(6);
		MoissaniteImGui.popFrameColors();
	}

	private void renderRoutesList(ImDrawList draw, RouteLayout layout) {
		float listX = layout.list.x() + LIST_PADDING;
		float listY = layout.list.y() + LIST_PADDING;
		float listWidth = layout.list.width() - (LIST_PADDING * 2.0f);
		float listHeight = layout.list.height() - (LIST_PADDING * 2.0f);
		ImGui.setCursorScreenPos(listX, listY);
		if (ImGui.beginChild("##routes_list", listWidth, listHeight, false, ImGuiWindowFlags.NoBackground)) {
			RouteDefinition editingRoute = editingRoute();
			if (editingRoute != null) {
				renderActionEditor(ImGui.getWindowDrawList(), editingRoute, listWidth);
				ImGui.endChild();
				return;
			}
			if (this.restoreListScroll) {
				ImGui.setScrollY(this.listScroll);
				this.restoreListScroll = false;
			}

			List<RouteDefinition> routes = RouteRepository.routes(this.selectedIsland.modeKey());
			if (routes.isEmpty()) {
				MoissaniteImGui.text(ImGui.getWindowDrawList(), "No routes for " + this.selectedIsland.displayName(), ImGui.getWindowPosX() + 9.0f, ImGui.getWindowPosY() + 10.0f, Colors.TEXT_MUTED);
				ImGui.dummy(1.0f, 42.0f);
				this.listScroll = ImGui.getScrollY();
				ImGui.endChild();
				return;
			}

			for (int i = 0; i < routes.size(); i++) {
				RouteDefinition route = routes.get(i);
				if (renderRouteRow(ImGui.getWindowDrawList(), route, i, listWidth)) {
					break;
				}
			}
			ImGui.setCursorPos(0.0f, routes.size() * (ROW_HEIGHT + ROW_GAP));
			ImGui.dummy(1.0f, 1.0f);
			this.listScroll = ImGui.getScrollY();
		}
		ImGui.endChild();
	}

	private boolean renderRouteRow(ImDrawList draw, RouteDefinition route, int index, float listWidth) {
		float originX = ImGui.getWindowPosX();
		float originY = ImGui.getWindowPosY();
		float scrollY = ImGui.getScrollY();
		float localY = index * (ROW_HEIGHT + ROW_GAP);
		float screenY = originY + localY - scrollY;
		float viewTop = ImGui.getWindowPosY();
		float viewBottom = viewTop + ImGui.getWindowHeight();
		if (screenY > viewBottom || screenY + ROW_HEIGHT < viewTop) {
			return false;
		}

		boolean selected = route.name != null && route.name.equals(this.selectedRouteName);
		float nameX = originX + 18.0f;
		float nameWidth = Math.max(90.0f, listWidth * 0.34f);
		float activeX = originX + Math.max(nameWidth + 26.0f, listWidth * 0.44f);
		float activeWidth = Math.min(82.0f, Math.max(56.0f, listWidth * 0.12f));
		String rowId = "##route_row_" + index;
		ImGui.setCursorPos(0.0f, localY);
		if (ImGui.invisibleButton(rowId, Math.max(1.0f, activeX - originX - 8.0f), ROW_HEIGHT)) {
			selectRoute(route.name);
		}
		boolean hovered = ImGui.isItemHovered();
		float state = MoissaniteImGui.animation(rowId + "_state", selected || hovered, 18.0f);
		int rowBg = selected ? Colors.LIST_SELECTED : MoissaniteImGui.withAlpha(Colors.LIST_BG, 210);
		int hoverBg = MoissaniteImGui.mixColor(rowBg, Colors.BUTTON_HOVER, state * 0.65f);
		draw.addRectFilled(originX, screenY, originX + listWidth, screenY + ROW_HEIGHT, MoissaniteImGui.color(hoverBg), 8.0f);
		draw.addRect(originX, screenY, originX + listWidth, screenY + ROW_HEIGHT, MoissaniteImGui.color(selected ? Colors.BUTTON_OUTLINE_HOVER : Colors.SECTION_OUTLINE_NESTED), 8.0f, 0, 1.0f);
		if (route.active) {
			draw.addRectFilled(originX + 3.0f, screenY + 8.0f, originX + 6.0f, screenY + ROW_HEIGHT - 8.0f, MoissaniteImGui.color(Colors.ACCENT), 2.0f);
		}

		String displayName = route.name == null || route.name.isBlank() ? "Unnamed" : route.name;
		MoissaniteImGui.clippedText(draw, displayName, nameX, screenY + 12.0f, nameWidth, selected ? Colors.TEXT_SELECTED : Colors.TEXT_PRIMARY, MoissaniteImGui.SECTION_TITLE_SIZE);
		MoissaniteImGui.clippedText(draw, actionSummary(route), nameX, screenY + 35.0f, nameWidth, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);

		if (MoissaniteImGui.switchControl(draw, "##route_active_" + index, route.active, activeX, screenY + 14.0f, activeWidth, 30.0f)) {
			boolean nextActive = !route.active;
			selectRoute(route.name);
			if (!RouteRepository.setRouteActive(this.selectedIsland.modeKey(), route.name, nextActive)) {
				setStatus("Route not found: " + route.name, STATUS_ERROR);
				return false;
			}
			RouteRepository.save();
			setStatus((nextActive ? "Activated " : "Deactivated ") + route.name + ".", STATUS_SUCCESS);
			return false;
		}

		if (listWidth >= 620.0f) {
			float statsX = activeX + activeWidth + 24.0f;
			float statsWidth = Math.max(90.0f, listWidth - statsX + originX - 186.0f);
			MoissaniteImGui.clippedText(draw, typeBreakdown(route), statsX, screenY + 20.0f, statsWidth, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);
		}

		float removeWidth = listWidth < 600.0f ? 34.0f : 76.0f;
		float editWidth = listWidth < 600.0f ? 48.0f : 66.0f;
		float removeX = originX + listWidth - removeWidth - 12.0f;
		float editX = removeX - editWidth - 8.0f;
		String removeLabel = listWidth < 600.0f ? "-" : "Remove";
		if (MoissaniteImGui.textButton(draw, "##route_edit_" + index, "Edit", editX, screenY + 14.0f, editWidth, 30.0f, true)) {
			selectRoute(route.name);
			startEditingRoute(route.name);
			RouteSystem.editRouteFromManager(this.selectedIsland.modeKey(), route.name);
			setStatus("Editing " + route.name + ".", STATUS_SUCCESS);
			return false;
		}
		if (MoissaniteImGui.textButton(draw, "##route_remove_" + index, removeLabel, removeX, screenY + 14.0f, removeWidth, 30.0f, true)) {
			this.selectedRouteName = route.name;
			removeSelectedRoute();
			return true;
		}
		return false;
	}

	private void renderActionEditor(ImDrawList draw, RouteDefinition route, float listWidth) {
		if (this.restoreActionScroll) {
			ImGui.setScrollY(this.actionScroll);
			this.restoreActionScroll = false;
		}

		float originX = ImGui.getWindowPosX();
		float originY = ImGui.getWindowPosY();
		float scrollY = ImGui.getScrollY();
		float headerY = originY - scrollY;
		draw.addRectFilled(originX, headerY, originX + listWidth, headerY + ACTION_EDITOR_HEADER_HEIGHT, MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.LIST_BG, 210)), 8.0f);
		MoissaniteImGui.clippedText(draw, route.name + " actions", originX + 14.0f, headerY + 10.0f, Math.max(80.0f, listWidth - 260.0f), Colors.TEXT_PRIMARY, MoissaniteImGui.SECTION_TITLE_SIZE);
		MoissaniteImGui.clippedText(draw, actionSummary(route), originX + 14.0f, headerY + 29.0f, 160.0f, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);

		float clearWidth = 70.0f;
		float routesWidth = 74.0f;
		if (MoissaniteImGui.textButton(draw, "##action_editor_routes", "Routes", originX + listWidth - routesWidth - 10.0f, headerY + 8.0f, routesWidth, 30.0f, true)) {
			this.editingRouteName = "";
			this.restoreListScroll = true;
			return;
		}
		if (MoissaniteImGui.textButton(draw, "##action_editor_clear", "Clear", originX + listWidth - routesWidth - clearWidth - 18.0f, headerY + 8.0f, clearWidth, 30.0f, hasActions(route))) {
			RouteRepository.clearActions(this.selectedIsland.modeKey(), route.name);
			RouteRepository.save();
			setStatus("Cleared " + route.name + ".", STATUS_SUCCESS);
		}

		List<RouteAction> actions = route.actions;
		if (actions == null || actions.isEmpty()) {
			MoissaniteImGui.text(draw, "No actions. Use /route add tp, /route add eth, or /route walk.", originX + 14.0f, headerY + ACTION_EDITOR_HEADER_HEIGHT + 14.0f, Colors.TEXT_MUTED);
			ImGui.setCursorPos(0.0f, ACTION_EDITOR_HEADER_HEIGHT + 48.0f);
			ImGui.dummy(1.0f, 1.0f);
			this.actionScroll = ImGui.getScrollY();
			return;
		}

		float localY = ACTION_EDITOR_HEADER_HEIGHT + ACTION_ROW_GAP;
		for (int i = 0; i < actions.size(); i++) {
			if (renderActionRow(draw, route, i, localY, listWidth)) {
				break;
			}
			localY += ACTION_ROW_HEIGHT + ACTION_ROW_GAP;
		}
		ImGui.setCursorPos(0.0f, localY);
		ImGui.dummy(1.0f, 1.0f);
		this.actionScroll = ImGui.getScrollY();
	}

	private boolean renderActionRow(ImDrawList draw, RouteDefinition route, int index, float localY, float listWidth) {
		RouteAction action = route.actions.get(index);
		if (action == null) {
			return false;
		}

		float originX = ImGui.getWindowPosX();
		float originY = ImGui.getWindowPosY();
		float scrollY = ImGui.getScrollY();
		float screenY = originY + localY - scrollY;
		float viewTop = ImGui.getWindowPosY();
		float viewBottom = viewTop + ImGui.getWindowHeight();
		if (screenY > viewBottom || screenY + ACTION_ROW_HEIGHT < viewTop) {
			return false;
		}

		draw.addRectFilled(originX, screenY, originX + listWidth, screenY + ACTION_ROW_HEIGHT, MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.LIST_BG, 205)), 8.0f);
		draw.addRect(originX, screenY, originX + listWidth, screenY + ACTION_ROW_HEIGHT, MoissaniteImGui.color(Colors.SECTION_OUTLINE_NESTED), 8.0f, 0, 1.0f);

		float typeX = originX + 14.0f;
		float fromX = originX + 96.0f;
		float removeWidth = 34.0f;
		float removeX = originX + listWidth - removeWidth - 12.0f;
		float usableRight = removeX - 10.0f;
		float fromWidth = Math.min(168.0f, Math.max(122.0f, usableRight - fromX));
		float toX = originX + 14.0f;
		float toWidth = Math.max(118.0f, Math.min(184.0f, usableRight - toX));

		MoissaniteImGui.clippedText(draw, "#" + (index + 1), typeX, screenY + 11.0f, 70.0f, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);
		boolean changed = drawActionTypeCombo(action, index, typeX, screenY + 29.0f, 68.0f);

		changed |= drawBlockEditor(draw, "FROM", "##action_from_" + index, actionBlock(action), fromX, screenY + 11.0f, fromWidth, block -> {
			action.x = block[0];
			action.y = block[1];
			action.z = block[2];
		});

		if (index + 1 < route.actions.size()) {
			changed |= drawBlockEditor(draw, "TO", "##action_to_" + index, targetBlock(route, index), toX, screenY + 61.0f, toWidth, block -> {
				RouteAction next = route.actions.get(index + 1);
				next.x = block[0];
				next.y = block[1];
				next.z = block[2];
			});
		}

		if (MoissaniteImGui.textButton(draw, "##action_remove_" + index, "-", removeX, screenY + 40.0f, removeWidth, 30.0f, true)) {
			RouteRepository.removeAction(this.selectedIsland.modeKey(), route.name, index);
			RouteRepository.save();
			setStatus("Removed action #" + (index + 1) + " from " + route.name + ".", STATUS_SUCCESS);
			return true;
		}

		if (changed) {
			RouteRepository.save();
			setStatus("Updated action #" + (index + 1) + " in " + route.name + ".", STATUS_SUCCESS);
		}
		return false;
	}

	private void renderFooter(ImDrawList draw, RouteLayout layout) {
		float x = layout.footer.x();
		float y = layout.footer.y();
		float width = layout.footer.width();
		float buttonWidth = 86.0f;
		if (MoissaniteImGui.textButton(draw, "##routes_back", "Back", x, y, buttonWidth, FOOTER_HEIGHT, true)) {
			openOverlay();
		}
		if (MoissaniteImGui.textButton(draw, "##routes_reload", "Reload", x + buttonWidth + 8.0f, y, buttonWidth, FOOTER_HEIGHT, true)) {
			RouteRepository.reload();
			this.selectedRouteName = firstRouteName();
			this.restoreListScroll = true;
			setStatus("Routes reloaded.", STATUS_SUCCESS);
		}
		if (MoissaniteImGui.textButton(draw, "##routes_save", "Save", x + (buttonWidth + 8.0f) * 2.0f, y, buttonWidth, FOOTER_HEIGHT, true)) {
			RouteRepository.save();
			setStatus("Routes saved.", STATUS_SUCCESS);
		}
		MoissaniteImGui.clippedText(draw, this.statusMessage, x + (buttonWidth + 8.0f) * 3.0f + 8.0f, y + 8.0f,
				Math.max(40.0f, width - ((buttonWidth + 8.0f) * 3.0f) - 8.0f), this.statusColor, MoissaniteImGui.SMALL_TEXT_SIZE);
	}

	private void createRoute() {
		String name = RouteRepository.normalizeName(this.routeNameBuffer.get());
		if (name.isBlank()) {
			setStatus("Enter a route name first.", STATUS_ERROR);
			return;
		}
		if (!RouteRepository.createRoute(this.selectedIsland.modeKey(), name)) {
			setStatus("Route already exists: " + name, STATUS_ERROR);
			return;
		}
		RouteRepository.save();
		this.routeNameBuffer.set("");
		selectRoute(name);
		setStatus("Created " + name + ".", STATUS_SUCCESS);
	}

	private boolean drawActionTypeCombo(RouteAction action, int index, float x, float y, float width) {
		RouteActionType current = action.actionType();
		boolean changed = false;
		ImGui.setCursorScreenPos(x, y);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 7.0f, 4.0f);
		if (ImGui.beginCombo("##action_type_" + index, current.name(), ImGuiComboFlags.HeightSmall)) {
			for (RouteActionType type : RouteActionType.values()) {
				boolean selected = type == current;
				if (ImGui.selectable(type.name(), selected)) {
					action.type = type.name();
					changed = true;
				}
				if (selected) {
					ImGui.setItemDefaultFocus();
				}
			}
			ImGui.endCombo();
		}
		ImGui.popStyleVar(2);
		MoissaniteImGui.popFrameColors();
		return changed;
	}

	private boolean drawBlockEditor(ImDrawList draw, String label, String id, int[] block, float x, float y, float width, Consumer<int[]> setter) {
		MoissaniteImGui.clippedText(draw, label, x, y, width, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);
		ImGui.setCursorScreenPos(x, y + 17.0f);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6.0f);
		ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 6.0f, 4.0f);
		boolean changed = ImGui.dragInt3(id, block, 0.18f);
		ImGui.popStyleVar(2);
		MoissaniteImGui.popFrameColors();
		if (changed) {
			setter.accept(block);
		}
		return changed;
	}

	private void autoSelectIsland() {
		SkyBlockLocationTracker.requestRefreshIfNeeded();
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		if (!location.isSkyBlock() || location.mode().isBlank()) {
			SkyBlockLocationTracker.requestRefreshNow(true);
			setStatus("Requested /locraw. Try AutoSelect again after the response.", Colors.TEXT_MUTED);
			return;
		}

		RouteIsland island = RouteIsland.fromMode(location.mode());
		if (island == null) {
			setStatus("Current mode is not in the route island list: " + location.mode(), STATUS_ERROR);
			return;
		}
		selectIsland(island);
		setStatus("Selected " + island.displayName() + ".", STATUS_SUCCESS);
	}

	private void removeSelectedRoute() {
		RouteDefinition route = selectedRoute();
		if (route == null) {
			setStatus("Select a route first.", STATUS_ERROR);
			return;
		}
		String removedName = route.name;
		if (!RouteRepository.deleteRoute(this.selectedIsland.modeKey(), removedName)) {
			setStatus("Route not found: " + removedName, STATUS_ERROR);
			return;
		}
		RouteRepository.save();
		if (removedName.equals(this.editingRouteName)) {
			this.editingRouteName = "";
		}
		this.selectedRouteName = firstRouteName();
		UiDefinitions.ROUTE_SELECT.setOptions(RouteRepository.routeNames(this.selectedIsland.modeKey()));
		UiDefinitions.ROUTE_SELECT.set(this.selectedRouteName);
		setStatus("Removed " + removedName + ".", STATUS_SUCCESS);
	}

	private void selectIsland(RouteIsland island) {
		if (island == null) {
			return;
		}
		this.selectedIsland = island;
		this.selectedRouteName = firstRouteName();
		this.editingRouteName = "";
		this.listScroll = 0.0f;
		this.restoreListScroll = true;
		UiDefinitions.ROUTE_ISLAND.set(this.selectedIsland.displayName());
		UiDefinitions.ROUTE_SELECT.setOptions(RouteRepository.routeNames(this.selectedIsland.modeKey()));
		UiDefinitions.ROUTE_SELECT.set(this.selectedRouteName);
	}

	private void selectRoute(String routeName) {
		this.selectedRouteName = RouteRepository.normalizeName(routeName);
		UiDefinitions.ROUTE_ISLAND.set(this.selectedIsland.displayName());
		UiDefinitions.ROUTE_SELECT.setOptions(RouteRepository.routeNames(this.selectedIsland.modeKey()));
		UiDefinitions.ROUTE_SELECT.set(this.selectedRouteName);
	}

	private RouteDefinition selectedRoute() {
		return RouteRepository.getRoute(this.selectedIsland.modeKey(), this.selectedRouteName);
	}

	private RouteDefinition editingRoute() {
		RouteDefinition route = RouteRepository.getRoute(this.selectedIsland.modeKey(), this.editingRouteName);
		if (route == null && !this.editingRouteName.isBlank()) {
			this.editingRouteName = "";
		}
		return route;
	}

	private void startEditingRoute(String routeName) {
		this.editingRouteName = RouteRepository.normalizeName(routeName);
		this.actionScroll = 0.0f;
		this.restoreActionScroll = true;
	}

	private String firstRouteName() {
		List<String> names = RouteRepository.routeNames(this.selectedIsland.modeKey());
		return names.isEmpty() ? "" : names.getFirst();
	}

	private static boolean hasActions(RouteDefinition route) {
		return route != null && route.actions != null && !route.actions.isEmpty();
	}

	private static int[] actionBlock(RouteAction action) {
		return new int[] { action.x, action.y, action.z };
	}

	private static int[] targetBlock(RouteDefinition route, int index) {
		RouteAction action = route == null || route.actions == null || index < 0 || index >= route.actions.size() ? null : route.actions.get(index);
		if (action == null) {
			return new int[] { 0, 0, 0 };
		}
		BlockPos target = action.explicitTargetBlock();
		if (target == null && index + 1 < route.actions.size()) {
			return actionBlock(route.actions.get(index + 1));
		}
		if (target == null) {
			target = action.triggerBlock();
		}
		return new int[] { target.getX(), target.getY(), target.getZ() };
	}

	private static String actionSummary(RouteDefinition route) {
		int count = route == null || route.actions == null ? 0 : route.actions.size();
		return count == 1 ? "1 action" : count + " actions";
	}

	private static String typeBreakdown(RouteDefinition route) {
		int tp = 0;
		int eth = 0;
		int walk = 0;
		if (route != null && route.actions != null) {
			for (RouteAction action : route.actions) {
				if (action == null) {
					continue;
				}
				RouteActionType type = action.actionType();
				if (type == RouteActionType.TP) {
					tp++;
				} else if (type == RouteActionType.ETH) {
					eth++;
				} else if (type == RouteActionType.WALK) {
					walk++;
				}
			}
		}
		return String.format(Locale.ROOT, "TP %d   ETH %d   WALK %d", tp, eth, walk);
	}

	private void setStatus(String message, int color) {
		this.statusMessage = message == null ? "" : message;
		this.statusColor = color;
	}

	private void openOverlay() {
		UiEntrypoints.open(ScreenIds.INVENTORY_OVERLAY);
	}

	private static RouteLayout computeLayout(float screenWidth, float screenHeight) {
		float maxWidth = screenWidth - 24.0f;
		float maxHeight = screenHeight - 24.0f;
		float outerWidth = (float) MoissaniteImGui.clamp(Math.min(screenWidth * 0.84f, 1540.0f), 520.0D, maxWidth);
		float outerHeight = (float) MoissaniteImGui.clamp(Math.min(screenHeight * 0.80f, 860.0f), 360.0D, maxHeight);
		float outerX = (screenWidth - outerWidth) / 2.0f;
		float outerY = (screenHeight - outerHeight) / 2.0f;
		Rect outer = new Rect(outerX, outerY, outerWidth, outerHeight);
		Rect inner = outer.inset(OUTER_PADDING);
		Rect top = new Rect(inner.x(), inner.y(), inner.width(), TOP_HEIGHT);
		float footerY = inner.bottom() - FOOTER_HEIGHT;
		float listY = top.bottom() + TOP_GAP;
		float listHeight = Math.max(40.0f, footerY - TOP_GAP - listY);
		Rect list = new Rect(inner.x(), listY, inner.width(), listHeight);
		Rect footer = new Rect(inner.x(), footerY, inner.width(), FOOTER_HEIGHT);
		return new RouteLayout(outer, top, list, footer);
	}

	private record Rect(float x, float y, float width, float height) {
		private float right() {
			return x + width;
		}

		private float bottom() {
			return y + height;
		}

		private Rect inset(float padding) {
			return new Rect(x + padding, y + padding, width - padding * 2.0f, height - padding * 2.0f);
		}
	}

	private record RouteLayout(Rect outer, Rect top, Rect list, Rect footer) {
	}
}
