package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.ui.screen.MoveElementScreen;
import com.crussion.moissanite.util.kuudra.KuudraPhase;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

public final class KuudraSplits {
	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath("moissanite", "kuudra_splits_overlay");

	private record Position(int x, int y) {
	}

	private record DrawState(List<String> lines, int width, int height) {
	}

	private static final MoveElementScreen.MovableElementAdapter<DrawState> MOVE_ADAPTER = new MoveElementScreen.MovableElementAdapter<>() {
		@Override
		public Component title() {
			return Component.literal("Move Kuudra Splits");
		}

		@Override
		public Component unavailableMessage() {
			return Component.literal("Unable to draw Kuudra Splits preview");
		}

		@Override
		public DrawState getDrawState(int screenWidth, int screenHeight) {
			return generateDrawState();
		}

		@Override
		public MoveElementScreen.Position getVisiblePosition(DrawState drawState, int screenWidth, int screenHeight, boolean persistIfAdjusted) {
			Position position = clampConfiguredPosition(drawState, screenWidth, screenHeight, persistIfAdjusted);
			return new MoveElementScreen.Position(position.x(), position.y());
		}

		@Override
		public MoveElementScreen.Position clampPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
			Position position = clampPositionToScreen(drawState, screenWidth, screenHeight, x, y);
			return new MoveElementScreen.Position(position.x(), position.y());
		}

		@Override
		public int drawWidth(DrawState drawState) {
			return drawState.width();
		}

		@Override
		public int drawHeight(DrawState drawState) {
			return drawState.height();
		}

		@Override
		public void draw(GuiGraphics graphics, DrawState drawState, int x, int y) {
			drawOverlay(graphics, drawState, x, y);
		}

