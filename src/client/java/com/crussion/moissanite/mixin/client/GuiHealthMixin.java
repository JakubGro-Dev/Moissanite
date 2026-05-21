package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public class GuiHealthMixin {
	@ModifyArg(
			method = "renderPlayerHealth",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"),
			index = 10)
	private boolean moissanite$disableHealthHeartBlink(boolean blinking) {
		return Boolean.TRUE.equals(UiDefinitions.STATIC_HEALTH_HEARTS.get()) ? false : blinking;
	}
}
