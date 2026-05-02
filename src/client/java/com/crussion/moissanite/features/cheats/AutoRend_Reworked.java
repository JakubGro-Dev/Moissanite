package com.crussion.moissanite.features.cheats;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.input.FakeKeybinds;
import com.crussion.moissanite.mixin.client.RenderTypeAccessor;
import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.HeldItemMatcher;
import com.crussion.moissanite.util.inventory.HotbarItemSearch;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.kuudra.KuudraTriggerArea;
import com.crussion.moissanite.util.render.WorldTextRenderer;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AutoRend_Reworked {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final int SEQUENCE_TIMEOUT_TICKS = 220;
	private static final int WORLD_LOAD_RESET_WINDOW_TICKS = 80;
	private static final int AUTO_REND_TRIGGER_COLOR = 0xFF29D6FF;
	private static final float AUTO_REND_TRIGGER_LINE_WIDTH = 2.5f;
	private static final Identifier AUTO_REND_RESULT_HUD_ID = Identifier.fromNamespaceAndPath(
			"moissanite",
			"auto_rend_reworked_result");
	private static final int REND_MIN_DIFF = 1_666;
	private static final int REND_DAMAGE_LOW_DIFF = 4_166;
	private static final int REND_DAMAGE_MED_DIFF = 7_291;
	private static final int REND_RESULT_BAD_COLOR = 0xFFFF5555;
	private static final int REND_RESULT_MID_COLOR = 0xFFFFFF55;
	private static final int REND_RESULT_HIGH_COLOR = 0xFF55FF55;
	private static final int REND_RESULT_HUD_DURATION_TICKS = 80;
	// Kuudra HP updates can land a few ticks after the pull finishes.
	private static final int REND_RESULT_FINISH_GRACE_TICKS = 8;
	private static final float REND_RESULT_HUD_SCALE = 3.5f;
	private static final int REND_RESULT_HUD_Y = 450;
	private static final double REND_RESULT_DISPLAY_MULTIPLIER = 1D;

	private static final int SWAP_MIN_WAIT_TICKS = 1;
	private static final int CLICK_MIN_WAIT_TICKS = 1;
	private static final int OTHER_MIN_WAIT_TICKS = 2;
	private static final int ARMOR_SWAP_TIMEOUT_TICKS = 120;
	private static final int BACKBONE_READY_DELAY_TICKS = 2;
	private static final int BONE_TRACK_EXPECT_WINDOW_TICKS = 12;
	private static final double BONE_TRACK_MAX_CANDIDATE_DIST_SQ = 16.0D;
	private static final double BONE_TRACK_OWNER_PROMOTE_DIST_HARD_SQ = 36.0D;
	private static final int BONE_TRACK_CANDIDATE_MAX_AGE_TICKS = 40;
	private static final double BONE_TRACK_FWD_RAY_DOT_MIN = 0.85D;
	private static final int BONE_TRACK_MAX_AGE_TICKS = 220;
	private static final double BONE_TRACK_MARKER_HEAD_Y_BIG = 1.78D;
	private static final double BONE_TRACK_MARKER_HEAD_Y_SMALL = 0.9D;
	private static final double BONE_TRACK_HEAD_HALF_XZ = 0.75D;
	private static final double BONE_TRACK_HEAD_HALF_Y = 0.15D;
	private static final double BONE_TRACK_FALLBACK_OUTBOUND_DOT_MIN = 0.02D;
	private static final double BONE_TRACK_FALLBACK_RETURNING_DOT_MAX = -0.02D;

	private static final String BONEMERANG_ID = "STARRED_BONE_BOOMERANG";
	private static final String ATOMSPLIT_ID = "ATOMSPLIT_KATANA";
	private static final String ENDSTONE_ID = "END_STONE_SWORD";
	private static final String PEARL_ID = "ENDER_PEARL";
	private static final String TERMINATOR_ID = "TERMINATOR";
	private static final String HYPERION_ID = "HYPERION";
	private static final String BAD_ITEMS_REND_RESULT_TEXT = "BAD ITEMS";
	private static final String BAD_ITEMS_MESSAGE = "Items are in bad slots.";

	private static final double PEARL_TARGET_X = -101.0D;
	private static final double PEARL_TARGET_Y = 6.0D;
	private static final double PEARL_TARGET_Z = -107.0D;

	private static boolean initialized;
	private static boolean sequenceRunning;
	private static boolean sequenceUsedThisWorld;
	private static int worldLoadResetTicksRemaining;

	private static SequenceStep currentStep = SequenceStep.IDLE;
	private static int sequenceElapsedTicks;
	private static int stepElapsedTicks;
	private static boolean stepStarted;

	private static boolean armorSwapEnabled;
	private static boolean armorSwapRequested;
	private static boolean armorSwapResolved;
	private static boolean armorSwapSucceeded;
	private static boolean armorSwapOutcomeLogged;
	private static boolean rendResultWindowActive;
	private static int rendResultGraceTicks = -1;
	private static int rendLastKuudraHp = -1;
	private static String rendResultText = "";
	private static int rendResultColor = REND_RESULT_BAD_COLOR;
	private static int rendResultHudTicks;
	private static int sequenceSessionId;
	private static boolean armorSwapStarted;
	private static boolean boneTrackingActive;
	private static long boneExpectWindowEndTick = -1L;
	private static long backboneReadyGameTime = -1L;
	private static Vec3 boneThrowOrigin;
	private static Vec3 boneThrowForward;
	private static Vec3 boneThrowPlayerPos;
	private static int currentBoneThrowSeq;
	private static TrackedBoneStand trackedBoneStand;
	private static final Map<Integer, CandidateBoneStand> trackedBoneCandidates = new HashMap<>();
	private static final Set<Integer> seenBoneStandIds = new HashSet<>();
	private static RenderType autoRendTriggerRenderType;
	private static int pullTickCounter = -1;

	private AutoRend_Reworked() {
	}

	public static int getTicksSincePull() {
		return pullTickCounter;
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			worldLoadResetTicksRemaining = WORLD_LOAD_RESET_WINDOW_TICKS;
			tryResetForKuudraWorldLoad();
			resetRendResultState();
			resetBackboneTracking();
		});

		FakeKeybinds.onKeyPress(UiDefinitions.AUTO_REND_DEBUG_TRIGGER_KEYBIND, AutoRend_Reworked::triggerDebugSequence);
		ClientTickEvents.END_CLIENT_TICK.register(AutoRend_Reworked::handleClientTick);
		WorldRenderEvents.END_MAIN.register(AutoRend_Reworked::renderActivationZones);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				AUTO_REND_RESULT_HUD_ID,
				AutoRend_Reworked::renderRendResultOverlay);
	}

	private static void handleClientTick(Minecraft client) {
		tickRendResultState(client);
		if (pullTickCounter >= 0) {
			pullTickCounter++;
		}
		if (worldLoadResetTicksRemaining > 0) {
			worldLoadResetTicksRemaining--;
			tryResetForKuudraWorldLoad();
		}

		if (client == null || client.player == null || client.level == null) {
			resetSequence();
			return;
		}
		tickBackboneTracking(client);

		if (sequenceRunning) {
			tickSequence();
			return;
		}

		if (sequenceUsedThisWorld) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND.get())) {
			return;
		}
		if (!hasAnyConfiguredItemSlot()) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		if (!isInDpsPhase()) {
			return;
		}
		if (!KuudraTriggerArea.isPlayerInTriggerArea()) {
			return;
		}

		startSequence(false);
	}

	public static void triggerDebugSequence() {
		if (!Boolean.TRUE.equals(UiDefinitions.DEBUG.get()) || !Boolean.TRUE.equals(UiDefinitions.AUTO_REND_DEBUG.get())) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (sequenceRunning) {
			return;
		}
		startSequence(true);
	}

	private static boolean isInDpsPhase() {
		return KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.PHASE_DPS;
	}

	private static void renderActivationZones(WorldRenderContext context) {
		if (context == null || context.matrices() == null || context.consumers() == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (!canRenderActivationZones(client)) {
			return;
		}

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		for (AABB box : KuudraTriggerArea.getTriggerAabbs()) {
			var lineBuffer = context.consumers().getBuffer(getAutoRendTriggerRenderType());
			ShapeRenderer.renderShape(
					context.matrices(),
					lineBuffer,
					Shapes.create(box),
					-cameraPos.x,
					-cameraPos.y,
					-cameraPos.z,
					AUTO_REND_TRIGGER_COLOR,
					AUTO_REND_TRIGGER_LINE_WIDTH);
			renderActivationCoords(context, box);
		}
	}

	private static boolean canRenderActivationZones(Minecraft client) {
		if (client == null || client.player == null || client.level == null || client.gameRenderer == null) {
			return false;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND.get())) {
			return false;
		}
		if (sequenceRunning || sequenceUsedThisWorld) {
			return false;
		}
		if (!hasAnyConfiguredItemSlot()) {
			return false;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return false;
		}
		return isInDpsPhase();
	}

	private static void renderActivationCoords(WorldRenderContext context, AABB box) {
		Vec3 center = box.getCenter();
		String label = formatCoordLabel(center);
		Vec3 labelPos = new Vec3(center.x, box.maxY + 0.35D, center.z);
		WorldTextRenderer.drawText(context, labelPos, label, AUTO_REND_TRIGGER_COLOR, 0.8f, true);
	}

	private static String formatCoordLabel(Vec3 center) {
		return String.format(Locale.ROOT, "%.1f %.1f %.1f", center.x, center.y, center.z);
	}

	private static RenderType getAutoRendTriggerRenderType() {
		if (autoRendTriggerRenderType == null) {
			autoRendTriggerRenderType = createAutoRendTriggerRenderType();
		}
		return autoRendTriggerRenderType;
	}

	private static RenderType createAutoRendTriggerRenderType() {
		RenderPipeline source = RenderPipelines.LINES;
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation("moissanite/auto_rend_trigger_lines")
				.withVertexShader(source.getVertexShader())
				.withFragmentShader(source.getFragmentShader())
				.withCull(source.isCull())
				.withDepthWrite(false)
				.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
				.withVertexFormat(source.getVertexFormat(), source.getVertexFormatMode());

		source.getBlendFunction().ifPresent(builder::withBlend);
		for (String sampler : source.getSamplers()) {
			builder.withSampler(sampler);
		}
		for (RenderPipeline.UniformDescription uniform : source.getUniforms()) {
			if (uniform.type() == UniformType.TEXEL_BUFFER) {
				builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
				continue;
			}
			builder.withUniform(uniform.name(), uniform.type());
		}
		source.getShaderDefines().flags().forEach(builder::withShaderDefine);
		source.getShaderDefines().values().forEach((key, value) -> applyNumericShaderDefine(builder, key, value));

		RenderPipeline pipeline = RenderPipelines.register(builder.build());
		RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
		return RenderType.create("moissanite_auto_rend_trigger_lines", setup);
	}

	private static void applyNumericShaderDefine(RenderPipeline.Builder builder, String key, String value) {
		if (builder == null || key == null || key.isBlank() || value == null || value.isBlank()) {
			return;
		}
		try {
			builder.withShaderDefine(key, Integer.parseInt(value));
			return;
		} catch (NumberFormatException ignored) {
		}
		try {
			builder.withShaderDefine(key, Float.parseFloat(value));
		} catch (NumberFormatException ignored) {
		}
	}

	private static void startSequence(boolean bypassWorldUseCheck) {
		if (sequenceRunning) {
			return;
		}
		if (!bypassWorldUseCheck && sequenceUsedThisWorld) {
			return;
		}
		if (!requiredItemsAreInConfiguredSlots()) {
			sequenceUsedThisWorld = true;
			showBadItemsResult();
			FeatureChat.sendPrefixed("Auto Rend", BAD_ITEMS_MESSAGE);
			return;
		}

		sequenceSessionId++;
		sequenceRunning = true;
		sequenceUsedThisWorld = true;
		sequenceElapsedTicks = 0;
		resetArmorSwapState();
		resetBackboneTracking();
		pullTickCounter = -1;
		enterStep(SequenceStep.SWAP_BONEMERANG);
		sendAutoRendMessage("Sequence started.");
	}

	private static void tickSequence() {
		if (!sequenceRunning) {
			return;
		}

		sequenceElapsedTicks++;
		stepElapsedTicks++;

		if (sequenceElapsedTicks > SEQUENCE_TIMEOUT_TICKS) {
			sendAutoRendMessage("Sequence timeout, resetting state.");
			resetSequence();
			return;
		}

		switch (currentStep) {
			case SWAP_BONEMERANG -> handleSwapBonemerang();
			case USE_BONEMERANG -> handleUseBonemerang();
			case SWAP_ATOMSPLIT -> handleSwapAtomsplit();
			case SWAP_ARMOR -> handleSwapArmor();
			case SWAP_ENDSTONE -> handleSwapEndstone();
			case USE_ENDSTONE -> handleUseEndstone();
			case SWAP_BONEMERANG_BACK -> handleSwapBonemerangBack();
			case SWAP_TERMINATOR -> handleSwapTerminator();
			case PULL_BONEMERANG -> handlePullBonemerang();
			case PULL_TERMINATOR -> handlePullTerminator();
			case ROTATE_TO_PEARL_POINT -> handleRotateToPearlPoint();
			case SWAP_PEARLS -> handleSwapPearls();
			case THROW_PEARL -> handleThrowPearl();
			case IDLE -> resetSequence();
		}
	}

	private static void handleSwapBonemerang() {
		int slot = UiDefinitions.AUTO_REND_BONEMERANG.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			
			boolean jumped = PlayerInputActions.jump();
			sendAutoRendMessage("Jump: " + actionStatus(jumped));
			
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Bonemerang (slot " + slot + "): " + actionStatus(swapped));
		}
		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.USE_BONEMERANG);
		}
	}
	
	private static void handleUseBonemerang() {
		if (!stepStarted) {
			stepStarted = true;
			boolean usedBonemerang = PlayerInputActions.rightClick();
			sendAutoRendMessage("Right click Bonemerang: " + actionStatus(usedBonemerang));
			if (usedBonemerang) {
				armBackboneWait();
			}
		}
		
		startArmorSwapIfNeeded();
		
		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.SWAP_ATOMSPLIT);
		}
	}

	private static void handleSwapAtomsplit() {
		int slot = UiDefinitions.AUTO_REND_ATOMSPLIT.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Atomsplit (slot " + slot + "): " + actionStatus(swapped));
		}

		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.SWAP_ARMOR);
		}
	}

	private static void handleSwapArmor() {
		if (!stepStarted) {
			stepStarted = true;
			startArmorSwapIfNeeded();
		}

		if (!armorSwapEnabled) {
			if (isBackboneReady()) {
				enterStep(SequenceStep.SWAP_ENDSTONE);
				return;
			}
			return;
		}

		if (armorSwapResolved) {
			if (!armorSwapOutcomeLogged) {
				armorSwapOutcomeLogged = true;
				sendAutoRendMessage("Swap armor completion: " + actionStatus(armorSwapSucceeded));
			}
			if (armorSwapSucceeded && isBackboneReady()) {
				enterStep(SequenceStep.SWAP_ENDSTONE);
				return;
			}
			return;
		}
	}

	private static void handleSwapEndstone() {
		int slot = UiDefinitions.AUTO_REND_ENDSTONE.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Endstone (slot " + slot + "): " + actionStatus(swapped));
		}
		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.USE_ENDSTONE);
		}
	}

	private static void handleUseEndstone() {
		if (!stepStarted) {
			stepStarted = true;
			boolean usedEndstone = PlayerInputActions.rightClick();
			sendAutoRendMessage("Right click Endstone: " + actionStatus(usedEndstone));
		}
		if (waitedAfterAction(CLICK_MIN_WAIT_TICKS)) {
			if (Boolean.TRUE.equals(UiDefinitions.AUTO_REND_TERMINATOR_PULL.get())) {
				enterStep(SequenceStep.SWAP_TERMINATOR);
				return;
			}
			enterStep(SequenceStep.SWAP_BONEMERANG_BACK);
		}
	}

	private static void handleSwapBonemerangBack() {
		int slot = UiDefinitions.AUTO_REND_BONEMERANG.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap back to Bonemerang (slot " + slot + "): " + actionStatus(swapped));
		}
		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.PULL_BONEMERANG);
		}
	}

	private static void handleSwapTerminator() {
		int slot = UiDefinitions.AUTO_REND_TERMINATOR.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Terminator (slot " + slot + "): " + actionStatus(swapped));
		}
		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.PULL_TERMINATOR);
		}
	}

	private static void handlePullBonemerang() {
		if (!stepStarted) {
			stepStarted = true;
			boolean pull = PlayerInputActions.leftClick();
			pullTickCounter = 0;
			beginRendResultWindow();
			sendAutoRendMessage("Left click Bonemerang pull: " + actionStatus(pull));
		}
		if (waitedAfterAction(CLICK_MIN_WAIT_TICKS)) {
			handleAfterPull();
		}
	}

	private static void handlePullTerminator() {
		if (!stepStarted) {
			stepStarted = true;
			boolean pull = PlayerInputActions.leftClick();
			pullTickCounter = 0;
			beginRendResultWindow();
			sendAutoRendMessage("Left click Terminator pull: " + actionStatus(pull));
		}
		if (waitedAfterAction(CLICK_MIN_WAIT_TICKS)) {
			handleAfterPull();
		}
	}

	private static void handleAfterPull() {
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_REND_AUTO_BACK_PEARL.get())) {
			enterStep(SequenceStep.ROTATE_TO_PEARL_POINT);
			return;
		}

		sendAutoRendMessage("Sequence finished.");
		markRendWindowFinished();
		resetSequence();
	}

	private static void handleRotateToPearlPoint() {
		int slot = UiDefinitions.AUTO_REND_PEARLS.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			double multiplier = UiDefinitions.AUTO_REND_ROTATION_MULTIPLIER.get();
			boolean rotated = RotationController.rotateTo(PEARL_TARGET_X, PEARL_TARGET_Y, PEARL_TARGET_Z, multiplier);
			sendAutoRendMessage("Rotate to pearl point: " + actionStatus(rotated));
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Pearls (slot " + slot + "): " + actionStatus(swapped));
		}

		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.THROW_PEARL);
		}
	}

	private static void handleSwapPearls() {
		int slot = UiDefinitions.AUTO_REND_PEARLS.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Pearls (slot " + slot + "): " + actionStatus(swapped));
		}
		if (waitedAfterAction(SWAP_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.THROW_PEARL);
		}
	}

	private static void handleThrowPearl() {
		if (!stepStarted) {
			stepStarted = true;
			boolean thrown = PlayerInputActions.rightClick();
			sendAutoRendMessage("Right click Pearl: " + actionStatus(thrown));
		}
		if (waitedAfterAction(CLICK_MIN_WAIT_TICKS)) {
			sendAutoRendMessage("Sequence finished.");
			markRendWindowFinished();
			resetSequence();
		}
	}

	private static boolean waitedAfterAction(int minimumTicks) {
		return stepElapsedTicks >= minimumTicks + 1;
	}

	private static void enterStep(SequenceStep nextStep) {
		currentStep = nextStep;
		stepElapsedTicks = 0;
		stepStarted = false;
	}

	private static void resetSequence() {
		boolean wasRunning = sequenceRunning;
		sequenceSessionId++;
		sequenceRunning = false;
		sequenceElapsedTicks = 0;
		resetArmorSwapState();
		resetBackboneTracking();
		pullTickCounter = -1;
		enterStep(SequenceStep.IDLE);
		if (wasRunning && rendResultWindowActive && rendResultGraceTicks < 0) {
			rendResultGraceTicks = 0;
		}
	}

	private static void tryResetForKuudraWorldLoad() {
		if (worldLoadResetTicksRemaining <= 0) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		sequenceUsedThisWorld = false;
		resetSequence();
		worldLoadResetTicksRemaining = 0;
	}

	private static boolean hasAnyConfiguredItemSlot() {
		return UiDefinitions.AUTO_REND_HYPERION.get() != -1
				|| UiDefinitions.AUTO_REND_BONEMERANG.get() != -1
				|| UiDefinitions.AUTO_REND_TERMINATOR.get() != -1
				|| UiDefinitions.AUTO_REND_ATOMSPLIT.get() != -1
				|| UiDefinitions.AUTO_REND_ENDSTONE.get() != -1
				|| UiDefinitions.AUTO_REND_PEARLS.get() != -1;
	}

	private static boolean requiredItemsAreInConfiguredSlots() {
		if (!slotMatchesSkyblockId(UiDefinitions.AUTO_REND_BONEMERANG.get().intValue(), BONEMERANG_ID)) {
			return false;
		}
		if (!slotMatchesSkyblockId(UiDefinitions.AUTO_REND_ATOMSPLIT.get().intValue(), ATOMSPLIT_ID)) {
			return false;
		}
		if (!slotMatchesSkyblockId(UiDefinitions.AUTO_REND_ENDSTONE.get().intValue(), ENDSTONE_ID)) {
			return false;
		}
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_REND_TERMINATOR_PULL.get())
				&& !slotMatchesSkyblockId(UiDefinitions.AUTO_REND_TERMINATOR.get().intValue(), TERMINATOR_ID)) {
			return false;
		}
		return !Boolean.TRUE.equals(UiDefinitions.AUTO_REND_AUTO_BACK_PEARL.get())
				|| slotMatchesSkyblockId(UiDefinitions.AUTO_REND_PEARLS.get().intValue(), PEARL_ID);
	}

	private static boolean slotMatchesSkyblockId(int hotbarSlot, String itemId) {
		if (!Inventory.isHotbarSlot(hotbarSlot)) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}

		ItemStack stack = client.player.getInventory().getItem(hotbarSlot);
		return HeldItemMatcher.stackMatchesSkyblockId(stack, itemId);
	}

	public static void scanItemSlots() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		int foundCount = 0;
		foundCount += scanItemAndUpdateSlider("Hyperion", "HYPERION", UiDefinitions.AUTO_REND_HYPERION);
		foundCount += scanItemAndUpdateSlider("Bonemerang", BONEMERANG_ID, UiDefinitions.AUTO_REND_BONEMERANG);
		foundCount += scanItemAndUpdateSlider("Terminator", TERMINATOR_ID, UiDefinitions.AUTO_REND_TERMINATOR);
		foundCount += scanItemAndUpdateSlider("Atomsplit", ATOMSPLIT_ID, UiDefinitions.AUTO_REND_ATOMSPLIT);
		foundCount += scanItemAndUpdateSlider("Endstone", ENDSTONE_ID, UiDefinitions.AUTO_REND_ENDSTONE);
		foundCount += scanItemAndUpdateSlider("Pearls", PEARL_ID, UiDefinitions.AUTO_REND_PEARLS);

		if (foundCount == 0) {
			sendScanMessage("Auto Rend Reworked scan: no matching items found in hotbar.");
		}
	}

	private static int scanItemAndUpdateSlider(String itemName, String nbtSuffix, UiSlider slider) {
		int slot = HotbarItemSearch.findFirstHotbarSlotByNbt(nbtSuffix);
		HotbarItemSearch.setSliderFromSlot(slider, slot);
		if (slot < 0) {
			return 0;
		}
		sendScanMessage("Auto Rend Reworked scan: " + itemName + " found in slot " + slot + ".");
		return 1;
	}

	private static void sendScanMessage(String text) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND_DEBUG.get())) {
			return;
		}
		FeatureChat.send(text);
	}

	private static void sendAutoRendMessage(String text) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND_DEBUG.get())) {
			return;
		}
		String tickInfo = pullTickCounter >= 0 ? " [" + pullTickCounter + "t]" : "";
		FeatureChat.sendPrefixed("Auto Rend", text + tickInfo);
	}

	private static void startArmorSwapIfNeeded() {
		if (armorSwapStarted) {
			return;
		}

		armorSwapStarted = true;
		int armorSlot = UiDefinitions.AUTO_REND_SWAP_ARMOR.get().intValue() + 1;
		armorSwapEnabled = armorSlot >= 1 && armorSlot <= 9;
		if (!armorSwapEnabled) {
			sendAutoRendMessage("Swap armor skipped (slider is -1).");
			return;
		}

		int requestSessionId = sequenceSessionId;
		armorSwapRequested = AutoRendHelper.WDSwapSlot(armorSlot, success -> {
			if (requestSessionId != sequenceSessionId) {
				return;
			}
			armorSwapResolved = true;
			armorSwapSucceeded = success;
		});

		if (!armorSwapRequested) {
			armorSwapResolved = true;
			armorSwapSucceeded = false;
		}
		sendAutoRendMessage("Swap armor (slot " + armorSlot + "): " + actionStatus(armorSwapRequested));
	}

	private static void resetArmorSwapState() {
		armorSwapStarted = false;
		armorSwapEnabled = false;
		armorSwapRequested = false;
		armorSwapResolved = false;
		armorSwapSucceeded = false;
		armorSwapOutcomeLogged = false;
	}

	private static void armBackboneTracking() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}

		currentBoneThrowSeq++;
		if (currentBoneThrowSeq == Integer.MAX_VALUE) {
			currentBoneThrowSeq = 1;
		}
		boneTrackingActive = true;
		boneExpectWindowEndTick = client.level.getGameTime() + BONE_TRACK_EXPECT_WINDOW_TICKS;
		backboneReadyGameTime = -1L;
		trackedBoneStand = null;
		trackedBoneCandidates.clear();
		boneThrowOrigin = new Vec3(client.player.getX(), client.player.getEyeY(), client.player.getZ());
		boneThrowForward = normalizeOrNull(client.player.getLookAngle());
		boneThrowPlayerPos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
		snapshotExistingBoneStands(client);
		sendAutoRendMessage("Backbone tracking armed.");
	}

	private static void armBackboneWait() {
		if (isAutoBackboneDetectionEnabled()) {
			armBackboneTracking();
			return;
		}
		armManualBackboneTimer();
	}

	private static void armManualBackboneTimer() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}

		int airTicks = Math.max(0, UiDefinitions.AUTO_REND_BONEMERANG_AIR_TICKS.get().intValue());
		resetBackboneTracking();
		backboneReadyGameTime = client.level.getGameTime() + airTicks;
		sendAutoRendMessage("Backbone timer armed for " + airTicks + " ticks.");
	}

	private static boolean isAutoBackboneDetectionEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.AUTO_REND_AUTO_BACKBONE_DETECTION.get());
	}

	private static void tickBackboneTracking(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		long gameTime = client.level.getGameTime();
		if (boneTrackingActive && boneExpectWindowEndTick >= 0L && gameTime > boneExpectWindowEndTick) {
			boneTrackingActive = false;
			boneExpectWindowEndTick = -1L;
		}

		if (!boneTrackingActive && trackedBoneCandidates.isEmpty() && trackedBoneStand == null) {
			return;
		}
		if (boneTrackingActive && gameTime <= boneExpectWindowEndTick) {
			scanForBoneStandCandidates(client);
		}
		tickBoneStandCandidates(client);
		if (trackedBoneStand == null) {
			return;
		}

		ArmorStand stand = findTrackedBoneStand(client);
		if (stand == null || !stand.isAlive() || !isBoneStand(stand)) {
			trackedBoneStand = null;
			return;
		}

		Vec3 headPos = getArmorStandHeadPos(stand);
		if (trackedBoneStand.prevHeadPos == null) {
			trackedBoneStand.prevHeadPos = headPos;
		}

		trackedBoneStand.currHeadPos = headPos;
		Vec3 delta = subtract(trackedBoneStand.currHeadPos, trackedBoneStand.prevHeadPos);
		double deltaLengthSq = lengthSquared(delta);
		updateTrackedBonePhase(client, trackedBoneStand, delta, deltaLengthSq);
		trackedBoneStand.ticksAlive++;
		if (trackedBoneStand.ticksAlive > BONE_TRACK_MAX_AGE_TICKS) {
			trackedBoneStand = null;
			return;
		}

		if (deltaLengthSq > 1.0E-12D && detectBackboneHit(client, trackedBoneStand)) {
			backboneReadyGameTime = gameTime + BACKBONE_READY_DELAY_TICKS;
			boneTrackingActive = false;
			trackedBoneStand = null;
			sendAutoRendMessage("Backbone detected, waiting " + BACKBONE_READY_DELAY_TICKS + " ticks.");
			return;
		}

		trackedBoneStand.prevHeadPos = trackedBoneStand.currHeadPos;
	}

	private static void snapshotExistingBoneStands(Minecraft client) {
		seenBoneStandIds.clear();

		if (client == null || client.level == null) {
			return;
		}
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof ArmorStand) {
				seenBoneStandIds.add(entity.getId());
			}
		}
	}

	private static void scanForBoneStandCandidates(Minecraft client) {
		if (client == null || client.player == null || client.level == null || !boneTrackingActive) {
			return;
		}

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof ArmorStand stand) || !stand.isAlive()) {
				continue;
			}
			int entityId = stand.getId();
			if (!seenBoneStandIds.add(entityId)) {
				continue;
			}
			if (trackedBoneStand != null && trackedBoneStand.entityId == entityId) {
				continue;
			}
			if (trackedBoneCandidates.containsKey(entityId)) {
				continue;
			}
			registerBoneStandCandidate(client, stand);
		}
	}

	private static void registerBoneStandCandidate(Minecraft client, ArmorStand stand) {
		if (client == null || client.player == null || stand == null) {
			return;
		}

		Vec3 standPos = new Vec3(stand.getX(), stand.getY(), stand.getZ());
		Vec3 playerPos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
		double minPlayerDistSq = Math.min(
				distanceSquared(standPos, playerPos),
				boneThrowPlayerPos == null ? Double.POSITIVE_INFINITY : distanceSquared(standPos, boneThrowPlayerPos));
		if (minPlayerDistSq > BONE_TRACK_MAX_CANDIDATE_DIST_SQ) {
			return;
		}

		Vec3 headPos = getArmorStandHeadPos(stand);
		boolean ownerLocal = isLocalThrowCandidate(headPos);
		trackedBoneCandidates.put(stand.getId(), new CandidateBoneStand(stand.getId(), ownerLocal));
	}

	private static void tickBoneStandCandidates(Minecraft client) {
		if (client == null || client.level == null || trackedBoneCandidates.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<Integer, CandidateBoneStand>> iterator = trackedBoneCandidates.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, CandidateBoneStand> entry = iterator.next();
			int entityId = entry.getKey();
			if (trackedBoneStand != null && trackedBoneStand.entityId == entityId) {
				iterator.remove();
				continue;
			}

			ArmorStand stand = findBoneStandById(client, entityId);
			if (stand == null || !stand.isAlive()) {
				iterator.remove();
				continue;
			}

			CandidateBoneStand candidate = entry.getValue();
			candidate.ageTicks++;
			boolean promoted = tryPromoteBoneStandCandidate(stand, candidate);
			if (promoted || candidate.ageTicks > BONE_TRACK_CANDIDATE_MAX_AGE_TICKS) {
				iterator.remove();
			}
		}
	}

	private static boolean tryPromoteBoneStandCandidate(ArmorStand stand, CandidateBoneStand candidate) {
		if (!isBoneStand(stand)) {
			return false;
		}

		int entityId = stand.getId();
		if (trackedBoneStand != null && trackedBoneStand.entityId == entityId) {
			return true;
		}

		boolean ownerLocal = candidate.ownerLocal;
		if (ownerLocal && boneThrowForward != null && boneThrowOrigin != null) {
			Vec3 headPos = getArmorStandHeadPos(stand);
			Vec3 delta = subtract(headPos, boneThrowOrigin);
			double deltaLengthSq = lengthSquared(delta);
			if (deltaLengthSq > BONE_TRACK_OWNER_PROMOTE_DIST_HARD_SQ) {
				ownerLocal = false;
			} else {
				double deltaLength = Math.sqrt(deltaLengthSq);
				if (deltaLength > 1.0E-6D) {
					double forwardDot = dot(boneThrowForward, scale(delta, 1.0D / deltaLength));
					if (forwardDot < BONE_TRACK_FWD_RAY_DOT_MIN) {
						ownerLocal = false;
					}
				} else {
					ownerLocal = false;
				}
			}
		}

		claimTrackedBoneStand(stand, ownerLocal);
		return true;
	}

	private static void claimTrackedBoneStand(ArmorStand stand, boolean ownerLocal) {
		if (stand == null) {
			return;
		}
		if (trackedBoneStand != null && trackedBoneStand.throwSeq == currentBoneThrowSeq) {
			return;
		}

		trackedBoneStand = new TrackedBoneStand(
				stand.getId(),
				ownerLocal,
				getArmorStandHeadPos(stand),
				currentBoneThrowSeq,
				boneThrowOrigin,
				boneThrowForward);
		sendAutoRendMessage("Backbone stand claimed: " + stand.getId() + ".");
	}

	private static void updateTrackedBonePhase(Minecraft client, TrackedBoneStand tracked, Vec3 delta, double deltaLengthSq) {
		if (client == null || client.player == null || tracked == null || tracked.currHeadPos == null) {
			return;
		}

		BonePhase previousPhase = tracked.phase;
		if (tracked.origin != null && tracked.forwardUsed != null) {
			if (!tracked.forwardFixed && deltaLengthSq > 1.0E-6D) {
				if (dot(delta, tracked.forwardUsed) < 0.0D) {
					tracked.forwardUsed = scale(tracked.forwardUsed, -1.0D);
				}
				tracked.forwardFixed = true;
			}

			double currentAlong = dot(tracked.forwardUsed, subtract(tracked.currHeadPos, tracked.origin));
			if (!Double.isNaN(tracked.prevAlong)) {
				double deltaAlong = currentAlong - tracked.prevAlong;
				if (deltaAlong > 1.0E-4D) {
					tracked.phase = BonePhase.OUTBOUND;
					tracked.sawOutbound = true;
				} else if (deltaAlong < -1.0E-4D && tracked.sawOutbound) {
					tracked.phase = BonePhase.RETURNING;
				}
				if (!tracked.hasTurned && previousPhase == BonePhase.OUTBOUND && tracked.phase == BonePhase.RETURNING) {
					tracked.hasTurned = true;
				}
				if (tracked.hasTurned) {
					tracked.phase = BonePhase.RETURNING;
				}
			}
			tracked.prevAlong = currentAlong;
			return;
		}

		Vec3 toPlayer = tracked.currHeadPos.subtract(client.player.getX(), client.player.getY(), client.player.getZ());
		double playerLengthSq = lengthSquared(toPlayer);
		double phaseDot = 0.0D;
		if (deltaLengthSq >= 1.0E-12D && playerLengthSq >= 1.0E-12D) {
			phaseDot = dot(delta, toPlayer) / Math.sqrt(deltaLengthSq * playerLengthSq);
		}

		if (phaseDot > BONE_TRACK_FALLBACK_OUTBOUND_DOT_MIN) {
			tracked.phase = BonePhase.OUTBOUND;
		} else if (phaseDot < BONE_TRACK_FALLBACK_RETURNING_DOT_MAX) {
			tracked.phase = BonePhase.RETURNING;
		}
		if (previousPhase != BonePhase.RETURNING && tracked.phase == BonePhase.RETURNING) {
			tracked.hasTurned = true;
		}
	}

	private static boolean detectBackboneHit(Minecraft client, TrackedBoneStand tracked) {
		if (client == null || client.player == null || tracked == null || tracked.prevHeadPos == null || tracked.currHeadPos == null) {
			return false;
		}

		MagmaCube boss = KuudraPhaseTracker.getKuudraEntity();
		if (boss == null || !boss.isAlive()) {
			return false;
		}

		AABB targetBoundingBox = boss.getBoundingBox();
		AABB movementBox = new AABB(
				Math.min(tracked.prevHeadPos.x, tracked.currHeadPos.x),
				Math.min(tracked.prevHeadPos.y, tracked.currHeadPos.y),
				Math.min(tracked.prevHeadPos.z, tracked.currHeadPos.z),
				Math.max(tracked.prevHeadPos.x, tracked.currHeadPos.x),
				Math.max(tracked.prevHeadPos.y, tracked.currHeadPos.y),
				Math.max(tracked.prevHeadPos.z, tracked.currHeadPos.z))
				.inflate(4.0D);
		if (!targetBoundingBox.intersects(movementBox)) {
			return false;
		}

		AABB targetBox = targetBoundingBox.inflate(BONE_TRACK_HEAD_HALF_XZ, BONE_TRACK_HEAD_HALF_Y, BONE_TRACK_HEAD_HALF_XZ);
		double segmentX = tracked.currHeadPos.x - tracked.prevHeadPos.x;
		double segmentY = tracked.currHeadPos.y - tracked.prevHeadPos.y;
		double segmentZ = tracked.currHeadPos.z - tracked.prevHeadPos.z;
		double segmentLengthSq = (segmentX * segmentX) + (segmentY * segmentY) + (segmentZ * segmentZ);
		if (segmentLengthSq < 1.0E-9D) {
			segmentLengthSq = 1.0E-9D;
		}

		double dotStart = (segmentX * (tracked.prevHeadPos.x - client.player.getX()))
				+ (segmentY * (tracked.prevHeadPos.y - client.player.getY()))
				+ (segmentZ * (tracked.prevHeadPos.z - client.player.getZ()));
		double dotEnd = (segmentX * (tracked.currHeadPos.x - client.player.getX()))
				+ (segmentY * (tracked.currHeadPos.y - client.player.getY()))
				+ (segmentZ * (tracked.currHeadPos.z - client.player.getZ()));
		boolean crossesPlayerProjection = dotStart > 0.0D && dotEnd < 0.0D;
		double projectionT = -dotStart / segmentLengthSq;
		boolean projectionOnSegment = projectionT >= 0.0D && projectionT <= 1.0D;
		if (!projectionOnSegment) {
			crossesPlayerProjection = false;
		}

		Vec3 playerProjection = new Vec3(
				tracked.prevHeadPos.x + (segmentX * projectionT),
				tracked.prevHeadPos.y + (segmentY * projectionT),
				tracked.prevHeadPos.z + (segmentZ * projectionT));

		if (crossesPlayerProjection) {
			return segmentIntersectsAABB(targetBox, playerProjection, tracked.currHeadPos)
					|| containsPoint(targetBox, tracked.currHeadPos)
					|| containsPoint(targetBox, playerProjection);
		}

		boolean wholeSegmentHit = segmentIntersectsAABB(targetBox, tracked.prevHeadPos, tracked.currHeadPos)
				|| containsPoint(targetBox, tracked.prevHeadPos)
				|| containsPoint(targetBox, tracked.currHeadPos);
		BonePhase fallbackPhase = tracked.hasTurned ? BonePhase.RETURNING : BonePhase.OUTBOUND;
		return wholeSegmentHit && fallbackPhase == BonePhase.RETURNING;
	}

	private static ArmorStand findTrackedBoneStand(Minecraft client) {
		if (trackedBoneStand == null) {
			return null;
		}
		return findBoneStandById(client, trackedBoneStand.entityId);
	}

	private static ArmorStand findBoneStandById(Minecraft client, int entityId) {
		if (client == null || client.level == null) {
			return null;
		}
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof ArmorStand stand && stand.getId() == entityId) {
				return stand;
			}
		}
		return null;
	}

	private static boolean isBoneStand(ArmorStand stand) {
		return stand != null
				&& stand.getItemBySlot(EquipmentSlot.MAINHAND).getItem() == Items.BONE;
	}

	private static boolean isLocalThrowCandidate(Vec3 headPos) {
		if (headPos == null || boneThrowOrigin == null || boneThrowForward == null) {
			return false;
		}
		Vec3 delta = subtract(headPos, boneThrowOrigin);
		double deltaLengthSq = lengthSquared(delta);
		if (deltaLengthSq <= 1.0E-6D || deltaLengthSq > BONE_TRACK_OWNER_PROMOTE_DIST_HARD_SQ) {
			return false;
		}
		double inverseLength = 1.0D / Math.sqrt(deltaLengthSq);
		return dot(boneThrowForward, scale(delta, inverseLength)) >= BONE_TRACK_FWD_RAY_DOT_MIN;
	}

	private static Vec3 getArmorStandHeadPos(ArmorStand stand) {
		double yOffset = stand.isSmall() ? BONE_TRACK_MARKER_HEAD_Y_SMALL : BONE_TRACK_MARKER_HEAD_Y_BIG;
		return new Vec3(stand.getX(), stand.getY() + yOffset, stand.getZ());
	}

	private static boolean isBackboneReady() {
		Minecraft client = Minecraft.getInstance();
		return client != null
				&& client.level != null
				&& backboneReadyGameTime >= 0L
				&& client.level.getGameTime() >= backboneReadyGameTime;
	}

	private static void resetBackboneTracking() {
		boneTrackingActive = false;
		boneExpectWindowEndTick = -1L;
		backboneReadyGameTime = -1L;
		boneThrowOrigin = null;
		boneThrowForward = null;
		boneThrowPlayerPos = null;
		trackedBoneStand = null;
		trackedBoneCandidates.clear();
		seenBoneStandIds.clear();
	}

	private static boolean containsPoint(AABB box, Vec3 point) {
		return box != null
				&& point != null
				&& point.x >= box.minX && point.x <= box.maxX
				&& point.y >= box.minY && point.y <= box.maxY
				&& point.z >= box.minZ && point.z <= box.maxZ;
	}

	private static boolean segmentIntersectsAABB(AABB box, Vec3 start, Vec3 end) {
		double tMin = 0.0D;
		double tMax = 1.0D;

		double deltaX = end.x - start.x;
		if (Math.abs(deltaX) <= 1.0E-12D) {
			if (start.x < box.minX || start.x > box.maxX) {
				return false;
			}
		} else {
			double inverse = 1.0D / deltaX;
			double t1 = (box.minX - start.x) * inverse;
			double t2 = (box.maxX - start.x) * inverse;
			if (t1 > t2) {
				double swap = t1;
				t1 = t2;
				t2 = swap;
			}
			tMin = Math.max(tMin, t1);
			tMax = Math.min(tMax, t2);
			if (tMin > tMax) {
				return false;
			}
		}

		double deltaY = end.y - start.y;
		if (Math.abs(deltaY) <= 1.0E-12D) {
			if (start.y < box.minY || start.y > box.maxY) {
				return false;
			}
		} else {
			double inverse = 1.0D / deltaY;
			double t1 = (box.minY - start.y) * inverse;
			double t2 = (box.maxY - start.y) * inverse;
			if (t1 > t2) {
				double swap = t1;
				t1 = t2;
				t2 = swap;
			}
			tMin = Math.max(tMin, t1);
			tMax = Math.min(tMax, t2);
			if (tMin > tMax) {
				return false;
			}
		}

		double deltaZ = end.z - start.z;
		if (Math.abs(deltaZ) <= 1.0E-12D) {
			return start.z >= box.minZ && start.z <= box.maxZ;
		}

		double inverse = 1.0D / deltaZ;
		double t1 = (box.minZ - start.z) * inverse;
		double t2 = (box.maxZ - start.z) * inverse;
		if (t1 > t2) {
			double swap = t1;
			t1 = t2;
			t2 = swap;
		}
		tMin = Math.max(tMin, t1);
		tMax = Math.min(tMax, t2);
		return tMin <= tMax;
	}

	private static Vec3 normalizeOrNull(Vec3 vec) {
		if (vec == null) {
			return null;
		}
		double lengthSq = lengthSquared(vec);
		if (lengthSq <= 1.0E-12D) {
			return null;
		}
		return scale(vec, 1.0D / Math.sqrt(lengthSq));
	}

	private static Vec3 subtract(Vec3 left, Vec3 right) {
		return new Vec3(left.x - right.x, left.y - right.y, left.z - right.z);
	}

	private static Vec3 scale(Vec3 vec, double factor) {
		return new Vec3(vec.x * factor, vec.y * factor, vec.z * factor);
	}

	private static double dot(Vec3 left, Vec3 right) {
		return (left.x * right.x) + (left.y * right.y) + (left.z * right.z);
	}

	private static double lengthSquared(Vec3 vec) {
		return dot(vec, vec);
	}

	private static double distanceSquared(Vec3 left, Vec3 right) {
		double deltaX = left.x - right.x;
		double deltaY = left.y - right.y;
		double deltaZ = left.z - right.z;
		return (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ);
	}

	private static String actionStatus(boolean success) {
		return success ? "ok" : "failed";
	}

	private static void beginRendResultWindow() {
		rendResultWindowActive = true;
		rendResultGraceTicks = -1;
		rendLastKuudraHp = readKuudraHp();
		rendResultText = "";
		rendResultColor = REND_RESULT_BAD_COLOR;
		rendResultHudTicks = 0;
	}

	private static void showBadItemsResult() {
		rendResultWindowActive = false;
		rendResultGraceTicks = -1;
		rendLastKuudraHp = -1;
		rendResultText = BAD_ITEMS_REND_RESULT_TEXT;
		rendResultColor = REND_RESULT_BAD_COLOR;
		rendResultHudTicks = REND_RESULT_HUD_DURATION_TICKS;
	}

	private static void markRendWindowFinished() {
		if (!rendResultWindowActive) {
			return;
		}
		rendResultGraceTicks = REND_RESULT_FINISH_GRACE_TICKS;
	}

	private static void tickRendResultState(Minecraft client) {
		if (rendResultHudTicks > 0) {
			rendResultHudTicks--;
		}
		if (!rendResultWindowActive) {
			return;
		}

		if (rendResultGraceTicks == 0) {
			rendResultText = "BAD";
			rendResultColor = REND_RESULT_BAD_COLOR;
			rendResultWindowActive = false;
			rendResultGraceTicks = -1;
			rendResultHudTicks = REND_RESULT_HUD_DURATION_TICKS;
			return;
		}

		if (tryCaptureRendResult(client)) {
			rendResultWindowActive = false;
			rendResultGraceTicks = -1;
			rendResultHudTicks = REND_RESULT_HUD_DURATION_TICKS;
			return;
		}

		if (rendResultGraceTicks > 0) {
			rendResultGraceTicks--;
		}
	}

	private static boolean tryCaptureRendResult(Minecraft client) {
		int kuudraHp = readKuudraHp(client);
		if (kuudraHp < 0) {
			rendLastKuudraHp = -1;
			return false;
		}

		if (rendLastKuudraHp > 0) {
			int diff = rendLastKuudraHp - kuudraHp;
			if (diff > REND_MIN_DIFF) {
				rendResultText = formatRendDamage(diff * 9_600);
				rendResultColor = resolveRendResultColor(diff);
				sendAutoRendMessage("Rend result: " + rendResultText);
				return true;
			}
		}

		rendLastKuudraHp = kuudraHp;
		return false;
	}

	private static int readKuudraHp() {
		return readKuudraHp(Minecraft.getInstance());
	}

	private static int readKuudraHp(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return -1;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return -1;
		}
		if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.PHASE_DPS || client.player.getY() > 30.0D) {
			return -1;
		}

		MagmaCube boss = KuudraPhaseTracker.getKuudraEntity();
		if (boss == null) {
			return -1;
		}

		int hp = (int) boss.getHealth();
		return hp > 25_000 ? -1 : hp;
	}

	private static String formatRendDamage(int damage) {
		double adjustedDamage = damage * REND_RESULT_DISPLAY_MULTIPLIER;
		return String.format(Locale.ROOT, "%.1fM", adjustedDamage / 1_000_000.0D);
	}

	private static int resolveRendResultColor(int diff) {
		if (diff <= REND_DAMAGE_LOW_DIFF) {
			return REND_RESULT_BAD_COLOR;
		}
		if (diff <= REND_DAMAGE_MED_DIFF) {
			return REND_RESULT_MID_COLOR;
		}
		return REND_RESULT_HIGH_COLOR;
	}

	private static void renderRendResultOverlay(GuiGraphics graphics, DeltaTracker tickCounter) {
		if (graphics == null || rendResultHudTicks <= 0 || rendResultText == null || rendResultText.isEmpty()) {
			return;
		}
		if (!isRendResultHudEnabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null || client.getWindow() == null) {
			return;
		}

		int centerX = client.getWindow().getGuiScaledWidth() / 2;
		Component text = Component.literal(rendResultText)
				.withStyle(style -> style.withBold(true).withColor(rendResultColor & 0x00FFFFFF));

		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, REND_RESULT_HUD_Y);
		graphics.pose().scale(REND_RESULT_HUD_SCALE, REND_RESULT_HUD_SCALE);
		graphics.drawString(client.font, text, -client.font.width(rendResultText) / 2, 0, rendResultColor, true);
		graphics.pose().popMatrix();
	}

	private static void resetRendResultState() {
		rendResultWindowActive = false;
		rendResultGraceTicks = -1;
		rendLastKuudraHp = -1;
		rendResultText = "";
		rendResultColor = REND_RESULT_BAD_COLOR;
		rendResultHudTicks = 0;
	}

	private static boolean isRendResultHudEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.REND_DAMAGE.get())
				&& Boolean.TRUE.equals(UiDefinitions.REND_DAMAGE_AUTO_REND_SCREEN.get());
	}

	private enum BonePhase {
		UNKNOWN,
		OUTBOUND,
		RETURNING
	}

	private enum SequenceStep {
		IDLE,
		SWAP_BONEMERANG,
		USE_BONEMERANG,
		SWAP_ATOMSPLIT,
		SWAP_ARMOR,
		SWAP_ENDSTONE,
		USE_ENDSTONE,
		SWAP_BONEMERANG_BACK,
		SWAP_TERMINATOR,
		PULL_BONEMERANG,
		PULL_TERMINATOR,
		ROTATE_TO_PEARL_POINT,
		SWAP_PEARLS,
		THROW_PEARL
	}

	private static final class TrackedBoneStand {
		private final int entityId;
		private final boolean ownerLocal;
		private final int throwSeq;
		private final Vec3 origin;
		private Vec3 forwardUsed;
		private boolean forwardFixed;
		private BonePhase phase = BonePhase.UNKNOWN;
		private int ticksAlive;
		private Vec3 prevHeadPos;
		private Vec3 currHeadPos;
		private double prevAlong = Double.NaN;
		private boolean sawOutbound;
		private boolean hasTurned;

		private TrackedBoneStand(int entityId, boolean ownerLocal, Vec3 headPos, int throwSeq, Vec3 origin, Vec3 forward) {
			this.entityId = entityId;
			this.ownerLocal = ownerLocal;
			this.throwSeq = throwSeq;
			this.prevHeadPos = headPos;
			this.currHeadPos = headPos;
			this.origin = origin;
			this.forwardUsed = forward;
		}
	}

	private static final class CandidateBoneStand {
		private final int entityId;
		private final boolean ownerLocal;
		private int ageTicks;

		private CandidateBoneStand(int entityId, boolean ownerLocal) {
			this.entityId = entityId;
			this.ownerLocal = ownerLocal;
		}
	}
}
