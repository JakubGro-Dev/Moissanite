package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.spoofer.ClientSpooferOptions;
import com.crussion.moissanite.spoofer.util.ComponentUtils;
import com.crussion.moissanite.spoofer.util.ToastUtils;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreen.class)
public class SpooferAnvilScreenMixin {
    @Redirect(
            method = "slotChanged(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"
            )
    )
    private String moissanite$slotChangedGetString(Component instance) {
        if (ClientSpooferOptions.hideMods()) {
            String str = ComponentUtils.getString(instance);
            if (!str.equals(instance.getString())) {
                ToastUtils.showServerAttemptedReadingModsToast();
            }
            return str;
        }

        return instance.getString();
    }

    @Redirect(
            method = "onNameChanged(Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"
            )
    )
    private String moissanite$onNameChangedGetString(Component instance) {
        if (ClientSpooferOptions.hideMods()) {
            String str = ComponentUtils.getString(instance);
            if (!str.equals(instance.getString())) {
                ToastUtils.showServerAttemptedReadingModsToast();
            }
            return str;
        }

        return instance.getString();
    }
}
