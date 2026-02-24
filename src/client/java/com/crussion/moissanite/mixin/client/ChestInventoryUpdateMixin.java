package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.visual.storage.StorageOverlayFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1:1 port of Firmament's SlotUpdateListener.java.
 * Intercepts chest inventory content packets to trigger
 * StorageOverlayFeature.onChestContentUpdate().
 */
@Mixin(ClientPacketListener.class)
public abstract class ChestInventoryUpdateMixin extends ClientCommonPacketListenerImpl {
    protected ChestInventoryUpdateMixin(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void moissanite$onSingleSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        var player = this.minecraft.player;
        if (player != null && packet.getContainerId() == player.containerMenu.containerId
                && packet.getContainerId() != 0) {
            StorageOverlayFeature.onChestContentUpdate();
        }
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void moissanite$onMultiSlotUpdate(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        var player = this.minecraft.player;
        if (player != null && packet.containerId() == player.containerMenu.containerId && packet.containerId() != 0) {
            StorageOverlayFeature.onChestContentUpdate();
        }
    }
}
