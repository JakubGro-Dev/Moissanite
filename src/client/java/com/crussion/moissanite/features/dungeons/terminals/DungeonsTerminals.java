package com.crussion.moissanite.features.dungeons.terminals;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.Identifier;

public final class DungeonsTerminals {
	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath("moissanite", "dungeons_terminals_overlay");

	private static final TerminalCore TERMINAL_CORE = new TerminalCore();
	private static final AutoTermsModule AUTO_TERMS = new AutoTermsModule(TERMINAL_CORE);
	private static final AutoMelodyModule AUTO_MELODY = new AutoMelodyModule();
	private static final TerminalAuraModule TERMINAL_AURA = new TerminalAuraModule(TERMINAL_CORE);
	private static final InvwalkVisualizerModule VISUALIZER = new InvwalkVisualizerModule();
	private static final InvwalkMentalAudioModule MENTAL_AUDIO = new InvwalkMentalAudioModule();

	private static boolean initialized;

	private DungeonsTerminals() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(DungeonsTerminals::onClientTick);
		HudElementRegistry.attachElementBefore(VanillaHudElements.SUBTITLES, HUD_ELEMENT_ID,
				(graphics, tickCounter) -> renderHud(graphics));
	}

	public static boolean onOpenScreenPacket(ClientboundOpenScreenPacket packet) {
		AUTO_TERMS.onOpenWindow();
		boolean cancel = TERMINAL_CORE.onOpenWindow(packet);
		cancel |= AUTO_MELODY.onOpenWindow(packet);
		VISUALIZER.onOpenWindow(packet);
		TERMINAL_AURA.onOpenWindow(packet);
		return cancel;
	}

	public static boolean onContainerSetSlotPacket(ClientboundContainerSetSlotPacket packet) {
		boolean cancel = TERMINAL_CORE.onSetSlot(packet);
		AUTO_MELODY.onSetSlot(packet);
		VISUALIZER.onSetSlot(packet);
		return cancel;
	}

	public static void onContainerClosePacketReceived(ClientboundContainerClosePacket packet) {
		TERMINAL_CORE.onCloseWindow();
		AUTO_MELODY.onCloseWindow();
		VISUALIZER.onCloseWindow();
		MENTAL_AUDIO.onClose(Minecraft.getInstance());
	}

	public static void onContainerClosePacketSent(ServerboundContainerClosePacket packet) {
		TERMINAL_CORE.onCloseWindow();
		AUTO_MELODY.onCloseWindow();
		VISUALIZER.onCloseWindow();
		MENTAL_AUDIO.onClose(Minecraft.getInstance());
	}

	public static void onContainerSetDataPacket(ClientboundContainerSetDataPacket packet) {
		TERMINAL_AURA.onContainerSetData(packet);
	}

	public static void onSystemChat(Component content) {
		TERMINAL_AURA.onSystemChat(content);
	}

	public static boolean onPacketSent(Packet<?> packet) {
		if (packet instanceof ServerboundContainerClosePacket closePacket) {
			onContainerClosePacketSent(closePacket);
		}

		boolean cancel = AUTO_MELODY.onPacketSent(packet);
		cancel |= AUTO_TERMS.onPacketSent(packet);
		cancel |= TERMINAL_AURA.onPacketSent(packet);
		return cancel;
	}

	public static boolean onContainerScreenMouseClicked(MouseButtonEvent click) {
		return AUTO_TERMS.onMouseClick(click);
	}

	public static boolean onContainerScreenKeyPressed(KeyEvent input) {
		return AUTO_TERMS.onKeyPress(input);
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			TERMINAL_CORE.onWorldReset();
			AUTO_TERMS.onWorldReset();
			AUTO_MELODY.onWorldReset();
			TERMINAL_AURA.onWorldReset();
			VISUALIZER.onWorldReset();
			MENTAL_AUDIO.onClose(client);
			return;
		}

		AUTO_TERMS.onTick(client);
		AUTO_MELODY.onTick(client);
		TERMINAL_AURA.onTick(client);
		VISUALIZER.onTick(client);
		MENTAL_AUDIO.onTick(client, TERMINAL_CORE.isInvwalkAutoTermsActive());
	}

	private static void renderHud(GuiGraphics graphics) {
		TERMINAL_CORE.renderOverlay(graphics);
		AUTO_MELODY.renderOverlay(graphics);
		VISUALIZER.renderOverlay(graphics);
	}
}
