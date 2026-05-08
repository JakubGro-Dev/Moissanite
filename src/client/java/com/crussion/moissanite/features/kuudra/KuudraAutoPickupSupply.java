package com.crussion.moissanite.features.kuudra;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

/*
import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.features.cheats.AutoPearl;
import com.crussion.moissanite.input.FakeKeybinds;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KuudraAutoPickupSupply {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String CHEST_SLIPPED_MESSAGE = "you moved and the chest slipped out of your hands!";
	private static final String LAVA_RETRIEVED_MESSAGE = "you retrieved some of elle's supplies from the lava!";
	private static final String GHOST_MESSAGE_SUFFIX = "you were killed by kuudra follower and became a ghost.";
	private static final Pattern PICKUP_PROGRESS_PATTERN = Pattern.compile("\\[\\|+]\\s*(\\d+)%");
	private static final Pattern LEGACY_PICKUP_PROGRESS_PATTERN = Pattern.compile("(\\d{1,3})%");

	private static final double DEFAULT_RANGE = 3.0D;
	private static final double MIN_RANGE = 1.0D;
	private static final double MAX_RANGE = 6.0D;
	private static final double ROTATION_MULTIPLIER = 0.45D;
	private static final int STABLE_TARGET_TICKS = 2;
	private static final int ROTATE_TIMEOUT_TICKS = 50;
	private static final int WAIT_AFTER_ROTATE_TICKS = 1;
	private static final int CONFIRM_TIMEOUT_TICKS = 80;
	private static final int RETRY_DELAY_TICKS = 50;
	private static final int POST_RESET_COOLDOWN_TICKS = 40;
	private static final int MAX_ATTEMPTS_PER_TARGET = 2;
	private static final int MANUAL_REQUEST_TTL_TICKS = 40;
	private static final double DEFAULT_AUTO_DELAY_MS = 0.0D;
	private static final double MIN_AUTO_DELAY_MS = 0.0D;
	private static final double MAX_AUTO_DELAY_MS = 2000.0D;
	private static final String MANUAL_MODE = "Manual";

	private static boolean initialized;
	private static boolean pickedUpThisSupplyPhase;
	private static State state = State.IDLE;
	private static long stateStartedTick;
	private static long nextInteractTick;
	private static long manualRequestExpiresTick = -1L;
	private static int autoDelayTargetId = -1;
	private static long autoDelayReadyTick;
	private static int candidateEntityId = -1;
	private static int candidateStableTicks;
	private static int activeTargetId = -1;
	private static int attemptsForActiveTarget;
	private static int exhaustedTargetId = -1;
	private static int lastAutoInteractEntityId = -1;
	private static long lastAutoInteractTick = -1L;
	private static Packet<?> lastDebugPacket;
	private static long lastDebugPacketTick = -1L;

	private KuudraAutoPickupSupply() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(KuudraAutoPickupSupply::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetState());
		FakeKeybinds.onKeyPress(UiDefinitions.AUTO_PICKUP_SUPPLY_KEYBIND, KuudraAutoPickupSupply::requestManualPickup);
	}

	public static void requestManualPickup() {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PICKUP_SUPPLY.get()) || !manualModeEnabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (!isKuudraSupplyPhase(client)) {
			sendDebug("Manual pickup ignored outside Kuudra supply phase.");
			return;
		}

		long nowTick = currentGameTick(client);
		manualRequestExpiresTick = nowTick + MANUAL_REQUEST_TTL_TICKS;
		sendDebug("Manual pickup request queued for " + MANUAL_REQUEST_TTL_TICKS + " ticks.");
	}

	public static void onPacketSent(Packet<?> packet) {
		if (!isDebugEnabled() || packet == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		long nowTick = currentGameTick(client);
		if (packet == lastDebugPacket && nowTick == lastDebugPacketTick) {
			return;
		}
		lastDebugPacket = packet;
		lastDebugPacketTick = nowTick;

		if (packet instanceof ServerboundInteractPacket interactPacket) {
			debugInteractPacket(client, interactPacket, nowTick);
			return;
		}
		if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket) {
			debugUseItemOnPacket(useItemOnPacket);
			return;
		}
		if (packet instanceof ServerboundUseItemPacket useItemPacket) {
			sendDebug("Manual packet AIR_USE hand=" + useItemPacket.getHand()
					+ " yaw=" + formatNumber(useItemPacket.getYRot())
					+ " pitch=" + formatNumber(useItemPacket.getXRot()));
		}
	}

	public static void onSystemChat(Component message) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PICKUP_SUPPLY.get())) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}

		String normalized = TextNormalizer.normalize(message == null ? "" : message.getString());
		if (shouldResetCarry(normalized)) {
			resetCarryAfterServerMessage();
		}
	}

	public static void onTitleText(Component message) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PICKUP_SUPPLY.get())) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_SUPPLY) {
			return;
		}

		int percent = parsePickupProgress(message);
		if (percent < 0) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		long nowTick = currentGameTick(client);
		pickedUpThisSupplyPhase = true;
		sendDebug("Pickup title confirmation percent=" + percent + ".");
		candidateEntityId = -1;
		candidateStableTicks = 0;
		attemptsForActiveTarget = 0;
		exhaustedTargetId = -1;
		enterState(State.CONFIRMED_PICKUP, nowTick);
	}

	private static void handleClientTick(Minecraft client) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PICKUP_SUPPLY.get())) {
			resetState();
			return;
		}
		if (client == null || client.player == null || client.level == null) {
			resetState();
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			resetState();
			return;
		}

		int phase = KuudraPhaseTracker.getPhase();
		if (phase >= KuudraPhaseTracker.PHASE_BUILD) {
			disableFeature("Build phase reached; Auto Pickup Supply disabled.");
			return;
		}
		if (phase != KuudraPhaseTracker.PHASE_SUPPLY) {
			resetState();
			return;
		}

		long nowTick = currentGameTick(client);
		if (pickedUpThisSupplyPhase || AutoPearl.isPickupTrackingActive() || AutoPearl.isSequenceActive()) {
			pickedUpThisSupplyPhase = true;
			enterState(State.CONFIRMED_PICKUP, nowTick);
			return;
		}

		if (!canInteractNow(client)) {
			return;
		}

		if (state == State.WAITING_CONFIRM) {
			handleWaitingConfirm(nowTick);
			return;
		}
		if (state == State.ROTATING) {
			handleRotating(client, nowTick);
			return;
		}
		if (state == State.WAIT_AFTER_ROTATE) {
			handleWaitAfterRotate(client, nowTick);
			return;
		}
		if (state == State.COOLDOWN) {
			if (nowTick < nextInteractTick) {
				return;
			}
			enterState(State.IDLE, nowTick);
		}

		if (nowTick < nextInteractTick) {
			return;
		}
		if (manualModeEnabled() && !hasManualRequest(nowTick)) {
			return;
		}

		KuudraSupplyWaypoints.SupplyPickupTarget target = KuudraSupplyWaypoints.findClosestPickupTarget(
				client,
				configuredRange());
		if (target == null) {
			resetTargetTracking();
			resetAutoDelayTracking();
			exhaustedTargetId = -1;
			return;
		}

		Entity entity = target.entity();
		if (entity == null || entity.getId() == exhaustedTargetId) {
			return;
		}
		if (!trackStableTarget(entity.getId())) {
			resetAutoDelayTracking();
			return;
		}

		if (activeTargetId != entity.getId()) {
			activeTargetId = entity.getId();
			attemptsForActiveTarget = 0;
		}
		if (attemptsForActiveTarget >= MAX_ATTEMPTS_PER_TARGET) {
			exhaustedTargetId = activeTargetId;
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
			return;
		}

		if (!manualModeEnabled() && !autoDelayElapsed(entity.getId(), nowTick)) {
			return;
		}

		if (manualModeEnabled()) {
			clearManualRequest();
		}
		sendDebug("Target locked -> " + describeEntity(entity)
				+ " rangeDist=" + formatNumber(Math.sqrt(target.distanceSq()))
				+ " attempts=" + attemptsForActiveTarget + ".");
		startPickupAttempt(client, target, nowTick);
	}

	private static void handleRotating(Minecraft client, long nowTick) {
		if (nowTick - stateStartedTick > ROTATE_TIMEOUT_TICKS) {
			sendDebug("Rotation timeout for entity id=" + activeTargetId + ".");
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
			return;
		}
		if (RotationController.isRotating()) {
			return;
		}
		if (resolveActiveTarget(client) == null) {
			sendDebug("Lost active target while rotating. id=" + activeTargetId + ".");
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
			return;
		}
		enterState(State.WAIT_AFTER_ROTATE, nowTick);
	}

	private static void handleWaitAfterRotate(Minecraft client, long nowTick) {
		if (nowTick - stateStartedTick < WAIT_AFTER_ROTATE_TICKS) {
			return;
		}

		Entity entity = resolveActiveTarget(client);
		if (entity == null) {
			sendDebug("Lost active target after rotation. id=" + activeTargetId + ".");
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
			return;
		}
		if (interactWithSupply(client, entity)) {
			attemptsForActiveTarget++;
			enterState(State.WAITING_CONFIRM, nowTick);
		}
	}

	private static void startPickupAttempt(Minecraft client, KuudraSupplyWaypoints.SupplyPickupTarget target, long nowTick) {
		if (target == null || target.entity() == null) {
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
			return;
		}

		Entity entity = target.entity();
		if (useDirection()) {
			Vec3 aimPoint = target.cratePosition() != null ? target.cratePosition() : aimPoint(entity);
			if (aimPoint == null) {
				enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
				return;
			}
			sendDebug("Rotating toward " + describeEntity(entity) + " aim=" + formatVec(aimPoint) + ".");
			if (RotationController.rotateTo(aimPoint.x, aimPoint.y, aimPoint.z, ROTATION_MULTIPLIER)) {
				enterState(State.ROTATING, nowTick);
			}
			return;
		}

		sendDebug("Direction disabled, direct entity interact for " + describeEntity(entity) + ".");
		if (interactWithSupply(client, entity)) {
			attemptsForActiveTarget++;
			enterState(State.WAITING_CONFIRM, nowTick);
		} else {
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
		}
	}

	private static void handleWaitingConfirm(long nowTick) {
		if (nowTick - stateStartedTick < CONFIRM_TIMEOUT_TICKS) {
			return;
		}
		sendDebug("No pickup title after " + CONFIRM_TIMEOUT_TICKS + " ticks for id=" + activeTargetId
				+ ", attempt=" + attemptsForActiveTarget + ".");
		if (attemptsForActiveTarget >= MAX_ATTEMPTS_PER_TARGET) {
			exhaustedTargetId = activeTargetId;
			enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
			return;
		}

		nextInteractTick = nowTick + RETRY_DELAY_TICKS;
		enterState(State.IDLE, nowTick);
	}

	private static boolean isKuudraSupplyPhase(Minecraft client) {
		return client != null
				&& client.player != null
				&& client.level != null
				&& ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)
				&& KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.PHASE_SUPPLY;
	}

	private static boolean canInteractNow(Minecraft client) {
		return client != null
				&& client.player != null
				&& client.getConnection() != null
				&& client.screen == null
				&& client.player.containerMenu == client.player.inventoryMenu;
	}

	private static boolean trackStableTarget(int entityId) {
		if (candidateEntityId != entityId) {
			candidateEntityId = entityId;
			candidateStableTicks = 1;
			return false;
		}

		candidateStableTicks++;
		return candidateStableTicks >= STABLE_TARGET_TICKS;
	}

	private static boolean autoDelayElapsed(int entityId, long nowTick) {
		int delayTicks = configuredAutoDelayTicks();
		if (delayTicks <= 0) {
			resetAutoDelayTracking();
			return true;
		}

		if (autoDelayTargetId != entityId) {
			autoDelayTargetId = entityId;
			autoDelayReadyTick = nowTick + delayTicks;
			return false;
		}
		return nowTick >= autoDelayReadyTick;
	}

	private static Entity resolveActiveTarget(Minecraft client) {
		if (client == null || client.level == null || activeTargetId < 0) {
			return null;
		}

		KuudraSupplyWaypoints.SupplyPickupTarget target = KuudraSupplyWaypoints.findClosestPickupTarget(
				client,
				configuredRange());
		if (target == null || target.entity() == null || target.entity().getId() != activeTargetId) {
			return null;
		}

		Entity entity = client.level.getEntity(activeTargetId);
		return entity != null && entity.isAlive() ? entity : null;
	}

	private static Vec3 aimPoint(Entity entity) {
		if (entity == null) {
			return null;
		}

		AABB box = entity.getBoundingBox();
		return new Vec3(
				(box.minX + box.maxX) * 0.5D,
				(box.minY + box.maxY) * 0.5D,
				(box.minZ + box.maxZ) * 0.5D);
	}

	private static boolean interactWithSupply(Minecraft client, Entity entity) {
		if (client == null || client.player == null || client.gameMode == null || entity == null) {
			return false;
		}

		lastAutoInteractEntityId = entity.getId();
		lastAutoInteractTick = currentGameTick(client);
		sendDebug("Auto direct entity interact -> " + describeEntity(entity)
				+ " hand=" + InteractionHand.MAIN_HAND
				+ " secondary=" + client.player.isShiftKeyDown());
		client.gameMode.interact(client.player, entity, InteractionHand.MAIN_HAND);
		return true;
	}

	private static int parsePickupProgress(Component message) {
		String normalized = TextNormalizer.normalize(message == null ? "" : message.getString());
		if (normalized.isBlank()) {
			return -1;
		}

		Matcher matcher = PICKUP_PROGRESS_PATTERN.matcher(normalized);
		if (!matcher.find()) {
			matcher = LEGACY_PICKUP_PROGRESS_PATTERN.matcher(normalized);
			if (!matcher.find()) {
				return -1;
			}
		}

		try {
			return Mth.clamp(Integer.parseInt(matcher.group(1)), 0, 100);
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	private static boolean shouldResetCarry(String normalized) {
		if (normalized == null || normalized.isBlank()) {
			return false;
		}
		return CHEST_SLIPPED_MESSAGE.equals(normalized)
				|| LAVA_RETRIEVED_MESSAGE.equals(normalized)
				|| normalized.endsWith(GHOST_MESSAGE_SUFFIX);
	}

	private static void resetCarryAfterServerMessage() {
		Minecraft client = Minecraft.getInstance();
		long nowTick = currentGameTick(client);
		pickedUpThisSupplyPhase = false;
		resetTargetTracking();
		activeTargetId = -1;
		attemptsForActiveTarget = 0;
		exhaustedTargetId = -1;
		enterCooldown(nowTick, POST_RESET_COOLDOWN_TICKS);
	}

	private static double configuredRange() {
		Double raw = UiDefinitions.AUTO_PICKUP_SUPPLY_RANGE.get();
		double value = raw != null && Double.isFinite(raw) ? raw : DEFAULT_RANGE;
		return Mth.clamp(value, MIN_RANGE, MAX_RANGE);
	}

	private static int configuredAutoDelayTicks() {
		Double raw = UiDefinitions.AUTO_PICKUP_SUPPLY_DELAY.get();
		double delayMs = raw != null && Double.isFinite(raw) ? raw : DEFAULT_AUTO_DELAY_MS;
		delayMs = Mth.clamp(delayMs, MIN_AUTO_DELAY_MS, MAX_AUTO_DELAY_MS);
		return (int) Math.ceil(delayMs / 50.0D);
	}

	private static boolean manualModeEnabled() {
		String mode = UiDefinitions.AUTO_PICKUP_SUPPLY_MODE.get();
		return mode != null && mode.equalsIgnoreCase(MANUAL_MODE);
	}

	private static boolean useDirection() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_PICKUP_SUPPLY_USE_DIRECTION.get());
	}

	private static boolean hasManualRequest(long nowTick) {
		if (manualRequestExpiresTick < 0L) {
			return false;
		}
		if (nowTick <= manualRequestExpiresTick) {
			return true;
		}
		clearManualRequest();
		return false;
	}

	private static void clearManualRequest() {
		manualRequestExpiresTick = -1L;
	}

	private static void enterCooldown(long nowTick, int ticks) {
		nextInteractTick = nowTick + Math.max(1, ticks);
		enterState(State.COOLDOWN, nowTick);
		resetTargetTracking();
	}

	private static void enterState(State nextState, long nowTick) {
		if (state == nextState) {
			return;
		}
		state = nextState;
		stateStartedTick = nowTick;
	}

	private static void resetTargetTracking() {
		candidateEntityId = -1;
		candidateStableTicks = 0;
	}

	private static void resetAutoDelayTracking() {
		autoDelayTargetId = -1;
		autoDelayReadyTick = 0L;
	}

	private static void resetState() {
		pickedUpThisSupplyPhase = false;
		state = State.IDLE;
		stateStartedTick = 0L;
		nextInteractTick = 0L;
		clearManualRequest();
		resetTargetTracking();
		resetAutoDelayTracking();
		activeTargetId = -1;
		attemptsForActiveTarget = 0;
		exhaustedTargetId = -1;
	}

	private static void disableFeature(String reason) {
		UiDefinitions.AUTO_PICKUP_SUPPLY.set(false);
		resetState();
		sendDebug(reason);
	}

	private static void debugInteractPacket(Minecraft client, ServerboundInteractPacket packet, long nowTick) {
		int entityId = interactEntityId(packet);
		Entity entity = client != null && client.level != null ? client.level.getEntity(entityId) : null;
		String source = nowTick - lastAutoInteractTick <= 2L
				? "Auto packet"
				: "Manual packet";
		sendDebug(source + " " + describeInteractAction(packet)
				+ " secondary=" + packet.isUsingSecondaryAction()
				+ " -> " + describeEntity(entity, entityId));
	}

	private static String describeInteractAction(ServerboundInteractPacket packet) {
		final String[] action = { "INTERACT_UNKNOWN" };
		packet.dispatch(new ServerboundInteractPacket.Handler() {
			@Override
			public void onInteraction(InteractionHand hand) {
				action[0] = "INTERACT hand=" + hand;
			}

			@Override
			public void onInteraction(InteractionHand hand, Vec3 location) {
				action[0] = "INTERACT_AT hand=" + hand + " local=" + formatVec(location);
			}

			@Override
			public void onAttack() {
				action[0] = "ATTACK";
			}
		});
		return action[0];
	}

	private static void debugUseItemOnPacket(ServerboundUseItemOnPacket packet) {
		BlockHitResult hit = packet.getHitResult();
		sendDebug("Manual packet BLOCK_USE hand=" + packet.getHand()
				+ " block=" + hit.getBlockPos()
				+ " side=" + hit.getDirection()
				+ " hit=" + formatVec(hit.getLocation())
				+ " type=" + hit.getType()
				+ " inside=" + hit.isInside());
	}

	private static int interactEntityId(ServerboundInteractPacket packet) {
		Integer mapped = readIntField(packet, "entityId");
		if (mapped != null) {
			return mapped;
		}

		for (Field field : ServerboundInteractPacket.class.getDeclaredFields()) {
			if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Integer value = readIntField(packet, field.getName());
			if (value != null) {
				return value;
			}
		}
		return -1;
	}

	private static Integer readIntField(ServerboundInteractPacket packet, String fieldName) {
		try {
			Field field = ServerboundInteractPacket.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.getInt(packet);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static String describeEntity(Entity entity) {
		if (entity == null) {
			return "entity=null";
		}
		return describeEntity(entity, entity.getId());
	}

	private static String describeEntity(Entity entity, int fallbackId) {
		if (entity == null) {
			return "id=" + fallbackId + " entity=null";
		}

		String name = TextNormalizer.stripFormattingCodes(entity.getName().getString());
		AABB box = entity.getBoundingBox();
		return "id=" + entity.getId()
				+ " type=" + entity.getClass().getSimpleName()
				+ " name=\"" + name + "\""
				+ " pos=" + formatVec(entity.position())
				+ " box=" + formatBox(box);
	}

	private static String formatBox(AABB box) {
		if (box == null) {
			return "null";
		}
		return "min=" + formatVec(new Vec3(box.minX, box.minY, box.minZ))
				+ " max=" + formatVec(new Vec3(box.maxX, box.maxY, box.maxZ));
	}

	private static String formatVec(Vec3 vec) {
		if (vec == null) {
			return "(null)";
		}
		return "(" + formatNumber(vec.x) + ", " + formatNumber(vec.y) + ", " + formatNumber(vec.z) + ")";
	}

	private static String formatNumber(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static void sendDebug(String text) {
		if (!isDebugEnabled()) {
			return;
		}
		FeatureChat.sendPrefixed("Auto Supply Pickup", "[Debug] " + text);
	}

	private static boolean isDebugEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_PICKUP_SUPPLY_DEBUG.get());
	}

	private static long currentGameTick(Minecraft client) {
		if (client != null && client.level != null) {
			return client.level.getGameTime();
		}
		return 0L;
	}

	private enum State {
		IDLE,
		ROTATING,
		WAIT_AFTER_ROTATE,
		WAITING_CONFIRM,
		CONFIRMED_PICKUP,
		COOLDOWN
	}
}
*/

public final class KuudraAutoPickupSupply {
	private KuudraAutoPickupSupply() {
	}

	public static void init() {
	}

	public static void requestManualPickup() {
	}

	public static void onPacketSent(Packet<?> packet) {
	}

	public static void onSystemChat(Component message) {
	}

	public static void onTitleText(Component message) {
	}
}
