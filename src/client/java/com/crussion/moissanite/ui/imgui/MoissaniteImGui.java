package com.crussion.moissanite.ui.imgui;

import com.crussion.moissanite.ui.style.Colors;
import imgui.ImFont;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImString;
import java.util.HashMap;
import java.util.Map;

public final class MoissaniteImGui {
	public static final float FONT_SIZE = 17.0f;
	public static final float TITLE_SIZE = 24.0f;
	public static final float SECTION_TITLE_SIZE = 19.0f;
	public static final float SMALL_TEXT_SIZE = 15.0f;
	private static final int FRAME_COLOR_COUNT = 11;
	private static final int SLIDER_VALUE_BUFFER_SIZE = 32;
	private static final Map<Integer, Float> ANIMATIONS = new HashMap<>();
	private static final Map<Integer, ImString> SLIDER_EDIT_BUFFERS = new HashMap<>();
	private static ImFont regularFont;
	private static ImFont smallFont;
	private static ImFont sectionFont;
	private static ImFont titleFont;

	private MoissaniteImGui() {
	}

	public static int color(int argb) {
		int alpha = (argb >>> 24) & 0xFF;
		int red = (argb >>> 16) & 0xFF;
		int green = (argb >>> 8) & 0xFF;
		int blue = argb & 0xFF;
		return (alpha << 24) | (blue << 16) | (green << 8) | red;
	}

	public static void setFonts(ImFont regular, ImFont small, ImFont section, ImFont title) {
		regularFont = regular;
		smallFont = small;
		sectionFont = section;
		titleFont = title;
	}

	public static void text(ImDrawList draw, String text, float x, float y, int argb) {
		draw.addText(fontForSize(FONT_SIZE), Math.round(FONT_SIZE), x, y, color(argb), text == null ? "" : text);
	}

	public static void text(ImDrawList draw, String text, float x, float y, int argb, float size) {
		draw.addText(fontForSize(size), Math.round(size), x, y, color(argb), text == null ? "" : text);
	}

	public static void clippedText(ImDrawList draw, String text, float x, float y, float width, int argb) {
		draw.addText(fontForSize(FONT_SIZE), Math.round(FONT_SIZE), x, y, color(argb), fitText(text, width));
	}

	public static void clippedText(ImDrawList draw, String text, float x, float y, float width, int argb, float size) {
		draw.addText(fontForSize(size), Math.round(size), x, y, color(argb), fitText(text, width, size));
	}

	public static void panel(ImDrawList draw, float x, float y, float width, float height, float radius) {
		draw.addRectFilled(x, y + 5.0f, x + width, y + height + 5.0f, color(withAlpha(Colors.PANEL_SHADOW, 190)), radius + 2.0f);
		draw.addRectFilled(x, y, x + width, y + height, color(Colors.PANEL_BG), radius);
		draw.addRect(x, y, x + width, y + height, color(Colors.PANEL_OUTLINE), radius, 0, 1.1f);
		draw.addRect(x + 2.0f, y + 2.0f, x + width - 2.0f, y + height - 2.0f, color(withAlpha(Colors.PANEL_HIGHLIGHT, 38)), Math.max(0.0f, radius - 2.0f), 0, 1.0f);
	}

	public static void section(ImDrawList draw, float x, float y, float width, float height, float radius, int background, int outline) {
		draw.addRectFilled(x, y, x + width, y + height, color(background), radius);
		draw.addRect(x, y, x + width, y + height, color(outline), radius, 0, 1.0f);
		draw.addRect(x + 2.0f, y + 2.0f, x + width - 2.0f, y + height - 2.0f, color(withAlpha(Colors.PANEL_HIGHLIGHT, 26)), Math.max(0.0f, radius - 2.0f), 0, 1.0f);
	}

