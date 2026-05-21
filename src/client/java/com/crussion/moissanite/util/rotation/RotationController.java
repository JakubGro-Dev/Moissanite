package com.crussion.moissanite.util.rotation;

import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

public final class RotationController {
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
	private static final float YAW_ACCEL_MIN = 520.0f;
	private static final float YAW_ACCEL_MAX = 4_200.0f;
	private static final float PITCH_ACCEL_MIN = 420.0f;
	private static final float PITCH_ACCEL_MAX = 2_800.0f;
	private static final float YAW_GRAPH_SCALE = 110.0f;
	private static final float PITCH_GRAPH_SCALE = 85.0f;
	private static final float GRAPH_EXPONENTIAL_SHAPE = 4.35f;
	private static final float GRAPH_MINIMUM_JERK_WEIGHT = 0.68f;
	private static final float GRAPH_SIGMOID_SHAPE = 10.0f;
	private static final double GRAPH_SIGMOID_MIN = sigmoid(0.0D);
	private static final double GRAPH_SIGMOID_RANGE = sigmoid(1.0D) - GRAPH_SIGMOID_MIN;
	private static final float PROFILE_RESTART_YAW_DELTA = 3.0f;
	private static final float PROFILE_RESTART_PITCH_DELTA = 2.0f;
	private static final float PROFILE_MIN_DISTANCE = 1.4f;
	private static final float PROFILE_STRONG_OVERSHOOT_MAX = 7.2f;
	private static final float PROFILE_NORMAL_OVERSHOOT_MAX = 4.6f;
	private static final float PROFILE_PITCH_OVERSHOOT_RATIO = 0.55f;
	private static final float PROFILE_TREMOR_MAX_YAW = 0.34f;
	private static final float PROFILE_TREMOR_MAX_PITCH = 0.22f;
	private static final float YAW_DEADZONE = 0.03f;
	private static final float PITCH_DEADZONE = 0.03f;
	private static final float DEFAULT_ROTATION_MULTIPLIER = 1.0f;
	private static final float DEFAULT_ROTATE_FINISH_YAW = 0.01f;
	private static final float DEFAULT_ROTATE_FINISH_PITCH = 0.01f;

	private static boolean initialized;
	private static float yawVelocity;
	private static float pitchVelocity;
	private static float yawCarry;
	private static float pitchCarry;
	private static boolean rotateTaskActive;
	private static float rotateTargetYaw;
	private static float rotateTargetPitch;
	private static float rotateTaskMultiplier = DEFAULT_ROTATION_MULTIPLIER;
	private static float rotateFinishYaw = DEFAULT_ROTATE_FINISH_YAW;
	private static float rotateFinishPitch = DEFAULT_ROTATE_FINISH_PITCH;
	private static long lastRotateStepNanos;
	private static RotationMode rotationMode = RotationMode.DEFAULT;
	private static RotationMode rotationProfileMode = RotationMode.DEFAULT;
	private static float rotationProfileStrength;
	private static float rotationProfileDistance;
	private static float rotationProfileYawDistance;
	private static float rotationProfilePitchDistance;
	private static float rotationProfileElapsedSeconds;
	private static float rotationProfileYawOvershoot;
	private static float rotationProfilePitchOvershoot;
	private static float rotationProfileYawDrift;
	private static float rotationProfilePitchDrift;
	private static float rotationProfileYawTremorAmplitude;
	private static float rotationProfilePitchTremorAmplitude;
	private static float rotationProfileYawSpringScale = 1.0f;
	private static float rotationProfilePitchSpringScale = 1.0f;
	private static float rotationProfileYawDampingScale = 1.0f;
	private static float rotationProfilePitchDampingScale = 1.0f;
	private static float rotationProfileYawSpeedScale = 1.0f;
	private static float rotationProfilePitchSpeedScale = 1.0f;
	private static float rotationProfileYawAccelerationScale = 1.0f;
	private static float rotationProfilePitchAccelerationScale = 1.0f;
	private static float rotationProfileYawPulseAmplitude;
	private static float rotationProfilePitchPulseAmplitude;
	private static float rotationProfileYawPulseFrequency;
	private static float rotationProfilePitchPulseFrequency;
	private static float rotationProfileYawPulsePhase;
	private static float rotationProfilePitchPulsePhase;
	private static float rotationProfileYawTremorFrequency;
	private static float rotationProfilePitchTremorFrequency;
	private static float rotationProfileYawTremorPhase;
	private static float rotationProfilePitchTremorPhase;
	private static long lastSentRotationAtMs;
	private static float lastSentYaw;
	private static float lastSentPitch;

