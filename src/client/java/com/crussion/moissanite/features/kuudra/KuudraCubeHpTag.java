package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.mixin.client.RenderSetupAccessor;
import com.crussion.moissanite.mixin.client.RenderTypeAccessor;
import com.crussion.moissanite.util.kuudra.KuudraEntityFinder;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class KuudraCubeHpTag {
	private static final DecimalFormat HEALTH_FORMATTER = new DecimalFormat("###,###");
	private static final DecimalFormat SHORTHAND_FORMATTER = new DecimalFormat("0.#");
	private static final Map<String, RenderPipeline> HP_TEXT_PIPELINES = new HashMap<>();
	private static final Map<RenderType, RenderType> HP_TEXT_RENDER_TYPE_OVERRIDES = new WeakHashMap<>();
	private static boolean initialized;

	private KuudraCubeHpTag() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		WorldRenderEvents.END_MAIN.register(KuudraCubeHpTag::onWorldRender);
	}

	private static void onWorldRender(WorldRenderContext context) {
		boolean tagEnabled = Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_TAG.get());
		boolean bossbarEnabled = Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_BOSSBAR.get());
		if (!tagEnabled && !bossbarEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.gameRenderer == null || context.matrices() == null
				|| context.consumers() == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")) {
			return;
		}

		int phase = KuudraPhaseTracker.getPhase();

		MagmaCube boss = KuudraPhaseTracker.getKuudraEntity();
		if (boss == null) {
			boss = KuudraEntityFinder.findKuudra(client);
		}
		if (boss == null) {
			return;
		}

		float health = boss.getHealth();
		boolean dpsPhase = phase >= KuudraPhaseTracker.PHASE_DPS;
		String tag = dpsPhase
				? parseToShorthandNumber(health * 9600f)
				: parseToShorthandNumber(Math.max(0f, health));
		int color = dpsPhase ? 0xFFFFFF55 : 0xFFFF5555;

		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		Vec3 bossCenter = boss.getBoundingBox().getCenter();
		Vec3 toCamera = cameraPos.subtract(bossCenter);
		if (toCamera.lengthSqr() > 1.0e-6) {
			bossCenter = bossCenter.add(toCamera.normalize().scale(Math.max(0.5, boss.getBbWidth() * 0.1)));
		}
		PoseStack matrixStack = context.matrices();

		matrixStack.pushPose();
		matrixStack.translate(
				bossCenter.x - cameraPos.x,
				bossCenter.y - cameraPos.y,
				bossCenter.z - cameraPos.z);
		matrixStack.mulPose(camera.rotation());
		matrixStack.scale(-0.045f, -0.045f, 0.045f);

		Matrix4f pose = matrixStack.last().pose();
		float textWidth = client.font.width(tag);
		var buffer = context.consumers();
		MultiBufferSource remappedBuffer = renderType -> buffer.getBuffer(resolveHpTextRenderType(renderType));
		client.font.drawInBatch(
				tag,
				-textWidth / 2.0f,
				-4.0f,
				color,
				false,
				pose,
				remappedBuffer,
				net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
				0x66000000,
				LightTexture.FULL_BRIGHT);
		matrixStack.popPose();
	}

	private static RenderType resolveHpTextRenderType(RenderType sourceRenderType) {
		RenderType existing = HP_TEXT_RENDER_TYPE_OVERRIDES.get(sourceRenderType);
		if (existing != null) {
			return existing;
		}

		RenderPipeline sourcePipeline = sourceRenderType.pipeline();
		RenderPipeline targetPipeline = resolveHpTextPipeline(sourcePipeline);
		RenderSetup targetSetup = cloneRenderSetup(sourceRenderType, targetPipeline);
		String renderTypeName = "moissanite_kuudra_cube_hp_text_" + sanitizePipelineLocation(sourcePipeline) + "_"
				+ Integer.toHexString(System.identityHashCode(sourceRenderType));
		RenderType targetRenderType = RenderTypeAccessor.moissanite$invokeCreate(renderTypeName, targetSetup);
		HP_TEXT_RENDER_TYPE_OVERRIDES.put(sourceRenderType, targetRenderType);
		return targetRenderType;
	}

	private static RenderPipeline resolveHpTextPipeline(RenderPipeline sourcePipeline) {
		String key = sourcePipeline.getLocation().toString();
		RenderPipeline existing = HP_TEXT_PIPELINES.get(key);
		if (existing != null) {
			return existing;
		}
		RenderPipeline pipeline = createHpTextPipeline(sourcePipeline);
		HP_TEXT_PIPELINES.put(key, pipeline);
		return pipeline;
	}

	private static RenderPipeline createHpTextPipeline(RenderPipeline source) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation("moissanite/kuudra_cube_hp_text_" + sanitizePipelineLocation(source))
				.withVertexShader(source.getVertexShader())
				.withFragmentShader(source.getFragmentShader())
				.withCull(source.isCull())
				.withDepthWrite(false)
				.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
				.withVertexFormat(source.getVertexFormat(), source.getVertexFormatMode());

		source.getBlendFunction().ifPresent(builder::withBlend);
		for (String sampler : source.getSamplers()) {
			builder.withSampler(sampler);
		}
		for (RenderPipeline.UniformDescription uniform : source.getUniforms()) {
			if (uniform.type() == UniformType.TEXEL_BUFFER) {
				builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
				continue;
			}
			builder.withUniform(uniform.name(), uniform.type());
		}
		source.getShaderDefines().flags().forEach(builder::withShaderDefine);
		source.getShaderDefines().values().forEach((key, value) -> applyNumericShaderDefine(builder, key, value));
		return builder.build();
	}

	private static RenderSetup cloneRenderSetup(RenderType sourceRenderType, RenderPipeline targetPipeline) {
		RenderSetup sourceSetup = ((RenderTypeAccessor) (Object) sourceRenderType).moissanite$getState();
		RenderSetupAccessor sourceAccessor = (RenderSetupAccessor) (Object) sourceSetup;
		return RenderSetupAccessor.moissanite$invokeInit(
				targetPipeline,
				sourceAccessor.moissanite$getTextures(),
				sourceAccessor.moissanite$isUseLightmap(),
				sourceAccessor.moissanite$isUseOverlay(),
				sourceAccessor.moissanite$getLayeringTransform(),
				sourceAccessor.moissanite$getOutputTarget(),
				sourceAccessor.moissanite$getTextureTransform(),
				sourceAccessor.moissanite$getOutlineProperty(),
				sourceAccessor.moissanite$isAffectsCrumbling(),
				sourceAccessor.moissanite$isSortOnUpload(),
				sourceAccessor.moissanite$getBufferSize());
	}

	private static void applyNumericShaderDefine(RenderPipeline.Builder builder, String key, String value) {
		if (builder == null || key == null || key.isBlank() || value == null || value.isBlank()) {
			return;
		}
		try {
			builder.withShaderDefine(key, Integer.parseInt(value));
			return;
		} catch (NumberFormatException ignored) {
		}
		try {
			builder.withShaderDefine(key, Float.parseFloat(value));
		} catch (NumberFormatException ignored) {
		}
	}

	private static String sanitizePipelineLocation(RenderPipeline sourcePipeline) {
		return sourcePipeline.getLocation().toString()
				.replace(':', '_')
				.replace('/', '_');
	}

	private static String parseToShorthandNumber(float number) {
		float safe = Math.max(0f, number);
		if (safe >= 1_000_000_000f) {
			return SHORTHAND_FORMATTER.format(safe / 1_000_000_000f) + "B";
		}
		if (safe >= 1_000_000f) {
			return SHORTHAND_FORMATTER.format(safe / 1_000_000f) + "M";
		}
		if (safe >= 1_000f) {
			return SHORTHAND_FORMATTER.format(safe / 1_000f) + "K";
		}
		return HEALTH_FORMATTER.format(safe);
	}
}