	public static boolean invisibleButton(String id, float x, float y, float width, float height) {
		ImGui.setCursorScreenPos(x, y);
		return ImGui.invisibleButton(id, Math.max(1.0f, width), Math.max(1.0f, height));
	}

	public static boolean iconButton(ImDrawList draw, String id, float x, float y, float size, boolean active) {
		boolean clicked = invisibleButton(id, x, y, size, size);
		boolean hovered = ImGui.isItemHovered();
		float hover = animation(id + "_hover", hovered || active, 18.0f);
		int bg = mixColor(Colors.BUTTON_BG, active ? Colors.LIST_SELECTED : Colors.BUTTON_HOVER, hover);
		int outline = mixColor(Colors.BUTTON_OUTLINE, Colors.BUTTON_OUTLINE_HOVER, hover);
		draw.addRectFilled(x, y, x + size, y + size, color(bg), 8.0f);
		draw.addRect(x, y, x + size, y + size, color(outline), 8.0f, 0, 1.0f);
		drawSettingsIcon(draw, x + size / 2.0f, y + size / 2.0f, Math.max(5.0f, size * 0.28f), hovered || active ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
		return clicked;
	}

	public static boolean menuButton(ImDrawList draw, String id, float x, float y, float size, boolean active) {
		boolean clicked = invisibleButton(id, x, y, size, size);
		boolean hovered = ImGui.isItemHovered();
		float hover = animation(id + "_hover", hovered || active, 18.0f);
		int bg = mixColor(Colors.BUTTON_BG, active ? Colors.LIST_SELECTED : Colors.BUTTON_HOVER, hover);
		int outline = mixColor(Colors.BUTTON_OUTLINE, Colors.BUTTON_OUTLINE_HOVER, hover);
		draw.addRectFilled(x, y, x + size, y + size, color(bg), 9.0f);
		draw.addRect(x, y, x + size, y + size, color(outline), 9.0f, 0, 1.0f);
		int lineColor = color(hovered || active ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
		float lineX = x + 9.0f;
		float lineW = size - 18.0f;
		float centerY = y + size * 0.5f + 0.75f;
		draw.addLine(lineX, centerY - 6.0f, lineX + lineW, centerY - 6.0f, lineColor, 1.8f);
		draw.addLine(lineX, centerY, lineX + lineW, centerY, lineColor, 1.8f);
		draw.addLine(lineX, centerY + 6.0f, lineX + lineW, centerY + 6.0f, lineColor, 1.8f);
		return clicked;
	}

	public static boolean textButton(ImDrawList draw, String id, String label, float x, float y, float width, float height, boolean enabled) {
		if (!enabled) {
			ImGui.beginDisabled(true);
		}
		boolean clicked = invisibleButton(id, x, y, width, height);
		boolean hovered = enabled && ImGui.isItemHovered();
		boolean active = enabled && ImGui.isItemActive();
		if (!enabled) {
			ImGui.endDisabled();
		}

		float hover = animation(id + "_hover", hovered || active, 18.0f);
		int bg = active ? Colors.ACCENT_DIM : mixColor(Colors.BUTTON_BG, Colors.BUTTON_HOVER, hover);
		int outline = mixColor(Colors.BUTTON_OUTLINE, Colors.BUTTON_OUTLINE_HOVER, hover);
		int text = enabled ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED;
		draw.addRectFilled(x, y, x + width, y + height, color(bg), 8.0f);
		draw.addRect(x, y, x + width, y + height, color(outline), 8.0f, 0, 1.0f);
		float textWidth = textWidth(label, FONT_SIZE);
		draw.addText(fontForSize(FONT_SIZE), Math.round(FONT_SIZE), x + Math.max(6.0f, (width - textWidth) / 2.0f), y + Math.max(3.0f, (height - FONT_SIZE) / 2.0f), color(text), label);
		return enabled && clicked;
	}

	public static boolean switchControl(ImDrawList draw, String id, boolean value, float x, float y, float width, float height) {
		boolean clicked = invisibleButton(id, x, y, width, height);
		boolean hovered = ImGui.isItemHovered();
		float valueProgress = animation(id + "_value", value, 18.0f);
		float hover = animation(id + "_hover", hovered, 18.0f);
		float trackWidth = Math.min(width, 48.0f);
		float trackHeight = Math.min(height - 2.0f, 22.0f);
		float trackX = x + width - trackWidth;
		float trackY = y + (height - trackHeight) / 2.0f;
		float radius = trackHeight / 2.0f;
		int trackColor = mixColor(mixColor(Colors.SWITCH_TRACK, Colors.BUTTON_HOVER, hover), Colors.SWITCH_TRACK_ON, valueProgress);
		draw.addRectFilled(trackX, trackY, trackX + trackWidth, trackY + trackHeight, color(trackColor), radius);
		draw.addRect(trackX, trackY, trackX + trackWidth, trackY + trackHeight, color(mixColor(Colors.BUTTON_OUTLINE, Colors.BUTTON_OUTLINE_HOVER, hover)), radius, 0, 1.0f);
		float knobRadius = Math.max(5.0f, trackHeight * 0.34f);
		float knobX = trackX + radius + ((trackWidth - (radius * 2.0f)) * valueProgress);
		float knobY = trackY + radius;
		draw.addCircleFilled(knobX, knobY, knobRadius, color(value ? Colors.SWITCH_DOT_ON : Colors.SWITCH_DOT_OFF), 24);
		draw.addCircleFilled(knobX - knobRadius * 0.25f, knobY - knobRadius * 0.28f, knobRadius * 0.35f, color(Colors.SWITCH_DOT_HIGHLIGHT), 16);
		return clicked;
	}

	public static double sliderControl(ImDrawList draw, String id, double value, double min, double max, double step, float x, float y, float width, float height, String text) {
		float valueBoxWidth = Math.min(76.0f, Math.max(54.0f, width * 0.28f));
		float trackWidth = Math.max(40.0f, width - valueBoxWidth - 12.0f);
		float trackX = x;
		float trackY = y + (height / 2.0f) - 3.0f;
		float valueX = x + width - valueBoxWidth;

		boolean changed = invisibleButton(id + "_track", x, y, trackWidth, height);
		boolean trackHovered = ImGui.isItemHovered();
		boolean trackActive = ImGui.isItemActive();
		if (ImGui.isItemActive() && max > min) {
			float mouseX = ImGui.getIO().getMousePosX();
			double percent = clamp((mouseX - trackX) / trackWidth, 0.0D, 1.0D);
			double next = min + ((max - min) * percent);
			value = snap(next, min, max, step);
			changed = true;
		}

		int editId = ImGui.getID(id + "_value_edit");
		ImString editBuffer = SLIDER_EDIT_BUFFERS.get(editId);
		boolean editing = editBuffer != null;
		boolean valueHovered = isInside(ImGui.getIO().getMousePosX(), ImGui.getIO().getMousePosY(), valueX, y, valueBoxWidth, height);
		boolean startEditing = false;
		if (!editing) {
			boolean valueClicked = invisibleButton(id + "_value", valueX, y, valueBoxWidth, height);
			valueHovered = ImGui.isItemHovered();
			startEditing = valueClicked;
			if (startEditing) {
				editBuffer = new ImString(text == null ? "" : text, SLIDER_VALUE_BUFFER_SIZE);
				SLIDER_EDIT_BUFFERS.put(editId, editBuffer);
				editing = true;
			}
		}

		double percent = max <= min ? 0.0D : clamp((value - min) / (max - min), 0.0D, 1.0D);
		float activeWidth = (float) (trackWidth * percent);
		float knobX = trackX + activeWidth;
		float hover = animation(id + "_hover", trackHovered || trackActive, 18.0f);
		draw.addRectFilled(trackX, trackY, trackX + trackWidth, trackY + 6.0f, color(Colors.SLIDER_TRACK), 4.0f);
		draw.addRectFilled(trackX, trackY, trackX + activeWidth, trackY + 6.0f, color(Colors.SLIDER_ACTIVE), 4.0f);
		draw.addCircleFilled(knobX, trackY + 3.0f, 8.0f + hover * 2.0f, color(withAlpha(Colors.ACCENT_SOFT, Math.round(76.0f * hover))), 24);
		draw.addCircleFilled(knobX, trackY + 3.0f, 7.0f, color(Colors.SLIDER_KNOB), 24);
		draw.addCircle(knobX, trackY + 3.0f, 7.0f, color(Colors.SLIDER_ACTIVE), 24, 1.0f + hover);

		if (editing) {
			ImGui.setCursorScreenPos(valueX, y);
			ImGui.setNextItemWidth(valueBoxWidth);
			pushFrameColors();
			ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 7.0f);
			ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 6.0f, Math.max(2.0f, (height - FONT_SIZE) / 2.0f));
			if (startEditing) {
				ImGui.setKeyboardFocusHere();
			}
			boolean submitted = ImGui.inputText(id + "_value_input", editBuffer,
					ImGuiInputTextFlags.CharsDecimal | ImGuiInputTextFlags.AutoSelectAll | ImGuiInputTextFlags.EnterReturnsTrue);
			boolean deactivated = ImGui.isItemDeactivated();
			ImGui.popStyleVar(2);
			popFrameColors();
			if (submitted || deactivated) {
				SLIDER_EDIT_BUFFERS.remove(editId);
				double parsed = parseSliderEditValue(editBuffer.get());
				if (Double.isFinite(parsed)) {
					value = snap(parsed, min, max, step);
					changed = true;
				}
			}
			return changed ? value : Double.NaN;
		}

		float valueHover = animation(id + "_value_hover", valueHovered, 18.0f);
		draw.addRectFilled(valueX, y, valueX + valueBoxWidth, y + height, color(Colors.SLIDER_VALUE_BG), 7.0f);
		draw.addRect(valueX, y, valueX + valueBoxWidth, y + height, color(mixColor(Colors.SLIDER_VALUE_OUTLINE, Colors.BUTTON_OUTLINE_HOVER, valueHover)), 7.0f, 0, 1.0f);
		String label = fitText(text, valueBoxWidth - 10.0f);
		float textWidth = textWidth(label, FONT_SIZE);
		draw.addText(fontForSize(FONT_SIZE), Math.round(FONT_SIZE), valueX + Math.max(5.0f, (valueBoxWidth - textWidth) / 2.0f), y + Math.max(3.0f, (height - FONT_SIZE) / 2.0f), color(Colors.TEXT_PRIMARY), label);
		return changed ? value : Double.NaN;
	}

