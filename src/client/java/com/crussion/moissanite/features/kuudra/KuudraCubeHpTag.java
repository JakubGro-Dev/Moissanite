package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.kuudra.KuudraEntityFinder;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.text.DecimalFormat;

public final class KuudraCubeHpTag {
	private static final DecimalFormat HEALTH_FORMATTER = new DecimalFormat("###,###");
	private static final DecimalFormat SHORTHAND_FORMATTER = new DecimalFormat("0.#");
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
		client.font.drawInBatch(
				tag,
				-textWidth / 2.0f,
				-4.0f,
				color,
				false,
				pose,
				buffer,
				net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
				0x66000000,
				LightTexture.FULL_BRIGHT);
		matrixStack.popPose();
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
