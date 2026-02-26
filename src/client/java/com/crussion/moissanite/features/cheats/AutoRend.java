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
import com.crussion.moissanite.util.tick.TickTaskScheduler;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;

import java.util.Locale;

public final class AutoRend {
	private static final RenderType AUTO_REND_TRIGGER_RENDER_TYPE = createAutoRendTriggerRenderType();
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final int SEQUENCE_TIMEOUT_TICKS = 220;
	private static final int WORLD_LOAD_RESET_WINDOW_TICKS = 80;
	private static final int AUTO_REND_TRIGGER_COLOR = 0xFF29D6FF;
	private static final float AUTO_REND_TRIGGER_LINE_WIDTH = 2.5f;

	private static final String BONEMERANG_ID = "STARRED_BONE_BOOMERANG";
	private static final String ATOMSPLIT_ID = "ATOMSPLIT_KATANA";
	private static final String ENDSTONE_ID = "END_STONE_SWORD";
	private static final String PEARL_ID = "ENDER_PEARL";
	private static final double PEARL_TARGET_X = -101.0D;
	private static final double PEARL_TARGET_Y = 6.0D;
	private static final double PEARL_TARGET_Z = -107.0D;

	private static boolean initialized;
	private static boolean sequenceRunning;
	private static int sequenceGeneration;
	private static boolean sequenceUsedThisWorld;
	private static int worldLoadResetTicksRemaining;
	private static boolean armorSwapEnabled;
	private static boolean armorSwapRequested;
	private static boolean armorSwapResolved;
	private static boolean armorSwapSucceeded;
	private static boolean armorSwapOutcomeLogged;

