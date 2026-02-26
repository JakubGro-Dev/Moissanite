package com.crussion.moissanite.features.dungeons.terminals;

import com.crussion.moissanite.definitions.UiDefinitions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.ItemStack;

final class InvwalkVisualizerModule {
	private static final Pattern COLORS_TITLE = Pattern.compile("^Select all the ([\\w ]+) items!$");
	private static final Pattern STARTSWITH_TITLE = Pattern.compile("^What starts with: '(\\w)'\\?$");

	private enum VisualTerminalType {
		NONE,
		NUMBERS,
		COLORS,
		STARTSWITH,
		RUBIX,
		REDGREEN,
		MELODY
	}

	private static final class MelodyVisualizer {
		private int correct = -1;
		private int button = -1;
		private int current = -1;
	}

	private VisualTerminalType activeType = VisualTerminalType.NONE;
	private int windowSize;
	private final Map<Integer, TerminalItem> slots = new HashMap<>();
	private int highestSlotSeen = -1;
	private String extra = "";

	private final List<Integer> numbersSolution = new ArrayList<>();
	private final List<Integer> numbersFullSolution = new ArrayList<>();
	private final List<Integer> genericSolution = new ArrayList<>();
	private final Map<Integer, Integer> rubixSolution = new HashMap<>();
	private final MelodyVisualizer melody = new MelodyVisualizer();

	void onOpenWindow(ClientboundOpenScreenPacket packet) {
		String title = TerminalSupport.stripFormatting(packet.getTitle());

		if (!TerminalSupport.visualizerEnabled()) {
			reset();
			return;
		}

		if (TerminalType.NUMBERS.matches(title) && TerminalSupport.visualizerNumbersEnabled()) {
			setup(VisualTerminalType.NUMBERS, TerminalType.NUMBERS.slotCount() + 1);
			return;
		}

		Matcher colors = COLORS_TITLE.matcher(title);
		if (colors.matches() && TerminalSupport.visualizerColorsEnabled()) {
			setup(VisualTerminalType.COLORS, TerminalType.COLORS.slotCount() + 1);
			extra = colors.group(1).toLowerCase();
			return;
		}

		Matcher startsWith = STARTSWITH_TITLE.matcher(title);
		if (startsWith.matches() && TerminalSupport.visualizerStartsWithEnabled()) {
			setup(VisualTerminalType.STARTSWITH, TerminalType.STARTSWITH.slotCount() + 1);
			extra = startsWith.group(1).toLowerCase();
			return;
		}

		if (TerminalType.RUBIX.matches(title) && TerminalSupport.visualizerRubixEnabled()) {
			setup(VisualTerminalType.RUBIX, TerminalType.RUBIX.slotCount() + 1);
			return;
		}

		if (TerminalType.REDGREEN.matches(title) && TerminalSupport.visualizerRedGreenEnabled()) {
			setup(VisualTerminalType.REDGREEN, TerminalType.REDGREEN.slotCount() + 1);
			return;
		}

		if (TerminalType.MELODY.matches(title) && TerminalSupport.visualizerMelodyEnabled()) {
			setup(VisualTerminalType.MELODY, TerminalType.MELODY.slotCount() + 1);
			return;
		}

		reset();
	}

	void onCloseWindow() {
		reset();
	}

	void onSetSlot(ClientboundContainerSetSlotPacket packet) {
		if (activeType == VisualTerminalType.NONE) {
			return;
		}

		int slot = packet.getSlot();
		if (slot < 0 || slot >= windowSize) {
			return;
		}

		ItemStack stack = packet.getItem();
		if (stack != null && !stack.isEmpty()) {
			slots.put(slot, TerminalItem.from(packet.getContainerId(), slot, stack));
		} else {
			slots.remove(slot);
		}
		highestSlotSeen = Math.max(highestSlotSeen, slot);

		if (activeType == VisualTerminalType.MELODY) {
			updateMelody(slot);
			return;
		}

		if (activeType == VisualTerminalType.RUBIX) {
			if (highestSlotSeen + 1 >= windowSize && slot == windowSize - 1) {
				solveRubix();
			}
			return;
		}

		if (highestSlotSeen + 1 >= windowSize) {
			solveCurrent();
		}
	}

	void onTick(Minecraft client) {
		if (!TerminalSupport.visualizerEnabled()) {
			reset();
		}
	}

	void onWorldReset() {
		reset();
	}

	void renderOverlay(GuiGraphics graphics) {
		if (!TerminalSupport.visualizerEnabled() || activeType == VisualTerminalType.NONE || windowSize <= 0) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null || client.getWindow() == null || graphics == null) {
			return;
		}

