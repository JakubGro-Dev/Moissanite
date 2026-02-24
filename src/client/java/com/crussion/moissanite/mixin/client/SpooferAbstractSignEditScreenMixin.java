package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.spoofer.ClientSpooferOptions;
import com.crussion.moissanite.spoofer.util.ComponentUtils;
import com.crussion.moissanite.spoofer.util.ToastUtils;

import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSignEditScreen.class)
public abstract class SpooferAbstractSignEditScreenMixin {
    @Redirect(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"
            )
    )
    private Stream<String> moissanite$mapMessages(Stream<Component> instance, Function<Component, String> function) {
        return instance.map(message -> {
            if (ClientSpooferOptions.hideMods()) {
                String str = ComponentUtils.getString(message);
                if (!str.equals(message.getString())) {
                    ToastUtils.showServerAttemptedReadingModsToast();
                }
                return str;
            }

            return message.getString();
        });
    }
}
