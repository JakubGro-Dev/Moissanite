package com.crussion.moissanite.features.cheats;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.features.kuudra.KuudraNoPre;
import com.crussion.moissanite.input.FakeKeybinds;
import com.crussion.moissanite.ui.screen.MoveElementScreen;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.HeldItemMatcher;
import com.crussion.moissanite.util.inventory.HotbarItemSearch;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class AutoPearl {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String PEARL_ID = "ENDER_PEARL";
	private static final double DEFAULT_ROTATION_MULTIPLIER = 0.4D;
	private static final double ROTATION_MULTIPLIER_MIN = 0.0D;
	private static final double ROTATION_MULTIPLIER_MAX = 2.0D;
	private static final double INACCURACY_MIN = 0.0D;
	private static final double INACCURACY_MAX = 1.0D;
	private static final double MAX_INACCURACY_YAW_DEGREES = 4.0D;
	private static final double MAX_INACCURACY_PITCH_DEGREES = 3.0D;
	private static final int WAIT_AFTER_SWAP_TICKS = 2;
	private static final int ROTATE_TIMEOUT_TICKS = 120;
	private static final int SEQUENCE_TIMEOUT_TICKS = 200;
	private static final long SEQUENCE_START_LEAD_MS = 1500L;
	private static final double THROW_PATH_CHECK_DISTANCE = 7.0D;
	private static final double THROW_PATH_PROBE_RADIUS = 0.125D;
	private static final double TRAJECTORY_EPSILON = 1.0E-6D;
	private static final int TALISMAN_TIER_MIN = 0;
	private static final int TALISMAN_TIER_MAX = 3;
	private static final int KUUDRA_TIER_MIN = 1;
	private static final int KUUDRA_TIER_MAX = 5;
	private static final int[][] PEARL_DELAY_MS = {
			{ 0, 3000, 4000, 5000, 6000, 6000 },
			{ 0, 2750, 3750, 4500, 5500, 5500 },
			{ 0, 2500, 3250, 4000, 5000, 5000 },
			{ 0, 2250, 3000, 3500, 4250, 4250 }
	};
	private static final Pattern PICKUP_PROGRESS_PATTERN = Pattern.compile("\\[\\|+]\\s*(\\d+)%");
	private static final Pattern LEGACY_PICKUP_PROGRESS_PATTERN = Pattern.compile("(\\d{1,3})%");
	private static final Pattern KUUDRA_TIER_PATTERN = Pattern.compile("\\bt([1-5])\\b");
	private static final long PICKUP_TITLE_TIMEOUT_MS = 750L;

	private static final String SUPPLY_READY_MARKER = "bring supply chest here";
	private static final String SUPPLY_RECEIVED_MARKER = "supplies received";
	private static final String SUPPLY_PROGRESS_MARKER = "progress:";
	private static final String SUPPLY_COMPLETE_MARKER = "complete";
	private static final String CHEST_SLIPPED_MESSAGE = "you moved and the chest slipped out of your hands!";
	private static final String LAVA_RETRIEVED_MESSAGE = "you retrieved some of elle's supplies from the lava!";
	private static final String GHOST_MESSAGE_SUFFIX = "you were killed by kuudra follower and became a ghost.";
	private static final long PEARL_TIMER_QUANTUM_MS = 25L;

	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath("moissanite", "auto_pearl_status");
	private static final double HUD_POSITION_MIN = -10000.0D;
	private static final double HUD_POSITION_MAX = 10000.0D;
	private static final double HUD_DEFAULT_X = 6.0D;
	private static final double HUD_DEFAULT_Y = 6.0D;
	private static final String HUD_TEXT = "Auto Pearl";
	private static final int ENABLED_COLOR = 0x55FF55;
	private static final int DISABLED_COLOR = 0xFF5555;
	private static final MoveElementScreen.MovableElementAdapter<DrawState> MOVE_ADAPTER = new MoveElementScreen.MovableElementAdapter<>() {
		@Override
		public Component title() {
			return Component.literal("Move Auto Pearl");
		}

		@Override
		public Component unavailableMessage() {
			return Component.literal("Unable to draw Auto Pearl preview");
		}

		@Override
		public DrawState getDrawState(int screenWidth, int screenHeight) {
			return AutoPearl.getMoveDrawState();
		}

		@Override
		public MoveElementScreen.Position getVisiblePosition(DrawState drawState, int screenWidth, int screenHeight,
				boolean persistIfAdjusted) {
			Position position = AutoPearl.getVisibleConfiguredPosition(drawState, screenWidth, screenHeight,
					persistIfAdjusted);
			return new MoveElementScreen.Position(position.x(), position.y());
		}

		@Override
		public MoveElementScreen.Position clampPosition(DrawState drawState, int screenWidth, int screenHeight, int x,
				int y) {
			Position position = AutoPearl.clampPosition(drawState, screenWidth, screenHeight, x, y);
			return new MoveElementScreen.Position(position.x(), position.y());
		}

		@Override
		public int drawWidth(DrawState drawState) {
			return drawState.drawWidth();
		}

		@Override
		public int drawHeight(DrawState drawState) {
			return drawState.drawHeight();
		}

		@Override
		public void draw(GuiGraphics graphics, DrawState drawState, int x, int y) {
			AutoPearl.drawLabel(graphics, drawState, x, y);
		}

		@Override
		public void applyDraggedPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
			AutoPearl.setConfiguredPosition(x, y);
		}

		@Override
		public List<String> infoLines(DrawState drawState, int x, int y) {
			int configuredX = (int) Math.round(AutoPearl.getConfiguredX());
			int configuredY = (int) Math.round(AutoPearl.getConfiguredY());
			return List.of("x " + configuredX, "y " + configuredY);
		}

		@Override
		public void onScreenClosed() {
			AutoPearl.setMoveModeActive(false);
		}
	};

	private static boolean initialized;
	private static boolean moveModeActive;
	private static boolean runtimeEnabled = true;
	private static boolean pearlThrownForCurrentCarry;
	private static boolean disabledTriggerLogged;
	private static boolean pickupTimerActive;
	private static long pickupTimerStartServerMs = -1L;
	private static long lastPickupTitleServerMs = -1L;
	private static String lastBlockedReason = "";

	private static SequenceStep currentStep = SequenceStep.IDLE;
	private static int stepElapsedTicks;
	private static int sequenceElapsedTicks;
	private static boolean stepStarted;
	private static ThrowPlan currentPlan;
	private static long currentThrowAtMs = -1L;

	private static final Map<SupplySpot, SupplyState> SUPPLY_STATES = new EnumMap<>(SupplySpot.class);

	static {
		for (SupplySpot spot : SupplySpot.values()) {
			SUPPLY_STATES.put(spot, SupplyState.UNKNOWN);
		}
	}

	private AutoPearl() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		FakeKeybinds.onKeyPress(UiDefinitions.AUTO_PEARL_KEYBIND, AutoPearl::toggleEnabled);
		ClientTickEvents.END_CLIENT_TICK.register(AutoPearl::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetAllState());
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				HUD_ELEMENT_ID,
				(graphics, tickCounter) -> renderOverlay(graphics));
	}

	public static void openMoveScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		Screen returnScreen = client.screen;
		setMoveModeActive(true);
		client.setScreen(new MoveElementScreen<>(returnScreen, MOVE_ADAPTER));
	}

	public static void setMoveModeActive(boolean moveModeActive) {
		AutoPearl.moveModeActive = moveModeActive;
	}

	public static void onSystemChat(Component message) {
		if (message == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_SUPPLY) {
			return;
		}

		String normalized = TextNormalizer.normalize(message.getString());
		if (!shouldResetFromSystemChat(normalized)) {
			return;
		}

		sendDebug("Pickup tracking reset from chat.");
		resetPickupTracking();
		if (currentStep != SequenceStep.IDLE) {
			sendDebug("Cancelling active pearl sequence after pickup reset.");
			resetSequence();
		}
	}

	public static void onTitleText(Component message) {
		if (message == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_SUPPLY) {
			return;
		}

		String normalized = TextNormalizer.normalize(message.getString());
		if (normalized.isBlank()) {
			return;
		}

		Matcher matcher = PICKUP_PROGRESS_PATTERN.matcher(normalized);
		boolean matched = matcher.find();
		if (!matched) {
			matcher = LEGACY_PICKUP_PROGRESS_PATTERN.matcher(normalized);
			matched = matcher.find();
		}
		if (!matched) {
			return;
		}

		int percent;
		try {
			percent = Mth.clamp(Integer.parseInt(matcher.group(1)), 0, 100);
		} catch (NumberFormatException ex) {
			return;
		}

		long nowServerMs = serverNowMs();
		lastPickupTitleServerMs = nowServerMs;

		if (percent == 0) {
			startPickupTimer(nowServerMs);
			return;
		}
		if (pickupTimerActive && percent >= 100) {
			sendDebug("Pickup tracking completed at 100%.");
			resetPickupTracking();
			if (currentStep != SequenceStep.IDLE) {
				sendDebug("Cancelling active pearl sequence after pickup completion.");
				resetSequence();
			}
		}
	}

	private static boolean shouldResetFromSystemChat(String normalized) {
		if (normalized == null || normalized.isBlank()) {
			return false;
		}
		return CHEST_SLIPPED_MESSAGE.equals(normalized)
				|| LAVA_RETRIEVED_MESSAGE.equals(normalized)
				|| normalized.endsWith(GHOST_MESSAGE_SUFFIX);
	}

	private static void toggleEnabled() {
		if (!isGuiEnabled()) {
			sendMessage("Enable Auto Pearl in GUI first.");
			return;
		}

		runtimeEnabled = !runtimeEnabled;
		if (!runtimeEnabled) {
			resetRuntimeState();
		}
		sendMessage(runtimeEnabled ? "Enabled." : "Disabled.");
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			reportBlocked("Waiting for world and player.");
			disabledTriggerLogged = false;
			resetRuntimeState();
			return;
		}

		boolean inKuudra = ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW);
		boolean inSupplyPhase = inKuudra && KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.PHASE_SUPPLY;

		if (inSupplyPhase) {
			updateSupplyStates(client);
			refreshPickupTracking();
		}

		if (!isGuiEnabled()) {
			reportBlocked("Auto Pearl is disabled.");
			if (inSupplyPhase) {
				handleDisabledTrigger(client);
				resetSequence();
			} else {
				disabledTriggerLogged = false;
				resetRuntimeState();
			}
			return;
		}
		if (!runtimeEnabled) {
			reportBlocked("Auto Pearl toggle is disabled.");
			if (inSupplyPhase) {
				handleDisabledTrigger(client);
				resetSequence();
			} else {
				disabledTriggerLogged = false;
				resetRuntimeState();
			}
			return;
		}

		disabledTriggerLogged = false;

		if (!inKuudra) {
			reportBlocked("Not in Kuudra's Hollow.");
			resetRuntimeState();
			return;
		}
		if (!inSupplyPhase) {
			reportBlocked("Not in supply phase (phase " + KuudraPhaseTracker.getPhase() + ").");
			resetRuntimeState();
			return;
		}

		if (currentStep != SequenceStep.IDLE) {
			clearBlockedReason();
			tickSequence(client);
			return;
		}
		if (pearlThrownForCurrentCarry) {
			reportBlocked("Pearl already thrown for current timer window.");
			return;
		}
		if (!pickupTimerActive) {
			reportBlocked("Waiting for pickup timer start (0% title).");
			return;
		}
		WaypointEvaluation waypoint = evaluatePrimaryWaypoint(client);
		if (waypoint == null) {
			reportBlocked("No valid waypoint available yet.");
			return;
		}
		if (waypoint.timerRemainingMs() > SEQUENCE_START_LEAD_MS) {
			reportBlocked("Waiting for sequence lead window.");
			return;
		}
		clearBlockedReason();
		sendDebug("Entering sequence window, starting prep.");
		startSequence(waypoint.plan(), waypoint.throwAtMs());
	}

	private static void handleDisabledTrigger(Minecraft client) {
		if (!isDebugEnabled()) {
			disabledTriggerLogged = false;
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)
				|| KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_SUPPLY
				|| !pickupTimerActive) {
			disabledTriggerLogged = false;
			return;
		}
		WaypointEvaluation waypoint = evaluatePrimaryWaypoint(client);
		if (waypoint == null || waypoint.timerRemainingMs() > SEQUENCE_START_LEAD_MS) {
			disabledTriggerLogged = false;
			return;
		}
		if (disabledTriggerLogged) {
			return;
		}

		sendDebug("Wanted to trigger for " + waypoint.plan().target().name() + " while Auto Pearl is disabled.");
		disabledTriggerLogged = true;
	}

	private static void reportBlocked(String reason) {
		if (!isDebugEnabled()) {
			lastBlockedReason = "";
			return;
		}
		if (reason == null || reason.isBlank()) {
			return;
		}
		if (reason.equals(lastBlockedReason)) {
			return;
		}
		lastBlockedReason = reason;
		sendDebug(reason);
	}

	private static void clearBlockedReason() {
		lastBlockedReason = "";
	}

	private static void startPickupTimer(long startServerMs) {
		pickupTimerStartServerMs = startServerMs;
		lastPickupTitleServerMs = startServerMs;
		pickupTimerActive = true;
		pearlThrownForCurrentCarry = false;
		int pickupDelayMs = resolvePickupDelayMs();
		int talismanTier = configuredTalismanTier();
		int kuudraTier = configuredKuudraTier();
		sendDebug("Pickup timer started at 0% -> " + pickupDelayMs + "ms (talisman T" + talismanTier + ", kuudra T"
				+ kuudraTier + ").");
	}

	private static WaypointEvaluation evaluatePrimaryWaypoint(Minecraft client) {
		if (!pickupTimerActive || pickupTimerStartServerMs < 0L) {
			return null;
		}
		updateSupplyStates(client);
		PrimaryPlans plans = buildThrowPlans(client);
		if (plans == null) {
			return null;
		}

		TimedThrowPlan timedSky = timedPlan(plans.sky());
		TimedThrowPlan timedFlat = timedPlan(plans.flat());
		TimedThrowPlan selected = selectPreferredPlan(client, timedSky, timedFlat);
		if (selected == null) {
			return null;
		}

		long timerRemainingMs = selected.timerRemainingMs();
		long throwAtMs = serverNowMs() + timerRemainingMs;
		return new WaypointEvaluation(
				selected.plan(),
				throwAtMs,
				timerRemainingMs,
				timedSky == null ? null : timedSky.plan(),
				timedFlat == null ? null : timedFlat.plan());
	}

	private static long serverNowMs() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.level != null) {
			return client.level.getGameTime() * 50L;
		}
		return System.currentTimeMillis();
	}

	private static long timeUntilThrowMs(long flightTimeMs) {
		if (!pickupTimerActive || pickupTimerStartServerMs < 0L) {
			return Long.MAX_VALUE;
		}
		long rawRemainingMs = (long) resolvePickupDelayMs() - flightTimeMs - elapsedSincePickupStartMs();
		long quantizedRemainingMs = (rawRemainingMs / PEARL_TIMER_QUANTUM_MS) * PEARL_TIMER_QUANTUM_MS;
		return Math.max(0L, quantizedRemainingMs);
	}

	private static void refreshPickupTracking() {
		if (!pickupTimerActive || lastPickupTitleServerMs < 0L) {
			return;
		}
		if (serverNowMs() - lastPickupTitleServerMs <= PICKUP_TITLE_TIMEOUT_MS) {
			return;
		}

		sendDebug("Pickup tracking timed out.");
		resetPickupTracking();
		if (currentStep != SequenceStep.IDLE) {
			sendDebug("Pickup tracking timed out after sequence start; keeping locked throw time.");
		}
	}

	private static long elapsedSincePickupStartMs() {
		if (pickupTimerStartServerMs < 0L) {
			return 0L;
		}
		return Math.max(0L, serverNowMs() - pickupTimerStartServerMs);
	}

	private static int resolvePickupDelayMs() {
		int talismanTier = configuredTalismanTier();
		int kuudraTier = configuredKuudraTier();
		return (int) (PEARL_DELAY_MS[talismanTier][kuudraTier]);
	}

	private static int configuredTalismanTier() {
		return sliderToInt(UiDefinitions.AUTO_PEARL_TALISMAN_TIER.get(), TALISMAN_TIER_MIN, TALISMAN_TIER_MIN,
				TALISMAN_TIER_MAX);
	}

	private static int configuredKuudraTier() {
		Integer detectedTier = detectKuudraTierFromScoreboard();
		if (detectedTier != null) {
			return detectedTier;
		}
		return sliderToInt(UiDefinitions.AUTO_PEARL_KUUDRA_TIER.get(), KUUDRA_TIER_MAX, KUUDRA_TIER_MIN,
				KUUDRA_TIER_MAX);
	}

	private static int sliderToInt(Double value, int fallback, int min, int max) {
		double raw = value != null && Double.isFinite(value) ? value : fallback;
		return Mth.clamp((int) Math.round(raw), min, max);
	}

	private static double sliderToDouble(Double value, double fallback, double min, double max) {
		double raw = value != null && Double.isFinite(value) ? value : fallback;
		return Mth.clamp(raw, min, max);
	}

	private static double configuredRotationMultiplier() {
		return sliderToDouble(
				UiDefinitions.AUTO_PEARL_ROTATION_MULTIPLIER.get(),
				DEFAULT_ROTATION_MULTIPLIER,
				ROTATION_MULTIPLIER_MIN,
				ROTATION_MULTIPLIER_MAX);
	}

	private static double configuredInaccuracy() {
		return sliderToDouble(UiDefinitions.AUTO_PEARL_INACCURACY.get(), 0.0D, INACCURACY_MIN, INACCURACY_MAX);
	}

	private static double randomInaccuracyOffset(double inaccuracy, double maxDegrees) {
		if (inaccuracy <= 0.0D || maxDegrees <= 0.0D) {
			return 0.0D;
		}
		double range = Mth.clamp(inaccuracy, INACCURACY_MIN, INACCURACY_MAX) * maxDegrees;
		return ThreadLocalRandom.current().nextDouble(-range, range);
	}

	private static Integer detectKuudraTierFromScoreboard() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return null;
		}

		Scoreboard scoreboard = client.level.getScoreboard();
		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (sidebar == null) {
			return null;
		}

		Integer fallbackTier = extractTierFromText(sidebar.getDisplayName().getString(), false);
		Integer strictTier = extractTierFromText(sidebar.getDisplayName().getString(), true);
		if (strictTier != null) {
			return strictTier;
		}

		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
			if (entry == null || entry.isHidden()) {
				continue;
			}
			String line = sidebarLine(scoreboard, entry);
			strictTier = extractTierFromText(line, true);
			if (strictTier != null) {
				return strictTier;
			}
			if (fallbackTier == null) {
				fallbackTier = extractTierFromText(line, false);
			}
		}
		return fallbackTier;
	}

	private static Integer extractTierFromText(String rawText, boolean requireKuudraToken) {
		String normalized = TextNormalizer.normalize(rawText);
		if (normalized.isBlank()) {
			return null;
		}
		if (requireKuudraToken && !normalized.contains("kuudra")) {
			return null;
		}
		Matcher matcher = KUUDRA_TIER_PATTERN.matcher(normalized);
		if (!matcher.find()) {
			return null;
		}
		try {
			return Mth.clamp(Integer.parseInt(matcher.group(1)), KUUDRA_TIER_MIN, KUUDRA_TIER_MAX);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static String sidebarLine(Scoreboard scoreboard, PlayerScoreEntry entry) {
		if (entry.display() != null) {
			return entry.display().getString();
		}
		String owner = entry.owner();
		PlayerTeam team = scoreboard.getPlayersTeam(owner);
		return PlayerTeam.formatNameForTeam(team, Component.literal(owner)).getString();
	}

	private static void updateSupplyStates(Minecraft client) {
		if (client == null || client.level == null) {
			return;
		}

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof ArmorStand armorStand) || !armorStand.isAlive()) {
				continue;
			}
			SupplySpot spot = SupplySpot.fromEntityPosition(armorStand.getX(), armorStand.getZ());
			if (spot == null) {
				continue;
			}
			SupplyState state = stateFromLabel(armorStand.getName().getString());
			if (state != SupplyState.UNKNOWN) {
				SUPPLY_STATES.put(spot, state);
			}
		}
	}

	private static SupplyState stateFromLabel(String rawLabel) {
		String label = TextNormalizer.normalize(rawLabel);
		if (label.isBlank()) {
			return SupplyState.UNKNOWN;
		}
		if (label.contains(SUPPLY_READY_MARKER)) {
			return SupplyState.NOTHING;
		}
		if (label.contains(SUPPLY_RECEIVED_MARKER)) {
			return SupplyState.RECEIVED;
		}
		if (label.contains(SUPPLY_PROGRESS_MARKER)) {
			return label.contains(SUPPLY_COMPLETE_MARKER) ? SupplyState.COMPLETED : SupplyState.IN_PROGRESS;
		}
		return SupplyState.UNKNOWN;
	}

	private static PrimaryPlans buildThrowPlans(Minecraft client) {
		if (client == null || client.player == null) {
			return null;
		}

		Vec3 eyePos = getPlayerEyePos(client);
		Vec3 pearlStart = getPearlSpawnPos(client);
		if (eyePos == null || pearlStart == null) {
			return null;
		}

		SupplySpot targetSpot = resolveTargetSupply(eyePos, PickupSpot.closestTo(eyePos));
		if (targetSpot == null) {
			return null;
		}

		ThrowPlan skyPlan = createThrowPlan(true, pearlStart, targetSpot);
		ThrowPlan flatPlan = createThrowPlan(false, pearlStart, targetSpot);
		if (skyPlan == null && flatPlan == null) {
			return null;
		}
		return new PrimaryPlans(skyPlan, flatPlan);
	}

	private static ThrowPlan createThrowPlan(boolean sky, Vec3 pearlStart, SupplySpot targetSpot) {
		TrajectorySolver.PearlSolution solution = TrajectorySolver.solvePearl(sky, pearlStart, targetSpot.location());
		if (solution == null || solution.solution() == null) {
			return null;
		}
		return new ThrowPlan(
				targetSpot,
				solution.solution(),
				solution.yaw(),
				solution.pitch(),
				solution.flightTimeMs(),
				sky);
	}

	private static TimedThrowPlan timedPlan(ThrowPlan plan) {
		if (plan == null) {
			return null;
		}
		return new TimedThrowPlan(plan, timeUntilThrowMs(plan.flightTimeMs()));
	}

	private static TimedThrowPlan selectPreferredPlan(Minecraft client, TimedThrowPlan skyPlan, TimedThrowPlan flatPlan) {
		if (flatPlan != null && !isThrowPathBlocked(client, flatPlan.plan())) {
			return flatPlan;
		}
		if (skyPlan != null && !isThrowPathBlocked(client, skyPlan.plan())) {
			return skyPlan;
		}
		return null;
	}

	private static boolean isThrowPathBlocked(Minecraft client, ThrowPlan plan) {
		if (client == null || client.level == null || client.player == null || plan == null) {
			return true;
		}

		Vec3 position = getPearlSpawnPos(client);
		if (position == null) {
			return true;
		}

		Vec3 velocity = initialPearlVelocity(plan);
		double remainingDistance = THROW_PATH_CHECK_DISTANCE;
		for (int tick = 0; tick < TrajectorySolver.MAX_SIM_TICKS && remainingDistance > TRAJECTORY_EPSILON; tick++) {
			double segmentLength = velocity.length();
			if (segmentLength <= TRAJECTORY_EPSILON) {
				return false;
			}

			double distanceToCheck = Math.min(segmentLength, remainingDistance);
			Vec3 segmentEnd = position.add(velocity.scale(distanceToCheck / segmentLength));
			if (probeSegmentBlocked(client, position, segmentEnd)) {
				return true;
			}

			remainingDistance -= distanceToCheck;
			position = segmentEnd;
			velocity = new Vec3(
					velocity.x * TrajectorySolver.DRAG,
					(velocity.y * TrajectorySolver.DRAG) - TrajectorySolver.GRAVITY,
					velocity.z * TrajectorySolver.DRAG);
		}
		return false;
	}

	private static boolean probeSegmentBlocked(Minecraft client, Vec3 start, Vec3 end) {
		if (segmentHitBlock(client, start, end)) {
			return true;
		}
		Vec3 xOffset = new Vec3(THROW_PATH_PROBE_RADIUS, 0.0D, 0.0D);
		Vec3 yOffset = new Vec3(0.0D, THROW_PATH_PROBE_RADIUS, 0.0D);
		Vec3 zOffset = new Vec3(0.0D, 0.0D, THROW_PATH_PROBE_RADIUS);
		return segmentHitBlock(client, start.add(xOffset), end.add(xOffset))
				|| segmentHitBlock(client, start.subtract(xOffset), end.subtract(xOffset))
				|| segmentHitBlock(client, start.add(yOffset), end.add(yOffset))
				|| segmentHitBlock(client, start.subtract(yOffset), end.subtract(yOffset))
				|| segmentHitBlock(client, start.add(zOffset), end.add(zOffset))
				|| segmentHitBlock(client, start.subtract(zOffset), end.subtract(zOffset));
	}

	private static boolean segmentHitBlock(Minecraft client, Vec3 start, Vec3 end) {
		HitResult hitResult = client.level.clip(new ClipContext(
				start,
				end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				client.player));
		return hitResult.getType() == HitResult.Type.BLOCK;
	}

	private static Vec3 initialPearlVelocity(ThrowPlan plan) {
		double yawRad = Math.toRadians(plan.yaw());
		double pitchRad = Math.toRadians(plan.pitch());
		double cosPitch = Math.cos(pitchRad);
		double speed = TrajectorySolver.SPEED;
		return new Vec3(
				-Math.sin(yawRad) * cosPitch * speed,
				-Math.sin(pitchRad) * speed,
				Math.cos(yawRad) * cosPitch * speed);
	}

	private static SupplySpot resolveTargetSupply(Vec3 eyePos) {
		return resolveTargetSupply(eyePos, PickupSpot.closestTo(eyePos));
	}

	private static SupplySpot resolveTargetSupply(Vec3 eyePos, PickupSpot pickup) {
		SupplySpot confirmed = resolveTargetSupply(eyePos, pickup, false);
		if (confirmed != null) {
			return confirmed;
		}
		return resolveTargetSupply(eyePos, pickup, true);
	}

	private static SupplySpot resolveTargetSupply(Vec3 eyePos, PickupSpot pickup, boolean includeUnknown) {
		SupplySpot preSpotSupply = resolvePreSpotSupply();
		boolean preSpotAvailable = preSpotSupply != null && isSupplyAvailable(preSpotSupply, includeUnknown);

		if (pickup == PickupSpot.SQUARE && preSpotAvailable) {
			return preSpotSupply;
		}

		SupplySpot mapped = mapPickupToSupply(pickup);
		if (mapped != null && isSupplyAvailable(mapped, includeUnknown)) {
			return mapped;
		}

		SupplySpot closestAvailable = getClosestAvailableSupply(eyePos, includeUnknown);
		if (closestAvailable != null) {
			return closestAvailable;
		}

		return preSpotAvailable ? preSpotSupply : null;
	}

	private static SupplySpot mapPickupToSupply(PickupSpot pickup) {
		if (pickup == null) {
			return null;
		}
		return switch (pickup) {
			case SHOP -> SupplySpot.SUPPLY1;
			case X -> SupplySpot.SUPPLY2;
			case X_CANNON -> SupplySpot.SUPPLY3;
			case EQUALS -> SupplySpot.SUPPLY4;
			case SLASH -> SupplySpot.SUPPLY5;
			case TRIANGLE -> SupplySpot.SUPPLY6;
			case SQUARE, NONE -> null;
		};
	}

	private static SupplySpot resolvePreSpotSupply() {
		KuudraNoPre.PickupSpot preSpot = KuudraNoPre.getPreSpot();
		if (preSpot == null || preSpot == KuudraNoPre.PickupSpot.NONE) {
			return null;
		}
		return mapNoPrePickupToSupply(preSpot);
	}

	private static SupplySpot mapNoPrePickupToSupply(KuudraNoPre.PickupSpot pickup) {
		if (pickup == null) {
			return null;
		}
		return switch (pickup) {
			case SHOP -> SupplySpot.SUPPLY1;
			case X -> SupplySpot.SUPPLY2;
			case X_CANNON -> SupplySpot.SUPPLY3;
			case EQUALS -> SupplySpot.SUPPLY4;
			case SLASH -> SupplySpot.SUPPLY5;
			case TRIANGLE -> SupplySpot.SUPPLY6;
			case SQUARE, NONE -> null;
		};
	}

	private static SupplySpot getClosestAvailableSupply(Vec3 eyePos, boolean includeUnknown) {
		SupplySpot best = null;
		double bestDistSq = Double.MAX_VALUE;

		for (SupplySpot spot : SupplySpot.values()) {
			if (!isSupplyAvailable(spot, includeUnknown)) {
				continue;
			}
			double distSq = distanceSquared(eyePos, spot.location());
			if (distSq < bestDistSq) {
				bestDistSq = distSq;
				best = spot;
			}
		}
		return best;
	}

	private static double distanceSquared(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		return dx * dx + dy * dy + dz * dz;
	}

	private static boolean isSupplyAvailable(SupplySpot spot) {
		return isSupplyAvailable(spot, false);
	}

	private static boolean isSupplyAvailable(SupplySpot spot, boolean includeUnknown) {
		SupplyState state = SUPPLY_STATES.getOrDefault(spot, SupplyState.UNKNOWN);
		return state == SupplyState.NOTHING || (includeUnknown && state == SupplyState.UNKNOWN);
	}

	private static void startSequence(ThrowPlan plan, long throwAtMs) {
		currentPlan = plan;
		currentThrowAtMs = throwAtMs;
		sequenceElapsedTicks = 0;
		sendDebug("Sequence start -> " + (plan.sky() ? "SKY" : "FLAT") + " target " + plan.target().name()
				+ ", aim " + formatVec(plan.aimPoint()) + ", flight " + plan.flightTimeMs() + "ms.");
		enterStep(SequenceStep.ROTATE);
	}

	private static void tickSequence(Minecraft client) {
		if (currentStep == SequenceStep.IDLE || currentPlan == null) {
			resetSequence();
			return;
		}

		sequenceElapsedTicks++;
		stepElapsedTicks++;
		if (sequenceElapsedTicks > SEQUENCE_TIMEOUT_TICKS) {
			sendMessage("Sequence timeout, resetting.");
			resetSequence();
			return;
		}

		switch (currentStep) {
			case ROTATE -> handleRotateToAim();
			case SWAP_TO_PEARL -> handleSwapPearl(client);
			case WAIT_AFTER_SWAP -> handleWaitAfterSwap();
			case WAIT_FOR_THROW_WINDOW -> handleWaitForThrowWindow();
			case THROW_PEARL -> handleThrowPearl();
			case IDLE -> resetSequence();
		}
	}

	private static void handleRotateToAim() {
		if (currentPlan == null) {
			resetSequence();
			return;
		}
		if (!stepStarted) {
			stepStarted = true;
			double multiplier = configuredRotationMultiplier();
			double inaccuracy = configuredInaccuracy();
			double yaw = currentPlan.yaw() + randomInaccuracyOffset(inaccuracy, MAX_INACCURACY_YAW_DEGREES);
			double pitch = Mth.clamp(
					currentPlan.pitch() + randomInaccuracyOffset(inaccuracy, MAX_INACCURACY_PITCH_DEGREES),
					-90.0D,
					90.0D);
			sendDebug("ROTATE: aiming yaw=" + yaw + ", pitch=" + pitch + ", multiplier=" + multiplier
					+ ", inaccuracy=" + inaccuracy + ".");
			boolean started = RotationController.rotateYawPitch(
					yaw,
					pitch,
					multiplier,
					0.0D,
					0.0D);
			sendDebug("ROTATE: rotateTo " + actionStatus(started) + ".");
			if (!started) {
				sendMessage("Failed to start rotation.");
				resetSequence();
				return;
			}
		}
		if (stepElapsedTicks > ROTATE_TIMEOUT_TICKS) {
			sendMessage("Rotation timeout, resetting.");
			resetSequence();
			return;
		}
		if (waitedAfterAction(1) && !RotationController.isRotating()) {
			sendDebug("ROTATE: completed.");
			enterStep(SequenceStep.SWAP_TO_PEARL);
		}
	}

	private static void handleSwapPearl(Minecraft client) {
		if (!stepStarted) {
			stepStarted = true;
			sendDebug("SWAP_TO_PEARL: scanning hotbar for pearls.");
			int pearlSlot = HotbarItemSearch.findFirstHotbarSlotByNbt(PEARL_ID);
			if (pearlSlot < 0) {
				sendDebug("SWAP_TO_PEARL: no pearl slot found.");
				sendMessage("No pearls found in hotbar.");
				pearlThrownForCurrentCarry = true;
				resetSequence();
				return;
			}
			sendDebug("SWAP_TO_PEARL: pearl slot " + pearlSlot + ".");

			boolean swapped = HotbarItemSearch.swapHeldItem(pearlSlot);
			sendDebug("SWAP_TO_PEARL: swap " + actionStatus(swapped) + ".");
			if (!swapped) {
				sendMessage("Failed to swap to pearls.");
				resetSequence();
				return;
			}
		}

		if (waitedAfterAction(1) && isHoldingPearl(client)) {
			sendDebug("SWAP_TO_PEARL: pearl equipped.");
			enterStep(SequenceStep.WAIT_AFTER_SWAP);
		}
	}

	private static void handleWaitAfterSwap() {
		if (!stepStarted) {
			stepStarted = true;
			sendDebug("WAIT_AFTER_SWAP: waiting " + WAIT_AFTER_SWAP_TICKS + " ticks.");
		}
		if (waitedAfterAction(WAIT_AFTER_SWAP_TICKS)) {
			sendDebug("WAIT_AFTER_SWAP: completed.");
			enterStep(SequenceStep.WAIT_FOR_THROW_WINDOW);
		}
	}

	private static void handleWaitForThrowWindow() {
		if (!stepStarted) {
			stepStarted = true;
			sendDebug("WAIT_FOR_THROW_WINDOW: waiting for throw time.");
		}

		if (currentPlan == null) {
			resetSequence();
			return;
		}
		if (currentThrowAtMs < 0L) {
			sendDebug("WAIT_FOR_THROW_WINDOW: missing locked throw time.");
			resetSequence();
			return;
		}
		if ((currentThrowAtMs - serverNowMs()) > 50L) {
			return;
		}

		sendDebug("WAIT_FOR_THROW_WINDOW: throw time reached.");
		enterStep(SequenceStep.THROW_PEARL);
		handleThrowPearl();
	}

	private static void handleThrowPearl() {
		if (currentPlan == null) {
			resetSequence();
			return;
		}
		if (!stepStarted) {
			Minecraft client = Minecraft.getInstance();
			if (isThrowPathBlocked(client, currentPlan)) {
				sendDebug("THROW_PEARL: blocked path, cancelling sequence.");
				resetSequence();
				return;
			}
			stepStarted = true;
			sendDebug("THROW_PEARL: direct use.");
			boolean thrown = PlayerInputActions.useMainHandItemDirect();
			sendDebug("THROW_PEARL: direct use " + actionStatus(thrown) + ".");
			if (!thrown) {
				sendMessage("Failed to throw pearl.");
				resetSequence();
				return;
			}
			pearlThrownForCurrentCarry = true;
			resetPickupTracking();
		}

		if (waitedAfterAction(1)) {
			sendDebug("THROW_PEARL: sequence finished for this carry.");
			resetSequence();
		}
	}

	private static boolean isHoldingPearl(Minecraft client) {
		if (HeldItemMatcher.heldMatchesSkyblockId(PEARL_ID)) {
			return true;
		}
		if (client == null || client.player == null) {
			return false;
		}

		ItemStack held = client.player.getMainHandItem();
		return held != null && !held.isEmpty() && held.is(Items.ENDER_PEARL);
	}

	private static boolean waitedAfterAction(int minimumTicks) {
		return stepElapsedTicks >= minimumTicks + 1;
	}

	private static void enterStep(SequenceStep nextStep) {
		SequenceStep previousStep = currentStep;
		currentStep = nextStep;
		stepElapsedTicks = 0;
		stepStarted = false;
		if (nextStep != previousStep) {
			sendDebug("Step -> " + nextStep.name() + ".");
		}
	}

	private static void resetSequence() {
		currentPlan = null;
		currentThrowAtMs = -1L;
		sequenceElapsedTicks = 0;
		enterStep(SequenceStep.IDLE);
	}

	private static void resetPickupTracking() {
		pickupTimerActive = false;
		pickupTimerStartServerMs = -1L;
		lastPickupTitleServerMs = -1L;
	}

	private static void resetCarryState() {
		pearlThrownForCurrentCarry = false;
		resetPickupTracking();
	}

	private static void resetRuntimeState() {
		resetCarryState();
		resetSequence();
	}

	private static void resetAllState() {
		for (SupplySpot spot : SupplySpot.values()) {
			SUPPLY_STATES.put(spot, SupplyState.UNKNOWN);
		}
		disabledTriggerLogged = false;
		clearBlockedReason();
		resetRuntimeState();
	}

	private static void renderOverlay(GuiGraphics graphics) {
		if (graphics == null || !isGuiEnabled()) {
			return;
		}
		if (moveModeActive) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null || client.getWindow() == null) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		DrawState drawState = createDrawState(client);
		Position position = getVisibleConfiguredPosition(drawState, screenWidth, screenHeight, true);
		drawLabel(graphics, drawState, position.x(), position.y());
	}

	private static DrawState getMoveDrawState() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return null;
		}
		return createDrawState(client);
	}

	private static Position getVisibleConfiguredPosition(DrawState drawState, int screenWidth, int screenHeight,
			boolean persistIfAdjusted) {
		if (drawState == null) {
			return new Position((int) Math.round(getConfiguredX()), (int) Math.round(getConfiguredY()));
		}

		int configuredX = (int) Math.round(getConfiguredX());
		int configuredY = (int) Math.round(getConfiguredY());
		Position clamped = clampPosition(drawState, screenWidth, screenHeight, configuredX, configuredY);
		if (persistIfAdjusted && (clamped.x() != configuredX || clamped.y() != configuredY)) {
			setConfiguredPosition(clamped.x(), clamped.y());
		}
		return clamped;
	}

	private static Position clampPosition(DrawState drawState, int screenWidth, int screenHeight, int x, int y) {
		if (drawState == null) {
			return new Position(x, y);
		}
		int minX = 0;
		int maxX = Math.max(0, screenWidth - drawState.drawWidth());
		int minY = 0;
		int maxY = Math.max(0, screenHeight - drawState.drawHeight());
		return new Position(
				Mth.clamp(x, minX, maxX),
				Mth.clamp(y, minY, maxY));
	}

	private static void drawLabel(GuiGraphics graphics, DrawState drawState, int x, int y) {
		if (graphics == null || drawState == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return;
		}
		graphics.drawString(client.font, drawState.text(), x, y, drawState.argbColor(), true);
	}

	private static DrawState createDrawState(Minecraft client) {
		int color = resolveHudColor();
		int argbColor = 0xFF000000 | (color & 0x00FFFFFF);
		int width = Math.max(1, client.font.width(HUD_TEXT));
		int height = Math.max(1, client.font.lineHeight);
		return new DrawState(HUD_TEXT, argbColor, width, height);
	}

	private static int resolveHudColor() {
		return runtimeEnabled ? ENABLED_COLOR : DISABLED_COLOR;
	}

	private static double getConfiguredX() {
		return resolveHudPosition(UiDefinitions.AUTO_PEARL_X.get(), HUD_DEFAULT_X);
	}

	private static double getConfiguredY() {
		return resolveHudPosition(UiDefinitions.AUTO_PEARL_Y.get(), HUD_DEFAULT_Y);
	}

	private static void setConfiguredPosition(double x, double y) {
		UiDefinitions.AUTO_PEARL_X.set(clampHudPosition(x));
		UiDefinitions.AUTO_PEARL_Y.set(clampHudPosition(y));
	}

	private static double resolveHudPosition(Double raw, double fallback) {
		double value = raw != null && Double.isFinite(raw) ? raw : fallback;
		return Mth.clamp(value, HUD_POSITION_MIN, HUD_POSITION_MAX);
	}

	private static double clampHudPosition(double value) {
		return Mth.clamp(value, HUD_POSITION_MIN, HUD_POSITION_MAX);
	}

	private static void sendMessage(String text) {
		FeatureChat.sendPrefixed("Auto Pearl", text);
	}

	private static void sendDebug(String text) {
		if (!isDebugEnabled()) {
			return;
		}
		sendMessage("[Debug] " + text);
	}

	private static boolean isDebugEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL_DEBUG.get());
	}

	private static boolean isGuiEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get());
	}

	private static String actionStatus(boolean success) {
		return success ? "ok" : "failed";
	}

	private static String formatVec(Vec3 vec) {
		if (vec == null) {
			return "(null)";
		}
		return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
	}

	private static Vec3 getPlayerEyePos(Minecraft client) {
		if (client == null || client.player == null) {
			return null;
		}
		return new Vec3(client.player.getX(), client.player.getEyeY(), client.player.getZ());
	}

	private static Vec3 getPearlSpawnPos(Minecraft client) {
		if (client == null || client.player == null) {
			return null;
		}
		return new Vec3(client.player.getX(), client.player.getY() + 1.6D, client.player.getZ());
	}

	private enum SequenceStep {
		IDLE,
		ROTATE,
		SWAP_TO_PEARL,
		WAIT_AFTER_SWAP,
		WAIT_FOR_THROW_WINDOW,
		THROW_PEARL
	}

	private enum SupplyState {
		UNKNOWN,
		NOTHING,
		RECEIVED,
		IN_PROGRESS,
		COMPLETED
	}

	private enum PickupSpot {
		SHOP(new Vec3(-81.0D, 76.0D, -143.0D)),
		X(new Vec3(-142.5D, 77.0D, -148.0D)),
		X_CANNON(new Vec3(-143.0D, 76.0D, -125.0D)),
		EQUALS(new Vec3(-65.5D, 76.0D, -87.5D)),
		SLASH(new Vec3(-113.5D, 77.0D, -68.5D)),
		TRIANGLE(new Vec3(-67.5D, 77.0D, -122.5D)),
		SQUARE(new Vec3(-143.0D, 76.0D, -80.0D)),
		NONE(new Vec3(0.0D, 0.0D, 0.0D));

		private final Vec3 location;

		PickupSpot(Vec3 location) {
			this.location = location;
		}

		private static PickupSpot closestTo(Vec3 eyePos) {
			if (eyePos == null) {
				return NONE;
			}

			PickupSpot best = NONE;
			double bestDistSq = Double.MAX_VALUE;
			for (PickupSpot spot : values()) {
				if (spot == NONE) {
					continue;
				}
				double distSq = distanceSquared(eyePos, spot.location);
				if (distSq < bestDistSq) {
					bestDistSq = distSq;
					best = spot;
				}
			}
			return best;
		}
	}

	private enum SupplySpot {
		SUPPLY1(new Vec3(-98.0D, 79.0D, -112.94D)),
		SUPPLY2(new Vec3(-106.0D, 79.0D, -112.94D)),
		SUPPLY3(new Vec3(-110.0D, 79.0D, -106.0D)),
		SUPPLY4(new Vec3(-106.0D, 79.0D, -99.06D)),
		SUPPLY5(new Vec3(-98.0D, 79.0D, -99.06D)),
		SUPPLY6(new Vec3(-94.0D, 79.0D, -106.0D));

		private final Vec3 location;
		private final int intX;
		private final int intZ;

		SupplySpot(Vec3 location) {
			this.location = location;
			this.intX = (int) location.x;
			this.intZ = (int) location.z;
		}

		private Vec3 location() {
			return location;
		}

		private boolean matchesEntityPosition(double x, double z) {
			return (int) x == intX && (int) z == intZ;
		}

		private static SupplySpot fromEntityPosition(double x, double z) {
			for (SupplySpot spot : values()) {
				if (spot.matchesEntityPosition(x, z)) {
					return spot;
				}
			}
			return null;
		}
	}

	private record ThrowPlan(SupplySpot target, Vec3 aimPoint, float yaw, float pitch, long flightTimeMs, boolean sky) {
	}

	private record PrimaryPlans(ThrowPlan sky, ThrowPlan flat) {
	}

	private record TimedThrowPlan(ThrowPlan plan, long timerRemainingMs) {
	}

	private record WaypointEvaluation(ThrowPlan plan, long throwAtMs, long timerRemainingMs, ThrowPlan skyPlan,
			ThrowPlan flatPlan) {
	}

	private record DrawState(String text, int argbColor, int drawWidth, int drawHeight) {
	}

	private record Position(int x, int y) {
	}

	private static final class TrajectorySolver {
		private static final double GRAVITY = 0.03D;
		private static final double DRAG = 0.99D;
		private static final double SPEED = 1.5D;
		private static final double TICK_MS = 50.0D;
		private static final int MAX_SIM_TICKS = 120;
		private static final int SOLVER_ITERATIONS = 20;
		private static final double MIN_THETA = Math.toRadians(0.5D);
		private static final double MAX_THETA = Math.toRadians(89.5D);
		private static final double MIN_SKY_THETA = Math.toRadians(41.0D);
		private static final double MAX_FLAT_THETA = Math.toRadians(40.0D);
		private static final double EPS = Math.toRadians(0.05D);
		private static final double ONE_MINUS_DRAG = 1.0D - DRAG;
		private static final double INV_ONE_MINUS_DRAG = 1.0D / ONE_MINUS_DRAG;
		private static final double LOG_DRAG = Math.log(DRAG);
		private static final double BASE_RADIUS = 0.5D;
		private static final double MAX_RADIUS = 2.0D;
		private static final double RADIUS_STEP = 0.25D;
		private static final int REFINE_ROUNDS = 6;
		private static final double REFINE_INIT_STEP = Math.toRadians(0.6D);
		private static final double COARSE_SCAN_STEP = Math.toRadians(0.5D);
		private static final double SWEEP_RANGE = Math.toRadians(2.0D);
		private static final double SWEEP_STEP_COARSE = Math.toRadians(0.2D);
		private static final double SWEEP_STEP_FINE = Math.toRadians(0.1D);
		private static final double Y_CAP_EPS = 1.0E-4D;
		private static final int SKY_DISTANCE = 30;
		private static final int FLAT_DISTANCE = 15;
		private static final double[] DRAG_POW = createDragPow();
		private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

		private TrajectorySolver() {
		}

		private static PearlSolution solvePearl(boolean sky, Vec3 start, Vec3 target) {
			double aimDistance = sky ? SKY_DISTANCE : FLAT_DISTANCE;
			double targetY = target.y + 0.5D;
			double targetX = target.x;
			double targetZ = target.z;
			double startX = start.x;
			double startY = start.y;
			double startZ = start.z;
			double deltaX = targetX - startX;
			double deltaZ = targetZ - startZ;
			double deltaY = targetY - startY;
			double horizontalDist = Math.hypot(deltaX, deltaZ);
			if (horizontalDist < 0.5D) {
				return null;
			}

			double invLength = 1.0D / horizontalDist;
			double ux = deltaX * invLength;
			double uz = deltaZ * invLength;
			double minTheta = sky ? MIN_SKY_THETA : MIN_THETA;
			double maxTheta = sky ? MAX_THETA : MAX_FLAT_THETA;
			double maxReachableTheta = maxReachableTheta(horizontalDist);
			if (Double.isNaN(maxReachableTheta)) {
				return null;
			}
			maxTheta = Math.min(maxTheta, maxReachableTheta - EPS);
			if (maxTheta <= minTheta) {
				return null;
			}

			Scratch scratch = SCRATCH.get();
			RefineResult best = null;
			for (double radius = BASE_RADIUS; radius <= MAX_RADIUS + 1.0E-9D; radius += RADIUS_STEP) {
				double radiusSq = radius * radius;
				boolean allowFineSweep = radius + RADIUS_STEP > MAX_RADIUS + 1.0E-9D;
				RefineResult attempt = solveWithBudgetAndSweepFallback(
						scratch,
						horizontalDist,
						deltaY,
						minTheta,
						maxTheta,
						startX,
						startY,
						startZ,
						targetX,
						targetZ,
						targetY,
						ux,
						uz,
						radiusSq,
						allowFineSweep);
				if (attempt != null && attempt.sim.hit) {
					best = attempt;
					break;
				}
			}
			if (best == null) {
				return null;
			}

			double vx = best.vx;
			double vy = best.vy;
			double vz = best.vz;
			double cos = best.cos;
			double flatSpeed = SPEED * cos;
			float yaw = (float) (Math.toDegrees(Math.atan2(vz, vx)) - 90.0D);
			float pitch = (float) -Math.toDegrees(Math.atan2(vy, flatSpeed));
			double scale = aimDistance / SPEED;
			Vec3 aimPoint = new Vec3(
					startX + (vx * scale),
					startY + (vy * scale),
					startZ + (vz * scale));

			return new PearlSolution(aimPoint, Math.round(best.sim.hitTick * TICK_MS), yaw, pitch);
		}

		private static RefineResult solveWithBudgetAndSweepFallback(Scratch scratch, double horizontalDist,
				double verticalDelta, double minTheta, double maxTheta, double startX, double startY, double startZ,
				double targetX, double targetZ, double targetY, double ux, double uz, double radiusSq,
				boolean allowFineSweep) {
			Double theta = solveTheta(horizontalDist, verticalDelta, minTheta, maxTheta);
			if (theta == null) {
				RefineResult coarseScan = globalCoarseScan(
						scratch,
						minTheta,
						maxTheta,
						horizontalDist,
						startX,
						startY,
						startZ,
						targetX,
						targetZ,
						targetY,
						ux,
						uz,
						radiusSq);
				if (coarseScan != null) {
					theta = coarseScan.theta;
					if (coarseScan.sim.hit) {
						return coarseScan;
					}
				} else {
					theta = 0.5D * (minTheta + maxTheta);
				}
			}

			RefineResult best = scratch.r0;
			evalTheta(best, scratch.sim0, theta, horizontalDist, startX, startY, startZ, targetX, targetZ, targetY, ux,
					uz, radiusSq);
			double step = REFINE_INIT_STEP;
			double bestTheta = theta;
			for (int round = 0; round < REFINE_ROUNDS; round++) {
				double lower = clamp(bestTheta - step, minTheta, maxTheta);
				double upper = clamp(bestTheta + step, minTheta, maxTheta);
				RefineResult lowerResult = scratch.r1;
				RefineResult upperResult = scratch.r2;
				evalTheta(lowerResult, scratch.sim1, lower, horizontalDist, startX, startY, startZ, targetX, targetZ,
						targetY, ux, uz, radiusSq);
				evalTheta(upperResult, scratch.sim2, upper, horizontalDist, startX, startY, startZ, targetX, targetZ,
						targetY, ux, uz, radiusSq);
				best = better(best, lowerResult);
				best = better(best, upperResult);
				bestTheta = best.theta;
				if (best.sim.hit && best.sim.hitXZDist2 <= 1.0E-6D) {
					return best;
				}
				step *= 0.5D;
			}
			if (best.sim.hit) {
				return best;
			}

			RefineResult coarseSweep = sweepAround(
					scratch,
					bestTheta,
					SWEEP_RANGE,
					SWEEP_STEP_COARSE,
					minTheta,
					maxTheta,
					horizontalDist,
					startX,
					startY,
					startZ,
					targetX,
					targetZ,
					targetY,
					ux,
					uz,
					radiusSq);
			best = better(best, coarseSweep);
			if (best.sim.hit) {
				return best;
			}
			if (allowFineSweep) {
				RefineResult fineSweep = sweepAround(
						scratch,
						bestTheta,
						SWEEP_RANGE,
						SWEEP_STEP_FINE,
						minTheta,
						maxTheta,
						horizontalDist,
						startX,
						startY,
						startZ,
						targetX,
						targetZ,
						targetY,
						ux,
						uz,
						radiusSq);
				best = better(best, fineSweep);
			}
			return best;
		}

		private static RefineResult sweepAround(Scratch scratch, double centerTheta, double sweepRange, double sweepStep,
				double minTheta, double maxTheta, double horizontalDist, double startX, double startY, double startZ,
				double targetX, double targetZ, double targetY, double ux, double uz, double radiusSq) {
			RefineResult best = null;
			int steps = (int) Math.ceil(sweepRange / sweepStep);
			for (int step = 0; step <= steps; step++) {
				double upperTheta = centerTheta + (step * sweepStep);
				if (upperTheta >= minTheta && upperTheta <= maxTheta) {
					RefineResult candidate = scratch.tmpPick();
					evalTheta(candidate, candidate.sim, upperTheta, horizontalDist, startX, startY, startZ, targetX,
							targetZ, targetY, ux, uz, radiusSq);
					best = better(best, candidate);
					if (best.sim.hit) {
						return best;
					}
				}
				if (step == 0) {
					continue;
				}

				double lowerTheta = centerTheta - (step * sweepStep);
				if (lowerTheta >= minTheta && lowerTheta <= maxTheta) {
					RefineResult candidate = scratch.tmpPick();
					evalTheta(candidate, candidate.sim, lowerTheta, horizontalDist, startX, startY, startZ, targetX,
							targetZ, targetY, ux, uz, radiusSq);
					best = better(best, candidate);
					if (best.sim.hit) {
						return best;
					}
				}
			}
			return best;
		}

		private static void evalTheta(RefineResult result, SimResult sim, double theta, double horizontalDist,
				double startX, double startY, double startZ, double targetX, double targetZ, double targetY, double ux,
				double uz, double radiusSq) {
			double cos = Math.cos(theta);
			double sin = Math.sin(theta);
			double flatSpeed = SPEED * cos;
			double vx = flatSpeed * ux;
			double vy = SPEED * sin;
			double vz = flatSpeed * uz;
			int tickCap = estimateTickCap(horizontalDist, flatSpeed);
			simulateTopCap(sim, startX, startY, startZ, vx, vy, vz, targetX, targetZ, targetY, radiusSq, tickCap);
			if (!sim.hit && sim.planeCrossTick < 0 && tickCap < MAX_SIM_TICKS) {
				simulateTopCap(sim, startX, startY, startZ, vx, vy, vz, targetX, targetZ, targetY, radiusSq,
						MAX_SIM_TICKS);
			}

			result.theta = theta;
			result.cos = cos;
			result.vx = vx;
			result.vy = vy;
			result.vz = vz;
			result.sim = sim;
		}

		private static void simulateTopCap(SimResult sim, double startX, double startY, double startZ, double vx,
				double vy, double vz, double targetX, double targetZ, double targetY, double radiusSq, int maxTicks) {
			if (maxTicks > MAX_SIM_TICKS) {
				maxTicks = MAX_SIM_TICKS;
			}
			if (maxTicks <= 0) {
				maxTicks = 1;
			}

			double prevX = startX;
			double prevY = startY;
			double prevZ = startZ;
			double bestCrossXZDist2 = Double.POSITIVE_INFINITY;
			int planeCrossTick = -1;

			for (int tick = 1; tick <= maxTicks; tick++) {
				double dragPow = DRAG_POW[tick];
				double dragDistanceFactor = 1.0D - dragPow;
				double x = startX + (vx * dragDistanceFactor * INV_ONE_MINUS_DRAG);
				double z = startZ + (vz * dragDistanceFactor * INV_ONE_MINUS_DRAG);
				double yVelocityComponent = vy * dragDistanceFactor * INV_ONE_MINUS_DRAG;
				double gravityComponent = GRAVITY * (tick - (dragDistanceFactor * INV_ONE_MINUS_DRAG))
						* INV_ONE_MINUS_DRAG;
				double y = startY + yVelocityComponent - gravityComponent;

				if (planeCrossTick < 0 && prevY > targetY && y <= targetY) {
					planeCrossTick = tick;
				}

				double segmentX = x - prevX;
				double segmentY = y - prevY;
				double segmentZ = z - prevZ;
				double segmentLengthSq = (segmentX * segmentX) + (segmentY * segmentY) + (segmentZ * segmentZ);
				if (segmentLengthSq > 1.0E-16D) {
					double offsetX = prevX - targetX;
					double offsetY = prevY - targetY;
					double offsetZ = prevZ - targetZ;
					double projection = -((offsetX * segmentX) + (offsetY * segmentY) + (offsetZ * segmentZ))
							/ segmentLengthSq;
					double clampedProjection = projection < 0.0D ? 0.0D : Math.min(1.0D, projection);

					double closestX = prevX + (segmentX * clampedProjection);
					double closestY = prevY + (segmentY * clampedProjection);
					double closestZ = prevZ + (segmentZ * clampedProjection);
					if (closestY >= targetY - Y_CAP_EPS) {
						double deltaClosestX = closestX - targetX;
						double deltaClosestY = closestY - targetY;
						double deltaClosestZ = closestZ - targetZ;
						double hitDistSq = (deltaClosestX * deltaClosestX) + (deltaClosestY * deltaClosestY)
								+ (deltaClosestZ * deltaClosestZ);

						double hitXZDeltaX = closestX - targetX;
						double hitXZDeltaZ = closestZ - targetZ;
						double hitXZDist2 = (hitXZDeltaX * hitXZDeltaX) + (hitXZDeltaZ * hitXZDeltaZ);
						if (hitXZDist2 < bestCrossXZDist2) {
							bestCrossXZDist2 = hitXZDist2;
						}

						if (radiusSq > 0.0D && hitDistSq <= radiusSq) {
							sim.hit = true;
							sim.hitTick = tick;
							sim.bestCrossXZDist2 = bestCrossXZDist2;
							sim.hitXZDist2 = hitXZDist2;
							sim.planeCrossTick = planeCrossTick;
							return;
						}
					}
				}

				prevX = x;
				prevY = y;
				prevZ = z;
			}

			sim.hit = false;
			sim.hitTick = -1;
			sim.bestCrossXZDist2 = bestCrossXZDist2;
			sim.hitXZDist2 = Double.POSITIVE_INFINITY;
			sim.planeCrossTick = planeCrossTick;
		}

		private static Double solveTheta(double horizontalDist, double verticalDelta, double minTheta, double maxTheta) {
			double lower = minTheta;
			double upper = maxTheta;
			Double lowerError = verticalError(lower, horizontalDist, verticalDelta);
			Double upperError = verticalError(upper, horizontalDist, verticalDelta);
			if (lowerError == null || upperError == null || (lowerError * upperError) > 0.0D) {
				return null;
			}

			for (int iteration = 0; iteration < SOLVER_ITERATIONS; iteration++) {
				double theta = 0.5D * (lower + upper);
				Double error = verticalError(theta, horizontalDist, verticalDelta);
				if (error == null) {
					return null;
				}
				if (Math.abs(upper - lower) < 1.0E-6D) {
					return theta;
				}
				if ((lowerError * error) <= 0.0D) {
					upper = theta;
				} else {
					lower = theta;
					lowerError = error;
				}
			}
			return 0.5D * (lower + upper);
		}

		private static double maxReachableTheta(double horizontalDist) {
			double ratio = (horizontalDist * ONE_MINUS_DRAG) / SPEED;
			if (ratio >= 1.0D) {
				return Double.NaN;
			}
			if (ratio <= 0.0D) {
				return MAX_THETA;
			}
			return Math.acos(ratio);
		}

		private static Double verticalError(double theta, double horizontalDist, double verticalDelta) {
			double cos = Math.cos(theta);
			if (cos < 1.0E-9D) {
				return null;
			}
			int flightTicks = computeFlightTicksHoriz(horizontalDist, SPEED * cos);
			if (flightTicks <= 0) {
				return null;
			}
			double displacement = verticalDisplacement(theta, flightTicks);
			return displacement - verticalDelta;
		}

		private static int computeFlightTicksHoriz(double horizontalDist, double horizontalVelocity) {
			double ratio = (horizontalDist * ONE_MINUS_DRAG) / horizontalVelocity;
			if (ratio <= 0.0D || ratio >= 1.0D) {
				return -1;
			}
			double rawTicks = Math.log1p(-ratio) / LOG_DRAG;
			int ticks = (int) Math.ceil(rawTicks - 1.0E-12D);
			return ticks > 0 ? ticks : -1;
		}

		private static double verticalDisplacement(double theta, int ticks) {
			if (ticks < 0) {
				return Double.NaN;
			}

			double verticalVelocity = SPEED * Math.sin(theta);
			double dragPow = ticks <= MAX_SIM_TICKS ? DRAG_POW[ticks] : Math.pow(DRAG, ticks);
			double dragDistanceFactor = 1.0D - dragPow;
			double velocityComponent = verticalVelocity * dragDistanceFactor * INV_ONE_MINUS_DRAG;
			double gravityComponent = GRAVITY * (ticks - (dragDistanceFactor * INV_ONE_MINUS_DRAG))
					* INV_ONE_MINUS_DRAG;
			return velocityComponent - gravityComponent;
		}

		private static int estimateTickCap(double horizontalDist, double horizontalVelocity) {
			int flightTicks = computeFlightTicksHoriz(horizontalDist, horizontalVelocity);
			if (flightTicks <= 0) {
				return MAX_SIM_TICKS;
			}
			int tickCap = flightTicks + 20;
			if (tickCap < 40) {
				tickCap = 40;
			}
			return Math.min(tickCap, MAX_SIM_TICKS);
		}

		private static RefineResult better(RefineResult current, RefineResult candidate) {
			if (candidate == null) {
				return current;
			}
			if (current == null) {
				return candidate;
			}

			boolean currentHit = current.sim.hit;
			boolean candidateHit = candidate.sim.hit;
			if (currentHit != candidateHit) {
				return currentHit ? current : candidate;
			}
			if (currentHit) {
				int xzCompare = Double.compare(current.sim.hitXZDist2, candidate.sim.hitXZDist2);
				if (xzCompare != 0) {
					return xzCompare < 0 ? current : candidate;
				}
				if (current.sim.hitTick > 0
						&& candidate.sim.hitTick > 0
						&& current.sim.hitTick != candidate.sim.hitTick) {
					return current.sim.hitTick < candidate.sim.hitTick ? current : candidate;
				}
				return current;
			}
			return Double.compare(current.sim.bestCrossXZDist2, candidate.sim.bestCrossXZDist2) <= 0 ? current
					: candidate;
		}

		private static RefineResult globalCoarseScan(Scratch scratch, double minTheta, double maxTheta,
				double horizontalDist, double startX, double startY, double startZ, double targetX, double targetZ,
				double targetY, double ux, double uz, double radiusSq) {
			RefineResult best = null;
			for (double theta = minTheta; theta <= maxTheta + 1.0E-12D; theta += COARSE_SCAN_STEP) {
				RefineResult candidate = scratch.tmpPick();
				evalTheta(candidate, candidate.sim, theta, horizontalDist, startX, startY, startZ, targetX, targetZ,
						targetY, ux, uz, radiusSq);
				best = better(best, candidate);
				if (best != null && best.sim.hit) {
					return best;
				}
			}
			return best;
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

		private static double[] createDragPow() {
			double[] values = new double[MAX_SIM_TICKS + 1];
			values[0] = 1.0D;
			for (int i = 1; i <= MAX_SIM_TICKS; i++) {
				values[i] = values[i - 1] * DRAG;
			}
			return values;
		}

		private static final class Scratch {
			private final SimResult sim0 = new SimResult();
			private final SimResult sim1 = new SimResult();
			private final SimResult sim2 = new SimResult();
			private final SimResult sim3 = new SimResult();
			private final SimResult sim4 = new SimResult();
			private final RefineResult r0 = new RefineResult(sim0);
			private final RefineResult r1 = new RefineResult(sim1);
			private final RefineResult r2 = new RefineResult(sim2);
			private final RefineResult r3 = new RefineResult(sim3);
			private final RefineResult r4 = new RefineResult(sim4);
			private int pick;

			private RefineResult tmpPick() {
				pick ^= 1;
				return pick == 0 ? r3 : r4;
			}
		}

		private static final class RefineResult {
			private double theta;
			private double cos;
			private double vx;
			private double vy;
			private double vz;
			private SimResult sim;

			private RefineResult(SimResult sim) {
				this.sim = sim;
			}
		}

		private static final class SimResult {
			private boolean hit;
			private int hitTick;
			private double bestCrossXZDist2;
			private double hitXZDist2;
			private int planeCrossTick;
		}

		private record PearlSolution(Vec3 solution, long flightTimeMs, float yaw, float pitch) {
		}
	}
}