	public static void pushFrameColors() {
		pushColor(ImGuiCol.FrameBg, Colors.INPUT_BG);
		pushColor(ImGuiCol.FrameBgHovered, Colors.BUTTON_HOVER);
		pushColor(ImGuiCol.FrameBgActive, Colors.INPUT_OUTLINE_FOCUS);
		pushColor(ImGuiCol.Button, Colors.BUTTON_BG);
		pushColor(ImGuiCol.ButtonHovered, Colors.BUTTON_HOVER);
		pushColor(ImGuiCol.ButtonActive, Colors.ACCENT_DIM);
		pushColor(ImGuiCol.Border, Colors.BUTTON_OUTLINE);
		pushColor(ImGuiCol.PopupBg, Colors.rgba(36, 41, 66, 252));
		pushColor(ImGuiCol.Header, Colors.rgba(154, 127, 238, 150));
		pushColor(ImGuiCol.HeaderHovered, Colors.rgba(81, 91, 132, 244));
		pushColor(ImGuiCol.HeaderActive, Colors.rgba(168, 132, 255, 190));
	}

	public static void popFrameColors() {
		ImGui.popStyleColor(FRAME_COLOR_COUNT);
	}

	public static void pushColor(int target, int argb) {
		float alpha = ((argb >>> 24) & 0xFF) / 255.0f;
		float red = ((argb >>> 16) & 0xFF) / 255.0f;
		float green = ((argb >>> 8) & 0xFF) / 255.0f;
		float blue = (argb & 0xFF) / 255.0f;
		ImGui.pushStyleColor(target, red, green, blue, alpha);
	}

