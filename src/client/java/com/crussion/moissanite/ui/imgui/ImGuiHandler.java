package com.crussion.moissanite.ui.imgui;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30C;

public final class ImGuiHandler {
	private static final ImGuiImplGlfw IMGUI_GLFW = new ImGuiImplGlfw();
	private static final ImGuiImplGl3 IMGUI_GL3 = new ImGuiImplGl3();
	private static boolean initialized;

	private ImGuiHandler() {
	}

	public static void initialize(long windowHandle) {
		if (initialized) {
			return;
		}

		ImGui.createContext();
		ImGuiIO io = ImGui.getIO();
		io.setIniFilename(null);
		io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
		loadDefaultFonts(io);
		applyStyle();

		IMGUI_GLFW.init(windowHandle, true);
		IMGUI_GL3.init();
		initialized = true;
	}

	public static void open(ImGuiScreen screen) {
		if (screen == null) {
			return;
		}
		ensureInitialized(Minecraft.getInstance());
		prepareForScreen();
		Minecraft.getInstance().setScreen(screen);
	}

	public static void prepareForScreen() {
		ensureInitialized(Minecraft.getInstance());
		if (!initialized) {
			return;
		}

		ImGuiIO io = ImGui.getIO();
		io.clearEventsQueue();
		io.clearInputKeys();
		io.clearInputMouse();
		io.setFontGlobalScale(1.0f);
		applyStyle();
	}

