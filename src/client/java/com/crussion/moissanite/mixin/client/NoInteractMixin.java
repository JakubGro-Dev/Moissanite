package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class NoInteractMixin {
	@Shadow
	public abstract InteractionResult useItem(Player player, InteractionHand hand);

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void moissanite$blockCloseRangeBlockInteract(
			LocalPlayer player,
			InteractionHand hand,
			BlockHitResult hitResult,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!Boolean.TRUE.equals(UiDefinitions.NO_INTERACT.get())) {
			return;
		}
		if (player == null || hand == null || hitResult == null) {
			return;
		}

		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty() || !held.is(Items.ENDER_PEARL)) {
			return;
		}

		cir.setReturnValue(useItem(player, hand));
	}
}
