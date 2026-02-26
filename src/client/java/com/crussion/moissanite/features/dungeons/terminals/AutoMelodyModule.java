package com.crussion.moissanite.features.dungeons.terminals;

import com.crussion.moissanite.definitions.UiDefinitions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

final class AutoMelodyModule {
	private static final class PendingClick {
		private final int slot;
		private final int button;
		private final long executeAtMs;

		private PendingClick(int slot, int button, long executeAtMs) {
			this.slot = slot;
			this.button = button;
			this.executeAtMs = executeAtMs;
		}
	}

	private boolean inTerminal;
	private int currentWindowId = -1;
	private final Map<Integer, TerminalItem> slots = new HashMap<>();
	private int windowSize;
	private long lastClickMs;
	private long lastOpenMs;
	private final int[] progress = new int[] {0, 4};
	private final int[] progress2 = new int[] {0, 0};
	private boolean blink;
	private long movementResumeAtMs;
	private final List<PendingClick> pendingClicks = new ArrayList<>();
	private final Deque<Packet<?>> blinkQueue = new ArrayDeque<>();

	boolean onOpenWindow(ClientboundOpenScreenPacket packet) {
		currentWindowId = packet.getContainerId();
		String title = TerminalSupport.stripFormatting(packet.getTitle());
		if (TerminalType.MELODY.matches(title)) {
			if (!TerminalSupport.autoTermsMelodyEnabled()) {
				return false;
			}

			lastOpenMs = System.currentTimeMillis();
			inTerminal = true;
			slots.clear();
			windowSize = TerminalType.MELODY.slotCount() + 1;
			return TerminalSupport.autoTermsInvwalkMelodyEnabled();
		}

		if (TerminalSupport.autoTermsInvwalkMelodyEnabled() && inTerminal) {
			releaseMovementControl();
		}
		inTerminal = false;
		currentWindowId = -1;
		return false;
	}

	void onCloseWindow() {
		if (TerminalSupport.autoTermsInvwalkMelodyEnabled() && inTerminal) {
			releaseMovementControl();
		}
		inTerminal = false;
		currentWindowId = -1;
		slots.clear();
		pendingClicks.clear();
	}

