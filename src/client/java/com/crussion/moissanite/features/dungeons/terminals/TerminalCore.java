package com.crussion.moissanite.features.dungeons.terminals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.ItemStack;

final class TerminalCore {
	private static final Pattern COLORS_TITLE = Pattern.compile("^Select all the (.+?) items!$");
	private static final Pattern STARTSWITH_LETTER = Pattern.compile("^What starts with: '(\\w+)'\\??$");

	private boolean inTerminal;
	private final List<TerminalItem> currentItems = new ArrayList<>();
	private boolean shouldSolve;
	private int terminalId = -1;
	private int maxSlot = 999;
	private String currentTitle = "";
	private int solutionLength = -1;
	private int lastWindowId = -1;

	boolean onOpenWindow(ClientboundOpenScreenPacket packet) {
		String title = TerminalSupport.stripFormatting(packet.getTitle());
		currentTitle = title;

		TerminalType type = TerminalType.match(title);
		if (type != null) {
			terminalId = type.id();
			maxSlot = type.slotCount();
			inTerminal = true;
			currentItems.clear();
			lastWindowId = packet.getContainerId();
			shouldSolve = false;
			solutionLength = -1;
			return TerminalSupport.autoTermsInvwalkEnabled() && terminalId != TerminalType.MELODY.id();
		}

		inTerminal = false;
		reloadTerminal();
		return false;
	}

	boolean onSetSlot(ClientboundContainerSetSlotPacket packet) {
		if (!inTerminal || shouldSolve) {
			return false;
		}

		ItemStack stack = packet.getItem();
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		int slot = packet.getSlot();
		if (slot < 0) {
			return false;
		}

		boolean cancelPacket = TerminalSupport.autoTermsInvwalkEnabled() && terminalId != TerminalType.MELODY.id();
		if (cancelPacket) {
			Minecraft client = Minecraft.getInstance();
			if (client != null && client.screen instanceof AbstractContainerScreen<?>) {
				client.setScreen(null);
			}
		}

		int windowId = packet.getContainerId();
		if (windowId != lastWindowId) {
			return false;
		}

		if (slot > maxSlot) {
			shouldSolve = true;
			return false;
		}

		currentItems.add(TerminalItem.from(windowId, slot, stack));
		return cancelPacket;
	}

	void onCloseWindow() {
		if (!inTerminal) {
			return;
		}
		inTerminal = false;
		reloadTerminal();
	}

	void onWorldReset() {
		inTerminal = false;
		reloadTerminal();
	}

	List<TerminalClickAction> getSolution() {
		if (!shouldSolve) {
			return null;
		}

		List<TerminalClickAction> solution = new ArrayList<>();
		switch (terminalId) {
			case 0 -> currentItems.stream()
					.filter(item -> item.meta() == 14)
					.sorted(Comparator.comparingInt(TerminalItem::size))
					.map(item -> new TerminalClickAction(item.windowId(), item.slot(), 0))
					.forEach(solution::add);
			case 1 -> solveColors(solution);
			case 2 -> solveStartsWith(solution);
			case 3 -> solveRubix(solution);
			case 4 -> currentItems.stream()
					.filter(item -> item.meta() == 14)
					.map(item -> new TerminalClickAction(item.windowId(), item.slot(), 0))
					.forEach(solution::add);
			default -> {
			}
		}

		solutionLength = solution.size();
		return solution;
	}

	boolean isInTerminal() {
		return inTerminal;
	}

	boolean isInvwalkAutoTermsActive() {
		return inTerminal
				&& terminalId != TerminalType.MELODY.id()
				&& TerminalSupport.autoTermsEnabled()
				&& TerminalSupport.autoTermsInvwalkEnabled();
	}

	int getLastWindowId() {
		return lastWindowId;
	}

