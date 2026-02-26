package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void moissanite$cancelHurtCamera(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (Boolean.TRUE.equals(UiDefinitions.NO_TILT.get())) {
            ci.cancel();
        }
    }
}
