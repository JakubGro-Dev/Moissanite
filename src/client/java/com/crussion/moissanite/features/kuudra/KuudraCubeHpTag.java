package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.kuudra.KuudraEntityFinder;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.render.WorldTextRenderer;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.phys.Vec3;

import java.text.DecimalFormat;

public final class KuudraCubeHpTag {
	private static final double KUUDRA_MAX_RAW_HEALTH = 100_000.0D;
	private static final double KUUDRA_DPS_RAW_HEALTH_CAP = 25_000.0D;
	private static final double KUUDRA_DPS_HEALTH_MULTIPLIER = 9_600.0D;
	private static final double KUUDRA_PRE_DPS_HEALTH_FLOOR = 25_000.0D;
	private static final double KUUDRA_PRE_DPS_HEALTH_RANGE = 75_000.0D;
	private static final double KUUDRA_DPS_MAX_DISPLAY_HEALTH = KUUDRA_DPS_RAW_HEALTH_CAP * KUUDRA_DPS_HEALTH_MULTIPLIER;
	private static final float KUUDRA_HP_TAG_SCALE = 5.0f;
	private static final float KUUDRA_HP_TAG_Y_OFFSET = -10.0f;
	private static final int KUUDRA_HP_TAG_BACKGROUND = 0x88000000;
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
		if (!Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_TAG.get())) {
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

		double rawHealth = boss.getHealth();
		if (!isRenderableHealth(rawHealth)) {
			return;
		}

		String tag = resolveTagText(phase, rawHealth);
		int color = resolveTagColor(phase, rawHealth);

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		Vec3 bossCenter = boss.getBoundingBox().getCenter();
		Vec3 toCamera = cameraPos.subtract(bossCenter);
		if (toCamera.lengthSqr() > 1.0e-6) {
			bossCenter = bossCenter.add(toCamera.normalize().scale(Math.max(0.5, boss.getBbWidth() * 0.1)));
		}
		WorldTextRenderer.drawText(
				context,
				bossCenter,
				tag,
				color,
				KUUDRA_HP_TAG_SCALE,
				true,
				KUUDRA_HP_TAG_Y_OFFSET,
				KUUDRA_HP_TAG_BACKGROUND);
	}

	private static boolean isRenderableHealth(double rawHealth) {
		return rawHealth > 0.0D && rawHealth <= KUUDRA_MAX_RAW_HEALTH;
	}

	private static String resolveTagText(int phase, double rawHealth) {
		double displayedHealth = resolveDisplayedHealth(phase, rawHealth);
		double percent = resolveHealthPercent(phase, rawHealth) * 100.0D;
		return formatHealth(displayedHealth) + " HP [" + SHORTHAND_FORMATTER.format(percent) + "%]";
	}

	private static int resolveTagColor(int phase, double rawHealth) {
		double percent = resolveHealthPercent(phase, rawHealth);
		if (percent <= 0.33D) {
			return 0xFF55FF55;
		}
		if (percent <= 0.66D) {
			return 0xFFFFFF55;
		}
		return phase >= KuudraPhaseTracker.PHASE_DPS ? 0xFFFFAA00 : 0xFFFF5555;
	}

	private static double resolveDisplayedHealth(int phase, double rawHealth) {
		double safe = clamp(rawHealth, 0.0D, KUUDRA_MAX_RAW_HEALTH);
		if (phase >= KuudraPhaseTracker.PHASE_DPS) {
			return Math.min(safe, KUUDRA_DPS_RAW_HEALTH_CAP) * KUUDRA_DPS_HEALTH_MULTIPLIER;
		}
		return safe;
	}

	private static double resolveHealthPercent(int phase, double rawHealth) {
		double safe = clamp(rawHealth, 0.0D, KUUDRA_MAX_RAW_HEALTH);
		if (phase >= KuudraPhaseTracker.PHASE_DPS) {
			return clamp(resolveDisplayedHealth(phase, safe) / KUUDRA_DPS_MAX_DISPLAY_HEALTH, 0.0D, 1.0D);
		}
		return clamp((safe - KUUDRA_PRE_DPS_HEALTH_FLOOR) / KUUDRA_PRE_DPS_HEALTH_RANGE, 0.0D, 1.0D);
	}

	private static String formatHealth(double number) {
		double safe = Math.max(0.0D, number);
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

	private static double clamp(double value, double min, double max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}
}