		float scale = TerminalSupport.visualizerScale();
		int screenWidth = Math.round(client.getWindow().getGuiScaledWidth() / scale);
		int screenHeight = Math.round(client.getWindow().getGuiScaledHeight() / scale);
		int width = 9 * 18;
		int height = Math.max(18, (windowSize / 9) * 18);
		int globalOffsetX = TerminalSupport.parseInt(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_OFFSET_X.get(), 0);
		int globalOffsetY = TerminalSupport.parseInt(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_OFFSET_Y.get(), 0);
		int offsetX = screenWidth / 2 - width / 2 + globalOffsetX + 1;
		int offsetY = screenHeight / 2 - height / 2 + globalOffsetY;

		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.fill(offsetX - 2, offsetY - 2, offsetX + width + 2, offsetY + height + 2, TerminalSupport.visualizerBackgroundColor());

		switch (activeType) {
			case NUMBERS -> renderNumbers(graphics, client, offsetX, offsetY);
			case COLORS, STARTSWITH, REDGREEN -> renderGenericSolution(graphics, offsetX, offsetY);
			case RUBIX -> renderRubix(graphics, client, offsetX, offsetY);
			case MELODY -> renderMelody(graphics, offsetX, offsetY);
			default -> {
			}
		}

		graphics.pose().popMatrix();
	}

	private void setup(VisualTerminalType type, int terminalWindowSize) {
		activeType = type;
		windowSize = terminalWindowSize;
		slots.clear();
		highestSlotSeen = -1;
		extra = "";
		numbersSolution.clear();
		numbersFullSolution.clear();
		genericSolution.clear();
		rubixSolution.clear();
		melody.correct = -1;
		melody.button = -1;
		melody.current = -1;
	}

	private void solveCurrent() {
		switch (activeType) {
			case NUMBERS -> solveNumbers();
			case COLORS -> solveColors();
			case STARTSWITH -> solveStartsWith();
			case RUBIX -> solveRubix();
			case REDGREEN -> solveRedGreen();
			default -> {
			}
		}
	}

	private void solveNumbers() {
		numbersSolution.clear();
		numbersFullSolution.clear();

		slots.values().stream()
				.filter(item -> TerminalSupport.contains(TerminalSupport.NUMBERS_ALLOWED_SLOTS, item.slot()))
				.filter(item -> item.id() == 160 && item.meta() == 14)
				.sorted(Comparator.comparingInt(TerminalItem::size))
				.map(TerminalItem::slot)
				.forEach(numbersSolution::add);

		slots.values().stream()
				.filter(item -> TerminalSupport.contains(TerminalSupport.NUMBERS_ALLOWED_SLOTS, item.slot()))
				.filter(item -> item.id() == 160)
				.sorted(Comparator.comparingInt(TerminalItem::size))
				.map(TerminalItem::slot)
				.forEach(numbersFullSolution::add);
	}

	private void solveColors() {
		genericSolution.clear();
		slots.values().stream()
				.filter(item -> TerminalSupport.contains(TerminalSupport.COLORS_ALLOWED_SLOTS, item.slot()))
				.filter(item -> !item.enchanted())
				.filter(item -> TerminalSupport.fixColorItemName(item.name().toLowerCase()).startsWith(extra))
				.map(TerminalItem::slot)
				.forEach(genericSolution::add);
	}

	private void solveStartsWith() {
		genericSolution.clear();
		slots.values().stream()
				.filter(item -> TerminalSupport.contains(TerminalSupport.COLORS_ALLOWED_SLOTS, item.slot()))
				.filter(item -> !item.enchanted())
				.filter(item -> item.name().toLowerCase().startsWith(extra))
				.map(TerminalItem::slot)
				.forEach(genericSolution::add);
	}

	private void solveRubix() {
		rubixSolution.clear();
		int[] clicks = new int[] {0, 0, 0, 0, 0};

		for (int i = 0; i < 5; i++) {
			for (TerminalItem item : slots.values()) {
				if (!TerminalSupport.contains(TerminalSupport.RUBIX_ALLOWED_SLOTS, item.slot())) {
					continue;
				}
				if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(i)]) {
					continue;
				}
				if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(i - 2)]) {
					clicks[i] += 2;
				} else if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(i - 1)]) {
					clicks[i] += 1;
				} else if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(i + 1)]) {
					clicks[i] += 1;
				} else if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(i + 2)]) {
					clicks[i] += 2;
				}
			}
		}

		int origin = 0;
		int min = clicks[0];
		for (int i = 1; i < clicks.length; i++) {
			if (clicks[i] < min) {
				min = clicks[i];
				origin = i;
			}
		}

		for (TerminalItem item : slots.values()) {
			if (!TerminalSupport.contains(TerminalSupport.RUBIX_ALLOWED_SLOTS, item.slot())) {
				continue;
			}
			if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(origin)]) {
				continue;
			}
			if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(origin - 2)]) {
				rubixSolution.put(item.slot(), 2);
			} else if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(origin - 1)]) {
				rubixSolution.put(item.slot(), 1);
			} else if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(origin + 1)]) {
				rubixSolution.put(item.slot(), -1);
			} else if (item.meta() == TerminalSupport.COLOR_ORDER[TerminalSupport.calcIndex(origin + 2)]) {
				rubixSolution.put(item.slot(), -2);
			}
		}
	}

	private void solveRedGreen() {
		genericSolution.clear();
		slots.values().stream()
				.filter(item -> TerminalSupport.contains(TerminalSupport.REDGREEN_ALLOWED_SLOTS, item.slot()))
				.filter(item -> item.id() == 160 && item.meta() == 14)
				.map(TerminalItem::slot)
				.forEach(genericSolution::add);
	}

	private void updateMelody(int slot) {
		TerminalItem item = slots.get(slot);
		if (item == null || item.id() != 160 || item.meta() != 5) {
			return;
		}
		int correct = slots.values().stream()
				.filter(it -> it.id() == 160 && it.meta() == 2)
				.mapToInt(TerminalItem::slot)
				.findFirst()
				.orElse(0) - 1;
		int button = Math.floorDiv(slot, 9) - 1;
		int current = slot % 9 - 1;
		melody.correct = correct;
		melody.button = button;
		melody.current = current;
	}

	private void renderNumbers(GuiGraphics graphics, Minecraft client, int offsetX, int offsetY) {
		for (int i = 0; i < numbersSolution.size() && i < 3; i++) {
			int slot = numbersSolution.get(i);
			int x = slot % 9 * 18 + offsetX;
			int y = Math.floorDiv(slot, 9) * 18 + offsetY;
			graphics.fill(x, y, x + 16, y + 16, TerminalSupport.visualizerNumbersColor(i + 1));
		}

		if (!Boolean.TRUE.equals(UiDefinitions.DUNGEONS_INVWALK_VISUALIZER_NUMBERS_SHOW_NUMBERS.get())) {
			return;
		}
		for (int i = 0; i < numbersFullSolution.size(); i++) {
			int slot = numbersFullSolution.get(i);
			int x = slot % 9 * 18 + offsetX;
			int y = Math.floorDiv(slot, 9) * 18 + offsetY;
			String text = Integer.toString(i + 1);
			graphics.drawString(client.font, text, x + (16 - client.font.width(text)) / 2, y + 4, 0xFFFFFFFF, true);
		}
	}

	private void renderGenericSolution(GuiGraphics graphics, int offsetX, int offsetY) {
		int color = TerminalSupport.visualizerMainColor();
		for (int slot : genericSolution) {
			int x = slot % 9 * 18 + offsetX;
			int y = Math.floorDiv(slot, 9) * 18 + offsetY;
			graphics.fill(x, y, x + 16, y + 16, color);
		}
	}

	private void renderRubix(GuiGraphics graphics, Minecraft client, int offsetX, int offsetY) {
		for (Map.Entry<Integer, Integer> entry : rubixSolution.entrySet()) {
			int slot = entry.getKey();
			int value = entry.getValue();
			int x = slot % 9 * 18 + offsetX;
			int y = Math.floorDiv(slot, 9) * 18 + offsetY;
			graphics.fill(x, y, x + 16, y + 16, value > 0 ? TerminalSupport.visualizerRubixLeftColor() : TerminalSupport.visualizerRubixRightColor());
			String text = Integer.toString(value);
			graphics.drawString(client.font, text, x + (16 - client.font.width(text)) / 2, y + 4, 0xFFFFFFFF, true);
		}
	}

	private void renderMelody(GuiGraphics graphics, int offsetX, int offsetY) {
		graphics.fill(offsetX + (melody.correct + 1) * 18, offsetY + 18, offsetX + (melody.correct + 1) * 18 + 16,
				offsetY + 18 + 70, TerminalSupport.visualizerMelodyColumnColor());

		int buttonSlot = melody.button * 9 + 16;
		int currentSlot = melody.button * 9 + 10 + melody.current;
		for (int slot = 0; slot < windowSize; slot++) {
			int x = slot % 9 * 18 + offsetX;
			int y = Math.floorDiv(slot, 9) * 18 + offsetY;
			if (slot == buttonSlot) {
				graphics.fill(x, y, x + 16, y + 16, TerminalSupport.visualizerMelodyCorrectButtonColor());
			} else if (TerminalSupport.contains(TerminalSupport.MELODY_WRONG_BUTTON_SLOTS, slot)) {
				graphics.fill(x, y, x + 16, y + 16, TerminalSupport.visualizerMelodyIncorrectButtonColor());
			} else if (slot == currentSlot) {
				graphics.fill(x, y, x + 16, y + 16, TerminalSupport.visualizerMelodySlotColor());
			}
		}
	}

	private void reset() {
		activeType = VisualTerminalType.NONE;
		windowSize = 0;
		slots.clear();
		highestSlotSeen = -1;
		extra = "";
		numbersSolution.clear();
		numbersFullSolution.clear();
		genericSolution.clear();
		rubixSolution.clear();
		melody.correct = -1;
		melody.button = -1;
		melody.current = -1;
	}
}
