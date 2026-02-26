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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;

import java.util.Locale;

public final class AutoRend_Reworked {
	private static final RenderType AUTO_REND_TRIGGER_RENDER_TYPE = createAutoRendTriggerRenderType();
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
	private static final int REND_RESULT_FINISH_GRACE_TICKS = 2;
	private static final float REND_RESULT_HUD_SCALE = 2.2f;
	private static final int REND_RESULT_HUD_Y = 44;
	private static final double REND_RESULT_DISPLAY_MULTIPLIER = 0.94D;

	private static final int SWAP_MIN_WAIT_TICKS = 1;
	private static final int CLICK_MIN_WAIT_TICKS = 1;
	private static final int OTHER_MIN_WAIT_TICKS = 2;
	private static final int AFTER_BONE_RIGHT_CLICK_TICKS = 19;
	private static final int ARMOR_SWAP_TIMEOUT_TICKS = 120;

	private static final String BONEMERANG_ID = "STARRED_BONE_BOOMERANG";
	private static final String ATOMSPLIT_ID = "ATOMSPLIT_KATANA";
	private static final String ENDSTONE_ID = "END_STONE_SWORD";
	private static final String PEARL_ID = "ENDER_PEARL";

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

	private AutoRend_Reworked() {
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
		});

		ClientTickEvents.END_CLIENT_TICK.register(AutoRend_Reworked::handleClientTick);
		WorldRenderEvents.END_MAIN.register(AutoRend_Reworked::renderActivationZones);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.SUBTITLES,
				AUTO_REND_RESULT_HUD_ID,
				AutoRend_Reworked::renderRendResultOverlay);
		FakeKeybinds.onKeyPress(UiDefinitions.AUTO_REND_TRIGGER_KEYBIND, AutoRend_Reworked::triggerKuudraDead);
	}

	private static void handleClientTick(Minecraft client) {
		tickRendResultState(client);
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_REND_HARDCODE.get())) {
			return;
		}
		if (worldLoadResetTicksRemaining > 0) {
			worldLoadResetTicksRemaining--;
			tryResetForKuudraWorldLoad();
		}

		if (client == null || client.player == null || client.level == null) {
			resetSequence();
			return;
		}

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

	private static void triggerKuudraDead() {
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_REND_HARDCODE.get())) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND_DEBUG.get())) {
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
		if (Boolean.TRUE.equals(UiDefinitions.AUTO_REND_HARDCODE.get())) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (!canRenderActivationZones(client)) {
			return;
		}

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		for (AABB box : KuudraTriggerArea.getTriggerAabbs()) {
			var lineBuffer = context.consumers().getBuffer(AUTO_REND_TRIGGER_RENDER_TYPE);
			ShapeRenderer.renderShape(
					context.matrices(),
					lineBuffer,
					Shapes.create(box),
					-cameraPos.x,
					-cameraPos.y,
					-cameraPos.z,
					AUTO_REND_TRIGGER_COLOR,
					AUTO_REND_TRIGGER_LINE_WIDTH);
			renderActivationCoords(context, client, cameraPos, box);
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

	private static void renderActivationCoords(WorldRenderContext context, Minecraft client, Vec3 cameraPos, AABB box) {
		Vec3 center = box.getCenter();
		String label = formatCoordLabel(center);

		context.matrices().pushPose();
		context.matrices().translate(center.x - cameraPos.x, box.maxY - cameraPos.y + 0.35, center.z - cameraPos.z);
		context.matrices().mulPose(client.gameRenderer.getMainCamera().rotation());
		context.matrices().scale(-0.02f, -0.02f, 0.02f);

		Matrix4f pose = context.matrices().last().pose();
		float textWidth = client.font.width(label);
		// Use the world render consumer for this pass; ending a different/global batch
		// here
		// can invalidate active builders and crash with "Not building!". - Crussion.
		// For anyone that wants to change this (*baby cry* im not building)
		var buffer = context.consumers();
		client.font.drawInBatch(
				label,
				-textWidth / 2.0f,
				0.0f,
				AUTO_REND_TRIGGER_COLOR,
				false,
				pose,
				buffer,
				net.minecraft.client.gui.Font.DisplayMode.NORMAL,
				0,
				LightTexture.FULL_BRIGHT);
		context.matrices().popPose();
	}

	private static String formatCoordLabel(Vec3 center) {
		return String.format(Locale.ROOT, "%.1f %.1f %.1f", center.x, center.y, center.z);
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

		RenderPipeline pipeline = builder.build();
		RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
		return RenderTypeAccessor.moissanite$invokeCreate("moissanite_auto_rend_trigger_lines", setup);
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

		sequenceRunning = true;
		sequenceUsedThisWorld = true;
		sequenceElapsedTicks = 0;
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
			case PULL_BONEMERANG -> handlePullBonemerang();
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
		if (isSwapReady(BONEMERANG_ID)) {
			enterStep(SequenceStep.USE_BONEMERANG);
		}
	}

	private static void handleUseBonemerang() {
		if (!stepStarted) {
			stepStarted = true;
			boolean usedBonemerang = PlayerInputActions.rightClick();
			sendAutoRendMessage("Right click Bonemerang: " + actionStatus(usedBonemerang));
		}

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
		// Start armor swap right after Atomsplit swap; don't block on held-item verification.
		
		if (stepElapsedTicks >= SWAP_MIN_WAIT_TICKS) {
			enterStep(SequenceStep.SWAP_ARMOR);
		}
	}

	private static void handleSwapArmor() {
		if (!stepStarted) {
			stepStarted = true;
			int armorSlot = UiDefinitions.AUTO_REND_SWAP_ARMOR.get().intValue();
			armorSwapEnabled = armorSlot >= 1 && armorSlot <= 9;

			if (!armorSwapEnabled) {
				sendAutoRendMessage("Swap armor skipped (slider is -1/0).");
			} else {
				armorSwapRequested = AutoRendHelper.WDSwapSlot(armorSlot, success -> {
					armorSwapResolved = true;
					armorSwapSucceeded = success;
				});

				if (!armorSwapRequested) {
					armorSwapResolved = true;
					armorSwapSucceeded = false;
				}
				sendAutoRendMessage("Swap armor (slot " + armorSlot + "): " + actionStatus(armorSwapRequested));
			}
		}

		if (!armorSwapEnabled) {
			if (waitedAfterAction(OTHER_MIN_WAIT_TICKS + AFTER_BONE_RIGHT_CLICK_TICKS)) {
				enterStep(SequenceStep.SWAP_ENDSTONE);
			}
			return;
		}

		if (armorSwapResolved) {
			if (!armorSwapOutcomeLogged) {
				armorSwapOutcomeLogged = true;
				sendAutoRendMessage("Swap armor completion: " + actionStatus(armorSwapSucceeded));
			}
			if (waitedAfterAction(OTHER_MIN_WAIT_TICKS + AFTER_BONE_RIGHT_CLICK_TICKS)) {
				enterStep(SequenceStep.SWAP_ENDSTONE);
			}
			return;
		}

		if (waitedAfterAction(ARMOR_SWAP_TIMEOUT_TICKS)) {
			sendAutoRendMessage("Swap armor timeout, continuing.");
			enterStep(SequenceStep.SWAP_ENDSTONE);
		}
	}

	private static void handleSwapEndstone() {
		int slot = UiDefinitions.AUTO_REND_ENDSTONE.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Endstone (slot " + slot + "): " + actionStatus(swapped));
		}
		if (isSwapReady(ENDSTONE_ID)) {
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
		if (isSwapReady(BONEMERANG_ID)) {
			enterStep(SequenceStep.PULL_BONEMERANG);
		}
	}

	private static void handlePullBonemerang() {
		if (!stepStarted) {
			stepStarted = true;
			boolean pull = PlayerInputActions.leftClick();
			beginRendResultWindow();
			sendAutoRendMessage("Left click pull: " + actionStatus(pull));
		}
		if (waitedAfterAction(CLICK_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.ROTATE_TO_PEARL_POINT);
		}
	}

	private static void handleRotateToPearlPoint() {
		if (!stepStarted) {
			stepStarted = true;
			double multiplier = UiDefinitions.AUTO_REND_ROTATION_MULTIPLIER.get();
			boolean rotated = RotationController.rotateTo(PEARL_TARGET_X, PEARL_TARGET_Y, PEARL_TARGET_Z, multiplier);
			sendAutoRendMessage("Rotate to pearl point: " + actionStatus(rotated));
		}
		if (waitedAfterAction(OTHER_MIN_WAIT_TICKS)) {
			enterStep(SequenceStep.SWAP_PEARLS);
		}
	}

	private static void handleSwapPearls() {
		int slot = UiDefinitions.AUTO_REND_PEARLS.get().intValue();
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(slot);
			sendAutoRendMessage("Swap to Pearls (slot " + slot + "): " + actionStatus(swapped));
		}
		if (isSwapReady(PEARL_ID)) {
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

	private static boolean isSwapReady(String itemId) {
		return waitedAfterAction(SWAP_MIN_WAIT_TICKS) && HeldItemMatcher.heldMatchesSkyblockId(itemId);
	}

	private static boolean waitedAfterAction(int minimumTicks) {
		return stepElapsedTicks >= minimumTicks + 1;
	}

	private static void enterStep(SequenceStep nextStep) {
		currentStep = nextStep;
		stepElapsedTicks = 0;
		stepStarted = false;

		armorSwapEnabled = false;
		armorSwapRequested = false;
		armorSwapResolved = false;
		armorSwapSucceeded = false;
		armorSwapOutcomeLogged = false;
	}

	private static void resetSequence() {
		boolean wasRunning = sequenceRunning;
		sequenceRunning = false;
		sequenceElapsedTicks = 0;
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
				|| UiDefinitions.AUTO_REND_ATOMSPLIT.get() != -1
				|| UiDefinitions.AUTO_REND_ENDSTONE.get() != -1
				|| UiDefinitions.AUTO_REND_PEARLS.get() != -1;
	}

	public static void scanItemSlots() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		int foundCount = 0;
		foundCount += scanItemAndUpdateSlider("Hyperion", "HYPERION", UiDefinitions.AUTO_REND_HYPERION);
		foundCount += scanItemAndUpdateSlider("Bonemerang", BONEMERANG_ID, UiDefinitions.AUTO_REND_BONEMERANG);
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
		FeatureChat.sendPrefixed("Auto Rend Reworked", text);
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

	private enum SequenceStep {
		IDLE,
		SWAP_BONEMERANG,
		USE_BONEMERANG,
		SWAP_ATOMSPLIT,
		SWAP_ARMOR,
		SWAP_ENDSTONE,
		USE_ENDSTONE,
		SWAP_BONEMERANG_BACK,
		PULL_BONEMERANG,
		ROTATE_TO_PEARL_POINT,
		SWAP_PEARLS,
		THROW_PEARL
	}
}
