package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private void moissanite$onPlaySound(SoundInstance sound, CallbackInfoReturnable<?> ci) {
        if (sound == null || sound.getIdentifier() == null) return;
        
        boolean swapRag = Boolean.TRUE.equals(UiDefinitions.AUTO_SWAP_ARMOR_RAG.get());
        boolean trackDebug = Boolean.TRUE.equals(UiDefinitions.TRACK_SOUNDS_DEBUG.get());

        if (!swapRag && !trackDebug) return;

        String soundName = sound.getIdentifier().toString();
        float pitch = 1.0f;
        try {
            pitch = sound.getPitch();
        } catch (Throwable t) {}
        
        if (swapRag) {
            com.crussion.moissanite.features.kuudra.KuudraAutoSwapArmorRag.onSound(soundName, pitch);
        }

        if (trackDebug) {
            float vol = 1.0f;
            try {
                vol = sound.getVolume();
            } catch (Throwable t) {}
            
            FeatureChat.sendPrefixed("Track Sounds", String.format("%s (Vol: %.2f Pitch: %.2f)", 
                soundName, vol, pitch));
        }
    }
}
