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
import com.crussion.moissanite.ui.render.UiTextRenderer;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.widget.ListView;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class OverlayRightPanel {
	private static final int PANEL_PADDING = 8;
	private static final int LABEL_CONTROL_GAP = 8;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int SECTION_SPACING = 6;
	private static final int LABEL_WIDTH_LIMIT = 160;
	private static final float LABEL_WIDTH_RATIO = 0.45f;
	private static final int CONTROL_MIN_WIDTH = 60;

	private final List<AbstractWidget> widgets = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
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
			addLabel(metrics.contentX, cursor.drawY, uiText.apply("Settings go here"), Colors.TEXT_MUTED);
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
			boolean sectionHeader = label.color == Colors.TEXT_MUTED;
			float scale = sectionHeader ? 1.5f : 1.06f;
			int drawX = label.x;
			if (sectionHeader) {
				int textWidth = Math.round(font.width(label.text) * scale);
				drawX = right.x() + (right.width() - textWidth) / 2;
			}
			UiTextRenderer.drawBoldString(graphics, font, label.text, drawX, label.y, label.color, scale);
		}
	}

	private void renderSection(SectionView view,
							   Cursor cursor,
							   PanelMetrics metrics,
							   Font font,
							   EntryWidgetFactory entryFactory,
							   Function<String, Component> uiText,
							   Function<AbstractWidget, AbstractWidget> addWidget) {
		addLabel(metrics.contentX, cursor.drawY, uiText.apply(view.title), Colors.TEXT_MUTED);
		int lineStep = font.lineHeight + ROW_SPACING;
		cursor.drawY += lineStep;
		cursor.rawY += lineStep;

		for (UiEntry<?> entry : view.entries) {
			int labelY = cursor.drawY + (ROW_HEIGHT - font.lineHeight) / 2;
			addLabel(metrics.contentX, labelY, uiText.apply(entry.name()), Colors.TEXT_PRIMARY);
			AbstractWidget widget = entryFactory.create(entry, metrics.controlX, cursor.drawY, metrics.controlWidth, ROW_HEIGHT);
			if (widget != null) {
				widgets.add(addWidget.apply(widget));
			}
			int rowStep = ROW_HEIGHT + ROW_SPACING;
			cursor.drawY += rowStep;
			cursor.rawY += rowStep;
		}

		cursor.drawY += SECTION_SPACING;
		cursor.rawY += SECTION_SPACING;
	}

	private void addLabel(int x, int y, Component text, int color) {
		labels.add(new Label(x, y, text, color));
	}

	private void clearWidgets(Consumer<AbstractWidget> removeWidget) {
		for (AbstractWidget widget : widgets) {
			removeWidget.accept(widget);
		}
		widgets.clear();
		labels.clear();
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
					addVisibleSection(views, section, section.name());
				}
			}
			showSettingsPlaceholder = views.isEmpty();
		} else if (searchQuery != null && !searchQuery.isBlank()) {
			String query = searchQuery.toLowerCase(Locale.ROOT);
			for (UiCategory category : categories) {
				for (UiSection section : category.sections()) {
					List<UiEntry<?>> matches = matchingEntries(section, query);
					if (!matches.isEmpty()) {
						views.add(new SectionView(category.name() + " / " + section.name(), matches));
					}
				}
			}
		} else {
			UiCategory category = currentCategory(categories, selectionList);
			if (category != null) {
				for (UiSection section : category.sections()) {
					addVisibleSection(views, section, section.name());
				}
			}
		}

		return new PanelViews(views, showSettingsPlaceholder);
	}

	private static void addVisibleSection(List<SectionView> views, UiSection section, String title) {
		List<UiEntry<?>> visibleEntries = visibleEntries(section);
		if (!visibleEntries.isEmpty()) {
			views.add(new SectionView(title, visibleEntries));
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
		return Math.max(min, Math.min(max, value));
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
		private final int contentX;
		private final int baseY;
		private final int controlX;
		private final int controlWidth;
		private final int viewportTop;
		private final int viewportBottom;

		private PanelMetrics(int contentX, int baseY, int controlX, int controlWidth, int viewportTop, int viewportBottom) {
			this.contentX = contentX;
			this.baseY = baseY;
			this.controlX = controlX;
			this.controlWidth = controlWidth;
			this.viewportTop = viewportTop;
			this.viewportBottom = viewportBottom;
		}

		private static PanelMetrics of(Layouts.Rect right) {
			int contentX = right.x() + PANEL_PADDING;
			int baseY = right.y() + PANEL_PADDING;
			int contentWidth = right.width() - (PANEL_PADDING * 2);
			int viewportTop = right.y() + PANEL_PADDING;
			int viewportBottom = right.bottom() - PANEL_PADDING;
			int labelWidth = Math.min(LABEL_WIDTH_LIMIT, (int) (contentWidth * LABEL_WIDTH_RATIO));
			int controlX = contentX + labelWidth + LABEL_CONTROL_GAP;
			int controlWidth = Math.max(CONTROL_MIN_WIDTH, contentWidth - labelWidth - LABEL_CONTROL_GAP);
			return new PanelMetrics(contentX, baseY, controlX, controlWidth, viewportTop, viewportBottom);
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

		private SectionView(String title, List<UiEntry<?>> entries) {
			this.title = title;
			this.entries = entries;
		}
	}

	private static final class Label {
		private final int x;
		private final int y;
		private final Component text;
		private final int color;

		private Label(int x, int y, Component text, int color) {
			this.x = x;
			this.y = y;
			this.text = text;
			this.color = color;
		}
	}
}
