package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.cheats.WardrobeKeybinds;

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
		if (packet instanceof ServerboundContainerClosePacket) {
			WardrobeKeybinds.onClosePacketSent();
		}
	}

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
	private void moissanite$send(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
		if (packet instanceof ServerboundContainerClosePacket) {
			WardrobeKeybinds.onClosePacketSent();
		}
	}

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
	private void moissanite$send(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
		if (packet instanceof ServerboundContainerClosePacket) {
			WardrobeKeybinds.onClosePacketSent();
		}
	}
}
