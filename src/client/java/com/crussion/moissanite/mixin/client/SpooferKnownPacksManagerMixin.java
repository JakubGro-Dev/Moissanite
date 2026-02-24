package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.spoofer.ClientSpooferOptions;
import com.crussion.moissanite.spoofer.SpoofMode;

import java.util.Locale;
import java.util.Map;

import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KnownPacksManager.class)
public class SpooferKnownPacksManagerMixin {
    @Redirect(
            method = "trySelectingPacks(Ljava/util/List;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private <V> V moissanite$redirectSelectPacks(Map<KnownPack, V> instance, Object object) {
        KnownPack pack = (KnownPack) object;

        if (!"fabric".equalsIgnoreCase(pack.namespace())) {
            return instance.get(pack);
        }

        SpoofMode mode = ClientSpooferOptions.SPOOF_MODE;
        if (mode == SpoofMode.OFF) {
            return instance.get(pack);
        }
        if (mode == SpoofMode.VANILLA) {
            return null;
        }
        if (mode == SpoofMode.MODDED) {
            String id = pack.id().toLowerCase(Locale.ROOT);
            for (String mod : ClientSpooferOptions.ALLOWED_MODS) {
                if (id.startsWith(mod.toLowerCase(Locale.ROOT))) {
                    return instance.get(pack);
                }
            }
            return null;
        }
        if (!ClientSpooferOptions.hideMods()) {
            return instance.get(pack);
        }
        return ClientSpooferOptions.isBlacklistedMod(pack.id()) ? null : instance.get(pack);
    }
}
