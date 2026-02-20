package com.crussion.moissanite.features.cheats;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.input.FakeKeybinds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;

public final class WardrobeKeybinds {
	private static final int COOLDOWN_TICKS = 19;
	private static final String OVERLAY_TEXT = "Equiping Wardrobe";

	private static int cwid = -1;
	private static int index = 36;
	private static int cooldownTicks;
	private static long tickIndex;
	private static long closeAtTick = -1;
	private static int closeContainerId = -1;
	private static boolean wardrobeCooldown;
	private static boolean awaitingWardrobe;
	private static boolean clickSlotRegistered;
	private static boolean overlayRegistered;
	private static boolean initialized;

	private WardrobeKeybinds() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		FakeKeybinds.onKeyPress(UiDefinitions.WD_ONE_KEYBIND, () -> {
			wardrobe();
			index = 36;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_TWO_KEYBIND, () -> {
			wardrobe();
			index = 37;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_THREE_KEYBIND, () -> {
			wardrobe();
			index = 38;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_FOUR_KEYBIND, () -> {
			wardrobe();
			index = 39;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_FIVE_KEYBIND, () -> {
			wardrobe();
			index = 40;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_SIX_KEYBIND, () -> {
			wardrobe();
			index = 41;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_SEVEN_KEYBIND, () -> {
			wardrobe();
			index = 42;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_EIGHT_KEYBIND, () -> {
			wardrobe();
			index = 43;
		});
		FakeKeybinds.onKeyPress(UiDefinitions.WD_NINE_KEYBIND, () -> {
			wardrobe();
			index = 55;
		});

		ClientTickEvents.END_CLIENT_TICK.register(WardrobeKeybinds::handleClientTick);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				Identifier.fromNamespaceAndPath("moissanite", "wardrobe_overlay"),
				(graphics, tickCounter) -> renderOverlay(graphics));
	}

	private static void wardrobe() {
		if (wardrobeCooldown) {
			return;
		}
		if (index <= 0) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null || client.player.connection == null) {
			return;
		}

		wardrobeCooldown = true;
		awaitingWardrobe = true;
		clickSlotRegistered = true;
		overlayRegistered = true;
		cooldownTicks = COOLDOWN_TICKS;
		client.player.connection.sendCommand("wardrobe");
	}

	private static void handleClientTick(Minecraft client) {
		tickIndex++;
		if (cooldownTicks > 0) {
			cooldownTicks--;
			if (cooldownTicks == 0) {
				wardrobeCooldown = false;
			}
		}
		if (closeContainerId != -1 && closeAtTick != -1 && tickIndex >= closeAtTick) {
			sendClosePacket(closeContainerId);
			closeAtTick = -1;
			closeContainerId = -1;
		}
		if (client == null || client.player == null || client.level == null) {
			return;
		}
	}

	public static boolean onOpenScreenPacket(ClientboundOpenScreenPacket packet) {
		if (!clickSlotRegistered || !awaitingWardrobe || packet == null) {
			return false;
		}
		String title = packet.getTitle() == null ? "" : packet.getTitle().getString();
		if (!title.contains("Wardrobe")) {
			return false;
		}

		awaitingWardrobe = false;
		cwid = packet.getContainerId();
		click(index);
		clickSlotRegistered = false;
		overlayRegistered = false;
		closeContainerId = cwid;
		closeAtTick = tickIndex + 1;
		return true;
	}

	public static void onClosePacketSent() {
		cwid = -1;
	}

	public static void onClosePacketReceived() {
		cwid = -1;
	}

	private static boolean click(int slot) {
		if (cwid == -1) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getConnection() == null) {
			return false;
		}
		client.getConnection().send(new ServerboundContainerClickPacket(
				cwid,
				0,
				(short) slot,
				(byte) 0,
				ClickType.PICKUP,
				new Int2ObjectOpenHashMap<>(),
				HashedStack.EMPTY
		));
		return true;
	}

	private static void sendClosePacket(int containerId) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getConnection() == null || containerId == -1) {
			return;
		}
		client.getConnection().send(new ServerboundContainerClosePacket(containerId));
	}

	private static void renderOverlay(GuiGraphics graphics) {
		if (!overlayRegistered || graphics == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null || client.getWindow() == null) {
			return;
		}

		float scale = 1.5f;
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int x = Math.round((screenWidth / scale - client.font.width(OVERLAY_TEXT)) / 2.0f);
		int y = Math.round(screenHeight / scale / 2.0f + 16.0f);

		graphics.drawString(client.font, OVERLAY_TEXT, x, y, 0xFFFFFF, true);
		graphics.pose().popMatrix();
	}
}
