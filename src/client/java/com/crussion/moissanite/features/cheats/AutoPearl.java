package com.crussion.moissanite.features.cheats;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class AutoPearl {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String PEARL_ID = "ENDER_PEARL";
	private static final double ROTATION_MULTIPLIER = 0.4D;
	private static final int WAIT_AFTER_SWAP_TICKS = 2;
	private static final int ROTATE_TIMEOUT_TICKS = 120;
	private static final int SEQUENCE_TIMEOUT_TICKS = 200;
	private static final long SEQUENCE_START_LEAD_MS = 1500L;
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
	private static final Pattern PICKUP_PROGRESS_PATTERN = Pattern.compile("(\\d{1,3})%");
	private static final Pattern KUUDRA_TIER_PATTERN = Pattern.compile("\\bt([1-5])\\b");

	private static final String SUPPLY_READY_MARKER = "bring supply chest here";
	private static final String SUPPLY_RECEIVED_MARKER = "supplies received";
	private static final String SUPPLY_PROGRESS_MARKER = "progress:";
	private static final String SUPPLY_COMPLETE_MARKER = "complete";

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
	private static boolean pearlThrownForCurrentCarry;
	private static boolean disabledTriggerLogged;
	private static boolean pickupTimerActive;
	private static long pickupTimerStartMs = -1L;
	private static long pickupTimerStartServerMs = -1L;
	private static int pickupTimerDelayMs;
	private static int lastPickupProgressPercent = -1;
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
		if (normalized.isBlank() || !normalized.contains("|") || !normalized.contains("%")) {
			return;
		}

		Matcher matcher = PICKUP_PROGRESS_PATTERN.matcher(normalized);
		if (!matcher.find()) {
			return;
		}

		int percent;
		try {
			percent = Mth.clamp(Integer.parseInt(matcher.group(1)), 0, 100);
		} catch (NumberFormatException ex) {
			return;
		}

		if (percent == 0) {
			if (!pickupTimerActive || lastPickupProgressPercent > 0 || pearlThrownForCurrentCarry) {
				startPickupTimer();
			}
			lastPickupProgressPercent = 0;
			return;
		}
		lastPickupProgressPercent = percent;
	}

	private static void toggleEnabled() {
		boolean next = !Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get());
		UiDefinitions.AUTO_PEARL.set(next);
		if (!next) {
			resetRuntimeState();
		}
		sendMessage(next ? "Enabled." : "Disabled.");
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			reportBlocked("Waiting for world and player.");
			disabledTriggerLogged = false;
			resetRuntimeState();
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get())) {
			reportBlocked("Auto Pearl is disabled.");
			handleDisabledTrigger(client);
			resetSequence();
			return;
		}

		disabledTriggerLogged = false;

		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			reportBlocked("Not in Kuudra's Hollow.");
			resetRuntimeState();
			return;
		}
		if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_SUPPLY) {
			reportBlocked("Not in supply phase (phase " + KuudraPhaseTracker.getPhase() + ").");
			resetRuntimeState();
			return;
		}

		updateSupplyStates(client);

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

	private static void startPickupTimer() {
		pickupTimerDelayMs = resolvePickupDelayMs();
		pickupTimerStartMs = nowMs();
		pickupTimerStartServerMs = serverNowMs();
		pickupTimerActive = true;
		pearlThrownForCurrentCarry = false;
		int talismanTier = configuredTalismanTier();
		int kuudraTier = configuredKuudraTier();
		sendDebug("Pickup timer started at 0% -> " + pickupTimerDelayMs + "ms (talisman T" + talismanTier + ", kuudra T"
				+ kuudraTier + ").");
	}

	private static WaypointEvaluation evaluatePrimaryWaypoint(Minecraft client) {
		if (!pickupTimerActive || pickupTimerStartMs < 0L) {
			return null;
		}
		updateSupplyStates(client);
		ThrowPlan plan = buildThrowPlan(client);
		if (plan == null) {
			return null;
		}

		long timerRemainingMs = timeUntilThrowMs(plan.flightTimeMs());
		long throwAtMs = nowMs() + timerRemainingMs;
		return new WaypointEvaluation(plan, throwAtMs, timerRemainingMs);
	}

	private static long nowMs() {
		return System.currentTimeMillis();
	}

	private static long serverNowMs() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.level != null) {
			return client.level.getGameTime() * 50L;
		}
		return nowMs();
	}

	private static long timeUntilThrowMs(long flightTimeMs) {
		if (!pickupTimerActive || pickupTimerStartMs < 0L) {
			return Long.MAX_VALUE;
		}
		long elapsedSincePickupStart = elapsedSincePickupStartMs();
		return pickupTimerDelayMs - flightTimeMs - elapsedSincePickupStart;
	}

	private static long elapsedSincePickupStartMs() {
		if (pickupTimerStartMs < 0L) {
			return 0L;
		}
		long elapsedWallMs = nowMs() - pickupTimerStartMs;
		long elapsedServerMs = -1L;
		if (pickupTimerStartServerMs >= 0L) {
			elapsedServerMs = serverNowMs() - pickupTimerStartServerMs;
		}
		long elapsedMs = elapsedServerMs >= 0L ? Math.max(elapsedWallMs, elapsedServerMs) : elapsedWallMs;
		return Math.max(0L, elapsedMs);
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
		return sliderToInt(UiDefinitions.AUTO_PEARL_KUUDRA_TIER.get(), KUUDRA_TIER_MAX, KUUDRA_TIER_MIN, KUUDRA_TIER_MAX);
	}

	private static int sliderToInt(Double value, int fallback, int min, int max) {
		double raw = value != null && Double.isFinite(value) ? value : fallback;
		return Mth.clamp((int) Math.round(raw), min, max);
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

	private static ThrowPlan buildThrowPlan(Minecraft client) {
		if (client == null || client.player == null) {
			return null;
		}

		Vec3 eyePos = new Vec3(client.player.getX(), client.player.getEyeY(), client.player.getZ());
		SupplySpot targetSpot = resolveTargetSupply(eyePos);
		if (targetSpot == null) {
			return null;
		}

		TrajectorySolver.PearlSolution solution = TrajectorySolver.solvePearl(true, eyePos, targetSpot.location());
		if (solution == null || solution.solution() == null) {
			return null;
		}
		return new ThrowPlan(targetSpot, solution.solution(), solution.flightTimeMs());
	}

	private static SupplySpot resolveTargetSupply(Vec3 eyePos) {
		PickupSpot pickup = PickupSpot.closestTo(eyePos);
		SupplySpot mapped = mapPickupToSupply(pickup);
		if (mapped != null && isSupplyAvailable(mapped)) {
			return mapped;
		}

		SupplySpot preSpotSupply = resolvePreSpotSupply();
		if (preSpotSupply != null && isSupplyAvailable(preSpotSupply)) {
			return preSpotSupply;
		}

		return getClosestAvailableSupply(eyePos);
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

	private static SupplySpot getClosestAvailableSupply(Vec3 eyePos) {
		SupplySpot best = null;
		double bestDistSq = Double.MAX_VALUE;

		for (SupplySpot spot : SupplySpot.values()) {
			if (!isSupplyAvailable(spot)) {
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
		SupplyState state = SUPPLY_STATES.getOrDefault(spot, SupplyState.UNKNOWN);
		return state == SupplyState.NOTHING || state == SupplyState.UNKNOWN;
	}

	private static void startSequence(ThrowPlan plan, long throwAtMs) {
		currentPlan = plan;
		currentThrowAtMs = throwAtMs;
		sequenceElapsedTicks = 0;
		sendDebug("Sequence start -> target " + plan.target().name() + ", aim " + formatVec(plan.aimPoint())
				+ ", flight " + plan.flightTimeMs() + "ms.");
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
			sendDebug("ROTATE: aiming at " + formatVec(currentPlan.aimPoint()) + ".");
			boolean started = RotationController.rotateTo(
					currentPlan.aimPoint().x,
					currentPlan.aimPoint().y,
					currentPlan.aimPoint().z,
					ROTATION_MULTIPLIER);
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
		if (timeUntilThrowMs(currentPlan.flightTimeMs()) > 0L) {
			return;
		}

		sendDebug("WAIT_FOR_THROW_WINDOW: throw time reached.");
		enterStep(SequenceStep.THROW_PEARL);
		handleThrowPearl();
	}

	private static void handleThrowPearl() {
		if (!stepStarted) {
			stepStarted = true;
			sendDebug("THROW_PEARL: right click.");
			boolean thrown = PlayerInputActions.rightClick();
			sendDebug("THROW_PEARL: right click " + actionStatus(thrown) + ".");
			if (!thrown) {
				sendMessage("Failed to throw pearl.");
				resetSequence();
				return;
			}
			pearlThrownForCurrentCarry = true;
			pickupTimerActive = false;
			pickupTimerStartMs = -1L;
			pickupTimerStartServerMs = -1L;
			pickupTimerDelayMs = 0;
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

	private static void resetCarryState() {
		pearlThrownForCurrentCarry = false;
		pickupTimerActive = false;
		pickupTimerStartMs = -1L;
		pickupTimerStartServerMs = -1L;
		pickupTimerDelayMs = 0;
		lastPickupProgressPercent = -1;
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
		if (graphics == null) {
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
		return Boolean.TRUE.equals(UiDefinitions.AUTO_PEARL.get()) ? ENABLED_COLOR : DISABLED_COLOR;
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

	private static String actionStatus(boolean success) {
		return success ? "ok" : "failed";
	}

	private static String formatVec(Vec3 vec) {
		if (vec == null) {
			return "(null)";
		}
		return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
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

	private record ThrowPlan(SupplySpot target, Vec3 aimPoint, long flightTimeMs) {
	}

	private record WaypointEvaluation(ThrowPlan plan, long throwAtMs, long timerRemainingMs) {
	}

	private record DrawState(String text, int argbColor, int drawWidth, int drawHeight) {
	}

	private record Position(int x, int y) {
	}

	private static final class TrajectorySolver {
		private static final double GRAVITY = 0.03D;
		private static final double SPEED = 1.5D;
		private static final double DRAG = 0.99D;
		private static final int MAX_TICKS = 100;
		private static final int REFINE_STEPS = 100;
		private static final int REFINE_ITERATIONS = 50;
		private static final int GRID_STEPS = 100;
		private static final double HIT_RADIUS_SQ = 0.7D * 0.7D;
		private static final double TOLERANCE_SQ = 0.1D * 0.1D;
		private static final int SKY_DISTANCE = 30;
		private static final int FLAT_DISTANCE = 15;
		private static final double MIN_THETA = 0.01D;
		private static final double MAX_THETA = (Math.PI / 2.0D) - 0.01D;
		private static final double INITIAL_REFINE_DEG = 1.0D;
		private static final double FLAT_ANGLE_BIAS_DEG = 0.4D;
		private static final double MIN_SKY_THETA = Math.toRadians(34.0D);
		private static final double MAX_FLAT_THETA = Math.toRadians(32.0D);
		private static final double TICK_MS = 50.0D;

		private TrajectorySolver() {
		}

		private static PearlSolution solvePearl(boolean sky, Vec3 start, Vec3 target) {
			double dx = target.x - start.x;
			double dz = target.z - start.z;
			double horizontalDist = Math.hypot(dx, dz);

			if (horizontalDist < 1.0D) {
				if (!sky) {
					return null;
				}
				return new PearlSolution(new Vec3(start.x, start.y + SKY_DISTANCE, start.z), 4500L, 0.0F, -90.0F);
			}

			double invLength = 1.0D / horizontalDist;
			double ux = dx * invLength;
			double uz = dz * invLength;

			SearchResult best = searchInitialBestAngle(start, target, ux, uz, sky);
			if (best == null) {
				return null;
			}

			double refineRange = Math.toRadians(INITIAL_REFINE_DEG);
			for (int iter = 0; iter < REFINE_ITERATIONS; iter++) {
				double lower = Math.max(MIN_THETA, best.theta - refineRange);
				double upper = Math.min(MAX_THETA, best.theta + refineRange);

				SearchResult refined = searchRefinedBestAngle(lower, upper, start, target, ux, uz, sky);
				if (refined == null || refined.errorSq >= best.errorSq) {
					break;
				}

				best = refined;
				if (best.errorSq < TOLERANCE_SQ) {
					break;
				}
				refineRange *= 0.5D;
			}

			double finalTheta = best.theta;
			int finalTick = best.tick;
			if (!sky) {
				finalTheta = Math.min(MAX_FLAT_THETA,
						Math.max(MIN_THETA, finalTheta + Math.toRadians(FLAT_ANGLE_BIAS_DEG)));
			}

			Vec3 velocity = computeVelocity(finalTheta, ux, uz);
			if (!sky) {
				SimResult finalSim = simulateTrajectory(start, velocity, target);
				if (finalSim.hit && finalSim.hitTick >= 0) {
					finalTick = finalSim.hitTick;
				}
			}

			Vec3 aimPoint = computeAimPoint(start, velocity, sky ? SKY_DISTANCE : FLAT_DISTANCE);
			long flightTimeMs = Math.round(finalTick * TICK_MS);
			double flatSpeed = Math.hypot(velocity.x, velocity.z);
			float yaw = (float) (Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90.0D);
			float pitch = (float) -Math.toDegrees(Math.atan2(velocity.y, flatSpeed));

			return new PearlSolution(aimPoint, flightTimeMs, yaw, pitch);
		}

		private static SearchResult searchRefinedBestAngle(double minTheta, double maxTheta, Vec3 start, Vec3 target,
				double ux, double uz, boolean sky) {
			SearchResult best = null;
			double clampedMin = Math.max(minTheta, sky ? MIN_SKY_THETA : MIN_THETA);
			double clampedMax = Math.min(maxTheta, sky ? MAX_THETA : MAX_FLAT_THETA);

			for (int i = 0; i <= REFINE_STEPS; i++) {
				double theta = clampedMin + ((clampedMax - clampedMin) * i / REFINE_STEPS);
				Vec3 velocity = computeVelocity(theta, ux, uz);
				SimResult sim = simulateTrajectory(start, velocity, target);
				if (!sim.hit) {
					continue;
				}

				SearchResult current = new SearchResult(theta, sim.errorSq, sim.hitTick);
				if (best == null || isBetter(current, best, sky)) {
					best = current;
				}
			}
			return best;
		}

		private static SearchResult searchInitialBestAngle(Vec3 start, Vec3 target, double ux, double uz, boolean sky) {
			double minTheta = sky ? MIN_SKY_THETA : MIN_THETA;
			double maxTheta = sky ? MAX_THETA : MAX_FLAT_THETA;
			SearchResult[] best = new SearchResult[1];

			IntStream.rangeClosed(0, GRID_STEPS).parallel().forEach(i -> {
				double theta = minTheta + ((maxTheta - minTheta) * i / GRID_STEPS);
				Vec3 velocity = computeVelocity(theta, ux, uz);
				SimResult sim = simulateTrajectory(start, velocity, target);
				if (!sim.hit) {
					return;
				}

				SearchResult candidate = new SearchResult(theta, sim.errorSq, sim.hitTick);
				synchronized (best) {
					if (best[0] == null || isBetter(candidate, best[0], sky)) {
						best[0] = candidate;
					}
				}
			});

			return best[0];
		}

		private static boolean isBetter(SearchResult candidate, SearchResult current, boolean sky) {
			if (candidate.errorSq < current.errorSq) {
				return true;
			}
			if (candidate.errorSq > current.errorSq) {
				return false;
			}
			return sky ? candidate.theta > current.theta : candidate.theta < current.theta;
		}

		private static SimResult simulateTrajectory(Vec3 start, Vec3 initialVelocity, Vec3 target) {
			double x = start.x;
			double y = start.y;
			double z = start.z;
			double vx = initialVelocity.x;
			double vy = initialVelocity.y;
			double vz = initialVelocity.z;

			double sx = start.x;
			double sy = start.y;
			double sz = start.z;
			double tx = target.x;
			double ty = target.y;
			double tz = target.z;

			double dx = tx - x;
			double dy = ty - y;
			double dz = tz - z;
			double bestErrorSq = (dx * dx) + (dy * dy) + (dz * dz);
			int bestTick = -1;

			double maxDistanceSq = bestErrorSq * 4.0D;
			double minY = ty - 5.0D;

			for (int tick = 0; tick < MAX_TICKS; tick++) {
				x += vx;
				y += vy;
				z += vz;

				dx = tx - x;
				dy = ty - y;
				dz = tz - z;
				double errorSq = (dx * dx) + (dy * dy) + (dz * dz);
				if (errorSq < bestErrorSq) {
					bestErrorSq = errorSq;
					bestTick = tick;
				}
				if (errorSq < HIT_RADIUS_SQ) {
					return new SimResult(true, errorSq, tick);
				}

				double sxDiff = x - sx;
				double syDiff = y - sy;
				double szDiff = z - sz;
				double distSqFromStart = (sxDiff * sxDiff) + (syDiff * syDiff) + (szDiff * szDiff);
				if (y < minY || distSqFromStart > maxDistanceSq) {
					break;
				}

				vx *= DRAG;
				vy = (vy - GRAVITY) * DRAG;
				vz *= DRAG;
			}

			return new SimResult(false, bestErrorSq, bestTick);
		}

		private static Vec3 computeVelocity(double theta, double ux, double uz) {
			double cos = Math.cos(theta);
			double sin = Math.sin(theta);
			return new Vec3(SPEED * cos * ux, SPEED * sin, SPEED * cos * uz);
		}

		private static Vec3 computeAimPoint(Vec3 start, Vec3 velocity, double distance) {
			double norm = Math.sqrt((velocity.x * velocity.x) + (velocity.y * velocity.y) + (velocity.z * velocity.z));
			if (norm < 1.0e-8D) {
				return start;
			}
			double invNorm = 1.0D / norm;
			return new Vec3(
					start.x + (velocity.x * invNorm * distance),
					start.y + (velocity.y * invNorm * distance),
					start.z + (velocity.z * invNorm * distance));
		}

		private record PearlSolution(Vec3 solution, long flightTimeMs, float yaw, float pitch) {
		}

		private record SimResult(boolean hit, double errorSq, int hitTick) {
		}

		private record SearchResult(double theta, double errorSq, int tick) {
		}
	}
}
