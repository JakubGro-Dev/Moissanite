package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.command.CommandReplacer;
import com.crussion.moissanite.command.CommandReplacer.AliasEntry;
import com.crussion.moissanite.command.CommandReplacer.OperationResult;
import com.crussion.moissanite.input.UiKeybinds;
import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.imgui.ImGuiScreen;
import com.crussion.moissanite.ui.imgui.MoissaniteImGui;
import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;
import com.mojang.blaze3d.platform.InputConstants;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.util.List;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class ImGuiCommandReplacerScreen extends ImGuiScreen {
	private static final int WINDOW_FLAGS = ImGuiWindowFlags.NoTitleBar
			| ImGuiWindowFlags.NoResize
			| ImGuiWindowFlags.NoMove
			| ImGuiWindowFlags.NoCollapse
			| ImGuiWindowFlags.NoBringToFrontOnFocus
			| ImGuiWindowFlags.NoSavedSettings
			| ImGuiWindowFlags.NoBackground;
	private static final float PANEL_PADDING = 12.0f;
	private static final float TITLE_BLOCK_HEIGHT = 70.0f;
	private static final float LIST_ROW_HEIGHT = 32.0f;
	private static final int STATUS_COLOR_SUCCESS = Colors.TEXT_ACCENT;
	private static final int STATUS_COLOR_ERROR = 0xFFFF7878;

	private List<AliasEntry> aliases = List.of();
	private final ImString aliasBuffer = new ImString(257);
	private final ImString replacementBuffer = new ImString(257);
	private int selectedIndex = -1;
	private String selectedAlias;
	private String statusMessage = "Command Replacer: Create shortcuts that keep your typed alias in chat history.";
	private int statusColor = Colors.TEXT_MUTED;
	private float listScroll;
	private boolean restoreListScroll = true;

	public ImGuiCommandReplacerScreen() {
		super(Component.literal("Command Replacer"));
	}

	@Override
	protected void init() {
		super.init();
		reloadAliases(this.selectedAlias);
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

		if (ImGui.begin("Moissanite Command Replacer##overlay", WINDOW_FLAGS)) {
			MoissaniteImGui.Layout layout = MoissaniteImGui.computeLayout(viewport.getSizeX(), viewport.getSizeY());
			ImDrawList draw = ImGui.getWindowDrawList();
			draw.addRectFilled(viewport.getPosX(), viewport.getPosY(), viewport.getPosX() + viewport.getSizeX(), viewport.getPosY() + viewport.getSizeY(),
					MoissaniteImGui.color(Colors.BACKDROP));
			renderChrome(draw, layout);
			renderAliasList(draw, layout.left());
			renderEditor(draw, layout.right(), layout.search());
			renderActions(draw, layout.search());
		}
		ImGui.end();
	}

	private void renderChrome(ImDrawList draw, MoissaniteImGui.Layout layout) {
		MoissaniteImGui.Rect outer = layout.outer();
		MoissaniteImGui.panel(draw, outer.x(), outer.y(), outer.width(), outer.height(), Theme.PANEL_RADIUS);
		MoissaniteImGui.section(draw, layout.left().x(), layout.left().y(), layout.left().width(), layout.left().height(), Theme.SECTION_RADIUS, Colors.LEFT_BG, Colors.SECTION_OUTLINE);
		MoissaniteImGui.section(draw, layout.right().x(), layout.right().y(), layout.right().width(), layout.right().height(), Theme.SECTION_RADIUS, Colors.RIGHT_BG, Colors.SECTION_OUTLINE_NESTED);
		float dividerX = layout.left().right() + 7.0f;
		draw.addRectFilled(dividerX, layout.left().y() + 8.0f, dividerX + 1.0f, layout.left().bottom() - 8.0f, MoissaniteImGui.color(Colors.DIVIDER), 1.0f);
		draw.addRectFilled(dividerX - 1.0f, layout.left().y() + 12.0f, dividerX + 2.0f, layout.left().bottom() - 12.0f, MoissaniteImGui.color(Colors.DIVIDER_GLOW), 1.0f);
	}

	private void renderAliasList(ImDrawList draw, MoissaniteImGui.Rect left) {
		float titleX = left.x() + PANEL_PADDING;
		float titleY = left.y() + PANEL_PADDING;
		MoissaniteImGui.text(draw, "ALIASES", titleX, titleY, Colors.TEXT_PRIMARY, MoissaniteImGui.SECTION_TITLE_SIZE);
		MoissaniteImGui.text(draw, this.aliases.isEmpty() ? "No shortcuts saved" : "Select one to edit",
				titleX, titleY + 24.0f, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);

		float listX = left.x() + PANEL_PADDING;
		float listY = left.y() + TITLE_BLOCK_HEIGHT;
		float listWidth = left.width() - (PANEL_PADDING * 2.0f);
		float listHeight = Math.max(0.0f, left.bottom() - listY - PANEL_PADDING);
		ImGui.setCursorScreenPos(listX, listY);
		if (ImGui.beginChild("##command_aliases", listWidth, listHeight, false, ImGuiWindowFlags.NoBackground)) {
			if (this.restoreListScroll) {
				ImGui.setScrollY(this.listScroll);
				this.restoreListScroll = false;
			}
			ImDrawList listDraw = ImGui.getWindowDrawList();
			float originX = ImGui.getWindowPosX();
			float originY = ImGui.getWindowPosY();
			float scrollY = ImGui.getScrollY();
			for (int i = 0; i < this.aliases.size(); i++) {
				AliasEntry entry = this.aliases.get(i);
				float localY = i * LIST_ROW_HEIGHT;
				ImGui.setCursorPos(0.0f, localY);
				if (ImGui.invisibleButton("##alias_" + i, listWidth, LIST_ROW_HEIGHT)) {
					applyListSelection(i);
				}
				boolean hovered = ImGui.isItemHovered();
				boolean selected = i == this.selectedIndex;
				float screenY = originY + localY - scrollY;
				float state = MoissaniteImGui.animation("command_alias_" + i, selected || hovered, 18.0f);
				if (state > 0.001f) {
					int bg = selected ? Colors.LIST_SELECTED : Colors.LIST_HOVER;
					listDraw.addRectFilled(originX, screenY + 2.0f, originX + listWidth, screenY + LIST_ROW_HEIGHT - 2.0f,
							MoissaniteImGui.color(MoissaniteImGui.withAlpha(bg, Math.round((selected ? 108.0f : 44.0f) * state))), 7.0f);
					if (selected) {
						listDraw.addRectFilled(originX + 1.0f, screenY + 6.0f, originX + 4.0f, screenY + LIST_ROW_HEIGHT - 6.0f,
								MoissaniteImGui.color(MoissaniteImGui.withAlpha(Colors.ACCENT, Math.round(255.0f * state))), 2.0f);
					}
				}
				MoissaniteImGui.clippedText(listDraw, entry.displayLabel(), originX + 10.0f, screenY + 8.0f, listWidth - 18.0f,
						selected ? Colors.TEXT_SELECTED : Colors.TEXT_PRIMARY);
			}
			ImGui.setCursorPos(0.0f, this.aliases.size() * LIST_ROW_HEIGHT);
			ImGui.dummy(1.0f, 1.0f);
			this.listScroll = ImGui.getScrollY();
		}
		ImGui.endChild();
	}

	private void renderEditor(ImDrawList draw, MoissaniteImGui.Rect right, MoissaniteImGui.Rect bottom) {
		float x = right.x() + PANEL_PADDING;
		float y = right.y() + PANEL_PADDING;
		float width = right.width() - (PANEL_PADDING * 2.0f);
		MoissaniteImGui.text(draw, "COMMAND REPLACER", x, y, Colors.TEXT_PRIMARY, MoissaniteImGui.SECTION_TITLE_SIZE);
		MoissaniteImGui.text(draw, "Alias stays in history. Replacement is what gets sent.", x, y + 24.0f, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);
		MoissaniteImGui.text(draw, "Type both with or without /. Moissanite normalizes them.", x, y + 44.0f, Colors.TEXT_MUTED, MoissaniteImGui.SMALL_TEXT_SIZE);

		float aliasY = y + 86.0f;
		float replacementY = aliasY + 66.0f;
		drawLabeledInput("Alias", "e.g. /home", this.aliasBuffer, x, aliasY, width);
		drawLabeledInput("Replacement", "e.g. /warp home", this.replacementBuffer, x, replacementY, width);

		float previewY = replacementY + 68.0f;
		MoissaniteImGui.text(draw, "Preview", x, previewY, Colors.TEXT_SECTION_TITLE, MoissaniteImGui.FONT_SIZE);
		String normalizedAlias = CommandReplacer.normalizeUserCommand(this.aliasBuffer.get());
		String normalizedReplacement = CommandReplacer.normalizeUserCommand(this.replacementBuffer.get());
		if (normalizedAlias != null && normalizedReplacement != null) {
			MoissaniteImGui.clippedText(draw, "History: " + CommandReplacer.formatCommand(normalizedAlias), x, previewY + 25.0f, width, Colors.TEXT_MUTED);
			MoissaniteImGui.clippedText(draw, "Sends:  " + CommandReplacer.formatCommand(normalizedReplacement), x, previewY + 48.0f, width, Colors.TEXT_ACCENT);
		} else {
			MoissaniteImGui.clippedText(draw, "Fill in both fields to preview the rewrite.", x, previewY + 25.0f, width, Colors.TEXT_MUTED);
		}

		float statusY = bottom.y() - 24.0f;
		MoissaniteImGui.clippedText(draw, this.statusMessage, x, statusY, width, this.statusColor);
	}

	private static void drawLabeledInput(String label, String hint, ImString buffer, float x, float y, float width) {
		MoissaniteImGui.text(ImGui.getWindowDrawList(), label, x, y, Colors.TEXT_SECTION_TITLE, MoissaniteImGui.FONT_SIZE);
		ImGui.setCursorScreenPos(x, y + 23.0f);
		ImGui.setNextItemWidth(width);
		MoissaniteImGui.pushFrameColors();
		ImGui.inputTextWithHint("##" + label, hint, buffer, ImGuiInputTextFlags.EscapeClearsAll);
		MoissaniteImGui.popFrameColors();
	}

	private void renderActions(ImDrawList draw, MoissaniteImGui.Rect bottom) {
		float gap = 8.0f;
		float buttonWidth = Math.max(60.0f, (bottom.width() - gap * 3.0f) / 4.0f);
		float x = bottom.x();
		if (MoissaniteImGui.textButton(draw, "##back", "Back", x, bottom.y(), buttonWidth, bottom.height(), true)) {
			openOverlay();
		}
		x += buttonWidth + gap;
		if (MoissaniteImGui.textButton(draw, "##new", "New", x, bottom.y(), buttonWidth, bottom.height(), true)) {
			clearSelection();
		}
		x += buttonWidth + gap;
		if (MoissaniteImGui.textButton(draw, "##save", "Save", x, bottom.y(), buttonWidth, bottom.height(), canSave())) {
			saveAlias();
		}
		x += buttonWidth + gap;
		if (MoissaniteImGui.textButton(draw, "##delete", "Delete", x, bottom.y(), buttonWidth, bottom.height(), canDelete())) {
			deleteAlias();
		}
	}

	private boolean canSave() {
		return CommandReplacer.normalizeUserCommand(this.aliasBuffer.get()) != null
				&& CommandReplacer.normalizeUserCommand(this.replacementBuffer.get()) != null;
	}

	private boolean canDelete() {
		String normalizedAlias = CommandReplacer.normalizeUserCommand(this.aliasBuffer.get());
		if (this.selectedAlias != null) {
			return true;
		}
		if (normalizedAlias == null) {
			return false;
		}
		for (AliasEntry entry : this.aliases) {
			if (normalizedAlias.equals(entry.alias())) {
				return true;
			}
		}
		return false;
	}

	private void saveAlias() {
		OperationResult result = CommandReplacer.upsertAlias(this.selectedAlias, this.aliasBuffer.get(), this.replacementBuffer.get());
		if (!result.success()) {
			setStatus(result);
			return;
		}
		reloadAliases(result.alias());
		setStatus(result);
	}

	private void deleteAlias() {
		String aliasToDelete = this.selectedAlias != null ? this.selectedAlias : CommandReplacer.normalizeUserCommand(this.aliasBuffer.get());
		OperationResult result = CommandReplacer.removeAlias(aliasToDelete);
		if (!result.success()) {
			setStatus(result);
			return;
		}
		clearSelection();
		reloadAliases(null);
		setStatus(result);
	}

	private void clearSelection() {
		this.selectedAlias = null;
		this.selectedIndex = -1;
		this.aliasBuffer.set("");
		this.replacementBuffer.set("");
		setStatus("Command Replacer: Enter an alias and replacement, then press Save.", Colors.TEXT_MUTED);
	}

	private void applyListSelection(int index) {
		if (index < 0 || index >= this.aliases.size()) {
			this.selectedAlias = null;
			this.selectedIndex = -1;
			return;
		}

		AliasEntry entry = this.aliases.get(index);
		this.selectedIndex = index;
		this.selectedAlias = entry.alias();
		this.aliasBuffer.set(CommandReplacer.formatCommand(entry.alias()));
		this.replacementBuffer.set(CommandReplacer.formatCommand(entry.replacement()));
		setStatus("Command Replacer: Editing " + CommandReplacer.formatCommand(entry.alias()), Colors.TEXT_MUTED);
	}

	private void reloadAliases(String aliasToSelect) {
		this.aliases = CommandReplacer.getAliases();
		this.selectedIndex = -1;
		for (int i = 0; i < this.aliases.size(); i++) {
			AliasEntry entry = this.aliases.get(i);
			if (aliasToSelect != null && aliasToSelect.equals(entry.alias())) {
				applyListSelection(i);
				return;
			}
		}
		if (aliasToSelect != null) {
			this.selectedAlias = null;
		}
	}

	private void setStatus(OperationResult result) {
		if (result == null) {
			return;
		}
		setStatus(result.message(), result.success() ? STATUS_COLOR_SUCCESS : STATUS_COLOR_ERROR);
	}

	private void setStatus(String message, int color) {
		this.statusMessage = message == null ? "" : message;
		this.statusColor = color;
	}

	private void openOverlay() {
		UiEntrypoints.open(ScreenIds.INVENTORY_OVERLAY);
	}
}
