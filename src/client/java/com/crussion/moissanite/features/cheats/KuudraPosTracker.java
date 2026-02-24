package com.crussion.moissanite.features.cheats;

import java.util.List;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.ui.screen.MoveElementScreen;
import com.crussion.moissanite.util.kuudra.KuudraEntityFinder;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.MagmaCube;

public final class KuudraPosTracker {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final double MAX_TRACKING_Y = 20.0D;
	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath("moissanite", "kuudra_pos_tracker");
	private static final float HUD_SCALE = 4.0f;
	private static final double HUD_OFFSET_MIN = -10000.0D;
	private static final double HUD_OFFSET_MAX = 10000.0D;
	private static final double DEFAULT_OFFSET_X = 0.0D;
	private static final double DEFAULT_OFFSET_Y = 20.0D;
	private static final double BACK_DOT_THRESHOLD = Math.cos(Math.toRadians(140.0D));
	private static final double FRONT_DOT_THRESHOLD = Math.cos(Math.toRadians(26.0D));
	private static final String PREVIEW_SYMBOL = ">";
	private static final int PREVIEW_COLOR = 0xFFFFFFFF;
	private static final MoveElementScreen.MovableElementAdapter<DrawState> MOVE_ADAPTER = new MoveElementScreen.MovableElementAdapter<>() {
		@Override
		public Component title() {
			return Component.literal("Move Kuudra Pos Tracker");
		}

		@Override
		public Component unavailableMessage() {
			return Component.literal("Unable to draw tracker preview");
		}

		@Override
		public DrawState getDrawState(int screenWidth, int screenHeight) {
			return KuudraPosTracker.getMoveDrawState();
		}

		@Override
		public MoveElementScreen.Position getVisiblePosition(DrawState drawState, int screenWidth, int screenHeight, boolean persistIfAdjusted) {
			Position position = KuudraPosTracker.getVisibleConfiguredPosition(drawState, screenWidth, screenHeight, persistIfAdjusted);
			return new MoveElementScreen.Position(position.x(), position.y());
		}

		@Override
		public MoveElementScreen.Position clampPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
			Position position = KuudraPosTracker.clampPosition(drawState, screenWidth, screenHeight, x, y);
			return new MoveElementScreen.Position(position.x(), position.y());
		}

		@Override
		public int drawWidth(DrawState drawState) {
			return drawState.drawWidth();
		}

		@Override
		public int drawHeight(DrawState drawState) {
			return drawState.drawHeight();
		}

		@Override
		public void draw(GuiGraphics graphics, DrawState drawState, int x, int y) {
			KuudraPosTracker.drawSymbol(graphics, drawState, x, y);
		}

