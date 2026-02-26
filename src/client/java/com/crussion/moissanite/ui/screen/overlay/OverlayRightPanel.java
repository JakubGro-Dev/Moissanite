package com.crussion.moissanite.ui.screen.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import com.crussion.moissanite.ui.data.UiCategory;
import com.crussion.moissanite.ui.data.UiEntry;
import com.crussion.moissanite.ui.data.UiSection;
import com.crussion.moissanite.ui.layout.Layouts;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.render.UiTextRenderer;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;
import com.crussion.moissanite.ui.style.XmlUiTheme;
import com.crussion.moissanite.ui.widget.ListView;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class OverlayRightPanel {
	private static final int PANEL_PADDING = dim("RIGHT_PANEL_PADDING", 12);
	private static final int LABEL_CONTROL_GAP = dim("RIGHT_LABEL_CONTROL_GAP", 12);
	private static final int ROW_HEIGHT = dim("RIGHT_ROW_HEIGHT", 24);
	private static final int ROW_SPACING = dim("RIGHT_ROW_SPACING", 8);
	private static final int SECTION_SPACING = dim("RIGHT_SECTION_SPACING", 10);
	private static final int LABEL_WIDTH_LIMIT = dim("RIGHT_LABEL_WIDTH_LIMIT", 250);
	private static final float LABEL_WIDTH_RATIO = 0.54f;
	private static final int CONTROL_MIN_WIDTH = dim("RIGHT_CONTROL_MIN_WIDTH", 132);
	private static final int CONTROL_MAX_WIDTH = dim("RIGHT_CONTROL_MAX_WIDTH", 260);
	private static final int SECTION_CONTENT_PADDING_X = dim("RIGHT_SECTION_CONTENT_PADDING_X", 12);
	private static final int SECTION_HEADER_TOP_PADDING = dim("RIGHT_SECTION_HEADER_TOP_PADDING", 8);
	private static final int SECTION_HEADER_BOTTOM_PADDING = dim("RIGHT_SECTION_HEADER_BOTTOM_PADDING", 6);
	private static final int SECTION_BODY_TOP_PADDING = dim("RIGHT_SECTION_BODY_TOP_PADDING", 6);
	private static final int SECTION_BODY_BOTTOM_PADDING = dim("RIGHT_SECTION_BODY_BOTTOM_PADDING", 8);
	private static final float SECTION_TITLE_SCALE = 1.08f;
	private static final float ENTRY_LABEL_SCALE = 1.0f;

	private final List<AbstractWidget> widgets = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<SectionChrome> sectionChromes = new ArrayList<>();
	private int contentScroll;
	private int contentScrollMax;

	public int getContentScrollMax() {
		return contentScrollMax;
	}

	public int getContentScroll() {
		return contentScroll;
	}

	public void setContentScroll(int contentScroll) {
		this.contentScroll = Math.max(0, contentScroll);
	}

	public void resetScroll() {
		contentScroll = 0;
	}

	public boolean scrollBy(int delta) {
		int next = clamp(contentScroll + delta, 0, contentScrollMax);
		if (next == contentScroll) {
			return false;
		}
		contentScroll = next;
		return true;
	}

	public interface EntryWidgetFactory {
		AbstractWidget create(UiEntry<?> entry, int x, int y, int width, int height);
	}

	public void rebuild(Layouts.Layout layout,
						Font font,
						List<UiCategory> categories,
						UiCategory settingsCategory,
						ListView selectionList,
						boolean showingSettings,
						String searchQuery,
						EntryWidgetFactory entryFactory,
						Function<String, Component> uiText,
						Consumer<AbstractWidget> removeWidget,
						Function<AbstractWidget, AbstractWidget> addWidget) {
		clearWidgets(removeWidget);
		if (layout == null) {
			return;
		}

		PanelViews panelViews = collectPanelViews(categories, settingsCategory, selectionList, showingSettings, searchQuery);
		if (panelViews.views.isEmpty() && !panelViews.showSettingsPlaceholder) {
			contentScroll = 0;
			contentScrollMax = 0;
			return;
		}

		PanelMetrics metrics = PanelMetrics.of(layout.right());
		Cursor cursor = new Cursor(metrics.baseY - contentScroll, metrics.baseY);
		int maxContentY = metrics.baseY;

		if (panelViews.showSettingsPlaceholder) {
			addLabel(metrics.sectionX + SECTION_CONTENT_PADDING_X, cursor.drawY, uiText.apply("Settings go here"), Colors.TEXT_MUTED, false);
			int lineStep = font.lineHeight + ROW_SPACING;
			cursor.drawY += lineStep;
			cursor.rawY += lineStep;
			maxContentY = Math.max(maxContentY, cursor.rawY);
		} else {
			for (SectionView view : panelViews.views) {
				renderSection(view, cursor, metrics, font, entryFactory, uiText, addWidget);
				maxContentY = Math.max(maxContentY, cursor.rawY);
			}
		}

		int contentHeight = Math.max(0, maxContentY - metrics.baseY);
		int viewportHeight = Math.max(0, metrics.viewportBottom - metrics.viewportTop);
		contentScrollMax = Math.max(0, contentHeight - viewportHeight);
		contentScroll = clamp(contentScroll, 0, contentScrollMax);
		applyVisibility(metrics.viewportTop, metrics.viewportBottom);
	}

	public void renderSectionBackgrounds(GuiGraphics graphics, Layouts.Layout layout) {
		if (layout == null) {
			return;
		}
		Layouts.Rect right = layout.right();
		int viewTop = right.y() + PANEL_PADDING;
		int viewBottom = right.bottom() - PANEL_PADDING;
		int viewLeft = right.x() + PANEL_PADDING;
		int viewRight = right.right() - PANEL_PADDING;

		for (SectionChrome chrome : sectionChromes) {
			int sectionLeft = Math.max(chrome.x, viewLeft);
			int sectionRight = Math.min(chrome.x + chrome.width, viewRight);
			int sectionTop = Math.max(chrome.y, viewTop);
			int sectionBottom = Math.min(chrome.y + chrome.height, viewBottom);
			if (sectionRight - sectionLeft < 2 || sectionBottom - sectionTop < 2) {
				continue;
			}
			boolean fullyVisible = sectionLeft == chrome.x
					&& sectionRight == chrome.x + chrome.width
					&& sectionTop == chrome.y
					&& sectionBottom == chrome.y + chrome.height;
			int radius = Math.max(4, Theme.SECTION_RADIUS - chrome.level);
			if (fullyVisible) {
				UiShapes.fillRoundedOutline(
						graphics,
						chrome.x,
						chrome.y,
						chrome.width,
						chrome.height,
						radius,
						1,
						outlineColorFor(chrome.level),
						sectionColorFor(chrome.level)
				);
			} else {
				graphics.fill(sectionLeft, sectionTop, sectionRight, sectionBottom, outlineColorFor(chrome.level));
				graphics.fill(sectionLeft + 1, sectionTop + 1, sectionRight - 1, sectionBottom - 1, sectionColorFor(chrome.level));
			}

			int headerTop = Math.max(sectionTop + 1, chrome.y + 1);
			int headerBottom = Math.min(sectionBottom - 1, chrome.y + chrome.headerHeight);
			if (headerBottom > headerTop) {
				if (fullyVisible) {
					UiShapes.fillRoundedRect(
							graphics,
							chrome.x + 1,
							chrome.y + 1,
							chrome.width - 2,
							chrome.headerHeight - 1,
							Math.max(3, radius - 1),
							headerColorFor(chrome.level)
					);
				} else {
					graphics.fill(sectionLeft + 1, headerTop, sectionRight - 1, headerBottom, headerColorFor(chrome.level));
				}
			}

			int headerDividerY = chrome.y + chrome.headerHeight;
			if (headerDividerY >= sectionTop && headerDividerY < sectionBottom) {
				graphics.fill(sectionLeft + 1, headerDividerY, sectionRight - 1, headerDividerY + 1, headerDividerColorFor(chrome.level));
			}

			int dividerLeft = sectionLeft + chrome.contentPaddingX;
			int dividerRight = sectionRight - chrome.contentPaddingX;
			if (dividerRight - dividerLeft < 4) {
				continue;
			}
			for (int dividerY : chrome.rowDividers) {
				if (dividerY <= sectionTop + 1 || dividerY >= sectionBottom - 1) {
					continue;
				}
				graphics.fill(dividerLeft, dividerY, dividerRight, dividerY + 1, Colors.DIVIDER);
				graphics.fill(dividerLeft, dividerY - 1, dividerRight, dividerY, Colors.DIVIDER_GLOW);
			}
		}
	}

	public void renderLabels(GuiGraphics graphics, Font font, Layouts.Layout layout) {
		if (layout == null) {
			return;
		}
		Layouts.Rect right = layout.right();
		int viewTop = right.y() + PANEL_PADDING;
		int viewBottom = right.bottom() - PANEL_PADDING;
		for (Label label : labels) {
			if (label.y + font.lineHeight < viewTop || label.y > viewBottom) {
				continue;
			}
			float scale = label.sectionHeader ? SECTION_TITLE_SCALE : ENTRY_LABEL_SCALE;
			UiTextRenderer.drawBoldString(graphics, font, label.text, label.x, label.y, label.color, scale);
		}
	}

	private void renderSection(SectionView view,
							   Cursor cursor,
							   PanelMetrics metrics,
							   Font font,
							   EntryWidgetFactory entryFactory,
							   Function<String, Component> uiText,
							   Function<AbstractWidget, AbstractWidget> addWidget) {
		int sectionTop = cursor.drawY;
		int sectionContentX = metrics.sectionX + SECTION_CONTENT_PADDING_X;
		int sectionContentWidth = Math.max(0, metrics.sectionWidth - (SECTION_CONTENT_PADDING_X * 2));
		int labelWidth = Math.min(LABEL_WIDTH_LIMIT, (int) (sectionContentWidth * LABEL_WIDTH_RATIO));
		int controlAvailable = Math.max(0, sectionContentWidth - labelWidth - LABEL_CONTROL_GAP);
		int controlWidth = clamp(controlAvailable, CONTROL_MIN_WIDTH, CONTROL_MAX_WIDTH);
		controlWidth = Math.min(controlWidth, controlAvailable);
		int controlX = metrics.sectionX + metrics.sectionWidth - SECTION_CONTENT_PADDING_X - controlWidth;
		int labelX = sectionContentX;
		int rowDividerCount = Math.max(0, view.entries.size() - 1);
		int[] rowDividers = new int[rowDividerCount];
		int dividerIndex = 0;

		addLabel(labelX, sectionTop + SECTION_HEADER_TOP_PADDING, uiText.apply(view.title), sectionTitleColorFor(view.level), true);

		int headerHeight = font.lineHeight + SECTION_HEADER_TOP_PADDING + SECTION_HEADER_BOTTOM_PADDING;
		int headerStep = headerHeight + SECTION_BODY_TOP_PADDING;
		cursor.drawY += headerStep;
		cursor.rawY += headerStep;

		for (int i = 0; i < view.entries.size(); i++) {
			UiEntry<?> entry = view.entries.get(i);
			int rowTop = cursor.drawY;
			int labelY = cursor.drawY + (ROW_HEIGHT - font.lineHeight) / 2;
			addLabel(labelX, labelY, uiText.apply(entry.name()), Colors.TEXT_MUTED, false);
			AbstractWidget widget = entryFactory.create(entry, controlX, cursor.drawY, controlWidth, ROW_HEIGHT);
			if (widget != null) {
				widgets.add(addWidget.apply(widget));
			}
			if (i < view.entries.size() - 1) {
				rowDividers[dividerIndex++] = rowTop + ROW_HEIGHT + (ROW_SPACING / 2);
			}
			int rowStep = ROW_HEIGHT + ROW_SPACING;
			cursor.drawY += rowStep;
			cursor.rawY += rowStep;
		}

		cursor.drawY += SECTION_BODY_BOTTOM_PADDING;
		cursor.rawY += SECTION_BODY_BOTTOM_PADDING;

		int sectionHeight = Math.max(1, cursor.drawY - sectionTop);
		sectionChromes.add(
				new SectionChrome(
						metrics.sectionX,
						sectionTop,
						metrics.sectionWidth,
						sectionHeight,
						headerHeight,
						view.level,
						SECTION_CONTENT_PADDING_X,
						rowDividers
				)
		);

		cursor.drawY += SECTION_SPACING;
		cursor.rawY += SECTION_SPACING;
	}

	private void addLabel(int x, int y, Component text, int color, boolean sectionHeader) {
		labels.add(new Label(x, y, text, color, sectionHeader));
	}

	private void clearWidgets(Consumer<AbstractWidget> removeWidget) {
		for (AbstractWidget widget : widgets) {
			removeWidget.accept(widget);
		}
		widgets.clear();
		labels.clear();
		sectionChromes.clear();
	}

	private void applyVisibility(int viewTop, int viewBottom) {
		for (AbstractWidget widget : widgets) {
			boolean visible = widget.getY() + widget.getHeight() >= viewTop && widget.getY() <= viewBottom;
			widget.visible = visible;
			widget.active = visible;
		}
	}

	private static PanelViews collectPanelViews(List<UiCategory> categories,
												 UiCategory settingsCategory,
												 ListView selectionList,
												 boolean showingSettings,
												 String searchQuery) {
		List<SectionView> views = new ArrayList<>();
		boolean showSettingsPlaceholder = false;

		if (showingSettings) {
			if (settingsCategory != null) {
				for (UiSection section : settingsCategory.sections()) {
					addVisibleSection(views, section, section.name(), 0);
				}
			}
			showSettingsPlaceholder = views.isEmpty();
		} else if (searchQuery != null && !searchQuery.isBlank()) {
			String query = searchQuery.toLowerCase(Locale.ROOT);
			for (UiCategory category : categories) {
				for (UiSection section : category.sections()) {
					List<UiEntry<?>> matches = matchingEntries(section, query);
					if (!matches.isEmpty()) {
						views.add(new SectionView(category.name() + " / " + section.name(), matches, 1));
					}
				}
			}
		} else {
			UiCategory category = currentCategory(categories, selectionList);
			if (category != null) {
				for (UiSection section : category.sections()) {
					addVisibleSection(views, section, section.name(), 0);
				}
			}
		}

		return new PanelViews(views, showSettingsPlaceholder);
	}

	private static void addVisibleSection(List<SectionView> views, UiSection section, String title, int level) {
		List<UiEntry<?>> visibleEntries = visibleEntries(section);
		if (!visibleEntries.isEmpty()) {
			views.add(new SectionView(title, visibleEntries, level));
		}
	}

	private static List<UiEntry<?>> matchingEntries(UiSection section, String query) {
		List<UiEntry<?>> matches = new ArrayList<>();
		for (UiEntry<?> entry : section.entries()) {
			if (entry.isVisible() && matches(entry, query)) {
				matches.add(entry);
			}
		}
		return matches;
	}

	private static UiCategory currentCategory(List<UiCategory> categories, ListView selectionList) {
		if (categories.isEmpty()) {
			return null;
		}
		int index = selectionList.getSelectedIndex();
		if (index < 0 || index >= categories.size()) {
			return null;
		}
		return categories.get(index);
	}

	private static boolean matches(UiEntry<?> entry, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String name = entry.name();
		return name != null && name.toLowerCase(Locale.ROOT).contains(query);
	}

	private static List<UiEntry<?>> visibleEntries(UiSection section) {
		List<UiEntry<?>> visibleEntries = new ArrayList<>();
		for (UiEntry<?> entry : section.entries()) {
			if (entry.isVisible()) {
				visibleEntries.add(entry);
			}
		}
		return visibleEntries;
	}

	private static int clamp(int value, int min, int max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	private static int dim(String token, int fallback) {
		return XmlUiTheme.dimension(token, fallback);
	}

	private static int sectionColorFor(int level) {
		return level > 0 ? Colors.SECTION_BG_NESTED : Colors.SECTION_BG;
	}

	private static int outlineColorFor(int level) {
		return level > 0 ? Colors.SECTION_OUTLINE_NESTED : Colors.SECTION_OUTLINE;
	}

	private static int headerColorFor(int level) {
		return level > 0 ? Colors.SECTION_HEADER_BG_NESTED : Colors.SECTION_HEADER_BG;
	}

	private static int headerDividerColorFor(int level) {
		return level > 0 ? Colors.SECTION_HEADER_DIVIDER_NESTED : Colors.SECTION_HEADER_DIVIDER;
	}

	private static int sectionTitleColorFor(int level) {
		return level > 0 ? Colors.TEXT_SECTION_TITLE_NESTED : Colors.TEXT_SECTION_TITLE;
	}

	private static int inferSectionLevel(String title) {
		if (title == null || title.isBlank()) {
			return 0;
		}
		int level = 0;
		int index = title.indexOf('/');
		while (index >= 0) {
			level++;
			index = title.indexOf('/', index + 1);
		}
		return level;
	}

	private static final class PanelViews {
		private final List<SectionView> views;
		private final boolean showSettingsPlaceholder;

		private PanelViews(List<SectionView> views, boolean showSettingsPlaceholder) {
			this.views = views;
			this.showSettingsPlaceholder = showSettingsPlaceholder;
		}
	}

	private static final class PanelMetrics {
		private final int sectionX;
		private final int baseY;
		private final int sectionWidth;
		private final int viewportTop;
		private final int viewportBottom;

		private PanelMetrics(int sectionX, int baseY, int sectionWidth, int viewportTop, int viewportBottom) {
			this.sectionX = sectionX;
			this.baseY = baseY;
			this.sectionWidth = sectionWidth;
			this.viewportTop = viewportTop;
			this.viewportBottom = viewportBottom;
		}

		private static PanelMetrics of(Layouts.Rect right) {
			int sectionX = right.x() + PANEL_PADDING;
			int baseY = right.y() + PANEL_PADDING;
			int sectionWidth = Math.max(0, right.width() - (PANEL_PADDING * 2));
			int viewportTop = right.y() + PANEL_PADDING;
			int viewportBottom = right.bottom() - PANEL_PADDING;
			return new PanelMetrics(sectionX, baseY, sectionWidth, viewportTop, viewportBottom);
		}
	}

	private static final class Cursor {
		private int drawY;
		private int rawY;

		private Cursor(int drawY, int rawY) {
			this.drawY = drawY;
			this.rawY = rawY;
		}
	}

	private static final class SectionView {
		private final String title;
		private final List<UiEntry<?>> entries;
		private final int level;

		private SectionView(String title, List<UiEntry<?>> entries, int level) {
			this.title = title;
			this.entries = entries;
			this.level = Math.max(Math.max(0, level), inferSectionLevel(title));
		}
	}

	private static final class Label {
		private final int x;
		private final int y;
		private final Component text;
		private final int color;
		private final boolean sectionHeader;

		private Label(int x, int y, Component text, int color, boolean sectionHeader) {
			this.x = x;
			this.y = y;
			this.text = text;
			this.color = color;
			this.sectionHeader = sectionHeader;
		}
	}

	private static final class SectionChrome {
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final int headerHeight;
		private final int level;
		private final int contentPaddingX;
		private final int[] rowDividers;

		private SectionChrome(int x, int y, int width, int height, int headerHeight, int level, int contentPaddingX, int[] rowDividers) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.headerHeight = headerHeight;
			this.level = level;
			this.contentPaddingX = Math.max(0, contentPaddingX);
			this.rowDividers = rowDividers == null ? new int[0] : rowDividers;
		}
	}
}
