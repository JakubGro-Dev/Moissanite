package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.MagmaCube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin<T extends Entity> {
	@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
	private void moissanite$hideKuudraVanillaHpTag(T entity, double distanceSquared,
			CallbackInfoReturnable<Boolean> cir) {
		if (!(entity instanceof MagmaCube magmaCube)) {
			return;
		}
		boolean customHpEnabled = Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_TAG.get());
		if (!customHpEnabled) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")) {
			return;
		}
		MagmaCube kuudra = KuudraPhaseTracker.getKuudraEntity();
		if (kuudra == null || kuudra.getId() != magmaCube.getId()) {
			return;
		}
		cir.setReturnValue(false);
	}
}
