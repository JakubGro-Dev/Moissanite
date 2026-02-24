package com.crussion.moissanite.features.cheats;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.function.Consumer;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.input.FakeKeybinds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ClickType;

public final class WardrobeKeybinds {
	private static final int COOLDOWN_TICKS = 19;
	private static final long SWAP_TIMEOUT_TICKS = 80L;
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
	private static boolean overlaySoundPlayed;
	private static boolean initialized;
	private static boolean swapInProgress;
	private static boolean swapClickSent;
	private static long swapStartTick = -1L;
	private static Consumer<Boolean> swapCompletion;

	private WardrobeKeybinds() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				Identifier.fromNamespaceAndPath("moissanite", "wardrobe_overlay"),
				(graphics, tickCounter) -> renderOverlay(graphics));

		FakeKeybinds.onKeyPress(UiDefinitions.WD_ONE_KEYBIND, () -> requestSwapSlot(1, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_TWO_KEYBIND, () -> requestSwapSlot(2, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_THREE_KEYBIND, () -> requestSwapSlot(3, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_FOUR_KEYBIND, () -> requestSwapSlot(4, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_FIVE_KEYBIND, () -> requestSwapSlot(5, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_SIX_KEYBIND, () -> requestSwapSlot(6, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_SEVEN_KEYBIND, () -> requestSwapSlot(7, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_EIGHT_KEYBIND, () -> requestSwapSlot(8, null));
		FakeKeybinds.onKeyPress(UiDefinitions.WD_NINE_KEYBIND, () -> requestSwapSlot(9, null));

		ClientTickEvents.END_CLIENT_TICK.register(WardrobeKeybinds::handleClientTick);
	}

	public static boolean requestSwapSlot(int slot, Consumer<Boolean> completion) {
		int menuSlot = menuSlotForWardrobeSlot(slot);
		if (menuSlot == -1) {
			complete(completion, false);
			return false;
		}
		if (swapInProgress) {
			complete(completion, false);
			return false;
		}

		index = menuSlot;
		swapInProgress = true;
		swapClickSent = false;
		swapStartTick = tickIndex;
		swapCompletion = completion;
		if (!wardrobe()) {
			finishSwap(false);
			return false;
		}
		return true;
	}

	private static boolean wardrobe() {
		if (wardrobeCooldown) {
			return false;
		}
		if (index <= 0) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null || client.player.connection == null) {
			return false;
		}

		wardrobeCooldown = true;
		awaitingWardrobe = true;
		clickSlotRegistered = true;
		overlayRegistered = true;
		overlaySoundPlayed = false;
		cooldownTicks = COOLDOWN_TICKS;
		client.player.connection.sendCommand("wardrobe");
		return true;
	}

	private static void handleClientTick(Minecraft client) {
		tickIndex++;
		if (cooldownTicks > 0) {
			cooldownTicks--;
			if (cooldownTicks == 0) {
				wardrobeCooldown = false;
			}
		}
		if (swapInProgress && swapStartTick != -1L && tickIndex - swapStartTick > SWAP_TIMEOUT_TICKS) {
			finishSwap(false);
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
		if (!click(index)) {
			finishSwap(false);
			return false;
		}
		swapClickSent = true;
		clickSlotRegistered = false;
		overlayRegistered = false;
		closeContainerId = cwid;
		closeAtTick = tickIndex + 1;
		return true;
	}

	public static void onClosePacketSent() {
		cwid = -1;
		if (swapInProgress && swapClickSent) {
			finishSwap(true);
		}
	}

	public static void onClosePacketReceived() {
		cwid = -1;
		if (swapInProgress && swapClickSent) {
			finishSwap(true);
		}
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

	private static int menuSlotForWardrobeSlot(int slot) {
		return switch (slot) {
			case 1 -> 36;
			case 2 -> 37;
			case 3 -> 38;
			case 4 -> 39;
			case 5 -> 40;
			case 6 -> 41;
			case 7 -> 42;
			case 8 -> 43;
			case 9 -> 55;
			default -> -1;
		};
	}

	private static void finishSwap(boolean success) {
		Consumer<Boolean> callback = swapCompletion;
		swapCompletion = null;
		swapInProgress = false;
		swapClickSent = false;
		swapStartTick = -1L;
		awaitingWardrobe = false;
		clickSlotRegistered = false;
		overlayRegistered = false;
		closeAtTick = -1L;
		closeContainerId = -1;
		cwid = -1;
		overlaySoundPlayed = false;
		complete(callback, success);
	}

	private static void complete(Consumer<Boolean> callback, boolean success) {
		if (callback == null) {
			return;
		}
		try {
			callback.accept(success);
		} catch (Exception ignored) {
		}
	}

	private static void renderOverlay(GuiGraphics graphics) {
		if (!overlayRegistered || graphics == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null || client.getWindow() == null) {
			return;
		}

		client.gui.setTimes(0, 20, 0);
		client.gui.setSubtitle(Component.empty());
		client.gui.setTitle(Component.literal("Changing").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		if (!overlaySoundPlayed && client.player != null) {
			client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F);
			overlaySoundPlayed = true;
		}
	}
}