		@Override
		public void applyDraggedPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
			double centerX = screenWidth / 2.0D;
			double centerY = screenHeight / 2.0D;
			KuudraPosTracker.setConfiguredOffsets(x - centerX, y - centerY);
		}

		@Override
		public List<String> infoLines(DrawState drawState, int x, int y) {
			int offsetX = (int) Math.round(KuudraPosTracker.getConfiguredOffsetX());
			int offsetY = (int) Math.round(KuudraPosTracker.getConfiguredOffsetY());
			return List.of("offset x " + offsetX, "offset y " + offsetY);
		}

		@Override
		public void onScreenClosed() {
			KuudraPosTracker.setMoveModeActive(false);
		}
	};
	private static boolean initialized;
	private static boolean moveModeActive;
	private static Direction direction = Direction.NONE;

	private KuudraPosTracker() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(KuudraPosTracker::handleClientTick);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				HUD_ELEMENT_ID,
				(graphics, tickCounter) -> renderOverlay(graphics));
	}

	public static void openMoveScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		Screen returnScreen = client.screen;
		setMoveModeActive(true);
		client.setScreen(new MoveElementScreen<>(returnScreen, MOVE_ADAPTER));
	}

	public static void setMoveModeActive(boolean moveModeActive) {
		KuudraPosTracker.moveModeActive = moveModeActive;
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			direction = Direction.NONE;
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.KUUDRA_POS_TRACKER.get())) {
			direction = Direction.NONE;
			return;
		}
		if (!canTrackInCurrentContext(client)) {
			direction = Direction.NONE;
			return;
		}

		MagmaCube kuudra = KuudraEntityFinder.findKuudra(client);
		if (kuudra == null) {
			direction = Direction.NONE;
			return;
		}

		double dx = kuudra.getX() - client.player.getX();
		double dz = kuudra.getZ() - client.player.getZ();
		if (Math.abs(dx) < 1.0e-4D && Math.abs(dz) < 1.0e-4D) {
			direction = Direction.NONE;
			return;
		}

		direction = resolveDirection(client, dx, dz);
		if (direction != Direction.NONE) {
			client.gui.setOverlayMessage(
					Component.literal("Kuudra " + direction.symbol).withStyle(style -> style.withColor(direction.color).withBold(true)),
					false);
		}
	}

	private static void renderOverlay(GuiGraphics graphics) {
		if (graphics == null || direction == Direction.NONE || moveModeActive) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.KUUDRA_POS_TRACKER.get())) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (!canTrackInCurrentContext(client)) {
			return;
		}

		if (client == null || client.font == null || client.getWindow() == null) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		DrawState drawState = createDrawState(client, direction.symbol, direction.color);
		Position position = getVisibleConfiguredPosition(drawState, screenWidth, screenHeight, true);
		drawSymbol(graphics, drawState, position.x(), position.y());
	}

	public static DrawState getMoveDrawState() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return null;
		}
		if (direction == Direction.NONE) {
			return createDrawState(client, PREVIEW_SYMBOL, PREVIEW_COLOR);
		}
		return createDrawState(client, direction.symbol, direction.color);
	}

	public static Position getVisibleConfiguredPosition(DrawState drawState, int screenWidth, int screenHeight, boolean persistIfAdjusted) {
		if (drawState == null) {
			int fallbackX = Math.round((float) (screenWidth / 2.0D + getConfiguredOffsetX()));
			int fallbackY = Math.round((float) (screenHeight / 2.0D + getConfiguredOffsetY()));
			return new Position(fallbackX, fallbackY);
		}

		int configuredX = Math.round((float) (screenWidth / 2.0D + getConfiguredOffsetX()));
		int configuredY = Math.round((float) (screenHeight / 2.0D + getConfiguredOffsetY()));
		Position clamped = clampPosition(drawState, screenWidth, screenHeight, configuredX, configuredY);
		if (persistIfAdjusted && (clamped.x() != configuredX || clamped.y() != configuredY)) {
			setConfiguredOffsets(clamped.x() - (screenWidth / 2.0D), clamped.y() - (screenHeight / 2.0D));
		}
		return clamped;
	}

	public static Position clampPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
		if (drawState == null) {
			return new Position(x, y);
		}
		int minX = 0;
		int maxX = Math.max(0, screenWidth - drawState.drawWidth());
		int minY = 0;
		int maxY = Math.max(0, screenHeight - drawState.drawHeight());
		return new Position(
				Mth.clamp(x, minX, maxX),
				Mth.clamp(y, minY, maxY));
	}

	public static void drawSymbol(GuiGraphics graphics, DrawState drawState, int x, int y) {
		if (graphics == null || drawState == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return;
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(HUD_SCALE, HUD_SCALE);
		graphics.drawString(
				client.font,
				Component.literal(drawState.text()).withStyle(style -> style.withColor(drawState.rgbColor()).withBold(true)),
				0,
				0,
				drawState.argbColor(),
				true);
		graphics.pose().popMatrix();
	}

	public static double getConfiguredOffsetX() {
		return resolveHudOffset(UiDefinitions.KUUDRA_POS_TRACKER_X.get(), DEFAULT_OFFSET_X);
	}

	public static double getConfiguredOffsetY() {
		return resolveHudOffset(UiDefinitions.KUUDRA_POS_TRACKER_Y.get(), DEFAULT_OFFSET_Y);
	}

	public static void setConfiguredOffsets(double offsetX, double offsetY) {
		UiDefinitions.KUUDRA_POS_TRACKER_X.set(clampOffset(offsetX));
		UiDefinitions.KUUDRA_POS_TRACKER_Y.set(clampOffset(offsetY));
	}

	private static DrawState createDrawState(Minecraft client, String textRaw, int colorRaw) {
		String text = textRaw == null || textRaw.isEmpty() ? PREVIEW_SYMBOL : textRaw;
		int argbColor = withOpaqueAlpha(colorRaw);
		int width = Math.max(1, Math.round(client.font.width(text) * HUD_SCALE));
		int height = Math.max(1, Math.round(client.font.lineHeight * HUD_SCALE));
		return new DrawState(text, argbColor, width, height);
	}

	private static int withOpaqueAlpha(int color) {
		return 0xFF000000 | (color & 0x00FFFFFF);
	}

	private static int rgb(int color) {
		return color & 0x00FFFFFF;
	}

	private static double resolveHudOffset(Double raw, double fallback) {
		double value = raw != null && Double.isFinite(raw) ? raw : fallback;
		return Mth.clamp(value, HUD_OFFSET_MIN, HUD_OFFSET_MAX);
	}

	private static double clampOffset(double value) {
		return Mth.clamp(value, HUD_OFFSET_MIN, HUD_OFFSET_MAX);
	}

	private static Direction resolveDirection(Minecraft client, double dx, double dz) {
		if (client == null || client.player == null) {
			return Direction.NONE;
		}
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < 1.0e-4D) {
			return Direction.NONE;
		}

		double targetX = dx / distance;
		double targetZ = dz / distance;
		double yawRadians = Math.toRadians(client.player.getYRot());
		double forwardX = -Math.sin(yawRadians);
		double forwardZ = Math.cos(yawRadians);
		double dot = forwardX * targetX + forwardZ * targetZ;
		if (dot >= FRONT_DOT_THRESHOLD) {
			return Direction.NONE;
		}
		if (dot <= BACK_DOT_THRESHOLD) {
			return Direction.BACK;
		}

		double cross = forwardX * targetZ - forwardZ * targetX;
		return cross < 0.0D ? Direction.LEFT : Direction.RIGHT;
	}

	private static boolean canTrackInCurrentContext(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return false;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return false;
		}
		return client.player.getY() < MAX_TRACKING_Y;
	}

	private enum Direction {
		NONE("", 0x000000),
		LEFT("<", 0x55FF55),
		RIGHT(">", 0x55FF55),
		BACK("V", 0xFF5555);

		private final String symbol;
		private final int color;

		Direction(String symbol, int color) {
			this.symbol = symbol;
			this.color = color;
		}
	}

	public record DrawState(String text, int argbColor, int drawWidth, int drawHeight) {
		public int rgbColor() {
			return rgb(argbColor);
		}
	}

	public record Position(int x, int y) {
	}
}
