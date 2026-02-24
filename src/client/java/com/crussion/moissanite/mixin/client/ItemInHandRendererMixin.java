package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.features.misc.HandVisualTweaks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
		int packedLight
	);

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

	@Redirect(
		method = "renderArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
		)
	)
	private void moissanite$renderItemWithDeferredScale(
		ItemInHandRenderer instance,
		LivingEntity entity,
		ItemStack stack,
		ItemDisplayContext displayContext,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int packedLight
	) {
		if (!moissanite$hasItemVisualTransform()) {
			renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			return;
		}
		poseStack.pushPose();
		moissanite$applyItemVisualTransform(poseStack);
		renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
		poseStack.popPose();
	}

	@ModifyArg(
		method = "swingArm",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
		index = 0
	)
	private float moissanite$scaleSwingTranslateX(float x) {
		return x * moissanite$getSwingScale();
	}

	@ModifyArg(
		method = "swingArm",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
		index = 1
	)
	private float moissanite$scaleSwingTranslateY(float y) {
		return y * moissanite$getSwingScale();
	}

	@ModifyArg(
		method = "swingArm",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
		index = 2
	)
	private float moissanite$scaleSwingTranslateZ(float z) {
		return z * moissanite$getSwingScale();
	}

	@ModifyArg(
		method = "applyItemArmAttackTransform",
		at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 0),
		index = 0
	)
	private float moissanite$scaleSwingAttackRot0(float angle) {
		return angle * moissanite$getSwingScale();
	}

	@ModifyArg(
		method = "applyItemArmAttackTransform",
		at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 1),
		index = 0
	)
	private float moissanite$scaleSwingAttackRot1(float angle) {
		return angle * moissanite$getSwingScale();
	}

	@ModifyArg(
		method = "applyItemArmAttackTransform",
		at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2),
		index = 0
	)
	private float moissanite$scaleSwingAttackRot2(float angle) {
		return angle * moissanite$getSwingScale();
	}

	@ModifyArg(
		method = "applyItemArmAttackTransform",
		at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 3),
		index = 0
	)
	private float moissanite$scaleSwingAttackRot3(float angle) {
		return angle * moissanite$getSwingScale();
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

	@Unique
	private static float moissanite$getSwingScale() {
		if (!HandVisualTweaks.scaleSwingWithHandSize()) {
			return 1.0f;
		}
		double rawScale = Math.abs(1.0 + HandVisualTweaks.handSizeOffset());
		return (float) Math.max(0.0, rawScale);
	}
}
