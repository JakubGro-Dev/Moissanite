package com.crussion.moissanite.features.dungeons.terminals;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.sounds.SoundEvents;

final class AutoTermsModule {
	private static final long STEP_INTERVAL_NS = 10_000_000L;

	private final TerminalCore terminalCore;
	private long lastClickTimeMs = System.currentTimeMillis();
	private long lastStepTimeNs;
	private boolean clickedWindow;
	private boolean firstClick = true;
	private boolean inTerminal;
	private long startTimeMs = System.currentTimeMillis();
	private boolean disabledOnError;

	AutoTermsModule(TerminalCore terminalCore) {
		this.terminalCore = terminalCore;
	}

	void onTick(Minecraft client) {
		if (disabledOnError) {
			return;
		}

		long nowNs = System.nanoTime();
		if (lastStepTimeNs == 0L) {
			lastStepTimeNs = nowNs - STEP_INTERVAL_NS;
		}

		int iterations = 0;
		while (nowNs - lastStepTimeNs >= STEP_INTERVAL_NS && iterations < 20) {
			lastStepTimeNs += STEP_INTERVAL_NS;
			runStep();
			iterations++;
		}
		if (iterations == 20) {
			lastStepTimeNs = nowNs;
		}
	}

	private void runStep() {
		try {
			long now = System.currentTimeMillis();
			if (inTerminal && !terminalCore.isInTerminal()) {
				FeatureChat.send("Term took " + (now - startTimeMs) + "ms");
			}

			if (!terminalCore.isInTerminal()) {
				firstClick = true;
				startTimeMs = now;
				lastClickTimeMs = now;
				inTerminal = false;
			} else {
				inTerminal = true;
			}

			if (!TerminalSupport.autoTermsEnabled()) {
				return;
			}

			int clickDelay = TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_CLICK_DELAY.get(), 100);
			int firstClickDelay = TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_FIRST_CLICK_DELAY.get(), 350);
			int breakThreshold = TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_BREAK_THRESHOLD.get(), 500);

			if (firstClick && (now - lastClickTimeMs < firstClickDelay)) {
				return;
			}
			if (now - lastClickTimeMs < clickDelay) {
				return;
			}
			if (now - lastClickTimeMs > breakThreshold) {
				clickedWindow = false;
			}
			if (!terminalCore.isInTerminal() || clickedWindow) {
				return;
			}

			var solution = terminalCore.getSolution();
			if (solution == null || solution.isEmpty()) {
				return;
			}

			TerminalClickAction click = solution.remove(0);
			TerminalSupport.sendWindowClick(click.windowId(), click.slot(), click.clickType());
			lastClickTimeMs = now;
			clickedWindow = true;
			firstClick = false;
		} catch (Exception exception) {
			disabledOnError = true;
			FeatureChat.send("I would prefer you don't get banned for 0 first click delay so please forgive the inconvenience.");
			exception.printStackTrace();
		}
	}

	void onOpenWindow() {
		clickedWindow = false;
	}

	void onWorldReset() {
		clickedWindow = false;
		firstClick = true;
		inTerminal = false;
		startTimeMs = System.currentTimeMillis();
		lastClickTimeMs = startTimeMs;
		lastStepTimeNs = 0L;
	}

	boolean onPacketSent(Packet<?> packet) {
		if (!TerminalSupport.autoTermsEnabled() || !terminalCore.isInTerminal()) {
			return false;
		}
		if (packet instanceof ServerboundContainerClickPacket clickPacket) {
			int windowId = clickPacket.containerId();
			return windowId != terminalCore.getLastWindowId();
		}
		return false;
	}

	boolean onMouseClick(MouseButtonEvent click) {
		if (!TerminalSupport.autoTermsEnabled() || !terminalCore.isInTerminal()) {
			return false;
		}
		FeatureChat.send("You are in a terminal!");
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
		}
		return true;
	}

	boolean onKeyPress(KeyEvent input) {
		if (!TerminalSupport.autoTermsEnabled() || !terminalCore.isInTerminal()) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null || client.options.keyHotbarSlots == null) {
			return false;
		}

		for (KeyMapping keyMapping : client.options.keyHotbarSlots) {
			if (keyMapping != null && keyMapping.matches(input)) {
				FeatureChat.send("You are in a terminal!");
				if (client.player != null) {
					client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
				}
				return true;
			}
		}
		return false;
	}
}
