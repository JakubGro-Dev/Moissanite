package com.crussion.moissanite.ui.screen;

import java.util.ArrayList;
import java.util.List;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.ui.layout.Layouts;
import com.crussion.moissanite.ui.screen.overlay.OverlayRightPanel;
import com.crussion.moissanite.ui.screen.overlay.UiEntryWidgetFactory;
import com.crussion.moissanite.ui.state.UiState;
import com.crussion.moissanite.ui.state.UiStore;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;
import com.crussion.moissanite.ui.widget.IconButton;
import com.crussion.moissanite.ui.widget.ListView;
import com.crussion.moissanite.ui.widget.RoundedEditBox;
import com.crussion.moissanite.ui.text.UiText;
import com.crussion.moissanite.ui.data.UiCatalog;
import com.crussion.moissanite.ui.data.UiCategory;
import com.crussion.moissanite.ui.data.UiEntry;
import com.crussion.moissanite.ui.data.UiSection;
import com.crussion.moissanite.input.UiKeybinds;
import com.crussion.moissanite.ui.widget.KeybindWidget;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

public class InventoryOverlayScreen extends BaseScreen {
	private static final Identifier SETTINGS_ICON =
			Identifier.fromNamespaceAndPath("moissanite", "textures/gui/settings2.png");
	private static final int SETTINGS_ICON_TEXTURE_SIZE = 18;
	private EditBox searchBox;
	private final ListView selectionList = new ListView();
	private List<UiCategory> categories = List.of();
	private int lastCategoryIndex = -2;
	private String searchQuery = "";
	private boolean showingSettings;
	private int lastVisibilitySignature = Integer.MIN_VALUE;
	private final OverlayRightPanel rightPanel = new OverlayRightPanel();
	private OverlayRightPanel.EntryWidgetFactory entryWidgetFactory;

	public InventoryOverlayScreen() {
		super(Component.literal("Moissanite"));
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();
		this.entryWidgetFactory = new UiEntryWidgetFactory(this.font);

		UiDefinitions.init();
		this.categories = new ArrayList<>(UiCatalog.categories());
		this.categories.remove(UiDefinitions.UISETTINGS);
		UiState cachedState = UiStore.get().getState();
		this.searchQuery = sanitizeSearchQuery(cachedState.searchQuery());
		this.showingSettings = cachedState.showingSettings();

		Layouts.Layout layout = this.layout;
		Layouts.Rect searchBoxRect = layout.search();
		int settingsSize = Math.min(20, searchBoxRect.height());
		int settingsGap = 6;
		int searchX = searchBoxRect.x() + settingsSize + settingsGap;
		int searchWidth = Math.max(0, searchBoxRect.width() - settingsSize - settingsGap);
		this.searchBox = new RoundedEditBox(this.font, searchX, searchBoxRect.y(), searchWidth, searchBoxRect.height(), Component.empty(), this.font.lineHeight);
		UiText.applyUiFont(this.searchBox);
		this.searchBox.setMaxLength(120);
		this.searchBox.setValue(this.searchQuery);
		this.searchBox.setResponder(value -> {
			showingSettings = false;
			searchQuery = sanitizeSearchQuery(value);
			rebuildRightPanel();
		});
		this.searchBox.setCentered(true);
		this.searchBox.setTextColor(Colors.TEXT_PRIMARY);
		this.searchBox.setBordered(false);
		this.searchBox.setHint(UiText.uiText("Search"));
		this.addRenderableWidget(this.searchBox);

		int settingsX = searchBoxRect.x();
		int settingsY = searchBoxRect.y() + (searchBoxRect.height() - settingsSize) / 2;
		this.addRenderableWidget(new IconButton(settingsX, settingsY, settingsSize, SETTINGS_ICON,
				Component.literal("Settings"), button -> openSettingsView(), false, SETTINGS_ICON_TEXTURE_SIZE, SETTINGS_ICON_TEXTURE_SIZE));

		Layouts.Rect listBox = layout.list();
		this.selectionList.setBounds(listBox.x(), listBox.y(), listBox.width(), listBox.height());
		this.selectionList.setItems(categoryLabels());
		this.selectionList.setVisibleOnlyWhenScrollable(false);
		this.selectionList.setCentered(true);
		this.selectionList.setBold(true);
		this.selectionList.setItemHeight(Math.max(Theme.LIST_ITEM_HEIGHT, this.font.lineHeight + 6));
		this.selectionList.setTextScale(1.08f);
		this.selectionList.setSelectedScale(1.18f);
		this.selectionList.setScrollOffset(cachedState.leftListScroll());
		if (this.showingSettings) {
			this.selectionList.clearSelection();
		} else {
			this.selectionList.setSelectedIndex(cachedState.selectedCategoryIndex());
		}
		this.lastCategoryIndex = this.selectionList.getSelectedIndex();
		this.rightPanel.setContentScroll(cachedState.rightPanelScroll());

		this.lastVisibilitySignature = visibilitySignature();
		rebuildRightPanel();
	}

