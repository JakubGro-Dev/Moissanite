package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.util.customgui.CustomGui;
import com.crussion.moissanite.util.customgui.CustomGuiAccess;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ScreenCustomGuiInputMixin {
	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void moissanite$charTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof AbstractContainerScreen<?> containerScreen)) {
			return;
		}
		CustomGui customGui = CustomGuiAccess.get(containerScreen);
		if (customGui != null && customGui.charTyped(input)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true)
	private void moissanite$keyReleased(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof AbstractContainerScreen<?> containerScreen)) {
			return;
		}
		CustomGui customGui = CustomGuiAccess.get(containerScreen);
		if (customGui != null && customGui.keyReleased(input)) {
			cir.setReturnValue(true);
		}
	}
}
