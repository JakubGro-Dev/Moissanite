package com.crussion.moissanite.features.cheats;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.HeldItemMatcher;
import com.crussion.moissanite.util.inventory.HotbarItemSearch;
import com.crussion.moissanite.util.kuudra.KuudraEntityFinder;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;
import com.crussion.moissanite.util.tick.TickTaskScheduler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AutoDirection {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String TRIGGER_CHAT = "[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!";
	private static final String HYPERION_ID = "HYPERION";
	private static final long WAIT_FOR_TP_MS = 7_000L;
	private static final int HYPERION_CAST_DELAY_TICKS = 1;
	private static final double DPS_Y_MIN = 5.9D;
	private static final double DPS_Y_MAX = 6.1D;
	private static final double KUUDRA_AIM_MAX_Y = 33.0D;
	private static final double KUUDRA_AIM_LEFT_YAW_OFFSET_DEGREES = 2.0D;
	private static final float HEALTH_MIN = 24_900.0F;
	private static final float HEALTH_MAX = 100_000.0F; // FROM 25_000.0F
	private static boolean initialized;
	private static boolean waitForTpActive;
	private static long waitForTpUntilMs;
	private static boolean kuudraAimActive;
	private static boolean kuudraAimIssuedRotation;
	private static int kuudraAimTicksRemaining;

	private AutoDirection() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(AutoDirection::handleClientTick);
	}

	public static void onSystemChat(Component message) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION.get())) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}

		String sanitized = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString());
		if (!TRIGGER_CHAT.equals(sanitized)) {
			return;
		}

		waitForTpActive = true;
		waitForTpUntilMs = System.currentTimeMillis() + WAIT_FOR_TP_MS;
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			waitForTpActive = false;
			stopKuudraAim(true);
			return;
		}

		tickKuudraAim(client);

		if (!waitForTpActive) {
			return;
		}
		if (System.currentTimeMillis() > waitForTpUntilMs) {
			waitForTpActive = false;
			return;
		}
		if (!isAtDpsY(client.player.getY())) {
			return;
		}

		MagmaCube kuudra = KuudraEntityFinder.findKuudra(client);
		if (kuudra == null) {
			return;
		}
		if (kuudra.getHealth() > HEALTH_MAX || kuudra.getHealth() <= HEALTH_MIN) {
			return;
		}

		waitForTpActive = false;
		rotateTowardKuudra(kuudra);
		startKuudraAim(kuudra);
	}

	private static boolean isAtDpsY(double y) {
		return y > DPS_Y_MIN && y < DPS_Y_MAX;
	}

	private static void rotateTowardKuudra(MagmaCube kuudra) {
		double x = kuudra.getX();
		double z = kuudra.getZ();

		if (x < -128.0D) {
			rotateAndHyperionCast(90.0D, 0.0D);
			return;
		}
		if (z > -84.0D) {
			rotateAndHyperionCast(0.0D, 0.0D);
			return;
		}
		if (x > -72.0D) {
			rotateAndHyperionCast(-90.0D, 0.0D);
			return;
		}
		if (z < -132.0D) {
			rotateAndHyperionCast(179.0D, 0.0D);
			return;
		}
	}

	private static void rotateAndHyperionCast(double yaw, double pitch) {
		RotationController.rotateYawPitch(yaw, pitch, UiDefinitions.AUTO_DIRECTION_ROTATION_MULTIPLIER.get());
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION_AUTO_HYPERION.get())) {
			return;
		}

		int hyperionSlot = UiDefinitions.AUTO_REND_HYPERION.get().intValue();
		if (!HotbarItemSearch.swapHeldItem(hyperionSlot)) {
			return;
		}

		TickTaskScheduler.schedule(HYPERION_CAST_DELAY_TICKS, AutoDirection::tryAutoHyperionCast);
	}

	private static void startKuudraAim(MagmaCube kuudra) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION_AIM_AT_KUUDRA.get())) {
			return;
		}

		kuudraAimActive = true;
		kuudraAimIssuedRotation = false;
		kuudraAimTicksRemaining = getConfiguredKuudraAimTicks();
		if (isKuudraUnderAimY(kuudra)) {
			kuudraAimIssuedRotation = aimAtKuudraFace(kuudra);
		}
		kuudraAimTicksRemaining--;
	}

	private static void tickKuudraAim(Minecraft client) {
		if (!kuudraAimActive) {
			return;
		}
		if (kuudraAimTicksRemaining <= 0) {
			stopKuudraAim(true);
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION.get())
				|| !Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION_AIM_AT_KUUDRA.get())
				|| !ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)
				|| AutoRend_Reworked.isSequenceRunning()) {
			stopKuudraAim(true);
			return;
		}

		MagmaCube kuudra = KuudraEntityFinder.findKuudra(client);
		if (kuudra == null) {
			stopKuudraAim(true);
			return;
		}
		if (!isKuudraUnderAimY(kuudra)) {
			pauseKuudraAim();
			kuudraAimTicksRemaining--;
			return;
		}

		kuudraAimIssuedRotation = aimAtKuudraFace(kuudra);
		kuudraAimTicksRemaining--;
	}

	private static boolean isKuudraUnderAimY(MagmaCube kuudra) {
		return kuudra != null && kuudra.getY() < KUUDRA_AIM_MAX_Y;
	}

	private static void stopKuudraAim(boolean cancelRotation) {
		boolean wasActive = kuudraAimActive;
		boolean hadAimRotation = kuudraAimIssuedRotation;
		kuudraAimActive = false;
		kuudraAimIssuedRotation = false;
		kuudraAimTicksRemaining = 0;
		if (cancelRotation && wasActive && hadAimRotation) {
			RotationController.cancelRotation();
		}
	}

	private static void pauseKuudraAim() {
		if (kuudraAimIssuedRotation) {
			RotationController.cancelRotation();
			kuudraAimIssuedRotation = false;
		}
	}

	private static boolean aimAtKuudraFace(MagmaCube kuudra) {
		Vec3 target = getClosestHorizontalFaceCenter(kuudra);
		if (target == null) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}

		double dx = target.x - client.player.getX();
		double dy = target.y - client.player.getEyeY();
		double dz = target.z - client.player.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D - KUUDRA_AIM_LEFT_YAW_OFFSET_DEGREES;
		double pitch = -Math.toDegrees(Math.atan2(dy, horizontal));
		return RotationController.rotateYawPitch(
				yaw,
				pitch,
				UiDefinitions.AUTO_DIRECTION_AIM_AT_KUUDRA_ROTATION_MULTIPLIER.get());
	}

	private static Vec3 getClosestHorizontalFaceCenter(MagmaCube kuudra) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || kuudra == null) {
			return null;
		}

		AABB box = kuudra.getBoundingBox();
		Vec3 center = box.getCenter();
		Vec3 eye = client.player.getEyePosition();
		Vec3[] faceCenters = {
				new Vec3(box.minX, center.y, center.z),
				new Vec3(box.maxX, center.y, center.z),
				new Vec3(center.x, center.y, box.minZ),
				new Vec3(center.x, center.y, box.maxZ)
		};

		Vec3 closest = faceCenters[0];
		double closestDistanceSqr = closest.distanceToSqr(eye);
		for (int i = 1; i < faceCenters.length; i++) {
			double distanceSqr = faceCenters[i].distanceToSqr(eye);
			if (distanceSqr < closestDistanceSqr) {
				closest = faceCenters[i];
				closestDistanceSqr = distanceSqr;
			}
		}
		return closest;
	}

	private static int getConfiguredKuudraAimTicks() {
		Double rawTicks = UiDefinitions.AUTO_DIRECTION_AIM_AT_KUUDRA_TICKS.get();
		if (rawTicks == null || !Double.isFinite(rawTicks)) {
			return 30;
		}
		return Math.max(1, rawTicks.intValue());
	}

	private static void tryAutoHyperionCast() {
		if (!HeldItemMatcher.heldMatchesSkyblockId(HYPERION_ID)) {
			return;
		}
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_DIRECTION_AUTO_REAPER.get())) {
			PlayerInputActions.shiftRightClick();
			return;
		}
		PlayerInputActions.rightClick();
	}
}