		@Override
		public void applyDraggedPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
			setConfiguredPosition(x, y);
		}

		@Override
		public List<String> infoLines(DrawState drawState, int x, int y) {
			int configuredX = (int) Math.round(getConfiguredX());
			int configuredY = (int) Math.round(getConfiguredY());
			return List.of("x " + configuredX, "y " + configuredY);
		}

		@Override
		public void onScreenClosed() {
			moveModeActive = false;
		}
	};

	private static boolean initialized;
	private static boolean moveModeActive;

	private KuudraSplits() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		HudElementRegistry.attachElementBefore(VanillaHudElements.SUBTITLES, HUD_ELEMENT_ID, (graphics, tickCounter) -> {
			if (!Boolean.TRUE.equals(UiDefinitions.KUUDRA_SPLITS.get()) || moveModeActive) {
				return;
			}
			if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			if (client == null || client.font == null || client.getWindow() == null) {
				return;
			}

			int screenWidth = client.getWindow().getGuiScaledWidth();
			int screenHeight = client.getWindow().getGuiScaledHeight();
			DrawState drawState = generateDrawState();
			Position position = clampConfiguredPosition(drawState, screenWidth, screenHeight, true);
			drawOverlay(graphics, drawState, position.x(), position.y());
		});
	}

	public static void openMoveScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		Screen returnScreen = client.screen;
		moveModeActive = true;
		client.setScreen(new MoveElementScreen<>(returnScreen, MOVE_ADAPTER));
	}

	private static DrawState generateDrawState() {
		long now = System.currentTimeMillis();
		List<String> lines = List.of(
				ChatFormatting.GREEN + "Supplies: " + getSplitColor(KuudraPhase.SUPPLIES.getTime(now), KuudraPhase.SUPPLIES)
						+ formatTime(KuudraPhase.SUPPLIES.getTime(now)),
				ChatFormatting.YELLOW + "Build: " + getSplitColor(KuudraPhase.BUILD.getTime(now), KuudraPhase.BUILD)
						+ formatTime(KuudraPhase.BUILD.getTime(now)),
				ChatFormatting.RED + "Eaten: " + getSplitColor(KuudraPhase.EATEN.getTime(now), KuudraPhase.EATEN)
						+ formatTime(KuudraPhase.EATEN.getTime(now)),
				ChatFormatting.LIGHT_PURPLE + "Stun: " + getSplitColor(KuudraPhase.STUN.getTime(now), KuudraPhase.STUN)
						+ formatTime(KuudraPhase.STUN.getTime(now)),
				ChatFormatting.GOLD + "DPS: " + getSplitColor(KuudraPhase.DPS.getTime(now), KuudraPhase.DPS)
						+ formatTime(KuudraPhase.DPS.getTime(now)),
				ChatFormatting.AQUA + "Skip: " + getSplitColor(KuudraPhase.SKIP.getTime(now), KuudraPhase.SKIP)
						+ formatTime(KuudraPhase.SKIP.getTime(now)),
				ChatFormatting.DARK_RED + "Kuudra: " + getSplitColor(KuudraPhase.KILL.getTime(now), KuudraPhase.KILL)
						+ formatTime(KuudraPhase.KILL.getTime(now)),
				ChatFormatting.WHITE + "Overall: " + getSplitColor(KuudraPhase.END.getTime(now), KuudraPhase.END)
						+ formatTime(KuudraPhase.END.getTime(now)));

		Minecraft client = Minecraft.getInstance();
		int maxWidth = 0;
		if (client != null && client.font != null) {
			for (String line : lines) {
				int width = client.font.width(line);
				if (width > maxWidth) {
					maxWidth = width;
				}
			}
		} else {
			maxWidth = 100;
		}

		int height = lines.size() * 9;
		return new DrawState(lines, maxWidth, height);
	}

	private static String getSplitColor(long timeMs, KuudraPhase phase) {
		if (timeMs == 0) {
			return ChatFormatting.WHITE.toString();
		}

		long greenThreshold;
		long goldThreshold;
		switch (phase) {
			case SUPPLIES -> {
				greenThreshold = 29_500;
				goldThreshold = 32_000;
			}
			case BUILD -> {
				greenThreshold = 16_000;
				goldThreshold = 19_000;
			}
			case EATEN -> {
				greenThreshold = 5_800;
				goldThreshold = 6_200;
			}
			case STUN -> {
				greenThreshold = 200;
				goldThreshold = 500;
			}
			case DPS -> {
				greenThreshold = 5_700;
				goldThreshold = 6_300;
			}
			case SKIP -> {
				greenThreshold = 4_200;
				goldThreshold = 5_000;
			}
			case KILL -> {
				greenThreshold = 2_200;
				goldThreshold = 2_500;
			}
			case END -> {
				greenThreshold = 65_000;
				goldThreshold = 75_000;
			}
			default -> {
				return ChatFormatting.WHITE.toString();
			}
		}

		if (timeMs <= greenThreshold) {
			return ChatFormatting.GREEN.toString();
		}
		if (timeMs <= goldThreshold) {
			return ChatFormatting.GOLD.toString();
		}
		return ChatFormatting.RED.toString();
	}

	private static String formatTime(long timeMs) {
		long seconds = timeMs / 1000;
		long centiseconds = (timeMs % 1000) / 10;
		return String.format("%d.%02ds", seconds, centiseconds);
	}

	private static void drawOverlay(GuiGraphics graphics, DrawState drawState, int x, int y) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return;
		}

		int currentY = y;
		for (String line : drawState.lines()) {
			graphics.drawString(client.font, line, x, currentY, 0xFFFFFFFF, true);
			currentY += 9;
		}
	}

	private static Position clampConfiguredPosition(DrawState drawState, int screenWidth, int screenHeight, boolean persistIfAdjusted) {
		int configuredX = (int) Math.round(getConfiguredX());
		int configuredY = (int) Math.round(getConfiguredY());
		Position clamped = clampPositionToScreen(drawState, screenWidth, screenHeight, configuredX, configuredY);
		if (persistIfAdjusted && (clamped.x() != configuredX || clamped.y() != configuredY)) {
			setConfiguredPosition(clamped.x(), clamped.y());
		}
		return clamped;
	}

	private static Position clampPositionToScreen(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
		int minX = 0;
		int maxX = Math.max(0, screenWidth - drawState.width());
		int minY = 0;
		int maxY = Math.max(0, screenHeight - drawState.height());
		return new Position(Mth.clamp(x, minX, maxX), Mth.clamp(y, minY, maxY));
	}

	private static double getConfiguredX() {
		Double val = UiDefinitions.KUUDRA_SPLITS_X.get();
		return val != null && Double.isFinite(val) ? Mth.clamp(val, -10000.0, 10000.0) : 10.0;
	}

	private static double getConfiguredY() {
		Double val = UiDefinitions.KUUDRA_SPLITS_Y.get();
		return val != null && Double.isFinite(val) ? Mth.clamp(val, -10000.0, 10000.0) : 10.0;
	}

	private static void setConfiguredPosition(double x, double y) {
		UiDefinitions.KUUDRA_SPLITS_X.set(Mth.clamp(x, -10000.0, 10000.0));
		UiDefinitions.KUUDRA_SPLITS_Y.set(Mth.clamp(y, -10000.0, 10000.0));
	}
}
