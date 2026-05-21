package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.general.GeneralTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Shadow
	protected abstract BlockState asState();

	@Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
	private void moissanite$ignoreGrassOutlineShape(
			BlockGetter blockGetter,
			BlockPos blockPos,
			CollisionContext collisionContext,
			CallbackInfoReturnable<VoxelShape> cir) {
		if (GeneralTweaks.noGrassRenderEnabled() && GeneralTweaks.isGrassLike(asState())) {
			cir.setReturnValue(Shapes.empty());
		}
	}
}