	public static void render(ImGuiScreen screen) {
		if (screen == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return;
		}
		ensureInitialized(client);
		if (!initialized) {
			return;
		}

		int previousFramebuffer = bindMainRenderTarget(client);
		try {
			IMGUI_GL3.newFrame();
			IMGUI_GLFW.newFrame();
			ImGui.newFrame();

			screen.renderImGui(ImGui.getIO());

			ImGui.render();
			IMGUI_GL3.renderDrawData(ImGui.getDrawData());
		} finally {
			if (previousFramebuffer >= 0) {
				GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFramebuffer);
			}
		}
	}

	public static void dispose() {
		if (!initialized) {
			return;
		}

		IMGUI_GL3.shutdown();
		IMGUI_GLFW.shutdown();
		ImGui.destroyContext();
		initialized = false;
	}

	private static void ensureInitialized(Minecraft client) {
		if (initialized || client == null || client.getWindow() == null) {
			return;
		}
		initialize(client.getWindow().handle());
	}

	private static int bindMainRenderTarget(Minecraft client) {
		RenderTarget target = client.getMainRenderTarget();
		GpuTexture colorTexture = target.getColorTexture();
		if (!(colorTexture instanceof GlTexture glColorTexture) || !(RenderSystem.getDevice() instanceof GlDevice glDevice)) {
			GlStateManager._viewport(0, 0, client.getWindow().getWidth(), client.getWindow().getHeight());
			return -1;
		}

		int previousFramebuffer = GlStateManager.getFrameBuffer(GL30C.GL_FRAMEBUFFER);
		int targetFramebuffer = glColorTexture.getFbo(glDevice.directStateAccess(), target.getDepthTexture());
		GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, targetFramebuffer);
		GlStateManager._viewport(0, 0, target.width, target.height);
		return previousFramebuffer;
	}

	private static void loadDefaultFonts(ImGuiIO io) {
		ImFont fallback = io.getFonts().addFontDefault();
		ImFont regular = addSystemFont(io, 17.0f, "C:\\Windows\\Fonts\\segoeui.ttf", "C:\\Windows\\Fonts\\SegUIVar.ttf",
				"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
				"/System/Library/Fonts/Supplemental/Arial.ttf");
		ImFont small = addSystemFont(io, 15.0f, "C:\\Windows\\Fonts\\segoeui.ttf", "C:\\Windows\\Fonts\\SegUIVar.ttf",
				"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
				"/System/Library/Fonts/Supplemental/Arial.ttf");
		ImFont section = addSystemFont(io, 19.0f, "C:\\Windows\\Fonts\\seguisb.ttf", "C:\\Windows\\Fonts\\segoeuib.ttf",
				"/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
				"/System/Library/Fonts/Supplemental/Arial Bold.ttf");
		ImFont title = addSystemFont(io, 24.0f, "C:\\Windows\\Fonts\\seguisb.ttf", "C:\\Windows\\Fonts\\segoeuib.ttf",
				"/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
				"/System/Library/Fonts/Supplemental/Arial Bold.ttf");
		ImFont defaultFont = regular == null ? fallback : regular;
		io.setFontDefault(defaultFont);
		MoissaniteImGui.setFonts(
				defaultFont,
				small == null ? defaultFont : small,
				section == null ? defaultFont : section,
				title == null ? defaultFont : title);
	}

	private static ImFont addSystemFont(ImGuiIO io, float size, String... candidates) {
		for (String candidate : candidates) {
			Path path = Path.of(candidate);
			if (!Files.isRegularFile(path)) {
				continue;
			}
			try {
				return io.getFonts().addFontFromFileTTF(path.toString(), size);
			} catch (RuntimeException ignored) {
				continue;
			}
		}
		return null;
	}

	public static void applyStyle() {
		ImGui.styleColorsDark();
		ImGuiStyle style = ImGui.getStyle();
		style.setWindowPadding(0.0f, 0.0f);
		style.setFramePadding(12.0f, 6.0f);
		style.setItemSpacing(8.0f, 8.0f);
		style.setItemInnerSpacing(7.0f, 5.0f);
		style.setIndentSpacing(16.0f);
		style.setScrollbarSize(7.0f);
		style.setWindowRounding(0.0f);
		style.setChildRounding(9.0f);
		style.setChildBorderSize(0.0f);
		style.setPopupRounding(8.0f);
		style.setFrameRounding(8.0f);
		style.setGrabRounding(8.0f);
		style.setWindowBorderSize(0.0f);
		style.setFrameBorderSize(0.0f);

		setColor(style, ImGuiCol.Text, 0xFFF0F3FF);
		setColor(style, ImGuiCol.TextDisabled, 0xFFA6ACCA);
		setColor(style, ImGuiCol.WindowBg, 0x00000000);
		setColor(style, ImGuiCol.ChildBg, 0x002D3353);
		setColor(style, ImGuiCol.PopupBg, 0xF42D3353);
		setColor(style, ImGuiCol.Border, 0x9D51587C);
		setColor(style, ImGuiCol.FrameBg, 0xE64C5475);
		setColor(style, ImGuiCol.FrameBgHovered, 0xEE56608A);
		setColor(style, ImGuiCol.FrameBgActive, 0xF0616BA0);
		setColor(style, ImGuiCol.Button, 0xE64C5475);
		setColor(style, ImGuiCol.ButtonHovered, 0xEF56608A);
		setColor(style, ImGuiCol.ButtonActive, 0xFFA986FF);
		setColor(style, ImGuiCol.Header, 0x6CA380F6);
		setColor(style, ImGuiCol.HeaderHovered, 0x4A9D7BFA);
		setColor(style, ImGuiCol.HeaderActive, 0x90A380F6);
		setColor(style, ImGuiCol.CheckMark, 0xFFA986FF);
		setColor(style, ImGuiCol.SliderGrab, 0xFFF6F8FF);
		setColor(style, ImGuiCol.SliderGrabActive, 0xFFA986FF);
		setColor(style, ImGuiCol.Separator, 0x85646B90);
		setColor(style, ImGuiCol.ScrollbarBg, 0x552A304F);
		setColor(style, ImGuiCol.ScrollbarGrab, 0xAA656D92);
		setColor(style, ImGuiCol.ScrollbarGrabHovered, 0xD0A986FF);
		setColor(style, ImGuiCol.ScrollbarGrabActive, 0xFFA986FF);
		setColor(style, ImGuiCol.TextSelectedBg, 0x66A986FF);
	}

	private static void setColor(ImGuiStyle style, int target, int argb) {
		float alpha = ((argb >>> 24) & 0xFF) / 255.0f;
		float red = ((argb >>> 16) & 0xFF) / 255.0f;
		float green = ((argb >>> 8) & 0xFF) / 255.0f;
		float blue = (argb & 0xFF) / 255.0f;
		style.setColor(target, red, green, blue, alpha);
	}
}
