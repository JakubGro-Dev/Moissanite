package com.crussion.moissanite.mixin.client;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Language.class)
public interface SpooferLanguageAccessor {
    @Invoker("loadDefault")
    static Language invokeLoadDefault() {
        throw new UnsupportedOperationException();
    }
}
