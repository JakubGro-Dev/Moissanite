package com.crussion.moissanite.util.kuudra;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

public final class KuudraTriggerArea {
	private static final double[] TRIGGER_X = {-106.0, -100.0, -115.0, -112.5, -104.0, -99.0, -91.82, -90.0};
	private static final double[] TRIGGER_Y = {6.0, 15.0, 5.0, 15.0, 6.0, 15.0, 5.0, 15.0};
	private static final double[] TRIGGER_Z = {-93.6, -92.0, -108.0, -103.0, -119.0, -117.1, -107.0, -101.0};
	private static final int[][] TRIGGER_BOXES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}};
	private static final AABB[] TRIGGER_AABBS = createTriggerAabbs();

	private KuudraTriggerArea() {
	}

	public static boolean isPlayerInTriggerArea() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}
		return isInTriggerArea(client.player.getX(), client.player.getY(), client.player.getZ());
	}

	public static boolean isInTriggerArea(double x, double y, double z) {
		for (AABB box : TRIGGER_AABBS) {
			double minX = box.minX;
			double maxX = box.maxX;
			double minY = box.minY;
			double maxY = box.maxY;
			double minZ = box.minZ;
			double maxZ = box.maxZ;
			if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
				return true;
			}
		}
		return false;
	}

	public static AABB[] getTriggerAabbs() {
		return TRIGGER_AABBS.clone();
	}

	private static AABB[] createTriggerAabbs() {
		AABB[] boxes = new AABB[TRIGGER_BOXES.length];
		for (int boxIndex = 0; boxIndex < TRIGGER_BOXES.length; boxIndex++) {
			int i = TRIGGER_BOXES[boxIndex][0];
			int j = TRIGGER_BOXES[boxIndex][1];
			double minX = Math.min(TRIGGER_X[i], TRIGGER_X[j]);
			double maxX = Math.max(TRIGGER_X[i], TRIGGER_X[j]);
			double minY = Math.min(TRIGGER_Y[i], TRIGGER_Y[j]);
			double maxY = Math.max(TRIGGER_Y[i], TRIGGER_Y[j]);
			double minZ = Math.min(TRIGGER_Z[i], TRIGGER_Z[j]);
			double maxZ = Math.max(TRIGGER_Z[i], TRIGGER_Z[j]);
			boxes[boxIndex] = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		}
		return boxes;
	}
}
