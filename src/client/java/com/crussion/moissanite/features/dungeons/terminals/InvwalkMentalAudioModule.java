package com.crussion.moissanite.features.dungeons.terminals;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

final class InvwalkMentalAudioModule {
	private static final int MIN_SOUND_INDEX = 1;
	private static final int MAX_SOUND_INDEX = 5;

	private boolean activeSession;
	private SoundInstance playingSound;

	void onTick(Minecraft client, boolean invwalkActive) {
		boolean shouldPlay = invwalkActive && TerminalSupport.autoTermsInvwalkMentalEnabled();
		if (!shouldPlay) {
			stop(client);
			activeSession = false;
			return;
		}

		if (activeSession) {
			return;
		}

		activeSession = true;
		playRandomTrack(client);
	}

	void onClose(Minecraft client) {
		activeSession = false;
		stop(client);
	}

	private void playRandomTrack(Minecraft client) {
		if (client == null || client.getSoundManager() == null) {
			return;
		}

		int randomIndex = ThreadLocalRandom.current().nextInt(MIN_SOUND_INDEX, MAX_SOUND_INDEX + 1);
		Identifier soundId = Identifier.fromNamespaceAndPath("moissanite", "invwalk_mental_" + randomIndex);
		SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
		playingSound = SimpleSoundInstance.forUI(soundEvent, 1.0F, 1.0F);
		client.getSoundManager().play(playingSound);
	}

	private void stop(Minecraft client) {
		if (playingSound == null) {
			return;
		}
		if (client != null && client.getSoundManager() != null) {
			client.getSoundManager().stop(playingSound);
		}
		playingSound = null;
	}
}