	@Override
	public void tick() {
		int selectedIndex = this.selectionList.getSelectedIndex();
		if (selectedIndex != lastCategoryIndex) {
			lastCategoryIndex = selectedIndex;
			rightPanel.resetScroll();
			rebuildRightPanel();
		}
		int visibilitySignature = visibilitySignature();
		if (visibilitySignature != lastVisibilitySignature) {
			lastVisibilitySignature = visibilitySignature;
			rebuildRightPanel();
		}
		super.tick();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		this.selectionList.render(graphics, this.font, mouseX, mouseY);
		rightPanel.renderLabels(graphics, this.font, layout);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
		if (this.selectionList.mouseClicked(event.x(), event.y(), event.button())) {
			showingSettings = false;
			saveUiCache();
			return true;
		}
		return super.mouseClicked(event, focused);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
		if (layout != null) {
			Layouts.Rect right = layout.right();
			if (isInside(mouseX, mouseY, right.x(), right.y(), right.width(), right.height())) {
				if (rightPanel.getContentScrollMax() > 0) {
					if (rightPanel.scrollBy((int) (-yDelta * 12))) {
						rebuildRightPanel();
						saveUiCache();
					}
					return true;
				}
			}
		}
		if (this.selectionList.mouseScrolled(mouseX, mouseY, yDelta)) {
			saveUiCache();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE && clearListeningKeybindIfAny()) {
			return true;
		}
		if (super.keyPressed(event)) {
			return true;
		}
		if (event.key() == InputConstants.KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		if (UiKeybinds.isToggleKey(event)) {
			this.onClose();
			return true;
		}
		return false;
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

	@Override
	protected void renderUnderlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (layout == null) {
			return;
		}
		Layouts.Rect left = layout.left();
		graphics.fill(left.x(), left.y(), left.right(), left.bottom(), Colors.LEFT_BG);
		Layouts.Rect right = layout.right();
		graphics.fill(right.x(), right.y(), right.right(), right.bottom(), Colors.RIGHT_BG);

		int dividerX = left.right() + (Theme.SECTION_GAP / 2);
		graphics.fill(dividerX, left.y(), dividerX + 1, left.bottom(), Colors.DIVIDER);
	}

	@Override
	protected void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (layout == null) {
			return;
		}
		drawTitle(graphics, layout);
	}

	private List<Component> categoryLabels() {
		List<Component> labels = new ArrayList<>(categories.size());
		for (UiCategory category : categories) {
			labels.add(UiText.uiText(category.name()));
		}
		return labels;
	}

	private void rebuildRightPanel() {
		rightPanel.rebuild(
				layout,
				this.font,
				categories,
				UiDefinitions.UISETTINGS,
				selectionList,
				showingSettings,
				searchQuery,
				entryWidgetFactory,
				UiText::uiText,
				this::removeWidget,
				this::addRenderableWidget
		);
		saveUiCache();
	}

	private void openSettingsView() {
		this.selectionList.clearSelection();
		this.lastCategoryIndex = -2;
		this.searchBox.setValue("");
		this.showingSettings = true;
		this.searchQuery = "";
		this.rightPanel.resetScroll();
		rebuildRightPanel();
	}

	private void drawTitle(GuiGraphics graphics, Layouts.Layout layout) {
		Layouts.Rect left = layout.left();
		Layouts.Rect titleArea = layout.input();
		float scale = 1.5f;
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		int centerX = (int) ((left.x() + left.width() / 2f) / scale);
		int titleY = (int) ((titleArea.y() + 4) / scale);
		Component title = UiText.uiText("Moissanite Client");
		graphics.drawCenteredString(this.font, title, centerX, titleY, Colors.TEXT_PRIMARY);
		graphics.pose().popMatrix();
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private void saveUiCache() {
		UiStore.get().setState(new UiState(
				this.searchQuery,
				this.selectionList.getSelectedIndex(),
				this.showingSettings,
				this.rightPanel.getContentScroll(),
				this.selectionList.getScrollOffset()
		));
	}

	private static String sanitizeSearchQuery(String searchQuery) {
		return searchQuery == null ? "" : searchQuery.trim();
	}

	private boolean clearListeningKeybindIfAny() {
		for (var child : this.children()) {
			if (child instanceof KeybindWidget keybindWidget && keybindWidget.isListening()) {
				keybindWidget.clearBindingAndStopListening();
				return true;
			}
		}
		return false;
	}

	private static int visibilitySignature() {
		int signature = 1;
		for (UiCategory category : UiCatalog.categories()) {
			for (UiSection section : category.sections()) {
				for (UiEntry<?> entry : section.entries()) {
					signature = 31 * signature + (entry.isVisible() ? 1 : 0);
				}
			}
		}
		return signature;
	}
}
