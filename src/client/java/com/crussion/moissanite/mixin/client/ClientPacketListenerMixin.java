package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.cheats.AutoDirection;
import com.crussion.moissanite.features.cheats.AutoPearl;
import com.crussion.moissanite.features.cheats.WardrobeKeybinds;
import com.crussion.moissanite.util.chat.SystemChatFilter;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
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

	@Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
	private void moissanite$filterSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (SystemChatFilter.shouldHide(packet.content())) {
			ci.cancel();
		}
	}

	@Inject(method = "handleSystemChat", at = @At("TAIL"))
	private void moissanite$handleSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		AutoDirection.onSystemChat(packet.content());
		KuudraPhaseTracker.onSystemChat(packet.content());
		com.crussion.moissanite.features.kuudra.KuudraNoPre.onSystemChat(packet.content());
	}

	@Inject(method = "setTitleText", at = @At("TAIL"))
	private void moissanite$handleSetTitleText(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
		AutoPearl.onTitleText(packet.text());
	}

	@Inject(method = "setSubtitleText", at = @At("TAIL"))
	private void moissanite$handleSetSubtitleText(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
		AutoPearl.onTitleText(packet.text());
	}

	@Inject(method = "setActionBarText", at = @At("TAIL"))
	private void moissanite$handleSetActionBarText(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		AutoPearl.onTitleText(packet.text());
	}
}
