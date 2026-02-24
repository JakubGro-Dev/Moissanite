package com.crussion.moissanite.util.kuudra;

import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.MagmaCube;

public final class KuudraEntityFinder {
	private static final float KUUDRA_MIN_WIDTH = 12.0F;
	private static final float KUUDRA_HEALTH_CAP = 100_000.0F;

	private KuudraEntityFinder() {
	}

	public static MagmaCube findKuudra(Minecraft client) {
		if (client == null || client.level == null) {
			return null;
		}

		MagmaCube best = null;

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof MagmaCube cube) || !cube.isAlive()) {
				continue;
			}
			if (!isCandidate(cube)) {
				continue;
			}
			if (best == null || isBetterCandidate(cube, best)) {
				best = cube;
			}
		}
		return best;
	}

	private static boolean isCandidate(MagmaCube cube) {
		return cube != null && cube.getBbWidth() >= KUUDRA_MIN_WIDTH;
	}

	private static boolean isBetterCandidate(MagmaCube current, MagmaCube best) {
		int compare = Boolean.compare(hasKuudraName(current), hasKuudraName(best));
		if (compare != 0) {
			return compare > 0;
		}

		compare = Boolean.compare(isKuudraHealth(current), isKuudraHealth(best));
		if (compare != 0) {
			return compare > 0;
		}

		compare = Float.compare(current.getBbWidth(), best.getBbWidth());
		if (compare != 0) {
			return compare > 0;
		}

		compare = Double.compare(current.getY(), best.getY());
		if (compare != 0) {
			return compare > 0;
		}

		return current.getId() < best.getId();
	}

	private static boolean hasKuudraName(MagmaCube cube) {
		if (cube == null || !cube.hasCustomName() || cube.getCustomName() == null) {
			return false;
		}
		String name = TextNormalizer.stripFormattingCodes(cube.getCustomName().getString());
		return name != null && name.toLowerCase().contains("kuudra");
	}

	private static boolean isKuudraHealth(MagmaCube cube) {
		return cube != null && cube.getHealth() > 0.0F && cube.getHealth() <= KUUDRA_HEALTH_CAP;
	}
}
