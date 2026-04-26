package com.crussion.moissanite.features.visual;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.mixin.client.RenderTypeAccessor;
import com.crussion.moissanite.util.kuudra.KuudraEntityFinder;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class KuudraEsp {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static RenderType kuudraEspRenderType;

	private static boolean initialized;

	private KuudraEsp() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		WorldRenderEvents.END_MAIN.register(KuudraEsp::render);
	}

	private static void render(WorldRenderContext context) {
		if (context == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null || client.gameRenderer == null) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.KUUDRA_ESP.get())) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}

		MagmaCube kuudra = KuudraEntityFinder.findKuudra(client);
		if (kuudra == null || context.matrices() == null || context.consumers() == null) {
			return;
		}

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		AABB box = kuudra.getBoundingBox();
		float lineWidth = resolveLineWidth(UiDefinitions.KUUDRA_ESP_LINE_WIDTH.get());
		int color = UiDefinitions.KUUDRA_ESP_COLOR.argb();

		ShapeRenderer.renderShape(
				context.matrices(),
				context.consumers().getBuffer(getKuudraEspRenderType()),
				Shapes.create(box),
				-cameraPos.x,
				-cameraPos.y,
				-cameraPos.z,
				color,
				lineWidth);
	}

	private static float resolveLineWidth(Double raw) {
		double value = raw != null && Double.isFinite(raw) ? raw : 3.0;
		return (float) Mth.clamp(value, 1.0, 10.0);
	}

	private static RenderType getKuudraEspRenderType() {
		if (kuudraEspRenderType == null) {
			kuudraEspRenderType = createKuudraEspRenderType();
		}
		return kuudraEspRenderType;
	}

	private static RenderType createKuudraEspRenderType() {
		RenderPipeline source = RenderPipelines.LINES;
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation("moissanite/kuudra_esp_lines")
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

		RenderPipeline pipeline = RenderPipelines.register(builder.build());
		RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
		return RenderType.create("moissanite_kuudra_esp_lines", setup);
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
}
