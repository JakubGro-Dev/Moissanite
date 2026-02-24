package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.StarItemstack;

import java.util.Objects;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsStarItemstackMixin {
	@Unique
	private boolean moissanite$starOverrideRenderActive;

	@Inject(
			method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void moissanite$overrideItemCountFromStars(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
		moissanite$applyStarCountOverride(font, stack, x, y, null, ci);
	}

	@Inject(
			method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void moissanite$overrideItemCountFromStarsWithText(Font font, ItemStack stack, int x, int y,
			String text, CallbackInfo ci) {
		moissanite$applyStarCountOverride(font, stack, x, y, text, ci);
	}

	@Unique
	private void moissanite$applyStarCountOverride(Font font, ItemStack stack, int x, int y, String currentText,
			CallbackInfo ci) {
		if (this.moissanite$starOverrideRenderActive) {
			return;
		}

		String overrideCount = StarItemstack.resolveCountText(stack);
		if (overrideCount == null || Objects.equals(overrideCount, currentText)) {
			return;
		}

		GuiGraphics self = (GuiGraphics) (Object) this;
		this.moissanite$starOverrideRenderActive = true;
		try {
			self.renderItemDecorations(font, stack, x, y, overrideCount);
		} finally {
			this.moissanite$starOverrideRenderActive = false;
		}
		ci.cancel();
	}
}
