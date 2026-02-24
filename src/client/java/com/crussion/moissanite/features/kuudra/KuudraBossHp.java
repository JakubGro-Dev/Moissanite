package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.MagmaCube;

import java.text.DecimalFormat;

public final class KuudraBossHp {
	private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("##.##");
	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath("moissanite", "kuudra_boss_hp_bar");
	private static boolean initialized;

	private KuudraBossHp() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		HudElementRegistry.attachElementBefore(VanillaHudElements.SUBTITLES, HUD_ELEMENT_ID, KuudraBossHp::onHudRender);
	}

	private static void onHudRender(GuiGraphics graphics, DeltaTracker tickCounter) {
		if (!Boolean.TRUE.equals(UiDefinitions.KUUDRA_HP_BOSSBAR.get())) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")) {
			return;
		}

		int phase = KuudraPhaseTracker.getPhase();
		if (phase < KuudraPhaseTracker.PHASE_SUPPLY || phase == KuudraPhaseTracker.PHASE_END) {
			return;
		}

		MagmaCube boss = KuudraPhaseTracker.getKuudraEntity();
		if (boss == null) {
			return;
		}

		float health = boss.getHealth();
		float percent = resolveKuudraPercent(phase, health);
		String bossHpMessage = "Kuudra HP: " + PERCENT_FORMATTER.format(percent * 100f) + "%";

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int centerX = screenWidth / 2;
		int textY = 23;
		int color = phase >= KuudraPhaseTracker.PHASE_DPS ? 0xFFFFFF55 : 0xFFFF5555;
		graphics.drawCenteredString(client.font, bossHpMessage, centerX, textY, color);
	}

	private static float resolveKuudraPercent(int phase, float health) {
		float value;
		if (phase >= KuudraPhaseTracker.PHASE_DPS) {
			value = Math.max(0f, health * 9600f) / 240_000_000f;
		} else {
			value = Math.max(0f, health - 25_000f) / 75_000f;
		}
		return Math.max(0.0f, Math.min(1.0f, value));
	}
}
