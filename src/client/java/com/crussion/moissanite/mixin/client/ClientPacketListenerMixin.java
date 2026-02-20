package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.cheats.WardrobeKeybinds;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
	private void moissanite$handleOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
		if (WardrobeKeybinds.onOpenScreenPacket(packet)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleContainerClose", at = @At("HEAD"))
	private void moissanite$handleContainerClose(ClientboundContainerClosePacket packet, CallbackInfo ci) {
		WardrobeKeybinds.onClosePacketReceived();
	}
}
