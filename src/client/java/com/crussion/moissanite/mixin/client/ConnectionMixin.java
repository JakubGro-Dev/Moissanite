package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.cheats.WardrobeKeybinds;
import com.crussion.moissanite.features.dungeons.terminals.DungeonsTerminals;
import com.crussion.moissanite.features.kuudra.KuudraAutoPickupSupply;
import com.crussion.moissanite.util.rotation.RotationController;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void moissanite$send(Packet<?> packet, CallbackInfo ci) {
		KuudraAutoPickupSupply.onPacketSent(packet);
		if (packet instanceof ServerboundContainerClosePacket && WardrobeKeybinds.isEnabledOrActive()) {
			WardrobeKeybinds.onClosePacketSent();
		}
		RotationController.onPacketSent(packet);
	}

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
	private void moissanite$send(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
		KuudraAutoPickupSupply.onPacketSent(packet);
		if (packet instanceof ServerboundContainerClosePacket && WardrobeKeybinds.isEnabledOrActive()) {
			WardrobeKeybinds.onClosePacketSent();
		}
		RotationController.onPacketSent(packet);
	}

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
	private void moissanite$send(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
		KuudraAutoPickupSupply.onPacketSent(packet);
		if (packet instanceof ServerboundContainerClosePacket && WardrobeKeybinds.isEnabledOrActive()) {
			WardrobeKeybinds.onClosePacketSent();
		}
		if (DungeonsTerminals.onPacketSent(packet)) {
			ci.cancel();
			return;
		}
		RotationController.onPacketSent(packet);
	}
}
