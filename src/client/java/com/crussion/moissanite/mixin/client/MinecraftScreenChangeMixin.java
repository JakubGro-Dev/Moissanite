package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.visual.storage.StorageOverlayFeature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenChangeMixin {
	@Shadow
	public Screen screen;

	@ModifyVariable(
			method = "setScreen",
			at = @At("HEAD"),
			argsOnly = true)
	private Screen moissanite$onSetScreen(Screen nextScreen) {
		return StorageOverlayFeature.onScreenChange(this.screen, nextScreen);
	}
}