	void renderOverlay(GuiGraphics graphics) {
		if (!TerminalSupport.autoTermsInvwalkEnabled() || !inTerminal || terminalId == TerminalType.MELODY.id()) {
			return;
		}

		String inTerminalText = ChatFormatting.DARK_AQUA + "In Terminal";
		TerminalRenderSupport.drawCenteredScaledText(graphics, inTerminalText, 1.5F, 10);
		if (solutionLength < 0) {
			return;
		}

		String clicksRemainingText = ChatFormatting.DARK_AQUA + "Clicks Remaining: " + ChatFormatting.GREEN + solutionLength;
		TerminalRenderSupport.drawCenteredScaledText(graphics, clicksRemainingText, 1.5F, 20);
	}

	private void solveColors(List<TerminalClickAction> solution) {
		Matcher match = COLORS_TITLE.matcher(currentTitle);
		if (!match.matches()) {
			return;
		}
		String color = match.group(1).toLowerCase(Locale.ROOT);
		currentItems.stream()
				.filter(item -> !item.enchanted())
				.filter(item -> TerminalSupport.fixColorItemName(item.name().toLowerCase(Locale.ROOT)).startsWith(color))
				.map(item -> new TerminalClickAction(item.windowId(), item.slot(), 0))
				.forEach(solution::add);
	}

	private void solveStartsWith(List<TerminalClickAction> solution) {
		Matcher match = STARTSWITH_LETTER.matcher(currentTitle);
		if (!match.matches()) {
			return;
		}
		String letter = match.group(1).toLowerCase(Locale.ROOT);
		currentItems.stream()
				.filter(item -> !item.enchanted())
				.filter(item -> item.name().toLowerCase(Locale.ROOT).startsWith(letter))
				.map(item -> new TerminalClickAction(item.windowId(), item.slot(), 0))
				.forEach(solution::add);
	}

	private void solveRubix(List<TerminalClickAction> solution) {
		List<TerminalItem> rubixItems = currentItems.stream()
				.filter(item -> item.id() == 160 && item.meta() != 15)
				.toList();

		int minIndex = -1;
		int minTotalClicks = Integer.MAX_VALUE;
		for (int targetIndex = 0; targetIndex < TerminalSupport.COLOR_ORDER.length; targetIndex++) {
			int totalClicks = 0;
			for (TerminalItem item : rubixItems) {
				int currentIndex = TerminalSupport.colorIndex(item.meta());
				if (currentIndex == -1) {
					continue;
				}
				int clockwise = Math.floorMod(targetIndex - currentIndex, TerminalSupport.COLOR_ORDER.length);
				int counterclockwise = Math.floorMod(currentIndex - targetIndex, TerminalSupport.COLOR_ORDER.length);
				totalClicks += Math.min(clockwise, counterclockwise);
			}
			if (totalClicks < minTotalClicks) {
				minTotalClicks = totalClicks;
				minIndex = targetIndex;
			}
		}

		for (TerminalItem item : rubixItems) {
			int currentIndex = TerminalSupport.colorIndex(item.meta());
			if (currentIndex == -1 || minIndex == -1) {
				continue;
			}

			int clockwise = Math.floorMod(minIndex - currentIndex, TerminalSupport.COLOR_ORDER.length);
			int counterclockwise = Math.floorMod(currentIndex - minIndex, TerminalSupport.COLOR_ORDER.length);
			if (clockwise <= counterclockwise) {
				for (int i = 0; i < clockwise; i++) {
					solution.add(new TerminalClickAction(item.windowId(), item.slot(), 0));
				}
			} else {
				for (int i = 0; i < counterclockwise; i++) {
					solution.add(new TerminalClickAction(item.windowId(), item.slot(), 1));
				}
			}
		}
	}

	private void reloadTerminal() {
		currentItems.clear();
		shouldSolve = false;
		terminalId = -1;
		maxSlot = 999;
		solutionLength = -1;
		currentTitle = "";
		lastWindowId = -1;
	}
}