	private RotationController() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(RotationController::onClientTick);
		WorldRenderEvents.END_MAIN.register(context -> onRotateRenderFrame());
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
		return rotateYawPitch(yaw, pitch, DEFAULT_ROTATION_MULTIPLIER, DEFAULT_ROTATE_FINISH_YAW, DEFAULT_ROTATE_FINISH_PITCH);
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

		float nextTargetYaw = Mth.wrapDegrees((float) yaw);
		float nextTargetPitch = Mth.clamp((float) pitch, -90.0F, 90.0F);
		float nextMultiplier = sanitizeRotationMultiplier(multiplier);
		float nextFinishYaw = sanitizeFinishTolerance(finishYaw, DEFAULT_ROTATE_FINISH_YAW);
		float nextFinishPitch = sanitizeFinishTolerance(finishPitch, DEFAULT_ROTATE_FINISH_PITCH);
		boolean restartProfile = shouldRestartProfile(nextTargetYaw, nextTargetPitch);

		rotateTargetYaw = nextTargetYaw;
		rotateTargetPitch = nextTargetPitch;
		rotateTaskMultiplier = nextMultiplier;
		rotateFinishYaw = nextFinishYaw;
		rotateFinishPitch = nextFinishPitch;
		rotateTaskActive = true;
		lastRotateStepNanos = 0L;
		if (restartProfile) {
			startRotationProfile(client, nextTargetYaw, nextTargetPitch);
		}
		return true;
	}

	public static boolean isRotating() {
		return rotateTaskActive;
	}

	public static void setRotationMode(RotationMode mode) {
		RotationMode nextMode = mode == null ? RotationMode.DEFAULT : mode;
		if (rotationMode == nextMode) {
			return;
		}
		rotationMode = nextMode;
		resetRotationMotion();
		resetRotationProfile();
	}

	public static RotationMode rotationMode() {
		return rotationMode;
	}

	public static void onPacketSent(Packet<?> packet) {
		if (!(packet instanceof ServerboundMovePlayerPacket movePacket) || !movePacket.hasRotation()) {
			return;
		}
		lastSentYaw = Mth.wrapDegrees(movePacket.getYRot(lastSentYaw));
		lastSentPitch = Mth.clamp(movePacket.getXRot(lastSentPitch), -90.0f, 90.0f);
		lastSentRotationAtMs = System.currentTimeMillis();
	}

	public static boolean hasRecentlySentRotation(double yaw, double pitch, long maxAgeMs, double toleranceDegrees) {
		if (lastSentRotationAtMs <= 0L || System.currentTimeMillis() - lastSentRotationAtMs > Math.max(0L, maxAgeMs)) {
			return false;
		}
		float targetYaw = Mth.wrapDegrees((float) yaw);
		float targetPitch = Mth.clamp((float) pitch, -90.0f, 90.0f);
		double tolerance = Math.max(0.0D, toleranceDegrees);
		return Math.abs(Mth.wrapDegrees(targetYaw - lastSentYaw)) <= tolerance
				&& Math.abs(targetPitch - lastSentPitch) <= tolerance;
	}

	public static boolean sendCurrentRotationPacket() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.getConnection() == null) {
			return false;
		}
		client.getConnection().send(new ServerboundMovePlayerPacket.Rot(
				client.player.getYRot(),
				client.player.getXRot(),
				client.player.onGround(),
				client.player.horizontalCollision));
		return true;
	}

	public static void cancelRotation() {
		resetRotationController();
		rotateTaskActive = false;
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
		float pitchError = targetPitch - client.player.getXRot();
		boolean exactYawFinish = rotateFinishYaw <= 0.0f;
		boolean exactPitchFinish = rotateFinishPitch <= 0.0f;

		RotationMode mode = rotationMode;
		rotationProfileElapsedSeconds += dtSeconds;
		float remainingDistance = rotationDistance(Math.abs(yawError), Math.abs(pitchError));
		float adjustedTargetYaw = targetYaw + humanizedTargetOffset(yawError, remainingDistance, true);
		float adjustedTargetPitch = Mth.clamp(targetPitch + humanizedTargetOffset(pitchError, remainingDistance, false), -90.0f, 90.0f);
		yawError = Mth.wrapDegrees(adjustedTargetYaw - client.player.getYRot());
		pitchError = adjustedTargetPitch - client.player.getXRot();

		float yawGraph = rotationGraph(Math.abs(yawError), YAW_GRAPH_SCALE, mode);
		float pitchGraph = rotationGraph(Math.abs(pitchError), PITCH_GRAPH_SCALE, mode);

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
		float remainingPitch = Math.abs(targetPitch - client.player.getXRot());
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
		float maxAcceleration = yawAxis
				? Mth.lerp(graph, YAW_ACCEL_MIN, YAW_ACCEL_MAX)
				: Mth.lerp(graph, PITCH_ACCEL_MIN, PITCH_ACCEL_MAX);
		float dynamicScale = dynamicProfileScale(yawAxis);
		spring *= (yawAxis ? rotationProfileYawSpringScale : rotationProfilePitchSpringScale) * dynamicScale;
		damping *= yawAxis ? rotationProfileYawDampingScale : rotationProfilePitchDampingScale;
		maxSpeed *= (yawAxis ? rotationProfileYawSpeedScale : rotationProfilePitchSpeedScale) * dynamicScale;
		maxAcceleration *= (yawAxis ? rotationProfileYawAccelerationScale : rotationProfilePitchAccelerationScale)
				* Mth.clamp(dynamicScale, 0.75f, 1.25f);

		float velocity = yawAxis ? yawVelocity : pitchVelocity;
		if (Math.abs(error) > 1.0e-4f && velocity != 0.0f && Math.signum(error) != Math.signum(velocity)) {
			velocity *= (float) Math.exp(-damping * dtSeconds * 2.0f);
		}

		float targetVelocity = velocity + (error * spring * dtSeconds);
		targetVelocity *= (float) Math.exp(-damping * dtSeconds);
		targetVelocity = Mth.clamp(targetVelocity, -maxSpeed, maxSpeed);

		float maxVelocityDelta = maxAcceleration * dtSeconds;
		velocity = Mth.clamp(targetVelocity, velocity - maxVelocityDelta, velocity + maxVelocityDelta);
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

	private static float rotationGraph(float absError, float fullScale, RotationMode mode) {
		if (absError <= 1.0e-6f) {
			return 0.0f;
		}

		float t = Mth.clamp(absError / fullScale, 0.0f, 1.0f);
		float graph = switch (mode) {
			case HUMANIZED_STRONG -> adaptiveSpringGraph(t);
			case HUMANIZED -> minimumJerkGraph(t);
			case LINEAR -> t;
			case SMOOTHSTEP -> smoothstepGraph(t);
			case SINE -> sineGraph(t);
			case MINIMUM_JERK -> minimumJerkGraph(t);
			case SIGMOID -> sigmoidGraph(t);
			case EXPONENTIAL -> exponentialGraph(t);
			case ADAPTIVE_SPRING -> adaptiveSpringGraph(t);
		};
		return Mth.clamp(graph, 0.0f, 1.0f);
	}

	private static float adaptiveSpringGraph(float t) {
		float minimumJerk = minimumJerkGraph(t);
		float exponential = exponentialGraph(t);
		return Mth.lerp(GRAPH_MINIMUM_JERK_WEIGHT, exponential, minimumJerk);
	}

	private static float smoothstepGraph(float t) {
		return t * t * (3.0f - (2.0f * t));
	}

	private static float sineGraph(float t) {
		return (float) Math.sin(t * Math.PI * 0.5D);
	}

	private static float minimumJerkGraph(float t) {
		return t * t * t * (10.0f + (t * (-15.0f + (6.0f * t))));
	}

	private static float sigmoidGraph(float t) {
		return (float) ((sigmoid(t) - GRAPH_SIGMOID_MIN) / GRAPH_SIGMOID_RANGE);
	}

	private static double sigmoid(double t) {
		return 1.0D / (1.0D + Math.exp(-GRAPH_SIGMOID_SHAPE * (t - 0.5D)));
	}

	private static float exponentialGraph(float t) {
		return (float) ((1.0D - Math.exp(-GRAPH_EXPONENTIAL_SHAPE * t))
				/ (1.0D - Math.exp(-GRAPH_EXPONENTIAL_SHAPE)));
	}

	private static boolean shouldRestartProfile(float targetYaw, float targetPitch) {
		if (!rotateTaskActive || rotationProfileMode != rotationMode || rotationProfileDistance <= 0.0f) {
			return true;
		}
		return Math.abs(Mth.wrapDegrees(targetYaw - rotateTargetYaw)) > PROFILE_RESTART_YAW_DELTA
				|| Math.abs(targetPitch - rotateTargetPitch) > PROFILE_RESTART_PITCH_DELTA;
	}

	private static void startRotationProfile(Minecraft client, float targetYaw, float targetPitch) {
		resetRotationProfile();
		float yawDistance = Math.abs(Mth.wrapDegrees(targetYaw - client.player.getYRot()));
		float pitchDistance = Math.abs(targetPitch - client.player.getXRot());
		float distance = rotationDistance(yawDistance, pitchDistance);
		rotationProfileMode = rotationMode;
		rotationProfileYawDistance = yawDistance;
		rotationProfilePitchDistance = pitchDistance;
		rotationProfileDistance = distance;
		rotationProfileStrength = rotationProfileStrength(rotationMode, distance);
		if (rotationProfileStrength <= 0.0f) {
			return;
		}

		ThreadLocalRandom random = ThreadLocalRandom.current();
		float yawDirection = Math.signum(Mth.wrapDegrees(targetYaw - client.player.getYRot()));
		float pitchDirection = Math.signum(targetPitch - client.player.getXRot());
		float overshootMax = rotationMode == RotationMode.HUMANIZED_STRONG ? PROFILE_STRONG_OVERSHOOT_MAX : PROFILE_NORMAL_OVERSHOOT_MAX;
		rotationProfileYawOvershoot = yawDirection * overshootAmount(yawDistance, overshootMax, random);
		rotationProfilePitchOvershoot = pitchDirection * overshootAmount(pitchDistance, overshootMax * PROFILE_PITCH_OVERSHOOT_RATIO, random);
		rotationProfileYawDrift = randomRange(random, -1.0f, 1.0f) * Math.min(1.45f, 0.010f * yawDistance + 0.24f) * rotationProfileStrength;
		rotationProfilePitchDrift = randomRange(random, -1.0f, 1.0f) * Math.min(0.9f, 0.010f * pitchDistance + 0.15f) * rotationProfileStrength;
		rotationProfileYawTremorAmplitude = Math.min(PROFILE_TREMOR_MAX_YAW, 0.020f + distance * 0.0025f) * rotationProfileStrength;
		rotationProfilePitchTremorAmplitude = Math.min(PROFILE_TREMOR_MAX_PITCH, 0.014f + distance * 0.0017f) * rotationProfileStrength;
		rotationProfileYawSpringScale = randomRange(random, 0.78f, 1.24f);
		rotationProfilePitchSpringScale = randomRange(random, 0.74f, 1.18f);
		rotationProfileYawDampingScale = randomRange(random, 0.82f, 1.18f);
		rotationProfilePitchDampingScale = randomRange(random, 0.86f, 1.24f);
		rotationProfileYawSpeedScale = randomRange(random, 0.80f, 1.34f);
		rotationProfilePitchSpeedScale = randomRange(random, 0.76f, 1.22f);
		rotationProfileYawAccelerationScale = randomRange(random, 0.74f, 1.35f);
		rotationProfilePitchAccelerationScale = randomRange(random, 0.72f, 1.25f);
		rotationProfileYawPulseAmplitude = randomRange(random, 0.04f, 0.18f) * rotationProfileStrength;
		rotationProfilePitchPulseAmplitude = randomRange(random, 0.035f, 0.15f) * rotationProfileStrength;
		rotationProfileYawPulseFrequency = randomRange(random, 5.5f, 12.5f);
		rotationProfilePitchPulseFrequency = randomRange(random, 5.0f, 11.0f);
		rotationProfileYawPulsePhase = randomRange(random, 0.0f, (float) (Math.PI * 2.0D));
		rotationProfilePitchPulsePhase = randomRange(random, 0.0f, (float) (Math.PI * 2.0D));
		rotationProfileYawTremorFrequency = randomRange(random, 16.0f, 28.0f);
		rotationProfilePitchTremorFrequency = randomRange(random, 14.0f, 24.0f);
		rotationProfileYawTremorPhase = randomRange(random, 0.0f, (float) (Math.PI * 2.0D));
		rotationProfilePitchTremorPhase = randomRange(random, 0.0f, (float) (Math.PI * 2.0D));
	}

	private static float rotationProfileStrength(RotationMode mode, float distance) {
		if (distance < PROFILE_MIN_DISTANCE) {
			return 0.0f;
		}
		float distanceScale = Mth.clamp((distance - PROFILE_MIN_DISTANCE) / 42.0f, 0.0f, 1.0f);
		float base = switch (mode) {
			case HUMANIZED_STRONG -> 1.0f;
			case HUMANIZED -> 0.72f;
			case ADAPTIVE_SPRING -> 0.42f;
			case MINIMUM_JERK, SIGMOID, EXPONENTIAL, SINE, SMOOTHSTEP -> 0.34f;
			case LINEAR -> 0.22f;
		};
		return base * (0.28f + distanceScale * 0.72f);
	}

	private static float overshootAmount(float distance, float maxOvershoot, ThreadLocalRandom random) {
		if (distance < 3.0f) {
			return 0.0f;
		}
		float amount = Math.min(maxOvershoot, distance * randomRange(random, 0.018f, 0.045f));
		return amount * rotationProfileStrength;
	}

	private static float humanizedTargetOffset(float trueError, float remainingDistance, boolean yawAxis) {
		if (rotationProfileStrength <= 0.0f || rotationProfileDistance <= PROFILE_MIN_DISTANCE) {
			return 0.0f;
		}
		float finishGuard = yawAxis
				? Math.max(0.55f, rotateFinishYaw * 2.8f)
				: Math.max(0.42f, rotateFinishPitch * 2.8f);
		if (Math.abs(trueError) <= finishGuard) {
			return 0.0f;
		}

		float progress = 1.0f - Mth.clamp(remainingDistance / rotationProfileDistance, 0.0f, 1.0f);
		float overshootWeight = smoothstepGraph(Mth.clamp((progress - 0.16f) / 0.34f, 0.0f, 1.0f))
				* (1.0f - smoothstepGraph(Mth.clamp((progress - 0.70f) / 0.24f, 0.0f, 1.0f)));
		float driftWeight = 1.0f - smoothstepGraph(Mth.clamp((progress - 0.48f) / 0.36f, 0.0f, 1.0f));
		float tremorWeight = (1.0f - smoothstepGraph(Mth.clamp((progress - 0.58f) / 0.30f, 0.0f, 1.0f)))
				* Mth.clamp((Math.abs(trueError) - finishGuard) / 14.0f, 0.0f, 1.0f);

		float overshoot = yawAxis ? rotationProfileYawOvershoot : rotationProfilePitchOvershoot;
		float drift = yawAxis ? rotationProfileYawDrift : rotationProfilePitchDrift;
		float tremor = profileTremor(yawAxis) * tremorWeight;
		return overshoot * overshootWeight + drift * driftWeight + tremor;
	}

	private static float profileTremor(boolean yawAxis) {
		float amplitude = yawAxis ? rotationProfileYawTremorAmplitude : rotationProfilePitchTremorAmplitude;
		float frequency = yawAxis ? rotationProfileYawTremorFrequency : rotationProfilePitchTremorFrequency;
		float phase = yawAxis ? rotationProfileYawTremorPhase : rotationProfilePitchTremorPhase;
		float time = rotationProfileElapsedSeconds;
		float wave = (float) Math.sin(time * frequency + phase);
		wave += 0.42f * (float) Math.sin(time * frequency * 1.73f + phase * 0.61f);
		return amplitude * wave;
	}

	private static float dynamicProfileScale(boolean yawAxis) {
		if (rotationProfileStrength <= 0.0f) {
			return 1.0f;
		}
		float amplitude = yawAxis ? rotationProfileYawPulseAmplitude : rotationProfilePitchPulseAmplitude;
		float frequency = yawAxis ? rotationProfileYawPulseFrequency : rotationProfilePitchPulseFrequency;
		float phase = yawAxis ? rotationProfileYawPulsePhase : rotationProfilePitchPulsePhase;
		float wave = (float) Math.sin(rotationProfileElapsedSeconds * frequency + phase);
		wave += 0.35f * (float) Math.sin(rotationProfileElapsedSeconds * frequency * 1.91f + phase * 0.37f);
		return Mth.clamp(1.0f + amplitude * wave, 0.62f, 1.46f);
	}

	private static float rotationDistance(float yawDistance, float pitchDistance) {
		return (float) Math.sqrt(yawDistance * yawDistance + pitchDistance * pitchDistance);
	}

	private static float randomRange(ThreadLocalRandom random, float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	private static void resetRotationMotion() {
		yawVelocity = 0.0f;
		pitchVelocity = 0.0f;
		yawCarry = 0.0f;
		pitchCarry = 0.0f;
	}

	private static void resetRotationProfile() {
		rotationProfileMode = rotationMode;
		rotationProfileStrength = 0.0f;
		rotationProfileDistance = 0.0f;
		rotationProfileYawDistance = 0.0f;
		rotationProfilePitchDistance = 0.0f;
		rotationProfileElapsedSeconds = 0.0f;
		rotationProfileYawOvershoot = 0.0f;
		rotationProfilePitchOvershoot = 0.0f;
		rotationProfileYawDrift = 0.0f;
		rotationProfilePitchDrift = 0.0f;
		rotationProfileYawTremorAmplitude = 0.0f;
		rotationProfilePitchTremorAmplitude = 0.0f;
		rotationProfileYawSpringScale = 1.0f;
		rotationProfilePitchSpringScale = 1.0f;
		rotationProfileYawDampingScale = 1.0f;
		rotationProfilePitchDampingScale = 1.0f;
		rotationProfileYawSpeedScale = 1.0f;
		rotationProfilePitchSpeedScale = 1.0f;
		rotationProfileYawAccelerationScale = 1.0f;
		rotationProfilePitchAccelerationScale = 1.0f;
		rotationProfileYawPulseAmplitude = 0.0f;
		rotationProfilePitchPulseAmplitude = 0.0f;
		rotationProfileYawPulseFrequency = 0.0f;
		rotationProfilePitchPulseFrequency = 0.0f;
		rotationProfileYawPulsePhase = 0.0f;
		rotationProfilePitchPulsePhase = 0.0f;
		rotationProfileYawTremorFrequency = 0.0f;
		rotationProfilePitchTremorFrequency = 0.0f;
		rotationProfileYawTremorPhase = 0.0f;
		rotationProfilePitchTremorPhase = 0.0f;
	}

	private static float sanitizeRotationMultiplier(double multiplier) {
		if (!Double.isFinite(multiplier)) {
			return DEFAULT_ROTATION_MULTIPLIER;
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
		resetRotationMotion();
		resetRotationProfile();
		rotateTaskMultiplier = DEFAULT_ROTATION_MULTIPLIER;
		rotateFinishYaw = DEFAULT_ROTATE_FINISH_YAW;
		rotateFinishPitch = DEFAULT_ROTATE_FINISH_PITCH;
		lastRotateStepNanos = 0L;
	}
}
