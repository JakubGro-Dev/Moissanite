package com.crussion.moissanite.ui.screen;

import java.util.ArrayList;
import java.util.List;

import com.crussion.moissanite.command.CommandReplacer;
import com.crussion.moissanite.command.CommandReplacer.AliasEntry;
import com.crussion.moissanite.command.CommandReplacer.OperationResult;
import com.crussion.moissanite.input.UiKeybinds;
import com.crussion.moissanite.ui.UiEntrypoints;
import com.crussion.moissanite.ui.data.UiButton;
import com.crussion.moissanite.ui.layout.Layouts;
import com.crussion.moissanite.ui.navigation.ScreenIds;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.render.UiTextRenderer;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;
import com.crussion.moissanite.ui.text.UiText;
import com.crussion.moissanite.ui.widget.ListView;
import com.crussion.moissanite.ui.widget.RoundedActionButton;
import com.crussion.moissanite.ui.widget.RoundedEditBox;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class CommandReplacerScreen extends BaseScreen {
	private static final int PANEL_PADDING = Theme.INNER_PADDING;
	private static final int FIELD_GAP = 28;
	private static final int LABEL_GAP = 6;
	private static final int BUTTON_GAP = 8;
	private static final int STATUS_COLOR_SUCCESS = Colors.TEXT_ACCENT;
	private static final int STATUS_COLOR_ERROR = Colors.rgba(255, 120, 120, 255);

	private final ListView aliasList = new ListView();
	private List<AliasEntry> aliases = List.of();
	private EditBox aliasInput;
	private EditBox replacementInput;
	private RoundedActionButton backButton;
	private RoundedActionButton newButton;
	private RoundedActionButton saveButton;
	private RoundedActionButton deleteButton;
	private int lastSelectedIndex = -2;
	private String selectedAlias;
	private String statusMessage = "Command Replacer: Create shortcuts that keep your typed alias in chat history.";
	private int statusColor = Colors.TEXT_MUTED;

	public CommandReplacerScreen() {
		super(Component.literal("Command Replacer"));
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();

		Layouts.Rect left = layout.left();
		Layouts.Rect right = layout.right();
		Layouts.Rect bottom = layout.search();

		int listTitleHeight = this.font.lineHeight * 2 + 12;
		this.aliasList.setBounds(
				left.x() + PANEL_PADDING,
				left.y() + PANEL_PADDING + listTitleHeight,
				left.width() - (PANEL_PADDING * 2),
				Math.max(0, left.height() - (PANEL_PADDING * 2) - listTitleHeight));
		this.aliasList.setVisibleOnlyWhenScrollable(false);
		this.aliasList.setCentered(false);
		this.aliasList.setBold(true);
		this.aliasList.setItemHeight(Math.max(Theme.LIST_ITEM_HEIGHT, this.font.lineHeight + 12));

		int fieldWidth = Math.max(80, right.width() - (PANEL_PADDING * 2));
		int fieldX = right.x() + PANEL_PADDING;
		int aliasY = right.y() + PANEL_PADDING + this.font.lineHeight * 3 + 22;
		int replacementY = aliasY + Theme.INPUT_HEIGHT + FIELD_GAP;

		this.aliasInput = createInput(fieldX, aliasY, fieldWidth, "Alias");
		this.aliasInput.setHint(UiText.uiText("e.g. /home"));
		this.replacementInput = createInput(fieldX, replacementY, fieldWidth, "Replacement");
		this.replacementInput.setHint(UiText.uiText("e.g. /warp home"));

		int buttonWidth = Math.max(60, (bottom.width() - (BUTTON_GAP * 3)) / 4);
		int buttonY = bottom.y();
		int buttonX = bottom.x();
		this.backButton = createButton(buttonX, buttonY, buttonWidth, bottom.height(), "Back", this::openOverlay);
		buttonX += buttonWidth + BUTTON_GAP;
		this.newButton = createButton(buttonX, buttonY, buttonWidth, bottom.height(), "New", this::clearSelection);
		buttonX += buttonWidth + BUTTON_GAP;
		this.saveButton = createButton(buttonX, buttonY, buttonWidth, bottom.height(), "Save", this::saveAlias);
		buttonX += buttonWidth + BUTTON_GAP;
		this.deleteButton = createButton(buttonX, buttonY, buttonWidth, bottom.height(), "Delete", this::deleteAlias);

		reloadAliases(this.selectedAlias);
		updateActionState();
		setInitialFocus(this.aliasInput);
	}

	@Override
	public void tick() {
		int selectedIndex = this.aliasList.getSelectedIndex();
		if (selectedIndex != this.lastSelectedIndex) {
			this.lastSelectedIndex = selectedIndex;
			applyListSelection(selectedIndex);
		}
		updateActionState();
		super.tick();
	}

	@Override
	protected void renderUnderlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (layout == null) {
			return;
		}

		Layouts.Rect left = layout.left();
		UiShapes.fillRoundedOutline(
				graphics,
				left.x(),
				left.y(),
				left.width(),
				left.height(),
				Theme.SECTION_RADIUS,
				1,
				Colors.SECTION_OUTLINE,
				Colors.LEFT_BG);
		UiShapes.fillRoundedRect(
				graphics,
				left.x() + 2,
				left.y() + 2,
				left.width() - 4,
				1,
				Math.max(0, Theme.SECTION_RADIUS - 2),
				Colors.PANEL_HIGHLIGHT);

		Layouts.Rect right = layout.right();
		UiShapes.fillRoundedOutline(
				graphics,
				right.x(),
				right.y(),
				right.width(),
				right.height(),
				Theme.SECTION_RADIUS,
				1,
				Colors.SECTION_OUTLINE_NESTED,
				Colors.RIGHT_BG);
		UiShapes.fillRoundedRect(
				graphics,
				right.x() + 2,
				right.y() + 2,
				right.width() - 4,
				1,
				Math.max(0, Theme.SECTION_RADIUS - 2),
				Colors.PANEL_HIGHLIGHT);

		int dividerX = left.right() + (Theme.SECTION_GAP / 2);
		UiShapes.fillRoundedRect(graphics, dividerX, left.y() + 8, 1, left.height() - 16, 1, Colors.DIVIDER);
		UiShapes.fillRoundedRect(graphics, dividerX - 1, left.y() + 12, 3, left.height() - 24, 1, Colors.DIVIDER_GLOW);
	}

	@Override
	protected void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (layout == null) {
			return;
		}

		this.aliasList.render(graphics, this.font, mouseX, mouseY);

		Layouts.Rect left = layout.left();
		int leftTextX = left.x() + PANEL_PADDING;
		int leftTextY = left.y() + PANEL_PADDING;
		UiTextRenderer.drawBoldString(graphics, this.font, UiText.uiText("ALIASES"), leftTextX, leftTextY, Colors.TEXT_PRIMARY, 1.14f);
		graphics.drawString(this.font, UiText.uiText(this.aliases.isEmpty() ? "No shortcuts saved" : "Select one to edit"), leftTextX, leftTextY + this.font.lineHeight + 5, Colors.TEXT_MUTED, false);

		Layouts.Rect right = layout.right();
		int textX = right.x() + PANEL_PADDING;
		int titleY = right.y() + PANEL_PADDING;
		UiTextRenderer.drawBoldString(graphics, this.font, UiText.uiText("COMMAND REPLACER"), textX, titleY, Colors.TEXT_PRIMARY, 1.14f);
		graphics.drawString(this.font, UiText.uiText("Alias stays in history. Replacement is what gets sent."), textX, titleY + this.font.lineHeight + 5, Colors.TEXT_MUTED, false);
		graphics.drawString(this.font, UiText.uiText("Type both with or without /. Moissanite normalizes them."), textX, titleY + (this.font.lineHeight * 2) + 9, Colors.TEXT_MUTED, false);

		int aliasLabelY = this.aliasInput.getY() - this.font.lineHeight - LABEL_GAP;
		int replacementLabelY = this.replacementInput.getY() - this.font.lineHeight - LABEL_GAP;
		graphics.drawString(this.font, UiText.uiText("Alias"), textX, aliasLabelY, Colors.TEXT_SECTION_TITLE, false);
		graphics.drawString(this.font, UiText.uiText("Replacement"), textX, replacementLabelY, Colors.TEXT_SECTION_TITLE, false);

		int previewY = this.replacementInput.getY() + this.replacementInput.getHeight() + 24;
		String normalizedAlias = CommandReplacer.normalizeUserCommand(this.aliasInput.getValue());
		String normalizedReplacement = CommandReplacer.normalizeUserCommand(this.replacementInput.getValue());
		graphics.drawString(this.font, UiText.uiText("Preview"), textX, previewY, Colors.TEXT_SECTION_TITLE, false);
		if (normalizedAlias != null && normalizedReplacement != null) {
			graphics.drawString(this.font, UiText.uiText("History: " + CommandReplacer.formatCommand(normalizedAlias)), textX, previewY + this.font.lineHeight + 6, Colors.TEXT_MUTED, false);
			graphics.drawString(this.font, UiText.uiText("Sends:  " + CommandReplacer.formatCommand(normalizedReplacement)), textX, previewY + (this.font.lineHeight * 2) + 10, Colors.TEXT_ACCENT, false);
		} else {
			graphics.drawString(this.font, UiText.uiText("Fill in both fields to preview the rewrite."), textX, previewY + this.font.lineHeight + 6, Colors.TEXT_MUTED, false);
		}

		int statusY = layout.search().y() - this.font.lineHeight - 8;
		graphics.drawString(this.font, UiText.uiText(statusMessage), textX, statusY, statusColor, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
		if (this.aliasList.mouseClicked(event.x(), event.y(), event.button())) {
			applyListSelection(this.aliasList.getSelectedIndex());
			return true;
		}
		return super.mouseClicked(event, focused);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
		if (this.aliasList.mouseScrolled(mouseX, mouseY, yDelta)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (super.keyPressed(event)) {
			return true;
		}
		if (UiKeybinds.isToggleKey(event)) {
			this.minecraft.setScreen(null);
			return true;
		}
		if (event.key() == InputConstants.KEY_ESCAPE) {
			openOverlay();
			return true;
		}
		return false;
	}

	private void saveAlias() {
		OperationResult result = CommandReplacer.upsertAlias(this.selectedAlias, this.aliasInput.getValue(), this.replacementInput.getValue());
		if (!result.success()) {
			setStatus(result);
			return;
		}
		reloadAliases(result.alias());
		setStatus(result);
	}

	private void deleteAlias() {
		String aliasToDelete = this.selectedAlias != null ? this.selectedAlias : CommandReplacer.normalizeUserCommand(this.aliasInput.getValue());
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
		this.aliasList.clearSelection();
		this.lastSelectedIndex = -1;
		this.aliasInput.setValue("");
		this.replacementInput.setValue("");
		this.aliasInput.setFocused(true);
		setStatus("Command Replacer: Enter an alias and replacement, then press Save.", Colors.TEXT_MUTED);
	}

	private void applyListSelection(int index) {
		if (index < 0 || index >= this.aliases.size()) {
			this.selectedAlias = null;
			return;
		}

		AliasEntry entry = this.aliases.get(index);
		this.selectedAlias = entry.alias();
		this.aliasInput.setValue(CommandReplacer.formatCommand(entry.alias()));
		this.replacementInput.setValue(CommandReplacer.formatCommand(entry.replacement()));
		setStatus("Command Replacer: Editing " + CommandReplacer.formatCommand(entry.alias()), Colors.TEXT_MUTED);
	}

	private void reloadAliases(String aliasToSelect) {
		this.aliases = CommandReplacer.getAliases();
		List<Component> items = new ArrayList<>(this.aliases.size());
		int selectedIndex = -1;
		for (int i = 0; i < this.aliases.size(); i++) {
			AliasEntry entry = this.aliases.get(i);
			items.add(UiText.uiText(entry.displayLabel()));
			if (aliasToSelect != null && aliasToSelect.equals(entry.alias())) {
				selectedIndex = i;
			}
		}
		this.aliasList.setItems(items);
		this.aliasList.setSelectedIndex(selectedIndex);
		this.lastSelectedIndex = selectedIndex;
		if (selectedIndex >= 0) {
			applyListSelection(selectedIndex);
		}
	}

	private void updateActionState() {
		String normalizedAlias = CommandReplacer.normalizeUserCommand(this.aliasInput.getValue());
		String normalizedReplacement = CommandReplacer.normalizeUserCommand(this.replacementInput.getValue());
		boolean canSave = normalizedAlias != null && normalizedReplacement != null;
		boolean canDelete = false;
		if (normalizedAlias != null) {
			for (AliasEntry entry : this.aliases) {
				if (normalizedAlias.equals(entry.alias())) {
					canDelete = true;
					break;
				}
			}
		}
		if (this.selectedAlias != null) {
			canDelete = true;
		}

		this.saveButton.active = canSave;
		this.deleteButton.active = canDelete;
	}

	private EditBox createInput(int x, int y, int width, String label) {
		EditBox box = new RoundedEditBox(this.font, x, y, width, Theme.INPUT_HEIGHT, Component.literal(label), this.font.lineHeight);
		UiText.applyUiFont(box);
		box.setMaxLength(256);
		box.setTextColor(Colors.TEXT_PRIMARY);
		box.setBordered(false);
		box.setCentered(false);
		this.addRenderableWidget(box);
		return box;
	}

	private RoundedActionButton createButton(int x, int y, int width, int height, String label, Runnable action) {
		RoundedActionButton button = new RoundedActionButton(
				this.font,
				x,
				y,
				width,
				height,
				new UiButton(label, label, ignored -> action.run()));
		this.addRenderableWidget(button);
		return button;
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
