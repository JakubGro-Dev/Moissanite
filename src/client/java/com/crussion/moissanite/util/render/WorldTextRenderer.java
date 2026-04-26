package com.crussion.moissanite.util.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class WorldTextRenderer {
	private static final float KIC_TEXT_SCALE = 0.025f;

	private WorldTextRenderer() {
	}

	public static void drawText(
			WorldRenderContext context,
			Vec3 worldPos,
			String text,
			int color,
			float scale,
			boolean seeThrough) {
		drawText(context, worldPos, text, color, scale, seeThrough, 0.0f, 0);
	}

	public static void drawText(
			WorldRenderContext context,
			Vec3 worldPos,
			String text,
			int color,
			float scale,
			boolean seeThrough,
			float yOffset,
			int backgroundColor) {
		Minecraft client = Minecraft.getInstance();
		if (context == null || context.matrices() == null || context.consumers() == null || client == null
				|| client.gameRenderer == null || client.font == null || worldPos == null || text == null || text.isEmpty()) {
			return;
		}

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		float actualScale = scale * KIC_TEXT_SCALE;

		context.matrices().pushPose();
		context.matrices().translate(
				worldPos.x - cameraPos.x,
				worldPos.y - cameraPos.y,
				worldPos.z - cameraPos.z);
		context.matrices().mulPose(client.gameRenderer.getMainCamera().rotation());
		context.matrices().scale(actualScale, -actualScale, actualScale);

		Matrix4f pose = context.matrices().last().pose();
		float textWidth = client.font.width(text);
		client.font.drawInBatch(
				text,
				-textWidth / 2.0f,
				yOffset,
				color,
				true,
				pose,
				context.consumers(),
				seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
				backgroundColor,
				LightTexture.FULL_BRIGHT);
		context.matrices().popPose();
	}
}
