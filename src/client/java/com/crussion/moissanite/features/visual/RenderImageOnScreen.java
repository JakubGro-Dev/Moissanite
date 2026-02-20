package com.crussion.moissanite.features.visual;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.ui.screen.ImageOnScreenMoveScreen;
import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class RenderImageOnScreen {
	private static final String MOD_NAMESPACE = "moissanite";
	private static final Path ASSETS_DIR = FabricLoader.getInstance().getConfigDir().resolve("moissanite").resolve("assets");
	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath(MOD_NAMESPACE, "render_image_on_screen");
	private static final String DYNAMIC_TEXTURE_PREFIX = "dynamic/config_image_";
	private static final double ROUND_TO_HUNDREDTHS = 100.0;
	private static final double MIN_SCALE = 0.1;
	private static final double MAX_SCALE = 2.0;

	private static final Map<String, Path> IMAGES_BY_NAME = new LinkedHashMap<>();

	private static boolean initialized;
	private static boolean moveModeActive;
	private static int textureCounter;
	private static String loadedSelection = "";
	private static Identifier loadedTextureId;
	private static int loadedWidth;
	private static int loadedHeight;

	private RenderImageOnScreen() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		refreshImageOptions();
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				HUD_ELEMENT_ID,
				(graphics, tickCounter) -> renderOverlay(graphics));
	}

	public static void refreshImageOptions() {
		ensureAssetsDirectory();
		IMAGES_BY_NAME.clear();
		IMAGES_BY_NAME.putAll(discoverImages());
		UiDefinitions.IMAGE_ON_SCREEN_SELECT.setOptions(IMAGES_BY_NAME.keySet());
		invalidateLoadedTexture();
	}

	public static void openMoveScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		String selected = selectedImageKey();
		if (selected.isBlank()) {
			return;
		}
		if (!ensureTextureLoaded(selected)) {
			return;
		}

		Screen returnScreen = client.screen;
		setMoveModeActive(true);
		client.setScreen(new ImageOnScreenMoveScreen(returnScreen));
	}

	public static void setMoveModeActive(boolean moveModeActive) {
		RenderImageOnScreen.moveModeActive = moveModeActive;
	}

	public static int getConfiguredX() {
		return UiDefinitions.IMAGE_ON_SCREEN_X.get();
	}

	public static int getConfiguredY() {
		return UiDefinitions.IMAGE_ON_SCREEN_Y.get();
	}

	public static void setConfiguredPosition(int x, int y) {
		UiDefinitions.IMAGE_ON_SCREEN_X.set(x);
		UiDefinitions.IMAGE_ON_SCREEN_Y.set(y);
	}

	public static double getConfiguredScale() {
		return clampScale(UiDefinitions.IMAGE_ON_SCREEN_SCALE.get());
	}

	public static void adjustConfiguredScale(double delta) {
		setConfiguredScale(getConfiguredScale() + delta);
	}

	public static void setConfiguredScale(double scale) {
		double rounded = roundToHundredths(clampScale(scale));
		UiDefinitions.IMAGE_ON_SCREEN_SCALE.set(rounded);
	}

	public static DrawState getCurrentDrawState(int screenWidth, int screenHeight) {
		String selected = selectedImageKey();
		if (selected.isBlank()) {
			return null;
		}
		return getDrawState(selected, screenWidth, screenHeight);
	}

	public static Position getVisibleConfiguredPosition(DrawState drawState, int screenWidth, int screenHeight, boolean persistIfAdjusted) {
		if (drawState == null) {
			return new Position(getConfiguredX(), getConfiguredY());
		}
		int configuredX = getConfiguredX();
		int configuredY = getConfiguredY();
		Position clamped = clampPosition(drawState, screenWidth, screenHeight, configuredX, configuredY);
		if (persistIfAdjusted && (clamped.x() != configuredX || clamped.y() != configuredY)) {
			setConfiguredPosition(clamped.x(), clamped.y());
		}
		return clamped;
	}

	public static Position clampPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
		if (drawState == null) {
			return new Position(x, y);
		}
		int minX = Math.min(0, screenWidth - drawState.drawWidth());
		int maxX = Math.max(0, screenWidth - drawState.drawWidth());
		int minY = Math.min(0, screenHeight - drawState.drawHeight());
		int maxY = Math.max(0, screenHeight - drawState.drawHeight());
		return new Position(clampToRange(x, minX, maxX), clampToRange(y, minY, maxY));
	}

	public static void drawImage(GuiGraphics graphics, DrawState drawState, int x, int y) {
		if (graphics == null || drawState == null) {
			return;
		}
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				drawState.textureId(),
				x,
				y,
				0f,
				0f,
				drawState.drawWidth(),
				drawState.drawHeight(),
				drawState.textureWidth(),
				drawState.textureHeight(),
				drawState.textureWidth(),
				drawState.textureHeight());
	}

	private static void renderOverlay(GuiGraphics graphics) {
		if (!shouldRenderOverlay(graphics)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		DrawState drawState = getCurrentDrawState(screenWidth, screenHeight);
		if (drawState == null) {
			return;
		}
		Position position = getVisibleConfiguredPosition(drawState, screenWidth, screenHeight, false);
		drawImage(graphics, drawState, position.x(), position.y());
	}

	private static DrawState getDrawState(String selected, int screenWidth, int screenHeight) {
		if (!ensureTextureLoaded(selected)) {
			return null;
		}
		int maxWidth = Math.max(1, screenWidth / 3);
		int maxHeight = Math.max(1, screenHeight / 3);
		float baseScale = Math.min(1.0f, Math.min((float) maxWidth / (float) loadedWidth, (float) maxHeight / (float) loadedHeight));
		float scale = baseScale * (float) getConfiguredScale();
		int drawWidth = scaledDimension(loadedWidth, scale);
		int drawHeight = scaledDimension(loadedHeight, scale);
		return new DrawState(loadedTextureId, loadedWidth, loadedHeight, drawWidth, drawHeight);
	}

	private static boolean ensureTextureLoaded(String selected) {
		if (hasLoadedTextureFor(selected)) {
			return true;
		}

		Path path = pathForSelection(selected);
		if (path == null || !Files.isRegularFile(path)) {
			invalidateLoadedTexture();
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getTextureManager() == null) {
			return false;
		}

		invalidateLoadedTexture();
		try (InputStream stream = Files.newInputStream(path)) {
			NativeImage nativeImage = NativeImage.read(stream);
			loadedWidth = nativeImage.getWidth();
			loadedHeight = nativeImage.getHeight();
			DynamicTexture texture = new DynamicTexture(() -> "config_image_" + selected, nativeImage);
			texture.upload();

			loadedTextureId = nextDynamicTextureId();
			client.getTextureManager().register(loadedTextureId, texture);
			loadedSelection = selected;
			return true;
		} catch (IOException ignored) {
			invalidateLoadedTexture();
			return false;
		}
	}

	private static void invalidateLoadedTexture() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.getTextureManager() != null && loadedTextureId != null) {
			client.getTextureManager().release(loadedTextureId);
		}
		loadedTextureId = null;
		loadedSelection = "";
		loadedWidth = 0;
		loadedHeight = 0;
	}

	private static boolean shouldRenderOverlay(GuiGraphics graphics) {
		if (graphics == null || moveModeActive) {
			return false;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.IMAGE_ON_SCREEN.get())) {
			invalidateLoadedTexture();
			return false;
		}
		return true;
	}

	private static void ensureAssetsDirectory() {
		try {
			Files.createDirectories(ASSETS_DIR);
		} catch (IOException ignored) {
		}
	}

	private static Map<String, Path> discoverImages() {
		Map<String, Path> discovered = new LinkedHashMap<>();
		if (!Files.isDirectory(ASSETS_DIR)) {
			return discovered;
		}
		try (var paths = Files.walk(ASSETS_DIR)) {
			List<Path> pngs = paths
					.filter(Files::isRegularFile)
					.filter(path -> path.getFileName() != null)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
					.sorted(Comparator.comparing(path -> normalizePath(ASSETS_DIR.relativize(path))))
					.toList();
			for (Path path : pngs) {
				discovered.put(normalizePath(ASSETS_DIR.relativize(path)), path);
			}
		} catch (IOException ignored) {
		}
		return discovered;
	}

	private static boolean hasLoadedTextureFor(String selected) {
		return selected.equals(loadedSelection) && loadedTextureId != null && loadedWidth > 0 && loadedHeight > 0;
	}

	private static Path pathForSelection(String selected) {
		return IMAGES_BY_NAME.get(selected);
	}

	private static Identifier nextDynamicTextureId() {
		textureCounter++;
		return Identifier.fromNamespaceAndPath(MOD_NAMESPACE, DYNAMIC_TEXTURE_PREFIX + textureCounter);
	}

	private static String normalizePath(Path path) {
		return path == null ? "" : path.toString().replace('\\', '/');
	}

	private static String selectedImageKey() {
		String selected = UiDefinitions.IMAGE_ON_SCREEN_SELECT.get();
		return selected == null ? "" : selected.trim();
	}

	private static double clampScale(double scale) {
		if (!Double.isFinite(scale)) {
			return 1.0;
		}
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
	}

	private static double roundToHundredths(double value) {
		return Math.round(value * ROUND_TO_HUNDREDTHS) / ROUND_TO_HUNDREDTHS;
	}

	private static int scaledDimension(int baseSize, float scale) {
		return Math.max(1, Math.round(baseSize * scale));
	}

	private static int clampToRange(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public record DrawState(Identifier textureId, int textureWidth, int textureHeight, int drawWidth, int drawHeight) {
	}

	public record Position(int x, int y) {
	}
}
