package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.features.cheats.AutoRendHelper;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.tick.TickTaskScheduler;
import net.minecraft.client.Minecraft;

public final class KuudraAutoSwapArmorRag {

	private KuudraAutoSwapArmorRag() {
	}

	public static void onSound(String soundId, float pitch) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_SWAP_ARMOR_RAG.get())) {
			return;
		}

		int phase = KuudraPhaseTracker.getPhase();
		if (phase != KuudraPhaseTracker.PHASE_HIT && phase != KuudraPhaseTracker.PHASE_SKIP) {
			return;
		}

		if (Math.abs(pitch - 1.4920635f) > 0.00001f) {
			return;
		}

		int sliderVal = UiDefinitions.AUTO_SWAP_ARMOR_RAG_SLOT.get().intValue();
		int armorSlot = sliderVal + 1;
		if (armorSlot < 1 || armorSlot > 9) {
			return;
		}

		TickTaskScheduler.schedule(2, () -> triggerSwap(armorSlot));
	}

	private static void triggerSwap(int armorSlot) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		FeatureChat.sendPrefixed("Swap Armor", "Rag sound detected! Swapping to slot " + armorSlot + "...");
		AutoRendHelper.WDSwapSlot(armorSlot, success -> {
			if (!success) {
				FeatureChat.sendPrefixed("Swap Armor", "Failed to swap armor!");
			}
		});
	}
}
