package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.spoofer.ClientSpooferOptions;
import com.crussion.moissanite.spoofer.SpoofMode;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class SpooferClientBrandRetrieverMixin {
    @Inject(
            method = "getClientModName()Ljava/lang/String;",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private static void moissanite$getClientModName(CallbackInfoReturnable<String> cir) {
        SpoofMode spoofMode = ClientSpooferOptions.SPOOF_MODE;
        if (spoofMode == SpoofMode.VANILLA) {
            cir.setReturnValue("vanilla");
        } else if (spoofMode == SpoofMode.MODDED || spoofMode == SpoofMode.HIDE_ONLY_MOISSANITE) {
            cir.setReturnValue("fabric");
        } else if (spoofMode == SpoofMode.CUSTOM) {
            cir.setReturnValue(ClientSpooferOptions.CUSTOM_CLIENT);
        }
    }
}
