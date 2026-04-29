package com.crussion.moissanite.util.rotation;

import com.crussion.moissanite.definitions.UiDefinitions;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class RotationController {
	private static final Identifier ROTATE_DRIVER_HUD_ID = Identifier.fromNamespaceAndPath("moissanite", "autorend_rotate_driver");
	private static final long RENDER_FALLBACK_NS = 120_000_000L;
	private static final float TICK_DT_SECONDS = 1.0f / 20.0f;
	private static final float YAW_SPRING_MIN = 44.0f;
	private static final float YAW_SPRING_MAX = 100.0f;
	private static final float PITCH_SPRING_MIN = 34.0f;
	private static final float PITCH_SPRING_MAX = 78.0f;
	private static final float YAW_DAMP_MIN = 5.2f;
	private static final float YAW_DAMP_MAX = 10.5f;
	private static final float PITCH_DAMP_MIN = 6.0f;
	private static final float PITCH_DAMP_MAX = 11.5f;
	private static final float YAW_SPEED_MIN = 94.0f;
	private static final float YAW_SPEED_MAX = 640.0f;
	private static final float PITCH_SPEED_MIN = 66.0f;
	private static final float PITCH_SPEED_MAX = 290.0f;
	private static final float YAW_DEADZONE = 0.06f;
	private static final float PITCH_DEADZONE = 0.05f;
	private static final float DEFAULT_ROTATE_FINISH_YAW = 0.2f;
	private static final float DEFAULT_ROTATE_FINISH_PITCH = 0.2f;

	private static boolean initialized;
	private static float yawVelocity;
	private static float pitchVelocity;
	private static float yawCarry;
	private static float pitchCarry;
	private static boolean rotateTaskActive;
	private static float rotateTargetYaw;
	private static float rotateTargetPitch;
	private static float rotateTaskMultiplier = 1.0f;
	private static float rotateFinishYaw = DEFAULT_ROTATE_FINISH_YAW;
	private static float rotateFinishPitch = DEFAULT_ROTATE_FINISH_PITCH;
	private static long lastRotateStepNanos;

	private RotationController() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(RotationController::onClientTick);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				ROTATE_DRIVER_HUD_ID,
				(graphics, tickCounter) -> onRotateRenderFrame());
	}

	public static boolean rotateTo(double x, double y, double z) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}

		double dx = x - client.player.getX();
		double dy = y - client.player.getEyeY();
		double dz = z - client.player.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
		float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
		return rotateYawPitch(yaw, pitch);
	}

	public static boolean rotateTo(double x, double y, double z, double multiplier) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}

		double dx = x - client.player.getX();
		double dy = y - client.player.getEyeY();
		double dz = z - client.player.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
		double pitch = -Math.toDegrees(Math.atan2(dy, horizontal));
		return rotateYawPitch(yaw, pitch, multiplier);
	}

	public static boolean rotateYawPitch(double yaw, double pitch) {
		return rotateYawPitch(yaw, pitch, rotationMultiplier(), DEFAULT_ROTATE_FINISH_YAW, DEFAULT_ROTATE_FINISH_PITCH);
	}

	public static boolean rotateYawPitch(double yaw, double pitch, double multiplier) {
		return rotateYawPitch(yaw, pitch, multiplier, DEFAULT_ROTATE_FINISH_YAW, DEFAULT_ROTATE_FINISH_PITCH);
	}

	public static boolean rotateYawPitch(double yaw, double pitch, double multiplier, double finishYaw, double finishPitch) {
		init();
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}

		rotateTargetYaw = Mth.wrapDegrees((float) yaw);
		rotateTargetPitch = Mth.clamp((float) pitch, -90.0F, 90.0F);
		rotateTaskMultiplier = sanitizeRotationMultiplier(multiplier);
		rotateFinishYaw = sanitizeFinishTolerance(finishYaw, DEFAULT_ROTATE_FINISH_YAW);
		rotateFinishPitch = sanitizeFinishTolerance(finishPitch, DEFAULT_ROTATE_FINISH_PITCH);
		rotateTaskActive = true;
		lastRotateStepNanos = 0L;
		return true;
	}

	public static boolean isRotating() {
		return rotateTaskActive;
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.player == null) {
			resetRotationController();
			rotateTaskActive = false;
			return;
		}
		tickRotateTaskFallback(client);
	}

	private static void tickRotateTaskFallback(Minecraft client) {
		if (!rotateTaskActive) {
			return;
		}

		long now = System.nanoTime();
		if (now - lastRotateStepNanos > RENDER_FALLBACK_NS) {
			applyRotationStep(client, rotateTargetYaw, rotateTargetPitch, TICK_DT_SECONDS);
			lastRotateStepNanos = now;
		}
	}

	private static void onRotateRenderFrame() {
		if (!rotateTaskActive) {
			lastRotateStepNanos = 0L;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			resetRotationController();
			rotateTaskActive = false;
			return;
		}

		long now = System.nanoTime();
		float dtSeconds = 1.0f / 60.0f;
		if (lastRotateStepNanos != 0L) {
			dtSeconds = Mth.clamp((now - lastRotateStepNanos) / 1_000_000_000.0f, 1.0f / 240.0f, 1.0f / 20.0f);
		}
		lastRotateStepNanos = now;
		applyRotationStep(client, rotateTargetYaw, rotateTargetPitch, dtSeconds);
	}

	private static void applyRotationStep(Minecraft client, float targetYaw, float targetPitch, float dtSeconds) {
		if (rotateTaskMultiplier <= 0.0f) {
			client.player.setYRot(targetYaw);
			client.player.setXRot(targetPitch);
			client.player.setYHeadRot(targetYaw);
			client.player.setYBodyRot(targetYaw);
			resetRotationController();
			rotateTaskActive = false;
			return;
		}

		float effectiveDt = dtSeconds / rotateTaskMultiplier;
		float yawError = Mth.wrapDegrees(targetYaw - client.player.getYRot());
		float pitchError = Mth.wrapDegrees(targetPitch - client.player.getXRot());
		boolean exactYawFinish = rotateFinishYaw <= 0.0f;
		boolean exactPitchFinish = rotateFinishPitch <= 0.0f;

		float yawGraph = smoothGraph(Math.abs(yawError), 110.0f);
		float pitchGraph = smoothGraph(Math.abs(pitchError), 85.0f);

		float yawStep = springStep(yawError, yawGraph, true, effectiveDt);
		float pitchStep = springStep(pitchError, pitchGraph, false, effectiveDt);

		yawStep = quantizeMouseStep(yawStep, true);
		pitchStep = quantizeMouseStep(pitchStep, false);

		yawStep = clampStepToError(yawStep, yawError);
		pitchStep = clampStepToError(pitchStep, pitchError);

		if (Math.abs(yawError) < YAW_DEADZONE && Math.abs(yawVelocity) < 1.3f) {
			yawVelocity = 0.0f;
			yawCarry = 0.0f;
			client.player.setYRot(exactYawFinish ? targetYaw : client.player.getYRot() + (yawError * 0.40f));
		} else {
			client.player.setYRot(client.player.getYRot() + yawStep);
		}

		if (Math.abs(pitchError) < PITCH_DEADZONE && Math.abs(pitchVelocity) < 1.0f) {
			pitchVelocity = 0.0f;
			pitchCarry = 0.0f;
			client.player.setXRot(exactPitchFinish ? targetPitch : client.player.getXRot() + (pitchError * 0.40f));
		} else {
			client.player.setXRot(client.player.getXRot() + pitchStep);
		}

		client.player.setXRot(Mth.clamp(client.player.getXRot(), -90.0f, 90.0f));
		float currentYaw = client.player.getYRot();
		client.player.setYHeadRot(currentYaw);
		client.player.setYBodyRot(currentYaw);

		float remainingYaw = Math.abs(Mth.wrapDegrees(targetYaw - client.player.getYRot()));
		float remainingPitch = Math.abs(Mth.wrapDegrees(targetPitch - client.player.getXRot()));
		if (remainingYaw <= rotateFinishYaw && remainingPitch <= rotateFinishPitch) {
			resetRotationController();
			rotateTaskActive = false;
		}
	}

	private static float springStep(float error, float graph, boolean yawAxis, float dtSeconds) {
		float spring = yawAxis
				? Mth.lerp(graph, YAW_SPRING_MIN, YAW_SPRING_MAX)
				: Mth.lerp(graph, PITCH_SPRING_MIN, PITCH_SPRING_MAX);
		float damping = yawAxis
				? Mth.lerp(graph, YAW_DAMP_MIN, YAW_DAMP_MAX)
				: Mth.lerp(graph, PITCH_DAMP_MIN, PITCH_DAMP_MAX);
		float maxSpeed = yawAxis
				? Mth.lerp(graph, YAW_SPEED_MIN, YAW_SPEED_MAX)
				: Mth.lerp(graph, PITCH_SPEED_MIN, PITCH_SPEED_MAX);

		float velocity = yawAxis ? yawVelocity : pitchVelocity;
		velocity += error * spring * dtSeconds;
		velocity *= (float) Math.exp(-damping * dtSeconds);
		velocity = Mth.clamp(velocity, -maxSpeed, maxSpeed);
		if (yawAxis) {
			yawVelocity = velocity;
		} else {
			pitchVelocity = velocity;
		}
		return velocity * dtSeconds;
	}

	private static float quantizeMouseStep(float delta, boolean yawAxis) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null || client.options.sensitivity() == null) {
			return delta;
		}

		double sensitivity = client.options.sensitivity().get();
		double base = (sensitivity * 0.6D) + 0.2D;
		float step = (float) (base * base * base * 1.2D);
		if (step < 1.0e-4f) {
			return delta;
		}

		float carry = yawAxis ? yawCarry : pitchCarry;
		float value = delta + carry;
		float snapped = Math.round(value / step) * step;
		if (snapped == 0.0f && Math.abs(value) >= step * 0.40f) {
			snapped = Math.copySign(step, value);
		}

		float remainder = value - snapped;
		if (yawAxis) {
			yawCarry = remainder;
		} else {
			pitchCarry = remainder;
		}
		return snapped;
	}

	private static float clampStepToError(float step, float error) {
		if (Math.abs(error) < 1.0e-6f) {
			return 0.0f;
		}
		if (Math.signum(step) != Math.signum(error)) {
			return 0.0f;
		}
		if (Math.abs(step) > Math.abs(error)) {
			return error;
		}
		return step;
	}

	private static float smoothGraph(float absError, float fullScale) {
		float t = Mth.clamp(absError / fullScale, 0.0f, 1.0f);
		return t * t * (3.0f - (2.0f * t));
	}

	private static float rotationMultiplier() {
		Double configured = UiDefinitions.AUTO_REND_ROTATION_MULTIPLIER.get();
		if (configured == null || !Double.isFinite(configured)) {
			return 1.0f;
		}
		return sanitizeRotationMultiplier(configured);
	}

	private static float sanitizeRotationMultiplier(double multiplier) {
		if (!Double.isFinite(multiplier)) {
			return 1.0f;
		}
		return (float) Mth.clamp(multiplier, 0.0, 2.0);
	}

	private static float sanitizeFinishTolerance(double tolerance, float fallback) {
		if (!Double.isFinite(tolerance)) {
			return fallback;
		}
		return (float) Mth.clamp(tolerance, 0.0, 10.0);
	}

	private static void resetRotationController() {
		yawVelocity = 0.0f;
		pitchVelocity = 0.0f;
		yawCarry = 0.0f;
		pitchCarry = 0.0f;
		rotateFinishYaw = DEFAULT_ROTATE_FINISH_YAW;
		rotateFinishPitch = DEFAULT_ROTATE_FINISH_PITCH;
		lastRotateStepNanos = 0L;
	}
}