	public static String fitText(String text, float maxWidth) {
		return fitText(text, maxWidth, FONT_SIZE);
	}

	public static String fitText(String text, float maxWidth, float size) {
		if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
			return "";
		}
		if (textWidth(text, size) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		float suffixWidth = textWidth(suffix, size);
		if (suffixWidth >= maxWidth) {
			return "";
		}
		int low = 0;
		int high = text.length();
		while (low < high) {
			int mid = (low + high + 1) / 2;
			String candidate = text.substring(0, mid);
			if (textWidth(candidate, size) + suffixWidth <= maxWidth) {
				low = mid;
			} else {
				high = mid - 1;
			}
		}
		return text.substring(0, Math.max(0, low)) + suffix;
	}

	public static float textWidth(String text, float size) {
		String safeText = text == null ? "" : text;
		ImFont font = fontForSize(size);
		if (font != null) {
			return font.calcTextSizeAX(size, Float.MAX_VALUE, 0.0f, safeText);
		}
		float scale = size / Math.max(1.0f, ImGui.getFontSize());
		return ImGui.calcTextSizeX(safeText) * scale;
	}

	public static boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	public static double snap(double value, double min, double max, double step) {
		double clamped = clamp(value, min, max);
		if (!Double.isFinite(step) || step <= 0.0D) {
			return clamped;
		}
		double snapped = min + (Math.round((clamped - min) / step) * step);
		return clamp(snapped, min, max);
	}

