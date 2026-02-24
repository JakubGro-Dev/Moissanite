package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.spoofer.ClientSpooferOptions;
import com.crussion.moissanite.spoofer.SpoofMode;

import io.netty.channel.ChannelFutureListener;
import java.util.Locale;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class SpooferConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void moissanite$sendPacket(
            Packet<?> packet,
            ChannelFutureListener channelFutureListener,
            boolean flush,
            CallbackInfo ci
    ) {
        if (!(packet instanceof ServerboundCustomPayloadPacket customPayloadPacket)) {
            return;
        }

        CustomPacketPayload payload = customPayloadPacket.payload();
        if (payload instanceof DiscardedPayload || payload instanceof BrandPayload) {
            return;
        }

        SpoofMode mode = ClientSpooferOptions.SPOOF_MODE;
        if (mode == SpoofMode.OFF) {
            return;
        }
        if (ClientSpooferOptions.DISABLE_CUSTOM_PAYLOADS) {
            if (mode == SpoofMode.CUSTOM
                    && matchesAnyPrefix(payload, ClientSpooferOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS)) {
                return;
            }
            ci.cancel();
            return;
        }
        if (mode == SpoofMode.VANILLA) {
            ci.cancel();
            return;
        }
        if (mode == SpoofMode.MODDED) {
            if (!matchesAnyPrefix(payload, ClientSpooferOptions.ALLOWED_MODS)) {
                ci.cancel();
            }
            return;
        }
        if (mode == SpoofMode.CUSTOM) {
            return;
        }
        if (mode == SpoofMode.HIDE_ONLY_MOISSANITE
                && ClientSpooferOptions.isBlacklistedChannel(payload.type().id().toString())) {
            ci.cancel();
        }
    }

    private static boolean matchesAnyPrefix(CustomPacketPayload payload, Iterable<String> prefixes) {
        String channel = payload.type().id().toString().toLowerCase(Locale.ROOT);
        for (String prefix : prefixes) {
            if (channel.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
