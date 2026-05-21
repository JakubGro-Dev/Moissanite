package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.general.GeneralTweaks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
	@Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
	private void moissanite$skipGrassRender(
			BlockState blockState,
			BlockPos blockPos,
			BlockAndTintGetter blockAndTintGetter,
			PoseStack poseStack,
			VertexConsumer vertexConsumer,
			boolean checkSides,
			List<BlockModelPart> parts,
			CallbackInfo ci) {
		if (GeneralTweaks.noGrassRenderEnabled() && GeneralTweaks.isGrassLike(blockState)) {
			ci.cancel();
		}
	}
}