	void onSetSlot(ClientboundContainerSetSlotPacket packet) {
		if (!inTerminal) {
			return;
		}

		int slot = packet.getSlot();
		if (slot < 0 || slot >= windowSize) {
			return;
		}

		ItemStack stack = packet.getItem();
		if (stack != null && !stack.isEmpty()) {
			TerminalItem item = TerminalItem.from(packet.getContainerId(), slot, stack);
			slots.put(slot, item);
			if (item.id() == 160 && item.meta() == 5) {
				int correct = slots.values().stream()
						.filter(it -> it.id() == 160 && it.meta() == 2)
						.mapToInt(TerminalItem::slot)
						.findFirst()
						.orElse(0) - 1;
				int button = Math.floorDiv(slot, 9) - 1;
				int current = slot % 9 - 1;
				progress[0] = button;
				progress2[0] = current;
				progress2[1] = correct;
				if (current != correct) {
					return;
				}

				int buttonSlot = button * 9 + 16;
				long now = System.currentTimeMillis();
				if (lastOpenMs + TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_MELODY_FIRST_DELAY.get(), 0) > now) {
					return;
				}

				click(buttonSlot, 0);
				int melodySkipMode = TerminalSupport.autoTermsMelodySkipMode();
				if ((melodySkipMode == 1 && (current == 0 || current == 4)) || melodySkipMode == 2) {
					int skipDelayMs = TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_MELODY_SKIP_DELAY.get(), 50);
					if (button <= 3) {
						pendingClicks.add(new PendingClick(buttonSlot + 9, 0, now + skipDelayMs));
					}
					if (button <= 2) {
						pendingClicks.add(new PendingClick(buttonSlot + 18, 0, now + skipDelayMs * 2L));
					}
					if (button <= 1) {
						pendingClicks.add(new PendingClick(buttonSlot + 27, 0, now + skipDelayMs * 3L));
					}
				}
			}
		} else {
			slots.remove(slot);
		}
	}

	void onTick(Minecraft client) {
		if (client == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (!pendingClicks.isEmpty()) {
			for (int i = pendingClicks.size() - 1; i >= 0; i--) {
				PendingClick pending = pendingClicks.get(i);
				if (now < pending.executeAtMs) {
					continue;
				}
				click(pending.slot, pending.button);
				pendingClicks.remove(i);
			}
		}

		if (inTerminal && TerminalSupport.autoTermsInvwalkMelodyEnabled() && TerminalSupport.autoTermsInvwalkMelodyMethod() == 0
				&& lastClickMs + TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK_MELODY_MOVE_DELAY.get(), 350) > now) {
			setMovementKeysDown(client, false);
		}

		if (movementResumeAtMs > 0L && now >= movementResumeAtMs) {
			if (TerminalSupport.autoTermsInvwalkMelodyMethod() == 0) {
				restoreMovementKeys(client);
			} else if (TerminalSupport.autoTermsInvwalkMelodyMethod() == 1) {
				blink = false;
			}
			movementResumeAtMs = 0L;
		}

		if (!blink && !blinkQueue.isEmpty() && client.getConnection() != null) {
			while (!blinkQueue.isEmpty()) {
				client.getConnection().send(blinkQueue.pollFirst());
			}
		}
	}

	void onWorldReset() {
		inTerminal = false;
		currentWindowId = -1;
		slots.clear();
		pendingClicks.clear();
		blinkQueue.clear();
		blink = false;
		movementResumeAtMs = 0L;
		progress[0] = 0;
		progress[1] = 4;
		progress2[0] = 0;
		progress2[1] = 0;
	}

	boolean onPacketSent(Packet<?> packet) {
		if (blink && packet instanceof ServerboundMovePlayerPacket) {
			blinkQueue.addLast(packet);
			return true;
		}

		if (blink && packet instanceof ServerboundUseItemOnPacket) {
			blinkQueue.addLast(packet);
			return true;
		}

		if (blink && packet instanceof ServerboundUseItemPacket useItemPacket) {
			if (isJerryOrBonzoStaff(useItemPacket.getHand())) {
				return true;
			}
		}

		return false;
	}

	void renderOverlay(GuiGraphics graphics) {
		if (!inTerminal || !TerminalSupport.autoTermsInvwalkMelodyEnabled()) {
			return;
		}

		long now = System.currentTimeMillis();
		int melodyMoveDelay = TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK_MELODY_MOVE_DELAY.get(), 350);
		String inTerminalText = ChatFormatting.DARK_AQUA + "In Terminal";
		if (lastClickMs + melodyMoveDelay > now) {
			inTerminalText += " " + ChatFormatting.RED + "(Melody " + (lastClickMs - now + melodyMoveDelay) + "ms)";
		} else {
			inTerminalText += " (Melody)";
		}
		TerminalRenderSupport.drawCenteredScaledText(graphics, inTerminalText, 1.5F, 10);

		String melodyProgress = ChatFormatting.AQUA + "[" + progress[0] + "/" + progress[1] + "]";
		if (Boolean.TRUE.equals(UiDefinitions.DUNGEONS_AUTO_TERMS_VISUALIZE_MELODY.get())) {
			String[] visualizer = new String[] {
					ChatFormatting.DARK_GRAY + "=",
					ChatFormatting.DARK_GRAY + "=",
					ChatFormatting.DARK_GRAY + "=",
					ChatFormatting.DARK_GRAY + "=",
					ChatFormatting.DARK_GRAY + "="};

			if (progress2[1] >= 0 && progress2[1] < visualizer.length) {
				visualizer[progress2[1]] = ChatFormatting.LIGHT_PURPLE + "=";
			}
			if (progress2[0] >= 0 && progress2[0] < visualizer.length) {
				visualizer[progress2[0]] = ChatFormatting.GREEN + "=";
			}
			melodyProgress += " " + ChatFormatting.GRAY + "[" + String.join("", visualizer) + ChatFormatting.GRAY + "]";
		}
		TerminalRenderSupport.drawCenteredScaledText(graphics, melodyProgress, 1.5F, 20);
	}

	private void click(int slot, int button) {
		if (slot < 0 || button < 0 || currentWindowId < 0) {
			return;
		}

		lastClickMs = System.currentTimeMillis();
		if (TerminalSupport.autoTermsInvwalkMelodyEnabled()) {
			int method = TerminalSupport.autoTermsInvwalkMelodyMethod();
			if (method == 0) {
				Minecraft client = Minecraft.getInstance();
				setMovementKeysDown(client, false);
			} else if (method == 1) {
				blink = true;
			}

			movementResumeAtMs = lastClickMs + TerminalSupport.intValue(UiDefinitions.DUNGEONS_AUTO_TERMS_INVWALK_MELODY_MOVE_DELAY.get(), 350);
		}

		TerminalSupport.sendWindowClick(currentWindowId, slot, button);
	}

	private void releaseMovementControl() {
		Minecraft client = Minecraft.getInstance();
		if (TerminalSupport.autoTermsInvwalkMelodyMethod() == 0) {
			restoreMovementKeys(client);
		} else if (TerminalSupport.autoTermsInvwalkMelodyMethod() == 1) {
			blink = false;
		}
		movementResumeAtMs = 0L;
	}

	private void restoreMovementKeys(Minecraft client) {
		if (client == null || client.options == null) {
			return;
		}
		TerminalSupport.restoreKeyState(client, client.options.keyUp);
		TerminalSupport.restoreKeyState(client, client.options.keyLeft);
		TerminalSupport.restoreKeyState(client, client.options.keyRight);
		TerminalSupport.restoreKeyState(client, client.options.keyDown);
	}

	private void setMovementKeysDown(Minecraft client, boolean down) {
		if (client == null || client.options == null) {
			return;
		}
		setKeyDown(client.options.keyUp, down);
		setKeyDown(client.options.keyLeft, down);
		setKeyDown(client.options.keyRight, down);
		setKeyDown(client.options.keyDown, down);
	}

	private void setKeyDown(KeyMapping mapping, boolean down) {
		if (mapping == null) {
			return;
		}
		mapping.setDown(down);
	}

	private boolean isJerryOrBonzoStaff(InteractionHand hand) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || hand == null) {
			return false;
		}
		ItemStack held = client.player.getItemInHand(hand);
		String itemId = TerminalSupport.hypixelItemId(held);
		return "JERRY_STAFF".equals(itemId) || "BONZO_STAFF".equals(itemId);
	}
}
