package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.ui.imgui.ImGuiHandler;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftImGuiMixin {
	@Shadow
	@Final
	private Window window;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void moissanite$initializeImGui(GameConfig gameConfig, CallbackInfo ci) {
		ImGuiHandler.initialize(this.window.handle());
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void moissanite$disposeImGui(CallbackInfo ci) {
		ImGuiHandler.dispose();
	}
}