	private AutoRend() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			worldLoadResetTicksRemaining = WORLD_LOAD_RESET_WINDOW_TICKS;
			tryResetForKuudraWorldLoad();
		});

		ClientTickEvents.END_CLIENT_TICK.register(AutoRend::handleClientTick);
		WorldRenderEvents.END_MAIN.register(AutoRend::renderActivationZones);
		FakeKeybinds.onKeyPress(UiDefinitions.AUTO_REND_OLD_TRIGGER_KEYBIND, AutoRend::triggerManual);
	}

	private static void handleClientTick(Minecraft client) {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND_HARDCODE.get())) {
			return;
		}

		if (worldLoadResetTicksRemaining > 0) {
			worldLoadResetTicksRemaining--;
			tryResetForKuudraWorldLoad();
		}

		if (client == null || client.player == null || client.level == null) {
			return;
		}

		if (sequenceRunning || sequenceUsedThisWorld) {
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

		startSequence();
	}

	private static void triggerManual() {
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND_HARDCODE.get())) {
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

		startSequence();
	}

	private static boolean isInDpsPhase() {
		return KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.PHASE_DPS;
	}

	// ── Rendering ──────────────────────────────────────────────────────────

	private static void renderActivationZones(WorldRenderContext context) {
		if (context == null || context.matrices() == null || context.consumers() == null) {
			return;
		}
		if (!Boolean.TRUE.equals(UiDefinitions.AUTO_REND_HARDCODE.get())) {
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
				.withLocation("moissanite/auto_rend_old_trigger_lines")
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
		return RenderTypeAccessor.moissanite$invokeCreate("moissanite_auto_rend_old_trigger_lines", setup);
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

	// ── Sequence ───────────────────────────────────────────────────────────

	private static void startSequence() {
		if (sequenceRunning) {
			return;
		}
		sequenceRunning = true;
		sequenceUsedThisWorld = true;
		sequenceGeneration++;
		int currentGeneration = sequenceGeneration;
		sendAutoRendMessage("Sequence started.");

		scheduleStep(currentGeneration, SEQUENCE_TIMEOUT_TICKS, () -> {
			sendAutoRendMessage("Sequence timeout, resetting state.");
			sequenceRunning = false;
		});
		scheduleStep(currentGeneration, 1, AutoRend::stepSwapBonemerang);
	}

	private static void stepSwapBonemerang() {
		int slot = UiDefinitions.AUTO_REND_BONEMERANG.get().intValue();
		boolean swapped = HotbarItemSearch.swapHeldItem(slot);
		sendAutoRendMessage("Swap to Bonemerang (slot " + slot + "): " + actionStatus(swapped));

		scheduleStep(sequenceGeneration, 1, () -> verifyHeldItem("Verify Bonemerang equipped", BONEMERANG_ID));
		scheduleStep(sequenceGeneration, 2, AutoRend::stepUseBonemerang);
	}

	private static void stepUseBonemerang() {
		boolean usedBonemerang = PlayerInputActions.rightClick();
		sendAutoRendMessage("Right click Bonemerang: " + actionStatus(usedBonemerang));
		boolean jumped = PlayerInputActions.jump();
		sendAutoRendMessage("Jump: " + actionStatus(jumped));

		scheduleStep(sequenceGeneration, 4, AutoRend::stepSwapAtomsplit);
	}

	private static void stepSwapAtomsplit() {
		int slot = UiDefinitions.AUTO_REND_ATOMSPLIT.get().intValue();
		boolean swapped = HotbarItemSearch.swapHeldItem(slot);
		int armorSlot = UiDefinitions.AUTO_REND_SWAP_ARMOR.get().intValue();
		armorSwapEnabled = armorSlot >= 1 && armorSlot <= 9;
		sendAutoRendMessage("Swap to Atomsplit (slot " + slot + "): " + actionStatus(swapped));

		scheduleStep(sequenceGeneration, 1, () -> verifyHeldItem("Verify Atomsplit equipped", ATOMSPLIT_ID));
		armorSwapRequested = AutoRendHelper.WDSwapSlot(armorSlot, success -> {
			armorSwapResolved = true;
			armorSwapSucceeded = success;
		});
		scheduleStep(sequenceGeneration, 26, AutoRend::stepSwapEndstone);
	}

	private static void stepSwapEndstone() {
		int slot = UiDefinitions.AUTO_REND_ENDSTONE.get().intValue();
		boolean swapped = HotbarItemSearch.swapHeldItem(slot);
		sendAutoRendMessage("Swap to Endstone (slot " + slot + "): " + actionStatus(swapped));

		scheduleStep(sequenceGeneration, 1, () -> verifyHeldItem("Verify Endstone equipped", ENDSTONE_ID));
		scheduleStep(sequenceGeneration, 3, AutoRend::stepUseEndstone);
	}

	private static void stepUseEndstone() {
		boolean usedEndstone = PlayerInputActions.rightClick();
		sendAutoRendMessage("Right click Endstone: " + actionStatus(usedEndstone));

		scheduleStep(sequenceGeneration, 4, AutoRend::stepSwapBonemerangBack);
	}

	private static void stepSwapBonemerangBack() {
		int slot = UiDefinitions.AUTO_REND_BONEMERANG.get().intValue();
		boolean swapped = HotbarItemSearch.swapHeldItem(slot);
		sendAutoRendMessage("Swap back to Bonemerang (slot " + slot + "): " + actionStatus(swapped));

		scheduleStep(sequenceGeneration, 1, () -> verifyHeldItem("Verify Bonemerang re-equipped", BONEMERANG_ID));
		scheduleStep(sequenceGeneration, 3, AutoRend::stepPullBonemerang);
	}

	private static void stepPullBonemerang() {
		boolean pull = PlayerInputActions.leftClick();
		sendAutoRendMessage("Left click pull: " + actionStatus(pull));

		scheduleStep(sequenceGeneration, 2, AutoRend::stepPullBonemerangAgain);
	}

	private static void stepPullBonemerangAgain() {
		boolean pull = PlayerInputActions.leftClick();
		sendAutoRendMessage("Left click pull: " + actionStatus(pull));

		scheduleStep(sequenceGeneration, 3, AutoRend::stepRotateToPearlPoint);
	}

	private static void stepRotateToPearlPoint() {
		double multiplier = UiDefinitions.AUTO_REND_ROTATION_MULTIPLIER.get();
		boolean rotated = RotationController.rotateTo(PEARL_TARGET_X, PEARL_TARGET_Y, PEARL_TARGET_Z, multiplier);
		sendAutoRendMessage("Rotate to pearl point: " + actionStatus(rotated));

		scheduleStep(sequenceGeneration, 9, AutoRend::stepSwapPearls);
	}

	private static void stepSwapPearls() {
		int slot = UiDefinitions.AUTO_REND_PEARLS.get().intValue();
		boolean swapped = HotbarItemSearch.swapHeldItem(slot);
		sendAutoRendMessage("Swap to Pearls (slot " + slot + "): " + actionStatus(swapped));

		scheduleStep(sequenceGeneration, 4, () -> verifyHeldItem("Verify Pearls equipped", PEARL_ID));
		scheduleStep(sequenceGeneration, 9, AutoRend::stepThrowPearl);
	}

	private static void stepThrowPearl() {
		boolean thrown = PlayerInputActions.rightClick();
		sendAutoRendMessage("Right click Pearl: " + actionStatus(thrown));
		sendAutoRendMessage("Sequence finished.");
		sequenceRunning = false;
	}

	private static void verifyHeldItem(String message, String itemId) {
		boolean equipped = HeldItemMatcher.heldMatchesSkyblockId(itemId);
		sendAutoRendMessage(message + ": " + actionStatus(equipped));
	}

	private static void scheduleStep(int generation, int ticks, Runnable action) {
		TickTaskScheduler.schedule(ticks, () -> {
			if (!sequenceRunning || generation != sequenceGeneration || action == null) {
				return;
			}
			action.run();
		});
	}

	private static void tryResetForKuudraWorldLoad() {
		if (worldLoadResetTicksRemaining <= 0) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		sequenceUsedThisWorld = false;
		sequenceRunning = false;
		worldLoadResetTicksRemaining = 0;
	}

	// ── Utility ────────────────────────────────────────────────────────────

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
			sendScanMessage("Auto Rend OLD scan: no matching items found in hotbar.");
		}
	}

	private static int scanItemAndUpdateSlider(String itemName, String nbtSuffix, UiSlider slider) {
		int slot = HotbarItemSearch.findFirstHotbarSlotByNbt(nbtSuffix);
		HotbarItemSearch.setSliderFromSlot(slider, slot);
		if (slot < 0) {
			return 0;
		}
		sendScanMessage("Auto Rend OLD scan: " + itemName + " found in slot " + slot + ".");
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
		FeatureChat.sendPrefixed("Auto Rend OLD", text);
	}

	private static String actionStatus(boolean success) {
		return success ? "ok" : "failed";
	}
}
