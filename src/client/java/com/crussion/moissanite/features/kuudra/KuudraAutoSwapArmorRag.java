package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.features.cheats.AutoRendHelper;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.tick.TickTaskScheduler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashSet;
import java.util.Set;

public final class KuudraAutoSwapArmorRag {
	private static final float RAGNAROCK_CAST_PITCH = 1.4920635f;
	private static final float RAGNAROCK_CAST_PITCH_EPSILON = 0.00001f;
	private static final String RAGNAROCK_AXE_ID = "RAGNAROCK_AXE";
	private static final Set<Identifier> RAGNAROCK_CAST_SOUND_IDS = createRagnarockCastSoundIds();

	private KuudraAutoSwapArmorRag() {
	}

	public static void onSoundPacket(ClientboundSoundPacket packet) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_SWAP_ARMOR_RAG.get())) {
			return;
		}
		if (packet == null) {
			return;
		}

		int phase = KuudraPhaseTracker.getPhase();
		if (phase != KuudraPhaseTracker.PHASE_HIT && phase != KuudraPhaseTracker.PHASE_SKIP) {
			return;
		}
		if (Math.abs(packet.getPitch() - RAGNAROCK_CAST_PITCH) > RAGNAROCK_CAST_PITCH_EPSILON) {
			return;
		}

		int sliderVal = UiDefinitions.AUTO_SWAP_ARMOR_RAG_SLOT.get().intValue();
		int armorSlot = sliderVal + 1;
		if (armorSlot < 1 || armorSlot > 9) {
			return;
		}
		if (!isRagnarockCastSound(packet.getSound())) {
			return;
		}
		if (!isHoldingRagnarockAxe()) {
			return;
		}

		TickTaskScheduler.schedule(2, () -> triggerSwap(armorSlot));
	}

	private static void triggerSwap(int armorSlot) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		AutoRendHelper.WDSwapSlot(armorSlot);
	}

	private static boolean isHoldingRagnarockAxe() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}

		ItemStack stack = client.player.getMainHandItem();
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag = customData == null ? CustomData.EMPTY.copyTag() : customData.copyTag();
		return RAGNAROCK_AXE_ID.equals(tag.getString("id").orElse(""));
	}

	private static boolean isRagnarockCastSound(Holder<SoundEvent> sound) {
		if (sound == null || sound.value() == null) {
			return false;
		}

		return RAGNAROCK_CAST_SOUND_IDS.contains(sound.value().location());
	}

	private static Set<Identifier> createRagnarockCastSoundIds() {
		Set<Identifier> soundIds = new HashSet<>();
		for (WolfSoundVariant variant : SoundEvents.WOLF_SOUNDS.values()) {
			if (variant == null || variant.deathSound() == null || variant.deathSound().value() == null) {
				continue;
			}
			soundIds.add(variant.deathSound().value().location());
		}
		return Set.copyOf(soundIds);
	}
}
