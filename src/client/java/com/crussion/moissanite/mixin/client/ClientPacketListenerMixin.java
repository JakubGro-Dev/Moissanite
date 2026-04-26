package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.cheats.AutoDirection;
import com.crussion.moissanite.features.cheats.AutoPearl;
import com.crussion.moissanite.features.cheats.WardrobeKeybinds;
import com.crussion.moissanite.features.dungeons.terminals.DungeonsTerminals;
import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.SystemChatFilter;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
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
		if (WardrobeKeybinds.isEnabledOrActive() && WardrobeKeybinds.onOpenScreenPacket(packet)) {
			ci.cancel();
			return;
		}
		if (DungeonsTerminals.onOpenScreenPacket(packet)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleContainerClose", at = @At("HEAD"))
	private void moissanite$handleContainerClose(ClientboundContainerClosePacket packet, CallbackInfo ci) {
		if (WardrobeKeybinds.isEnabledOrActive()) {
			WardrobeKeybinds.onClosePacketReceived();
		}
		DungeonsTerminals.onContainerClosePacketReceived(packet);
	}

	@Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
	private void moissanite$filterSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (SystemChatFilter.shouldHide(packet.content())) {
			ci.cancel();
		}
	}

	@Inject(method = "handleSystemChat", at = @At("TAIL"))
	private void moissanite$handleSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get())) {
			AutoPearl.onSystemChat(packet.content());
		}
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION.get())) {
			AutoDirection.onSystemChat(packet.content());
		}
		if (moissanite$shouldTrackKuudraPhase()) {
			KuudraPhaseTracker.onSystemChat(packet.content());
		}
		if (Boolean.TRUE.equals(UiDefinitions.NO_PRE.get())) {
			com.crussion.moissanite.features.kuudra.KuudraNoPre.onSystemChat(packet.content());
		}
		DungeonsTerminals.onSystemChat(packet.content());
	}

	@Inject(method = "handleContainerSetData", at = @At("TAIL"))
	private void moissanite$handleContainerSetData(ClientboundContainerSetDataPacket packet, CallbackInfo ci) {
		DungeonsTerminals.onContainerSetDataPacket(packet);
	}

	@Inject(method = "setTitleText", at = @At("TAIL"))
	private void moissanite$handleSetTitleText(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get())) {
			AutoPearl.onTitleText(packet.text());
		}
	}

	@Inject(method = "setSubtitleText", at = @At("TAIL"))
	private void moissanite$handleSetSubtitleText(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get())) {
			AutoPearl.onTitleText(packet.text());
		}
	}

	@Inject(method = "setActionBarText", at = @At("TAIL"))
	private void moissanite$handleSetActionBarText(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get())) {
			AutoPearl.onTitleText(packet.text());
		}
	}

	private static boolean moissanite$shouldTrackKuudraPhase() {
		return Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_BOSSBAR.get())
				|| Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_TAG.get())
				|| Boolean.TRUE.equals(UiDefinitions.KUUDRA_SPLITS.get())
				|| Boolean.TRUE.equals(UiDefinitions.KUUDRA_CRATE_WAYPOINTS.get())
				|| Boolean.TRUE.equals(UiDefinitions.KUUDRA_BALLISTA_BUILD_WAYPOINTS.get())
				|| Boolean.TRUE.equals(UiDefinitions.KUUDRA_ESP.get())
				|| Boolean.TRUE.equals(UiDefinitions.REND_DAMAGE.get())
				|| Boolean.TRUE.equals(UiDefinitions.AUTO_REND.get())
				|| Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get())
				|| Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION.get());
	}
}
