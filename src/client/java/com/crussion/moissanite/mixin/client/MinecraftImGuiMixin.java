package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.ui.imgui.ImGuiHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftImGuiMixin {
	@Inject(method = "close", at = @At("HEAD"))
	private void moissanite$disposeImGui(CallbackInfo ci) {
		ImGuiHandler.dispose();
	}
}
