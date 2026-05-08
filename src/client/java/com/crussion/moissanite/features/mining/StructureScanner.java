package com.crussion.moissanite.features.mining;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.render.WorldTextRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class StructureScanner {
	private static final int X_A = 513;
	private static final int Z_A = 513;
	private static final int X_B = 823;
	private static final int Z_B = 200;
	private static final int X_MIN = Math.min(X_A, X_B);
	private static final int X_MAX = Math.max(X_A, X_B);
	private static final int Z_MIN = Math.min(Z_A, Z_B);
	private static final int Z_MAX = Math.max(Z_A, Z_B);
	private static final int MIN_CHUNK_X = X_MIN >> 4;
	private static final int MAX_CHUNK_X = X_MAX >> 4;
	private static final int MIN_CHUNK_Z = Z_MIN >> 4;
	private static final int MAX_CHUNK_Z = Z_MAX >> 4;
	private static final int DRAGON_LAIR_WAYPOINT_COLOR = 0xFFFFAA00;
	private static final int MINES_OF_DIVAN_WAYPOINT_COLOR = 0xFF55FF55;
	private static final int WAYPOINT_BACKGROUND_COLOR = 0xAA000000;
	private static final float WAYPOINT_MIN_TEXT_SCALE = 1.5F;
	private static final float WAYPOINT_MAX_TEXT_SCALE = 20.0F;
	private static final float WAYPOINT_DISTANCE_SCALE_DIVISOR = 14.0F;
	private static final int MAX_CHUNKS_PER_RENDER = 8;
	private static final int[][] MINES_OF_DIVAN_LIME_WOOL_PATTERN = {
			{ 3, 0, 0 },
			{ 3, 1, 0 },
			{ 2, 2, -2 },
			{ 2, 1, -2 },
			{ 0, 1, -3 },
			{ 0, 0, -3 },
			{ -2, 2, -2 },
			{ -2, 1, -2 },
			{ -3, 1, 0 },
			{ -3, 0, 0 },
			{ -2, 2, 2 },
			{ -2, 1, 2 },
			{ 0, 1, 3 },
			{ 0, 0, 3 },
			{ 2, 2, 2 },
			{ 2, 1, 2 }
	};
	private static final List<Long> SCAN_CHUNKS = createScanChunks();
	private static final Direction[] HORIZONTAL_DIRECTIONS = {
			Direction.NORTH,
			Direction.SOUTH,
			Direction.WEST,
			Direction.EAST
	};
	private static final List<StructureScan> STRUCTURES = createStructures();

	private static boolean initialized;
	private static String activeServer = "";

	private StructureScanner() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(StructureScanner::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetForServer(""));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetForServer(""));
		WorldRenderEvents.END_MAIN.register(StructureScanner::onWorldRender);
	}

	private static void onClientTick(Minecraft client) {
		if (!isAnyStructureScannerEnabled()) {
			return;
		}
		if (client == null || client.player == null || client.level == null) {
			return;
		}

		SkyBlockLocationTracker.requestRefreshIfNeeded();
		updateServerResetState();
	}

	private static void onWorldRender(WorldRenderContext context) {
		if (context == null || context.matrices() == null) {
			return;
		}
		if (!isAnyStructureScannerEnabled()) {
			return;
		}
		if (!SkyBlockLocationTracker.isInCrystalHollows()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || client.gameRenderer == null || client.levelRenderer == null) {
			return;
		}

		updateServerResetState();
		scanAvailableChunks(client);
		renderWaypoints(context, client);
	}

	private static boolean isDragonLairScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_DRAGON_LAIR.get());
	}

	private static boolean isMinesOfDivanScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_MINES_OF_DIVAN.get());
	}

	private static boolean isAnyStructureScannerEnabled() {
		for (StructureScan structure : STRUCTURES) {
			if (structure.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	private static void scanAvailableChunks(Minecraft client) {
		for (StructureScan structure : STRUCTURES) {
			if (!structure.isEnabled() || structure.foundWaypoint != null) {
				continue;
			}
			scanAvailableChunks(client, structure);
		}
	}

	private static void scanAvailableChunks(Minecraft client, StructureScan structure) {
		if (structure.foundWaypoint != null) {
			return;
		}

		int scannedThisRender = 0;
		for (long chunkKey : orderedScanChunks(client)) {
			if (structure.scannedChunks.contains(chunkKey)) {
				continue;
			}

			int chunkX = ChunkPos.getX(chunkKey);
			int chunkZ = ChunkPos.getZ(chunkKey);
			LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
			if (chunk == null) {
				continue;
			}

			structure.scannedChunks.add(chunkKey);
			BlockPos markerPos = scanChunkForMarker(client, chunk, structure);
			if (markerPos != null) {
				structure.foundMarkerPos = markerPos;
				structure.foundWaypoint = new Vec3(
						markerPos.getX() + 0.5D,
						markerPos.getY() + structure.waypointYOffset,
						markerPos.getZ() + 0.5D);
				FeatureChat.send("Found " + structure.name + " at " + markerPos.getX() + " " + markerPos.getY() + " " + markerPos.getZ());
				if (!structure.titleText.isBlank()) {
					showFoundTitle(client, structure.titleText);
				}
				return;
			}
			scannedThisRender++;
			if (scannedThisRender >= MAX_CHUNKS_PER_RENDER) {
				return;
			}
		}

		if (!structure.notFoundReported && structure.scannedChunks.size() >= SCAN_CHUNKS.size()) {
			structure.notFoundReported = true;
			FeatureChat.send(structure.notFoundText);
		}
	}

	private static BlockPos scanChunkForMarker(Minecraft client, LevelChunk chunk, StructureScan structure) {
		ChunkPos chunkPos = chunk.getPos();
		int startX = Math.max(X_MIN, chunkPos.getMinBlockX());
		int endX = Math.min(X_MAX, chunkPos.getMinBlockX() + 15);
		int startZ = Math.max(Z_MIN, chunkPos.getMinBlockZ());
		int endZ = Math.min(Z_MAX, chunkPos.getMinBlockZ() + 15);
		int startY = client.level.getMinY();
		int endY = client.level.getMaxY() - 1;

		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				for (int y = startY; y <= endY; y++) {
					mutable.set(x, y, z);
					BlockState state = chunk.getBlockState(mutable);
					if (!structure.isAnchorBlock(state)) {
						continue;
					}
					BlockPos matchPos = structure.matchPosition(client, mutable, state);
					if (matchPos != null) {
						return matchPos;
					}
				}
			}
		}

		return null;
	}

	private static boolean matchesDragonLairMarker(Minecraft client, BlockPos pos, BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.SNOW_BLOCK && matchesSnowWoolRedstoneMarker(client, pos)) {
			return true;
		}
		if (isHeadBlock(block) && matchesHeadTerracottaMarker(client, pos)) {
			return true;
		}
		return block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE && matchesVerticalGlassMarker(client, pos);
	}

	private static BlockPos findMinesOfDivanMarker(Minecraft client, BlockPos anchorPos, BlockState state) {
		if (state == null || state.getBlock() != Blocks.LIME_WOOL) {
			return null;
		}
		if (client == null || client.level == null || anchorPos == null) {
			return null;
		}

		for (int[] anchorOffset : MINES_OF_DIVAN_LIME_WOOL_PATTERN) {
			int centerX = anchorPos.getX() - anchorOffset[0];
			int baseY = anchorPos.getY() - anchorOffset[1];
			int centerZ = anchorPos.getZ() - anchorOffset[2];
			if (matchesMinesOfDivanLimeWoolPatternAt(client, centerX, baseY, centerZ)) {
				return new BlockPos(centerX, baseY + 1, centerZ);
			}
		}
		return null;
	}

	private static boolean matchesMinesOfDivanLimeWoolPatternAt(Minecraft client, int centerX, int baseY, int centerZ) {
		for (int[] offset : MINES_OF_DIVAN_LIME_WOOL_PATTERN) {
			if (!isBlock(client, new BlockPos(centerX + offset[0], baseY + offset[1], centerZ + offset[2]), Blocks.LIME_WOOL)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesSnowWoolRedstoneMarker(Minecraft client, BlockPos snowPos) {
		if (!isBlock(client, snowPos.below(), Blocks.RED_WOOL)) {
			return false;
		}

		return hasSurroundingBlockWithAirAbove(client, snowPos, Blocks.REDSTONE_BLOCK);
	}

	private static boolean matchesHeadTerracottaMarker(Minecraft client, BlockPos headPos) {
		return isHeadBlock(client, headPos)
				&& isBlock(client, headPos.below(), Blocks.RED_TERRACOTTA)
				&& hasSurroundingBlockWithAirAbove(client, headPos, Blocks.RED_TERRACOTTA);
	}

	private static boolean matchesVerticalGlassMarker(Minecraft client, BlockPos topPos) {
		return isBlock(client, topPos, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE)
				&& isAir(client, topPos.below())
				&& isBlock(client, topPos.below(2), Blocks.LIGHT_GRAY_STAINED_GLASS_PANE)
				&& isAir(client, topPos.below(3))
				&& isBlock(client, topPos.below(4), Blocks.GRAY_STAINED_GLASS_PANE)
				&& isAir(client, topPos.below(5))
				&& isBlock(client, topPos.below(6), Blocks.GRAY_STAINED_GLASS_PANE)
				&& isBlock(client, topPos.below(7), Blocks.BLACK_STAINED_GLASS)
				&& isBlock(client, topPos.below(8), Blocks.BLACK_STAINED_GLASS_PANE);
	}

	private static boolean isDragonLairMarkerAnchorBlock(BlockState state) {
		if (state == null) {
			return false;
		}

		Block block = state.getBlock();
		return block == Blocks.SNOW_BLOCK
				|| isHeadBlock(block)
				|| block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE;
	}

	private static boolean isMinesOfDivanMarkerAnchorBlock(BlockState state) {
		return state != null && state.getBlock() == Blocks.LIME_WOOL;
	}

	private static boolean isHeadBlock(Minecraft client, BlockPos pos) {
		if (client == null || client.level == null || pos == null) {
			return false;
		}
		return isHeadBlock(client.level.getBlockState(pos).getBlock());
	}

	private static boolean isHeadBlock(Block block) {
		return block == Blocks.PLAYER_HEAD || block == Blocks.PLAYER_WALL_HEAD;
	}

	private static boolean hasSurroundingBlockWithAirAbove(Minecraft client, BlockPos anchorPos, Block block) {
		if (client == null || client.level == null || anchorPos == null || block == null) {
			return false;
		}

		BlockPos basePos = anchorPos.below();
		for (Direction direction : HORIZONTAL_DIRECTIONS) {
			BlockPos markerPos = basePos.relative(direction);
			if (isBlock(client, markerPos, block) && isAir(client, markerPos.above())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBlock(Minecraft client, BlockPos pos, Block block) {
		if (client == null || client.level == null || pos == null || block == null) {
			return false;
		}
		return client.level.getBlockState(pos).getBlock() == block;
	}

	private static boolean isAir(Minecraft client, BlockPos pos) {
		return client != null && client.level != null && pos != null && client.level.getBlockState(pos).isAir();
	}

	private static List<Long> orderedScanChunks(Minecraft client) {
		if (client == null || client.player == null) {
			return SCAN_CHUNKS;
		}

		int playerChunkX = Mth.floor(client.player.getX()) >> 4;
		int playerChunkZ = Mth.floor(client.player.getZ()) >> 4;
		List<Long> ordered = new ArrayList<>(SCAN_CHUNKS);
		ordered.sort((left, right) -> Long.compare(
				chunkDistanceSquared(left, playerChunkX, playerChunkZ),
				chunkDistanceSquared(right, playerChunkX, playerChunkZ)));
		return ordered;
	}

	private static long chunkDistanceSquared(long chunkKey, int centerChunkX, int centerChunkZ) {
		long dx = ChunkPos.getX(chunkKey) - centerChunkX;
		long dz = ChunkPos.getZ(chunkKey) - centerChunkZ;
		return dx * dx + dz * dz;
	}

	private static void renderWaypoints(WorldRenderContext context, Minecraft client) {
		for (StructureScan structure : STRUCTURES) {
			if (!structure.isEnabled() || structure.foundWaypoint == null) {
				continue;
			}
			renderWaypoint(context, client, structure);
		}
	}

	private static void renderWaypoint(WorldRenderContext context, Minecraft client, StructureScan structure) {
		WorldTextRenderer.drawText(
				context,
				new Vec3(structure.foundWaypoint.x, structure.foundWaypoint.y + 2.5D, structure.foundWaypoint.z),
				waypointLabel(client, structure),
				structure.waypointColor,
				waypointTextScale(client, structure),
				true,
				0.0F,
				WAYPOINT_BACKGROUND_COLOR);
	}

	private static String waypointLabel(Minecraft client, StructureScan structure) {
		if (client == null || client.player == null || structure.foundWaypoint == null) {
			return structure.name;
		}

		int distance = Math.max(0, Math.round((float) client.player.position().distanceTo(structure.foundWaypoint)));
		return structure.name + " (" + distance + ")";
	}

	private static float waypointTextScale(Minecraft client, StructureScan structure) {
		if (client == null || client.player == null || structure.foundWaypoint == null) {
			return WAYPOINT_MIN_TEXT_SCALE;
		}

		float distance = (float) client.player.position().distanceTo(structure.foundWaypoint);
		return Mth.clamp(distance / WAYPOINT_DISTANCE_SCALE_DIVISOR, WAYPOINT_MIN_TEXT_SCALE, WAYPOINT_MAX_TEXT_SCALE);
	}

	private static void showFoundTitle(Minecraft client, String titleText) {
		if (client == null || client.gui == null) {
			return;
		}

		client.gui.setTimes(5, 40, 10);
		client.gui.setSubtitle(Component.empty());
		client.gui.setTitle(Component.literal(titleText).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	private static void updateServerResetState() {
		String server = SkyBlockLocationTracker.currentLocation().server();
		if (server == null || server.isBlank()) {
			return;
		}
		if (activeServer.isBlank()) {
			activeServer = server;
			return;
		}
		if (!activeServer.equals(server)) {
			resetForServer(server);
		}
	}

	private static void resetForServer(String server) {
		activeServer = server == null ? "" : server;
		for (StructureScan structure : STRUCTURES) {
			structure.reset();
		}
	}

	private static List<StructureScan> createStructures() {
		return List.of(
				new StructureScan(
						"Dragon Lair",
						"No dragon lair was found.",
						"Dragon Lair found!",
						DRAGON_LAIR_WAYPOINT_COLOR,
						1.5D,
						StructureScanner::isDragonLairScannerEnabled,
						StructureScanner::isDragonLairMarkerAnchorBlock,
						(client, pos, state) -> matchesDragonLairMarker(client, pos, state) ? pos.immutable() : null),
				new StructureScan(
						"Mines Of Divan",
						"No Mines Of Divan was found.",
						"",
						MINES_OF_DIVAN_WAYPOINT_COLOR,
						0.5D,
						StructureScanner::isMinesOfDivanScannerEnabled,
						StructureScanner::isMinesOfDivanMarkerAnchorBlock,
						StructureScanner::findMinesOfDivanMarker));
	}

	private static List<Long> createScanChunks() {
		List<Long> chunks = new ArrayList<>();
		for (int chunkX = MIN_CHUNK_X; chunkX <= MAX_CHUNK_X; chunkX++) {
			for (int chunkZ = MIN_CHUNK_Z; chunkZ <= MAX_CHUNK_Z; chunkZ++) {
				chunks.add(ChunkPos.asLong(chunkX, chunkZ));
			}
		}
		return List.copyOf(chunks);
	}

	@FunctionalInterface
	private interface AnchorMatcher {
		boolean matches(BlockState state);
	}

	@FunctionalInterface
	private interface StructureMatcher {
		BlockPos matchPosition(Minecraft client, BlockPos pos, BlockState state);
	}

	private static final class StructureScan {
		private final String name;
		private final String notFoundText;
		private final String titleText;
		private final int waypointColor;
		private final double waypointYOffset;
		private final BooleanSupplier enabled;
		private final AnchorMatcher anchorMatcher;
		private final StructureMatcher structureMatcher;
		private final Set<Long> scannedChunks = new HashSet<>();

		private boolean notFoundReported;
		private BlockPos foundMarkerPos;
		private Vec3 foundWaypoint;

		private StructureScan(
				String name,
				String notFoundText,
				String titleText,
				int waypointColor,
				double waypointYOffset,
				BooleanSupplier enabled,
				AnchorMatcher anchorMatcher,
				StructureMatcher structureMatcher) {
			this.name = name;
			this.notFoundText = notFoundText;
			this.titleText = titleText;
			this.waypointColor = waypointColor;
			this.waypointYOffset = waypointYOffset;
			this.enabled = enabled;
			this.anchorMatcher = anchorMatcher;
			this.structureMatcher = structureMatcher;
		}

		private boolean isEnabled() {
			return enabled.getAsBoolean();
		}

		private boolean isAnchorBlock(BlockState state) {
			return anchorMatcher.matches(state);
		}

		private BlockPos matchPosition(Minecraft client, BlockPos pos, BlockState state) {
			return structureMatcher.matchPosition(client, pos, state);
		}

		private void reset() {
			notFoundReported = false;
			foundMarkerPos = null;
			foundWaypoint = null;
			scannedChunks.clear();
		}
	}
}
