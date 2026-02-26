package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.HandVisualTweaks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private ItemStack mainHandItem;

	@Shadow
	private ItemStack offHandItem;

	@Shadow
	private float mainHandHeight;

	@Shadow
	private float oMainHandHeight;

	@Shadow
	private float offHandHeight;

	@Shadow
	private float oOffHandHeight;

	@Shadow
	public abstract void renderItem(
			LivingEntity entity,
			ItemStack stack,
			ItemDisplayContext displayContext,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight);

	@Shadow
	protected abstract void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress);

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void moissanite$preventEquipReset(CallbackInfo ci) {
		if (!HandVisualTweaks.noEquipReset()) {
			return;
		}
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		mainHandItem = player.getMainHandItem();
		offHandItem = player.getOffhandItem();
		mainHandHeight = 1.0f;
		oMainHandHeight = 1.0f;
		offHandHeight = 1.0f;
		oOffHandHeight = 1.0f;
		ci.cancel();
	}

	@Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
	private void moissanite$preventUseEquipReset(InteractionHand hand, CallbackInfo ci) {
		if (HandVisualTweaks.noEquipReset()) {
			ci.cancel();
		}
	}

	@Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void moissanite$renderItemWithDeferredScale(
			ItemInHandRenderer instance,
			LivingEntity entity,
			ItemStack stack,
			ItemDisplayContext displayContext,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight) {
		if (!moissanite$hasItemVisualTransform()) {
			renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			return;
		}
		poseStack.pushPose();
		moissanite$applyItemVisualTransform(poseStack);
		renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
		poseStack.popPose();
	}

	@Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
	private void moissanite$applyCustomSwing(
			float swingProgress,
			PoseStack poseStack,
			int handDir,
			HumanoidArm arm,
			CallbackInfo ci) {
		if (!HandVisualTweaks.customSwingAnimation()) {
			return;
		}

		float angle = swingProgress * ((float) Math.PI * 2.0F); // 0 to 2PI
		// The cone path
		float coneZ = net.minecraft.util.Mth.sin(angle) * 30.0F; // Rolls left and right
		float coneX = (net.minecraft.util.Mth.cos(angle) - 1.0F) * 30.0F; // Pitches forward and back

		poseStack.translate(0.0F, -1.0F, 0.0F); // Pivot at the bottom
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(coneX));
		poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) handDir * coneZ));
		poseStack.translate(0.0F, 1.0F, 0.0F); // Translate back

		ci.cancel();
	}

	@Unique
	private static boolean moissanite$hasItemVisualTransform() {
		return HandVisualTweaks.hasHandTransform();
	}

	@Unique
	private static void moissanite$applyItemVisualTransform(PoseStack poseStack) {
		float handX = HandVisualTweaks.handX();
		float handY = HandVisualTweaks.handY();
		float handZ = HandVisualTweaks.handZ();
		if (Math.abs(handX) > 1.0e-4f || Math.abs(handY) > 1.0e-4f || Math.abs(handZ) > 1.0e-4f) {
			poseStack.translate(handX, handY, handZ);
		}
		if (Math.abs(HandVisualTweaks.handSizeOffset()) > 1.0e-4) {
			float handScale = HandVisualTweaks.handScale();
			poseStack.scale(handScale, handScale, handScale);
		}
	}

}
