package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.input.UiKeybinds;
import com.crussion.moissanite.ui.data.UiButton;
import com.crussion.moissanite.ui.data.UiCatalog;
import com.crussion.moissanite.ui.data.UiCategory;
import com.crussion.moissanite.ui.data.UiColor;
import com.crussion.moissanite.ui.data.UiDropdown;
import com.crussion.moissanite.ui.data.UiEntry;
import com.crussion.moissanite.ui.data.UiInput;
import com.crussion.moissanite.ui.data.UiKeybind;
import com.crussion.moissanite.ui.data.UiNumber;
import com.crussion.moissanite.ui.data.UiSection;
import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.data.UiSwitch;
import com.crussion.moissanite.ui.imgui.ImGuiScreen;
import com.crussion.moissanite.ui.imgui.MoissaniteImGui;
import com.crussion.moissanite.ui.state.UiState;
import com.crussion.moissanite.ui.state.UiStore;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;
import com.mojang.blaze3d.platform.InputConstants;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class ImGuiInventoryOverlayScreen extends ImGuiScreen {
	private static final int SEARCH_BUFFER_SIZE = 121;
	private static final int WINDOW_FLAGS = ImGuiWindowFlags.NoTitleBar
			| ImGuiWindowFlags.NoResize
			| ImGuiWindowFlags.NoMove
			| ImGuiWindowFlags.NoCollapse
			| ImGuiWindowFlags.NoBringToFrontOnFocus
			| ImGuiWindowFlags.NoSavedSettings
			| ImGuiWindowFlags.NoBackground;
	private static final float PANEL_PADDING = 12.0f;
	private static final float LEFT_PADDING = 12.0f;
	private static final float TITLE_BLOCK_HEIGHT = 72.0f;
	private static final float CATEGORY_ROW_HEIGHT = 31.0f;
	private static final float NAV_BUTTON_SIZE = 28.0f;
	private static final float SECTION_CONTENT_PADDING_X = 12.0f;
	private static final float SECTION_HEADER_HEIGHT = 38.0f;
	private static final float SECTION_BODY_TOP_PADDING = 8.0f;
	private static final float SECTION_BODY_BOTTOM_PADDING = 10.0f;
	private static final float SECTION_SPACING = 14.0f;
	private static final float ROW_HEIGHT = 30.0f;
	private static final float ROW_SPACING = 4.0f;
	private static final float LABEL_CONTROL_GAP = 12.0f;
	private static final float CONTROL_MIN_WIDTH = 136.0f;
	private static final float CONTROL_MAX_WIDTH = 330.0f;

	private final boolean startInSettings;
	private final ImString searchBuffer = new ImString(SEARCH_BUFFER_SIZE);
	private final Map<UiInput, ImString> inputBuffers = new IdentityHashMap<>();
	private List<UiCategory> categories = List.of();
	private int selectedCategoryIndex;
	private boolean showingSettings;
	private String searchQuery = "";
	private UiKeybind listeningKeybind;
	private float pendingLeftScroll;
	private float pendingRightScroll;
	private boolean restoreLeftScroll;
	private boolean restoreRightScroll;
	private boolean navigationCollapsed;

	public ImGuiInventoryOverlayScreen() {
		this(false);
	}

	public ImGuiInventoryOverlayScreen(boolean startInSettings) {
		super(Component.literal("Moissanite"));
		this.startInSettings = startInSettings;
	}

	@Override
	protected void init() {
		super.init();
		UiDefinitions.init();
		this.categories = new ArrayList<>(UiCatalog.categories());
		this.categories.remove(UiDefinitions.UISETTINGS);

		UiState cachedState = UiStore.get().getState();
		this.selectedCategoryIndex = MoissaniteImGui.clamp(cachedState.selectedCategoryIndex(), 0, Math.max(0, this.categories.size() - 1));
		this.showingSettings = this.startInSettings || cachedState.showingSettings();
		this.searchQuery = this.showingSettings ? "" : sanitizeSearchQuery(cachedState.searchQuery());
		this.searchBuffer.set(this.searchQuery);
		this.pendingRightScroll = cachedState.rightPanelScroll();
		this.pendingLeftScroll = cachedState.leftListScroll();
		this.restoreRightScroll = true;
		this.restoreLeftScroll = true;
	}

	@Override
	public void renderImGui(ImGuiIO io) {
		syncSearchState();
		renderMainWindow();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.listeningKeybind != null) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				this.listeningKeybind = null;
				return true;
			}
			this.listeningKeybind.set(event.key());
			this.listeningKeybind = null;
			saveUiCache();
			return true;
		}
		if (event.key() == InputConstants.KEY_ESCAPE || UiKeybinds.isToggleKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		saveUiCache();
		super.onClose();
	}

	@Override
	public void removed() {
		saveUiCache();
		super.removed();
	}

	private void renderMainWindow() {
		ImGuiViewport viewport = ImGui.getMainViewport();
		ImGui.setNextWindowPos(viewport.getPosX(), viewport.getPosY(), ImGuiCond.Always);
		ImGui.setNextWindowSize(viewport.getSizeX(), viewport.getSizeY(), ImGuiCond.Always);
		ImGui.setNextWindowViewport(viewport.getID());

		if (ImGui.begin("Moissanite##overlay", WINDOW_FLAGS)) {
			float navigationOpenProgress = MoissaniteImGui.animation("navigation_open", !this.navigationCollapsed, 15.0f);
			MoissaniteImGui.Layout layout = MoissaniteImGui.computeLayout(viewport.getSizeX(), viewport.getSizeY(), navigationOpenProgress);
			ImDrawList draw = ImGui.getWindowDrawList();
			draw.addRectFilled(viewport.getPosX(), viewport.getPosY(), viewport.getPosX() + viewport.getSizeX(), viewport.getPosY() + viewport.getSizeY(),
					MoissaniteImGui.color(Colors.BACKDROP));
			renderChrome(draw, layout, navigationOpenProgress);
			renderCategoryPanel(draw, layout.left(), navigationOpenProgress);
			renderContentPanel(draw, layout.right());
			renderSearch(draw, layout.search());
			saveUiCache();
		}
		ImGui.end();
	}

	private void renderChrome(ImDrawList draw, MoissaniteImGui.Layout layout, float navigationOpenProgress) {
		MoissaniteImGui.Rect outer = layout.outer();
		MoissaniteImGui.panel(draw, outer.x(), outer.y(), outer.width(), outer.height(), Theme.PANEL_RADIUS);
		MoissaniteImGui.section(draw, layout.left().x(), layout.left().y(), layout.left().width(), layout.left().height(), Theme.SECTION_RADIUS, Colors.LEFT_BG, Colors.SECTION_OUTLINE);
		MoissaniteImGui.section(draw, layout.right().x(), layout.right().y(), layout.right().width(), layout.right().height(), Theme.SECTION_RADIUS, Colors.RIGHT_BG, Colors.SECTION_OUTLINE_NESTED);
		if (navigationOpenProgress > 0.12f) {
			float dividerX = layout.left().right() + 7.0f;
			int divider = MoissaniteImGui.withAlpha(Colors.DIVIDER, Math.round(156.0f * navigationOpenProgress));
			int glow = MoissaniteImGui.withAlpha(Colors.DIVIDER_GLOW, Math.round(38.0f * navigationOpenProgress));
			draw.addRectFilled(dividerX, layout.left().y() + 8.0f, dividerX + 1.0f, layout.left().bottom() - 8.0f, MoissaniteImGui.color(divider), 1.0f);
			draw.addRectFilled(dividerX - 1.0f, layout.left().y() + 12.0f, dividerX + 2.0f, layout.left().bottom() - 12.0f, MoissaniteImGui.color(glow), 1.0f);
		}
	}

	private void renderCategoryPanel(ImDrawList draw, MoissaniteImGui.Rect left, float navigationOpenProgress) {
		float collapsedButtonX = left.x() + Math.max(7.0f, (left.width() - NAV_BUTTON_SIZE) / 2.0f);
		float expandedButtonX = left.x() + LEFT_PADDING;
		float buttonX = collapsedButtonX + ((expandedButtonX - collapsedButtonX) * navigationOpenProgress);
		float buttonY = left.y() + LEFT_PADDING + 3.0f;
		if (MoissaniteImGui.menuButton(draw, "##navigation_toggle", buttonX, buttonY, NAV_BUTTON_SIZE, this.navigationCollapsed)) {
			this.navigationCollapsed = !this.navigationCollapsed;
		}

		if (navigationOpenProgress < 0.35f) {
			renderCollapsedCategoryRail(draw, left, 1.0f - (navigationOpenProgress / 0.35f));
			return;
		}

		int textAlpha = Math.round(255.0f * navigationOpenProgress);
		float titleX = buttonX + NAV_BUTTON_SIZE + 11.0f;
		float titleY = buttonY - 3.0f;
		float titleWidth = left.right() - titleX - LEFT_PADDING;
		if (titleWidth > 24.0f) {
			MoissaniteImGui.clippedText(draw, "MOISSANITE", titleX, titleY, titleWidth, MoissaniteImGui.withAlpha(Colors.TEXT_PRIMARY, textAlpha), MoissaniteImGui.TITLE_SIZE);
			MoissaniteImGui.clippedText(draw, "Client Modules", titleX, titleY + 24.0f, titleWidth, MoissaniteImGui.withAlpha(Colors.TEXT_MUTED, textAlpha), MoissaniteImGui.SMALL_TEXT_SIZE);
		}
		draw.addLine(
				left.x() + LEFT_PADDING,
				left.y() + TITLE_BLOCK_HEIGHT - 11.0f,
				left.right() - LEFT_PADDING,
				left.y() + TITLE_BLOCK_HEIGHT - 11.0f,
				MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.DIVIDER, Math.round(120.0f * navigationOpenProgress))),
				1.0f);

		float listX = left.x() + LEFT_PADDING;
		float listY = left.y() + TITLE_BLOCK_HEIGHT;
		float listWidth = left.width() - (LEFT_PADDING * 2.0f);
		float listHeight = Math.max(0.0f, left.bottom() - listY - LEFT_PADDING);
		if (listWidth < 80.0f) {
			return;
		}
		ImGui.setCursorScreenPos(listX, listY);
		if (ImGui.beginChild("##moissanite_categories", listWidth, listHeight, false, ImGuiWindowFlags.NoBackground)) {
			if (this.restoreLeftScroll) {
				ImGui.setScrollY(this.pendingLeftScroll);
				this.restoreLeftScroll = false;
			}
			ImDrawList listDraw = ImGui.getWindowDrawList();
			float originX = ImGui.getWindowPosX();
			float originY = ImGui.getWindowPosY();
			float scrollY = ImGui.getScrollY();
			for (int i = 0; i < this.categories.size(); i++) {
				UiCategory category = this.categories.get(i);
				float localY = i * CATEGORY_ROW_HEIGHT;
				ImGui.setCursorPos(0.0f, localY);
				boolean clicked = ImGui.invisibleButton("##category_" + i, listWidth, CATEGORY_ROW_HEIGHT);
				boolean hovered = ImGui.isItemHovered();
				boolean selected = !this.showingSettings && this.searchQuery.isBlank() && this.selectedCategoryIndex == i;
				if (clicked) {
					selectCategory(i);
				}
				float screenY = originY + localY - scrollY;
				float state = MoissaniteImGui.animation("category_state_" + i, selected || hovered, 18.0f);
				if (state > 0.001f) {
					int bg = MoissaniteImGui.mixColor(Colors.LIST_HOVER, selected ? Colors.LIST_SELECTED : Colors.LIST_HOVER, selected ? 1.0f : 0.0f);
					bg = MoissaniteImGui.withAlpha(bg, Math.round(((selected ? 108.0f : 44.0f) * state)));
					listDraw.addRectFilled(originX + 2.0f, screenY + 2.0f, originX + listWidth - 2.0f, screenY + CATEGORY_ROW_HEIGHT - 2.0f, MoissaniteImGui.color(bg), 7.0f);
					if (selected) {
						listDraw.addRectFilled(originX + 5.0f, screenY + 7.0f, originX + 8.0f, screenY + CATEGORY_ROW_HEIGHT - 7.0f,
								MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.ACCENT, Math.round(255.0f * state))), 2.0f);
					}
				}
				float dotState = selected ? 1.0f : state * 0.6f;
				if (dotState > 0.001f) {
					listDraw.addCircleFilled(
							originX + 18.0f,
							screenY + (CATEGORY_ROW_HEIGHT / 2.0f),
							2.5f + (1.0f * dotState),
							MoissaniteImGui.color(MoissaniteImGui.withAlpha(selected ? Colors.ACCENT : Colors.TEXT_MUTED, Math.round(210.0f * dotState))),
							16);
				}
				MoissaniteImGui.clippedText(listDraw, category.name(), originX + 30.0f, screenY + 7.0f, listWidth - 38.0f,
						selected ? Colors.TEXT_SELECTED : Colors.TEXT_PRIMARY);
			}
			ImGui.setCursorPos(0.0f, this.categories.size() * CATEGORY_ROW_HEIGHT);
			ImGui.dummy(1.0f, 1.0f);
			this.pendingLeftScroll = ImGui.getScrollY();
		}
		ImGui.endChild();
	}

	private void renderCollapsedCategoryRail(ImDrawList draw, MoissaniteImGui.Rect left, float alphaProgress) {
		if (alphaProgress <= 0.001f) {
			return;
		}
		float itemSize = 30.0f;
		float itemGap = 7.0f;
		float x = left.x() + Math.max(7.0f, (left.width() - itemSize) / 2.0f);
		float y = left.y() + LEFT_PADDING + NAV_BUTTON_SIZE + 20.0f;
		int alpha = Math.round(255.0f * alphaProgress);
		for (int i = 0; i < this.categories.size(); i++) {
			if (y + itemSize > left.bottom() - LEFT_PADDING) {
				break;
			}
			UiCategory category = this.categories.get(i);
			boolean clicked = MoissaniteImGui.invisibleButton("##rail_category_" + i, x, y, itemSize, itemSize);
			boolean hovered = ImGui.isItemHovered();
			boolean selected = !this.showingSettings && this.searchQuery.isBlank() && this.selectedCategoryIndex == i;
			if (clicked) {
				selectCategory(i);
			}
			float state = MoissaniteImGui.animation("rail_category_state_" + i, selected || hovered, 18.0f);
			int bg = MoissaniteImGui.mixColor(Colors.BUTTON_BG, selected ? Colors.LIST_SELECTED : Colors.BUTTON_HOVER, state);
			int outline = MoissaniteImGui.mixColor(Colors.BUTTON_OUTLINE, Colors.BUTTON_OUTLINE_HOVER, state);
			draw.addRectFilled(x, y, x + itemSize, y + itemSize, MoissaniteImGui.color(MoissaniteImGui.withAlpha(bg, Math.round(alpha * 0.88f))), 9.0f);
			draw.addRect(x, y, x + itemSize, y + itemSize, MoissaniteImGui.color(MoissaniteImGui.withAlpha(outline, alpha)), 9.0f, 0, 1.0f);
			String label = category.name() == null || category.name().isBlank() ? "?" : category.name().substring(0, 1).toUpperCase(Locale.ROOT);
			float labelWidth = MoissaniteImGui.textWidth(label, MoissaniteImGui.FONT_SIZE);
			MoissaniteImGui.text(draw, label, x + Math.max(1.0f, (itemSize - labelWidth) / 2.0f), y + 6.0f,
					MoissaniteImGui.withAlpha(selected ? Colors.TEXT_SELECTED : Colors.TEXT_PRIMARY, alpha));
			y += itemSize + itemGap;
		}
	}

	private void renderSearch(ImDrawList draw, MoissaniteImGui.Rect search) {
		float settingsSize = search.height();
		if (MoissaniteImGui.iconButton(draw, "##settings_view", search.x(), search.y(), settingsSize, this.showingSettings)) {
			openSettingsView();
		}
		float inputX = search.x() + settingsSize + 8.0f;
		float inputWidth = Math.max(0.0f, search.width() - settingsSize - 8.0f);
		ImGui.setCursorScreenPos(inputX, search.y());
		ImGui.setNextItemWidth(inputWidth);
		MoissaniteImGui.pushFrameColors();
		if (ImGui.inputTextWithHint("##moissanite_search", "Search", this.searchBuffer, ImGuiInputTextFlags.EscapeClearsAll)) {
			this.showingSettings = false;
			syncSearchState();
		}
		MoissaniteImGui.popFrameColors();
	}

	private void renderContentPanel(ImDrawList draw, MoissaniteImGui.Rect right) {
		MoissaniteImGui.Rect content = right.inset(PANEL_PADDING);
		PanelViews panelViews = collectPanelViews();
		renderContentHeader(draw, content, panelViews);
		float contentHeaderHeight = 46.0f;
		float childY = content.y() + contentHeaderHeight;
		float childHeight = Math.max(1.0f, content.height() - contentHeaderHeight);
		ImGui.setCursorScreenPos(content.x(), childY);
		if (ImGui.beginChild("##moissanite_content", content.width(), childHeight, false, ImGuiWindowFlags.NoBackground)) {
			if (this.restoreRightScroll) {
				ImGui.setScrollY(this.pendingRightScroll);
				this.restoreRightScroll = false;
			}
			float cursorY = 0.0f;
			if (panelViews.views().isEmpty()) {
				MoissaniteImGui.text(ImGui.getWindowDrawList(), panelViews.placeholder(), ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY(), Colors.TEXT_MUTED);
				cursorY = 24.0f;
			} else {
				for (int i = 0; i < panelViews.views().size(); i++) {
					SectionView section = panelViews.views().get(i);
					cursorY = renderSection(section, cursorY, content.width());
					if (i < panelViews.views().size() - 1) {
						cursorY += SECTION_SPACING;
					}
				}
			}
			ImGui.setCursorPos(0.0f, cursorY + 8.0f);
			ImGui.dummy(1.0f, 1.0f);
			this.pendingRightScroll = ImGui.getScrollY();
		}
		ImGui.endChild();
	}

	private void renderContentHeader(ImDrawList draw, MoissaniteImGui.Rect content, PanelViews panelViews) {
		float headerHeight = 36.0f;
		draw.addRectFilled(
				content.x(),
				content.y(),
				content.right(),
				content.y() + headerHeight,
				MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.SECTION_HEADER_BG, 180)),
				9.0f);
		draw.addRect(
				content.x(),
				content.y(),
				content.right(),
				content.y() + headerHeight,
				MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.SECTION_OUTLINE_NESTED, 130)),
				9.0f,
				0,
				1.0f);
		draw.addRectFilled(
				content.x() + 12.0f,
				content.y() + 2.0f,
				content.x() + Math.min(190.0f, content.width() * 0.24f),
				content.y() + 4.0f,
				MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.ACCENT_SOFT, 120)),
				2.0f);
		String title = currentContentTitle();
		MoissaniteImGui.clippedText(draw, title, content.x() + 12.0f, content.y() + 9.0f, Math.max(80.0f, content.width() - 160.0f), Colors.TEXT_SECTION_TITLE, MoissaniteImGui.SECTION_TITLE_SIZE);
		String count = countEntries(panelViews) + " settings";
		float countWidth = Math.max(76.0f, MoissaniteImGui.textWidth(count, MoissaniteImGui.SMALL_TEXT_SIZE) + 20.0f);
		float countX = content.right() - countWidth - 10.0f;
		draw.addRectFilled(countX, content.y() + 7.0f, countX + countWidth, content.y() + headerHeight - 7.0f,
				MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.INPUT_BG, 210)), 8.0f);
		MoissaniteImGui.clippedText(draw, count, countX + 10.0f, content.y() + 11.0f, countWidth - 20.0f, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);
	}

	private float renderSection(SectionView section, float localY, float availableWidth) {
		ImDrawList draw = ImGui.getWindowDrawList();
		float originX = ImGui.getWindowPosX();
		float originY = ImGui.getWindowPosY();
		float scrollY = ImGui.getScrollY();
		float viewTop = ImGui.getWindowPosY();
		float viewBottom = viewTop + ImGui.getWindowHeight();
		float sectionWidth = Math.max(1.0f, availableWidth);
		float contentWidth = Math.max(0.0f, sectionWidth - (SECTION_CONTENT_PADDING_X * 2.0f));
		float controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, contentWidth * 0.30f));
		controlWidth = Math.min(controlWidth, Math.max(CONTROL_MIN_WIDTH, contentWidth * 0.42f));
		float controlX = Math.max(SECTION_CONTENT_PADDING_X, sectionWidth - SECTION_CONTENT_PADDING_X - controlWidth);
		float labelWidth = Math.max(40.0f, controlX - SECTION_CONTENT_PADDING_X - LABEL_CONTROL_GAP);
		float rowY = localY + SECTION_HEADER_HEIGHT + SECTION_BODY_TOP_PADDING;
		float bodyRowsHeight = section.entries().isEmpty()
				? 0.0f
				: (section.entries().size() * ROW_HEIGHT) + ((section.entries().size() - 1) * ROW_SPACING);
		float sectionHeight = SECTION_HEADER_HEIGHT + SECTION_BODY_TOP_PADDING + bodyRowsHeight + SECTION_BODY_BOTTOM_PADDING;
		float screenSectionY = originY + localY - scrollY;
		if (screenSectionY > viewBottom || screenSectionY + sectionHeight < viewTop) {
			return localY + sectionHeight;
		}
		int sectionBg = section.level() > 0 ? Colors.SECTION_BG_NESTED : Colors.SECTION_BG;
		int outline = section.level() > 0 ? Colors.SECTION_OUTLINE_NESTED : Colors.SECTION_OUTLINE;
		int headerBg = section.level() > 0 ? Colors.SECTION_HEADER_BG_NESTED : Colors.SECTION_HEADER_BG;
		int headerText = section.level() > 0 ? Colors.TEXT_SECTION_TITLE_NESTED : Colors.TEXT_SECTION_TITLE;
		draw.addRectFilled(originX, screenSectionY, originX + sectionWidth, screenSectionY + sectionHeight, MoissaniteImGui.color(sectionBg), 9.0f);
		draw.addRect(originX, screenSectionY, originX + sectionWidth, screenSectionY + sectionHeight, MoissaniteImGui.color(outline), 9.0f, 0, 1.0f);
		draw.addRectFilled(originX + 1.0f, screenSectionY + 1.0f, originX + sectionWidth - 1.0f, screenSectionY + SECTION_HEADER_HEIGHT,
				MoissaniteImGui.color(headerBg), 8.0f);
		draw.addRectFilled(originX + SECTION_CONTENT_PADDING_X, screenSectionY + 2.0f, originX + Math.min(sectionWidth - SECTION_CONTENT_PADDING_X, 180.0f), screenSectionY + 4.0f,
				MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.ACCENT_SOFT, section.level() > 0 ? 70 : 110)), 2.0f);
		draw.addLine(originX + 1.0f, screenSectionY + SECTION_HEADER_HEIGHT, originX + sectionWidth - 1.0f, screenSectionY + SECTION_HEADER_HEIGHT,
				MoissaniteImGui.color(section.level() > 0 ? Colors.SECTION_HEADER_DIVIDER_NESTED : Colors.SECTION_HEADER_DIVIDER), 1.0f);
		MoissaniteImGui.clippedText(draw, section.title(), originX + SECTION_CONTENT_PADDING_X, screenSectionY + 8.0f,
				sectionWidth - (SECTION_CONTENT_PADDING_X * 2.0f), headerText, MoissaniteImGui.SECTION_TITLE_SIZE);

		float mouseX = ImGui.getIO().getMousePosX();
		float mouseY = ImGui.getIO().getMousePosY();
		for (int i = 0; i < section.entries().size(); i++) {
			UiEntry<?> entry = section.entries().get(i);
			float entryLocalY = rowY + (i * (ROW_HEIGHT + ROW_SPACING));
			float entryScreenY = originY + entryLocalY - scrollY;
			if (entryScreenY > viewBottom || entryScreenY + ROW_HEIGHT < viewTop) {
				continue;
			}
			float rowHover = MoissaniteImGui.animation("entry_hover_" + System.identityHashCode(entry),
					MoissaniteImGui.isInside(mouseX, mouseY, originX + SECTION_CONTENT_PADDING_X, entryScreenY, sectionWidth - (SECTION_CONTENT_PADDING_X * 2.0f), ROW_HEIGHT),
					16.0f);
			if (rowHover > 0.001f) {
				draw.addRectFilled(
						originX + SECTION_CONTENT_PADDING_X - 4.0f,
						entryScreenY + 1.0f,
						originX + sectionWidth - SECTION_CONTENT_PADDING_X + 4.0f,
						entryScreenY + ROW_HEIGHT - 1.0f,
						MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.LIST_HOVER, Math.round(54.0f * rowHover))),
						7.0f);
			}
			float labelX = originX + SECTION_CONTENT_PADDING_X;
			MoissaniteImGui.clippedText(draw, entry.name(), labelX, entryScreenY + 7.0f, labelWidth - 4.0f,
					MoissaniteImGui.mixColor(Colors.TEXT_MUTED, Colors.TEXT_PRIMARY, rowHover * 0.55f));
			renderEntry(entry, originX + controlX, entryScreenY, controlWidth, ROW_HEIGHT);
			if (i < section.entries().size() - 1) {
				float dividerY = entryScreenY + ROW_HEIGHT + ROW_SPACING / 2.0f;
				draw.addLine(originX + SECTION_CONTENT_PADDING_X, dividerY, originX + sectionWidth - SECTION_CONTENT_PADDING_X, dividerY,
						MoissaniteImGui.color(Colors.DIVIDER), 1.0f);
			}
		}
		return localY + sectionHeight;
	}

	private void renderEntry(UiEntry<?> entry, float x, float y, float width, float height) {
		ImGui.pushID(System.identityHashCode(entry));
		if (entry instanceof UiSwitch value) {
			if (MoissaniteImGui.switchControl(ImGui.getWindowDrawList(), "##switch", Boolean.TRUE.equals(value.get()), x, y, width, height)) {
				value.toggle();
			}
		} else if (entry instanceof UiSlider value) {
			double current = value.get() == null ? value.min() : value.get();
			double next = MoissaniteImGui.sliderControl(ImGui.getWindowDrawList(), "##slider", current, value.min(), value.max(), value.step(), x, y, width, height, formatSliderValue(current, value.step()));
			if (!Double.isNaN(next)) {
				value.set(next);
			}
		} else if (entry instanceof UiNumber value) {
			int current = value.get() == null ? value.min() : value.get();
			double next = MoissaniteImGui.sliderControl(ImGui.getWindowDrawList(), "##number", current, value.min(), value.max(), 1.0D, x, y, width, height, Integer.toString(current));
			if (!Double.isNaN(next)) {
				value.set((int) Math.round(next));
			}
		} else if (entry instanceof UiDropdown value) {
			renderDropdown(value, x, y, width);
		} else if (entry instanceof UiColor value) {
			renderColor(value, x, y, width);
		} else if (entry instanceof UiInput value) {
			renderInput(value, x, y, width);
		} else if (entry instanceof UiKeybind value) {
			renderKeybind(value, x, y, width, height);
		} else if (entry instanceof UiButton value) {
			String label = value.buttonText().isBlank() ? "Run" : value.buttonText();
			float buttonWidth = Math.min(width, 170.0f);
			if (MoissaniteImGui.textButton(ImGui.getWindowDrawList(), "##button", label, x + width - buttonWidth, y, buttonWidth, height, true)) {
				value.press();
			}
		} else {
			MoissaniteImGui.clippedText(ImGui.getWindowDrawList(), Objects.toString(entry.get(), ""), x, y + 5.0f, width, Colors.TEXT_MUTED);
		}
		ImGui.popID();
	}

	private static void renderDropdown(UiDropdown value, float x, float y, float width) {
		List<String> options = value.options();
		if (options.isEmpty()) {
			MoissaniteImGui.text(ImGui.getWindowDrawList(), "No options", x, y + 5.0f, Colors.TEXT_MUTED);
			return;
		}
		String[] labels = options.toArray(String[]::new);
		int currentIndex = Math.max(0, options.indexOf(value.get()));
		ImInt selected = new ImInt(currentIndex);
		ImGui.setCursorScreenPos(x, y);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		if (ImGui.combo("##dropdown", selected, labels)) {
			value.set(options.get(MoissaniteImGui.clamp(selected.get(), 0, options.size() - 1)));
		}
		MoissaniteImGui.popFrameColors();
	}

	private static void renderColor(UiColor value, float x, float y, float width) {
		float[] color = {
				value.red() / 255.0f,
				value.green() / 255.0f,
				value.blue() / 255.0f,
				value.alpha() / 255.0f
		};
		ImGui.setCursorScreenPos(x, y);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		if (ImGui.colorEdit4("##color", color, ImGuiColorEditFlags.AlphaBar | ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel)) {
			value.set(UiColor.argb(toChannel(color[0]), toChannel(color[1]), toChannel(color[2]), toChannel(color[3])));
		}
		MoissaniteImGui.popFrameColors();
	}

	private void renderInput(UiInput value, float x, float y, float width) {
		ImString buffer = inputBuffer(value);
		ImGui.setCursorScreenPos(x, y);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		if (ImGui.inputText("##input", buffer, ImGuiInputTextFlags.EscapeClearsAll)) {
			value.set(buffer.get());
		}
		MoissaniteImGui.popFrameColors();
	}

	private void renderKeybind(UiKeybind value, float x, float y, float width, float height) {
		String label = this.listeningKeybind == value ? "Press a key..." : keyName(value.get());
		float clearWidth = 58.0f;
		float keyWidth = Math.min(150.0f, Math.max(60.0f, width - clearWidth - 8.0f));
		float startX = x + width - keyWidth - 8.0f - clearWidth;
		if (MoissaniteImGui.textButton(ImGui.getWindowDrawList(), "##keybind", label, startX, y, keyWidth, height, true)) {
			this.listeningKeybind = value;
		}
		if (MoissaniteImGui.textButton(ImGui.getWindowDrawList(), "##keybind_clear", "Clear", startX + keyWidth + 8.0f, y, clearWidth, height, true)) {
			value.set(InputConstants.UNKNOWN.getValue());
			if (this.listeningKeybind == value) {
				this.listeningKeybind = null;
			}
		}
	}

	private ImString inputBuffer(UiInput value) {
		int bufferSize = Math.max(2, value.maxLength() + 1);
		ImString buffer = this.inputBuffers.get(value);
		if (buffer == null || buffer.getBufferSize() < bufferSize) {
			buffer = new ImString(Objects.toString(value.get(), ""), bufferSize);
			this.inputBuffers.put(value, buffer);
			return buffer;
		}
		String current = Objects.toString(value.get(), "");
		if (!Objects.equals(buffer.get(), current)) {
			buffer.set(current);
		}
		return buffer;
	}

	private PanelViews collectPanelViews() {
		List<SectionView> views = new ArrayList<>();
		if (this.showingSettings) {
			if (UiDefinitions.UISETTINGS != null) {
				for (UiSection section : UiDefinitions.UISETTINGS.sections()) {
					addVisibleSection(views, section, section.name(), 0);
				}
			}
			return new PanelViews(views, "Settings go here");
		}
		if (!this.searchQuery.isBlank()) {
			String query = this.searchQuery.toLowerCase(Locale.ROOT);
			for (UiCategory category : this.categories) {
				for (UiSection section : category.sections()) {
					List<UiEntry<?>> matches = matchingEntries(category, section, query);
					if (!matches.isEmpty()) {
						views.add(new SectionView(category.name() + " / " + section.name(), matches, 1));
					}
				}
			}
			return new PanelViews(views, "No matching settings.");
		}
		UiCategory category = selectedCategory();
		if (category != null) {
			for (UiSection section : category.sections()) {
				addVisibleSection(views, section, section.name(), 0);
			}
		}
		return new PanelViews(views, "Nothing visible in this category.");
	}

	private static void addVisibleSection(List<SectionView> views, UiSection section, String title, int level) {
		List<UiEntry<?>> entries = visibleEntries(section);
		if (!entries.isEmpty()) {
			views.add(new SectionView(title, entries, level));
		}
	}

	private void selectCategory(int index) {
		this.selectedCategoryIndex = MoissaniteImGui.clamp(index, 0, Math.max(0, this.categories.size() - 1));
		this.showingSettings = false;
		this.searchQuery = "";
		this.searchBuffer.set("");
		this.listeningKeybind = null;
		this.pendingRightScroll = 0.0f;
		this.restoreRightScroll = true;
		saveUiCache();
	}

	private void openSettingsView() {
		this.showingSettings = true;
		this.searchQuery = "";
		this.searchBuffer.set("");
		this.listeningKeybind = null;
		this.pendingRightScroll = 0.0f;
		this.restoreRightScroll = true;
		saveUiCache();
	}

	private UiCategory selectedCategory() {
		if (this.categories.isEmpty()) {
			return null;
		}
		this.selectedCategoryIndex = MoissaniteImGui.clamp(this.selectedCategoryIndex, 0, this.categories.size() - 1);
		return this.categories.get(this.selectedCategoryIndex);
	}

	private String currentContentTitle() {
		if (this.showingSettings) {
			return "Settings";
		}
		if (!this.searchQuery.isBlank()) {
			return "Search: " + this.searchQuery;
		}
		UiCategory category = selectedCategory();
		return category == null ? "Moissanite" : category.name();
	}

	private static int countEntries(PanelViews panelViews) {
		int count = 0;
		for (SectionView view : panelViews.views()) {
			count += view.entries().size();
		}
		return count;
	}

	private void syncSearchState() {
		String nextSearch = sanitizeSearchQuery(this.searchBuffer.get());
		if (!Objects.equals(nextSearch, this.searchQuery)) {
			this.searchQuery = nextSearch;
			if (!this.searchQuery.isBlank()) {
				this.showingSettings = false;
			}
			this.pendingRightScroll = 0.0f;
			this.restoreRightScroll = true;
			saveUiCache();
		}
	}

	private void saveUiCache() {
		UiStore.get().setState(new UiState(
				this.searchQuery,
				this.selectedCategoryIndex,
				this.showingSettings,
				Math.round(this.pendingRightScroll),
				Math.round(this.pendingLeftScroll)));
	}

	private static List<UiEntry<?>> visibleEntries(UiSection section) {
		List<UiEntry<?>> entries = new ArrayList<>();
		for (UiEntry<?> entry : section.entries()) {
			if (entry.isVisible()) {
				entries.add(entry);
			}
		}
		return entries;
	}

	private static List<UiEntry<?>> matchingEntries(UiCategory category, UiSection section, String query) {
		List<UiEntry<?>> matches = new ArrayList<>();
		for (UiEntry<?> entry : section.entries()) {
			if (entry.isVisible() && matches(category, section, entry, query)) {
				matches.add(entry);
			}
		}
		return matches;
	}

	private static boolean matches(UiCategory category, UiSection section, UiEntry<?> entry, String query) {
		return contains(category.name(), query) || contains(section.name(), query) || contains(entry.name(), query);
	}

	private static boolean contains(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}

	private static String keyName(Integer keyCode) {
		if (keyCode == null || keyCode == InputConstants.UNKNOWN.getValue()) {
			return "Unbound";
		}
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
	}

	private static String formatSliderValue(double value, double step) {
		if (step >= 1.0D) {
			return Integer.toString((int) Math.round(value));
		}
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String sanitizeSearchQuery(String value) {
		return value == null ? "" : value.trim();
	}

	private static int toChannel(float value) {
		return MoissaniteImGui.clamp(Math.round(value * 255.0f), 0, 255);
	}

	private record PanelViews(List<SectionView> views, String placeholder) {
	}

	private record SectionView(String title, List<UiEntry<?>> entries, int level) {
	}
}
