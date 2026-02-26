package com.crussion.moissanite.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
	@Accessor("textures")
	Map<String, ?> moissanite$getTextures();

	@Accessor("textureTransform")
	TextureTransform moissanite$getTextureTransform();

	@Accessor("outputTarget")
	OutputTarget moissanite$getOutputTarget();

	@Accessor("outlineProperty")
	RenderSetup.OutlineProperty moissanite$getOutlineProperty();

	@Accessor("layeringTransform")
	LayeringTransform moissanite$getLayeringTransform();

	@Accessor("useLightmap")
	boolean moissanite$isUseLightmap();

	@Accessor("useOverlay")
	boolean moissanite$isUseOverlay();

	@Accessor("affectsCrumbling")
	boolean moissanite$isAffectsCrumbling();

	@Accessor("sortOnUpload")
	boolean moissanite$isSortOnUpload();

	@Accessor("bufferSize")
	int moissanite$getBufferSize();

	@Invoker("<init>")
	static RenderSetup moissanite$invokeInit(
			RenderPipeline pipeline,
			Map<String, ?> textures,
			boolean useLightmap,
			boolean useOverlay,
			LayeringTransform layeringTransform,
			OutputTarget outputTarget,
			TextureTransform textureTransform,
			RenderSetup.OutlineProperty outlineProperty,
			boolean affectsCrumbling,
			boolean sortOnUpload,
			int bufferSize) {
		throw new AssertionError("Mixin invoker not transformed");
	}
}
