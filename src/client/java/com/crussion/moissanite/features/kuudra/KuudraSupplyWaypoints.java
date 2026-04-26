package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.mixin.client.RenderTypeAccessor;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class KuudraSupplyWaypoints {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final String SUPPLY_READY_MARKER = "bring supply chest here";
	private static final String SUPPLY_RECEIVED_MARKER = "supplies received";
	private static final String SUPPLY_PROGRESS_MARKER = "progress:";
	private static final String SUPPLY_COMPLETE_MARKER = "complete";
	private static final double SUPPLY_ZOMBIE_DISTANCE_SQ = 9.0D;
	private static final int BEAM_HEIGHT = 150;
	private static final float BOX_LINE_WIDTH = 2.5F;

	private static final int CRATE_COLOR = 0xFF00D6FF;
	private static final int BUILD_EMPTY_COLOR = 0xFFFF5555;
	private static final int BUILD_PROGRESS_COLOR = 0xFFFFCC55;
	private static RenderType waypointBoxRenderType;

	private static final List<Vec3> CRATES = new ArrayList<>();
	private static final List<AABB> CRATE_HITBOXES = new ArrayList<>();
	private static final Map<SupplySpot, SupplyState> SUPPLY_STATES = new EnumMap<>(SupplySpot.class);
	private static boolean initialized;

	static {
		clearSupplyStates();
	}

	private KuudraSupplyWaypoints() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(KuudraSupplyWaypoints::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> clearTrackedData());
		WorldRenderEvents.END_MAIN.register(KuudraSupplyWaypoints::onWorldRender);
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.level == null || client.player == null) {
			clearTrackedData();
			return;
		}
		if (!isAnyToggleEnabled() || !ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			clearTrackedData();
			return;
		}

		int phase = KuudraPhaseTracker.getPhase();

		if (Boolean.TRUE.equals(UiDefinitions.KUUDRA_CRATE_WAYPOINTS.get())
				&& (phase == KuudraPhaseTracker.PHASE_SUPPLY || phase == KuudraPhaseTracker.PHASE_EATEN)) {
			updateCratesAndHitboxes(client);
		} else {
			CRATES.clear();
			CRATE_HITBOXES.clear();
		}

		if (Boolean.TRUE.equals(UiDefinitions.KUUDRA_BALLISTA_BUILD_WAYPOINTS.get())
				&& phase == KuudraPhaseTracker.PHASE_BUILD) {
			updateSupplyStates(client);
		} else {
			clearSupplyStates();
		}
	}

	private static void onWorldRender(WorldRenderContext context) {
		if (context == null || context.matrices() == null || context.consumers() == null || context.commandQueue() == null) {
			return;
		}
		if (!isAnyToggleEnabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || client.player == null || client.gameRenderer == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}

		Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
		int phase = KuudraPhaseTracker.getPhase();

		if (Boolean.TRUE.equals(UiDefinitions.KUUDRA_CRATE_WAYPOINTS.get())
				&& (phase == KuudraPhaseTracker.PHASE_SUPPLY || phase == KuudraPhaseTracker.PHASE_EATEN)) {
			for (Vec3 crate : CRATES) {
				renderBeaconBeam(context, cameraPos, crate, CRATE_COLOR);
			}
			for (AABB hitbox : CRATE_HITBOXES) {
				renderBox(context, cameraPos, hitbox, CRATE_COLOR);
			}
		}

		if (Boolean.TRUE.equals(UiDefinitions.KUUDRA_BALLISTA_BUILD_WAYPOINTS.get())
				&& phase == KuudraPhaseTracker.PHASE_BUILD) {
			for (SupplySpot spot : SupplySpot.values()) {
				SupplyState state = SUPPLY_STATES.getOrDefault(spot, SupplyState.UNKNOWN);
				if (!shouldRenderBuildWaypoint(state)) {
					continue;
				}

				int color = colorForBuildState(state);
				Vec3 location = spot.location();
				renderBeaconBeam(context, cameraPos, location, color);
				renderBox(context, cameraPos, boxAround(location, 1.15D), color);
			}
		}
	}

	private static void updateCratesAndHitboxes(Minecraft client) {
		CRATES.clear();
		CRATE_HITBOXES.clear();

		List<AABB> zombieBoxes = new ArrayList<>();

		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof Giant) {
				if (entity.getY() >= 67.0D) {
					continue;
				}

				double yawRad = Math.toRadians(entity.getYRot() + 130.0F);
				double offsetX = 3.7D * Math.cos(yawRad);
				double offsetZ = 3.7D * Math.sin(yawRad);
				double x = entity.getX() + 0.5D + offsetX;
				double z = entity.getZ() + 0.5D + offsetZ;
				CRATES.add(new Vec3(x, 75.0D, z));
				continue;
			}

			if (!(entity instanceof Zombie zombie) || !zombie.isInvisible()) {
				continue;
			}
			double y = zombie.getY();
			if (y < 72.0D || y > 78.0D) {
				continue;
			}

			AABB box = zombie.getBoundingBox();
			if (!isValidHitbox(box)) {
				continue;
			}
			zombieBoxes.add(box);
		}

		for (Vec3 crate : CRATES) {
			AABB merged = mergeNearbyHitboxes(crate, zombieBoxes);
			CRATE_HITBOXES.add(merged != null ? merged : boxAround(crate, 1.0D));
		}
	}

	private static void updateSupplyStates(Minecraft client) {
		clearSupplyStates();

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

	private static boolean shouldRenderBuildWaypoint(SupplyState state) {
		return state == SupplyState.NOTHING || state == SupplyState.RECEIVED || state == SupplyState.IN_PROGRESS;
	}

	private static int colorForBuildState(SupplyState state) {
		return state == SupplyState.NOTHING ? BUILD_EMPTY_COLOR : BUILD_PROGRESS_COLOR;
	}

	private static void renderBeaconBeam(WorldRenderContext context, Vec3 cameraPos, Vec3 worldPos, int color) {
		float animationTime = resolveBeamAnimationTime();
		context.matrices().pushPose();
		context.matrices().translate(
				worldPos.x - cameraPos.x - 0.5D,
				worldPos.y - cameraPos.y,
				worldPos.z - cameraPos.z - 0.5D);
		BeaconRenderer.submitBeaconBeam(
				context.matrices(),
				context.commandQueue(),
				BeaconRenderer.BEAM_LOCATION,
				1.0F,
				animationTime,
				0,
				BEAM_HEIGHT,
				color,
				BeaconRenderer.SOLID_BEAM_RADIUS,
				BeaconRenderer.BEAM_GLOW_RADIUS);
		context.matrices().popPose();
	}

	private static void renderBox(WorldRenderContext context, Vec3 cameraPos, AABB box, int color) {
		ShapeRenderer.renderShape(
				context.matrices(),
				context.consumers().getBuffer(getWaypointBoxRenderType()),
				Shapes.create(box),
				-cameraPos.x,
				-cameraPos.y,
				-cameraPos.z,
				color,
				BOX_LINE_WIDTH);
	}

	private static float resolveBeamAnimationTime() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return 0.0F;
		}
		return (float) (client.level.getGameTime() % 40L);
	}

	private static AABB mergeNearbyHitboxes(Vec3 center, List<AABB> boxes) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (AABB box : boxes) {
			double boxCenterX = (box.minX + box.maxX) * 0.5D;
			double boxCenterY = (box.minY + box.maxY) * 0.5D;
			double boxCenterZ = (box.minZ + box.maxZ) * 0.5D;

			double dx = boxCenterX - center.x;
			double dy = boxCenterY - center.y;
			double dz = boxCenterZ - center.z;
			if ((dx * dx) + (dy * dy) + (dz * dz) > SUPPLY_ZOMBIE_DISTANCE_SQ) {
				continue;
			}

			minX = Math.min(minX, box.minX);
			minY = Math.min(minY, box.minY);
			minZ = Math.min(minZ, box.minZ);
			maxX = Math.max(maxX, box.maxX);
			maxY = Math.max(maxY, box.maxY);
			maxZ = Math.max(maxZ, box.maxZ);
		}

		if (!Double.isFinite(minX)) {
			return null;
		}
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static boolean isValidHitbox(AABB box) {
		if (box == null) {
			return false;
		}
		double width = box.maxX - box.minX;
		double height = box.maxY - box.minY;
		double depth = box.maxZ - box.minZ;
		if (width > 10.0D || height > 10.0D || depth > 10.0D) {
			return false;
		}
		return Double.isFinite(box.minX)
				&& Double.isFinite(box.maxX)
				&& Double.isFinite(box.minY)
				&& Double.isFinite(box.maxY)
				&& Double.isFinite(box.minZ)
				&& Double.isFinite(box.maxZ);
	}

	private static AABB boxAround(Vec3 center, double size) {
		double half = size * 0.5D;
		return new AABB(
				center.x - half,
				center.y - half,
				center.z - half,
				center.x + half,
				center.y + half,
				center.z + half);
	}

	private static void clearTrackedData() {
		CRATES.clear();
		CRATE_HITBOXES.clear();
		clearSupplyStates();
	}

	private static void clearSupplyStates() {
		for (SupplySpot spot : SupplySpot.values()) {
			SUPPLY_STATES.put(spot, SupplyState.UNKNOWN);
		}
	}

	private static boolean isAnyToggleEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.KUUDRA_CRATE_WAYPOINTS.get())
				|| Boolean.TRUE.equals(UiDefinitions.KUUDRA_BALLISTA_BUILD_WAYPOINTS.get());
	}

	private static RenderType getWaypointBoxRenderType() {
		if (waypointBoxRenderType == null) {
			waypointBoxRenderType = createWaypointBoxRenderType();
		}
		return waypointBoxRenderType;
	}

	private static RenderType createWaypointBoxRenderType() {
		RenderPipeline source = RenderPipelines.LINES;
		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation("moissanite/kuudra_waypoint_boxes")
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
		return RenderType.create("moissanite_kuudra_waypoint_boxes", setup);
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

	private enum SupplyState {
		UNKNOWN,
		NOTHING,
		RECEIVED,
		IN_PROGRESS,
		COMPLETED
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

		private static SupplySpot fromEntityPosition(double x, double z) {
			int castX = (int) x;
			int castZ = (int) z;
			for (SupplySpot spot : values()) {
				if (spot.intX == castX && spot.intZ == castZ) {
					return spot;
				}
			}
			return null;
		}
	}
}
