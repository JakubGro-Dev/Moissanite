package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.command.CommandReplacer;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatScreen.class)
public class ChatScreenCommandReplacerMixin {
	@ModifyArg(
			method = "handleChatInput",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendCommand(Ljava/lang/String;)V"),
			index = 0)
	private String moissanite$rewriteTypedCommand(String command) {
		return CommandReplacer.rewriteTypedCommand(command);
	}
}