	private static double parseSliderEditValue(String text) {
		if (text == null) {
			return Double.NaN;
		}
		String trimmed = text.trim();
		if (trimmed.isEmpty() || "-".equals(trimmed) || ".".equals(trimmed) || "-.".equals(trimmed)) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(trimmed);
		} catch (NumberFormatException ignored) {
			return Double.NaN;
		}
	}

	public static double clamp(double value, double min, double max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	public static int clamp(int value, int min, int max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	public static float animation(String id, boolean active, float speed) {
		int key = ImGui.getID(id);
		float current = ANIMATIONS.getOrDefault(key, active ? 1.0f : 0.0f);
		float target = active ? 1.0f : 0.0f;
		float delta = Math.max(0.0f, Math.min(1.0f, ImGui.getIO().getDeltaTime() * Math.max(1.0f, speed)));
		float next = current + ((target - current) * delta);
		if (Math.abs(next - target) < 0.001f) {
			next = target;
		}
		ANIMATIONS.put(key, next);
		return easeOut(next);
	}

	public static float easeOut(float value) {
		float clamped = Math.max(0.0f, Math.min(1.0f, value));
		return 1.0f - ((1.0f - clamped) * (1.0f - clamped));
	}

	public static int withAlpha(int argb, int alpha) {
		return (clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
	}

	public static int mixColor(int from, int to, float amount) {
		float t = Math.max(0.0f, Math.min(1.0f, amount));
		int a = Math.round(channel(from, 24) + ((channel(to, 24) - channel(from, 24)) * t));
		int r = Math.round(channel(from, 16) + ((channel(to, 16) - channel(from, 16)) * t));
		int g = Math.round(channel(from, 8) + ((channel(to, 8) - channel(from, 8)) * t));
		int b = Math.round(channel(from, 0) + ((channel(to, 0) - channel(from, 0)) * t));
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static int channel(int argb, int shift) {
		return (argb >>> shift) & 0xFF;
	}

	private static ImFont fontForSize(float size) {
		if (Math.abs(size - TITLE_SIZE) < 0.5f && titleFont != null) {
			return titleFont;
		}
		if (Math.abs(size - SECTION_TITLE_SIZE) < 0.5f && sectionFont != null) {
			return sectionFont;
		}
		if (Math.abs(size - SMALL_TEXT_SIZE) < 0.5f && smallFont != null) {
			return smallFont;
		}
		return regularFont != null ? regularFont : ImGui.getFont();
	}

	private static void drawSettingsIcon(ImDrawList draw, float centerX, float centerY, float radius, int argb) {
		int color = color(argb);
		for (int i = 0; i < 8; i++) {
			double angle = (Math.PI * 2.0D * i) / 8.0D;
			float innerX = centerX + (float) Math.cos(angle) * (radius * 0.72f);
			float innerY = centerY + (float) Math.sin(angle) * (radius * 0.72f);
			float outerX = centerX + (float) Math.cos(angle) * (radius * 1.08f);
			float outerY = centerY + (float) Math.sin(angle) * (radius * 1.08f);
			draw.addLine(innerX, innerY, outerX, outerY, color, 1.4f);
		}
		draw.addCircle(centerX, centerY, radius * 0.72f, color, 24, 1.6f);
		draw.addCircleFilled(centerX, centerY, radius * 0.28f, color, 16);
	}

	public record Rect(float x, float y, float width, float height) {
		public float right() {
			return x + width;
		}

		public float bottom() {
			return y + height;
		}

		public Rect inset(float padding) {
			return new Rect(x + padding, y + padding, width - padding * 2.0f, height - padding * 2.0f);
		}
	}

	public record Layout(Rect outer, Rect left, Rect right, Rect search) {
	}

	public static Layout computeLayout(float screenWidth, float screenHeight) {
		return computeLayout(screenWidth, screenHeight, 1.0f);
	}

	public static Layout computeLayout(float screenWidth, float screenHeight, float navigationOpenProgress) {
		float maxWidth = screenWidth - 24.0f;
		float maxHeight = screenHeight - 24.0f;
		float outerWidth = (float) clamp(Math.min(screenWidth * 0.84f, 1540.0f), 520.0D, maxWidth);
		float outerHeight = (float) clamp(Math.min(screenHeight * 0.80f, 860.0f), 360.0D, maxHeight);
		float outerX = (screenWidth - outerWidth) / 2.0f;
		float outerY = (screenHeight - outerHeight) / 2.0f;
		Rect outer = new Rect(outerX, outerY, outerWidth, outerHeight);

		float outerPadding = 14.0f;
		float searchHeight = 32.0f;
		float searchGap = 12.0f;
		Rect inner = outer.inset(outerPadding);
		float contentHeight = Math.max(0.0f, inner.height() - searchHeight - searchGap);
		float gap = 14.0f;
		float maxLeftWidth = Math.min(278.0f, Math.max(220.0f, inner.width() - gap - 520.0f));
		float expandedLeftWidth = (float) clamp(inner.width() * 0.22f, 220.0D, maxLeftWidth);
		float collapsedLeftWidth = 52.0f;
		float open = Math.max(0.0f, Math.min(1.0f, navigationOpenProgress));
		float leftWidth = collapsedLeftWidth + ((expandedLeftWidth - collapsedLeftWidth) * open);
		float rightWidth = inner.width() - leftWidth - gap;
		Rect left = new Rect(inner.x(), inner.y(), leftWidth, contentHeight);
		Rect right = new Rect(left.right() + gap, inner.y(), rightWidth, contentHeight);
		Rect search = new Rect(inner.x(), inner.y() + contentHeight + searchGap, inner.width(), searchHeight);
		return new Layout(outer, left, right, search);
	}
}
