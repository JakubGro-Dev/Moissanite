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
	private static final ScanArea CRYSTAL_HOLLOWS = ScanArea.between(200, 200, 823, 832);
	private static final ScanArea MITHRIL_DEPOSIT = ScanArea.between(513, 513, 823, 200);
	private static final ScanArea GOBLIN_HIDEOUT = ScanArea.between(513, 513, 200, 823);
	private static final ScanArea PRECURSOR_REMNANTS = ScanArea.between(513, 513, 823, 832);
	private static final ScanArea JUNGLE = ScanArea.between(513, 513, 200, 200);
	private static final ScanArea NUCLEUS = ScanArea.between(561, 561, 465, 465);
	private static final int DRAGON_LAIR_WAYPOINT_COLOR = 0xFFFFAA00;
	private static final int MINES_OF_DIVAN_WAYPOINT_COLOR = 0xFF55FF55;
	private static final int PRECURSOR_CITY_WAYPOINT_COLOR = 0xFF55AAFF;
	private static final int GOBLIN_KING_WAYPOINT_COLOR = 0xFFFFAA00;
	private static final int GOBLIN_QUEEN_WAYPOINT_COLOR = 0xFFFFAA00;
	private static final int JUNGLE_TEMPLE_WAYPOINT_COLOR = 0xFFAA00AA;
	private static final int GROTTO_WAYPOINT_COLOR = 0xFFFF55FF;
	private static final int CORLEONE_WAYPOINT_COLOR = 0xFFAAAAAA;
	private static final int BAL_WAYPOINT_COLOR = 0xFFFF5555;
	private static final int WAYPOINT_BACKGROUND_COLOR = 0xAA000000;
	private static final float WAYPOINT_MIN_TEXT_SCALE = 1.5F;
	private static final float WAYPOINT_MAX_TEXT_SCALE = 20.0F;
	private static final float WAYPOINT_DISTANCE_SCALE_DIVISOR = 14.0F;
	private static final int MAX_CHUNKS_PER_RENDER = 8;
	private static final int GROTTO_SEARCH_HALF_SIZE = 7;
	private static final int GROTTO_SEARCH_OVERLAP_DISTANCE = GROTTO_SEARCH_HALF_SIZE * 2;
	private static final double GROTTO_WAYPOINT_Y_OFFSET = 8.0D;
	private static final int[][] JUNGLE_TEMPLE_STAIR_SIGNATURE = {
		{ -2, -2 },
		{ -2, -1 },
		{ -2, 1 },
		{ -2, 2 },
		{ -1, -2 },
		{ -1, -1 },
		{ -1, 1 },
		{ -1, 2 },
		{ 1, -2 },
		{ 1, -1 },
		{ 1, 1 },
		{ 1, 2 },
		{ 2, -2 },
		{ 2, -1 },
		{ 2, 1 },
		{ 2, 2 }
	};
	private static final int[][] BAL_COBBLESTONE_WALL_OFFSETS = {
		{ 0, 0, 0 },
		{ 0, 1, 0 },
		{ 0, 2, 0 },
		{ 0, 3, 0 },
		{ 0, 3, -1 },
		{ 0, 4, -1 },
		{ 0, 5, -1 },
		{ -1, 4, 0 },
		{ -1, 5, 0 },
		{ -1, 6, 0 }
	};

	private static final int[][] BAL_COBBLESTONE_OFFSETS = {
		{ 0, 4, 0 },
		{ 0, 5, 0 },
		{ 0, 6, 0 },
		{ 0, 6, -1 },
		{ 0, 7, -1 }
	};
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
	private static final int[][] BAL_ANCHOR_OFFSETS = {
		{ 0, 0, 0 },
		{ 0, 1, 0 },
		{ 0, 2, 0 },
		{ 0, 3, 0 },
		{ 0, 3, -1 },
		{ 0, 4, -1 },
		{ 0, 5, -1 },
		{ -1, 4, 0 },
		{ -1, 5, 0 },
		{ -1, 6, 0 }
	};
	private static final Direction[] HORIZONTAL_DIRECTIONS = {
			Direction.NORTH,
			Direction.SOUTH,
			Direction.WEST,
			Direction.EAST
	};
	private static final List<ScanChunk> SCAN_CHUNKS = createScanChunks(List.of(CRYSTAL_HOLLOWS));
	private static final List<ScanChunk> CORLEONE_SCAN_CHUNKS = createScanChunks(List.of(MITHRIL_DEPOSIT));
	private static final List<StructureScan> STRUCTURES = createStructures();
	private static final Set<Long> SCANNED_CHUNKS = new HashSet<>();
	private static final List<GrottoCluster> GROTTO_CLUSTERS = new ArrayList<>();
	private static final Set<BlockPos> CORLEONE_MARKERS = new HashSet<>();

	private static boolean initialized;
	private static String activeServer = "";
	private static boolean corleoneNotFoundReported;

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
		renderGrottoWaypoints(context, client);
		renderCorleoneWaypoints(context, client);
	}

	private static boolean isDragonLairScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_DRAGON_LAIR.get());
	}

	private static boolean isMinesOfDivanScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_MINES_OF_DIVAN.get());
	}

	private static boolean isBluePrecursorCityScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_BLUE_PRECURSOR_CITY.get());
	}

	private static boolean isGoblinKingScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_GOBLIN_KING.get());
	}

	private static boolean isGoblinQueenScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_GOBLIN_QUEEN.get());
	}

	private static boolean isJungleTempleScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_JUNGLE_TEMPLE.get());
	}

	private static boolean isGrottoScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_GROTTO.get());
	}

	private static boolean isCorleoneScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_CORLEONE.get());
	}
	private static boolean isBalScannerEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER.get())
				&& Boolean.TRUE.equals(UiDefinitions.STRUCTURE_SCANNER_BAL.get());
	}

	private static boolean isAnyStructureScannerEnabled() {
		if (isGrottoScannerEnabled() || isCorleoneScannerEnabled()) {
			return true;
		}
		for (StructureScan structure : STRUCTURES) {
			if (structure.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	private static void scanAvailableChunks(Minecraft client) {
		if (!hasPendingScanWork()) {
			reportMissingStructures();
			return;
		}

		int scannedThisRender = 0;
		for (ScanChunk scanChunk : orderedScanChunks(client)) {
			if (SCANNED_CHUNKS.contains(scanChunk.chunkKey())) {
				continue;
			}
			List<StructureScan> structuresToScan = structuresToScan(scanChunk);
			boolean scanGrotto = shouldScanGrotto(scanChunk);
			boolean scanCorleone = shouldScanCorleone(scanChunk);
			if (structuresToScan.isEmpty() && !scanGrotto && !scanCorleone) {
				continue;
			}

			int chunkX = ChunkPos.getX(scanChunk.chunkKey());
			int chunkZ = ChunkPos.getZ(scanChunk.chunkKey());
			LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
			if (chunk == null) {
				continue;
			}

			SCANNED_CHUNKS.add(scanChunk.chunkKey());
			for (StructureScan structure : structuresToScan) {
				structure.scannedChunks.add(scanChunk.chunkKey());
			}

			scanChunkForMarkers(client, chunk, scanChunk.area(), structuresToScan, scanGrotto, scanCorleone);
			scannedThisRender++;
			if (scannedThisRender >= MAX_CHUNKS_PER_RENDER) {
				return;
			}
		}

		reportMissingStructures();
	}

	private static boolean hasPendingScanWork() {
		for (ScanChunk scanChunk : SCAN_CHUNKS) {
			if (SCANNED_CHUNKS.contains(scanChunk.chunkKey())) {
				continue;
			}
			if (!structuresToScan(scanChunk).isEmpty()
					|| shouldScanGrotto(scanChunk)
					|| shouldScanCorleone(scanChunk)) {
				return true;
			}
		}
		return false;
	}

	private static List<StructureScan> structuresToScan(ScanChunk scanChunk) {
		List<StructureScan> structuresToScan = new ArrayList<>();
		for (StructureScan structure : STRUCTURES) {
			if (!structure.isEnabled()
					|| structure.foundWaypoint != null
					|| !structure.containsScanChunk(scanChunk.chunkKey())) {
				continue;
			}
			structuresToScan.add(structure);
		}
		return structuresToScan;
	}

	private static boolean shouldScanGrotto(ScanChunk scanChunk) {
		if (!isGrottoScannerEnabled()) {
			return false;
		}
		return GROTTO_CLUSTERS.isEmpty() || GROTTO_CLUSTERS.get(0).couldExtendFrom(scanChunk);
	}

	private static boolean shouldScanCorleone(ScanChunk scanChunk) {
		return isCorleoneScannerEnabled() && containsChunk(CORLEONE_SCAN_CHUNKS, scanChunk.chunkKey());
	}

	private static void scanChunkForMarkers(
			Minecraft client,
			LevelChunk chunk,
			ScanArea area,
			List<StructureScan> structuresToScan,
			boolean scanGrotto,
			boolean scanCorleone) {
		ChunkPos chunkPos = chunk.getPos();
		int startX = Math.max(area.minX(), chunkPos.getMinBlockX());
		int endX = Math.min(area.maxX(), chunkPos.getMinBlockX() + 15);
		int startZ = Math.max(area.minZ(), chunkPos.getMinBlockZ());
		int endZ = Math.min(area.maxZ(), chunkPos.getMinBlockZ() + 15);
		int startY = client.level.getMinY();
		int endY = client.level.getMaxY() - 1;

		List<StructureScan> pendingStructures = new ArrayList<>(structuresToScan);
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				if (NUCLEUS.contains(x, z)) {
					continue;
				}
				for (int y = startY; y <= endY; y++) {
					mutable.set(x, y, z);
					BlockState state = chunk.getBlockState(mutable);
					if (state.isAir()) {
						continue;
					}

					Block block = state.getBlock();
					if (scanGrotto && isGrottoMarkerBlock(block)) {
						addGrottoMarker(mutable.immutable());
					}
					if (scanCorleone
							&& MITHRIL_DEPOSIT.contains(x, z)
							&& block == Blocks.FIRE
							&& matchesCorleoneMarker(client, mutable)) {
						addCorleoneMarker(mutable.immutable());
					}

					for (int structureIndex = 0; structureIndex < pendingStructures.size(); ) {
						StructureScan structure = pendingStructures.get(structureIndex);
						if (!structure.containsBlock(x, z) || !structure.isAnchorBlock(state)) {
							structureIndex++;
							continue;
						}
						BlockPos matchPos = structure.matchPosition(client, mutable, state);
						if (matchPos == null) {
							structureIndex++;
							continue;
						}

						markStructureFound(client, structure, matchPos);
						pendingStructures.remove(structureIndex);
					}
				}
			}
		}
	}

	private static void markStructureFound(Minecraft client, StructureScan structure, BlockPos markerPos) {
		structure.foundMarkerPos = markerPos;
		structure.foundWaypoint = new Vec3(
				markerPos.getX() + 0.5D,
				markerPos.getY() + structure.waypointYOffset,
				markerPos.getZ() + 0.5D);
		FeatureChat.send("Found " + structure.name + " at " + markerPos.getX() + " " + markerPos.getY() + " " + markerPos.getZ());
		if (!structure.titleText.isBlank()) {
			showFoundTitle(client, structure.titleText);
		}
	}

	private static void reportMissingStructures() {
		for (StructureScan structure : STRUCTURES) {
			if (structure.isEnabled()
					&& structure.foundWaypoint == null
					&& !structure.notFoundReported
					&& structure.allChunksScanned(SCANNED_CHUNKS)) {
				structure.notFoundReported = true;
				FeatureChat.send(structure.notFoundText);
			}
		}
		if (isCorleoneScannerEnabled()
				&& CORLEONE_MARKERS.isEmpty()
				&& !corleoneNotFoundReported
				&& allChunksScanned(CORLEONE_SCAN_CHUNKS)) {
			corleoneNotFoundReported = true;
			FeatureChat.send("No Corleone was found.");
		}
	}

	private static void addGrottoMarker(BlockPos markerPos) {
		if (GROTTO_CLUSTERS.isEmpty()) {
			GrottoCluster cluster = new GrottoCluster();
			cluster.add(markerPos);
			GROTTO_CLUSTERS.add(cluster);
			Vec3 waypoint = cluster.waypoint();
			FeatureChat.send("Found Grotto at " + Mth.floor(waypoint.x) + " " + Mth.floor(waypoint.y) + " " + Mth.floor(waypoint.z));
			return;
		}

		GROTTO_CLUSTERS.get(0).add(markerPos);
	}

	private static void addCorleoneMarker(BlockPos markerPos) {
		if (!CORLEONE_MARKERS.add(markerPos)) {
			return;
		}
		FeatureChat.send("Found Corleone at " + markerPos.getX() + " " + markerPos.getY() + " " + markerPos.getZ());
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

	private static boolean matchesBluePrecursorCityMarker(Minecraft client, BlockPos headPos) {
		return isHeadBlock(client, headPos)
				&& isBlock(client, headPos.above(), Blocks.QUARTZ_SLAB)
				&& isAir(client, headPos.above(2))
				&& isBlock(client, headPos.above(3), Blocks.QUARTZ_SLAB)
				&& isBlock(client, headPos.below(), Blocks.QUARTZ_BLOCK)
				&& isBlock(client, headPos.below(2), Blocks.QUARTZ_SLAB);
	}

	private static BlockPos findGoblinKingMarker(Minecraft client, BlockPos anchorPos, BlockState state) {
		if (state == null || state.getBlock() != Blocks.RED_WOOL) {
			return null;
		}
		if (!matchesGoblinKingMarker(client, anchorPos)) {
			return null;
		}
		return anchorPos.offset(1, 7, 1).immutable();
	}

	private static boolean matchesGoblinKingMarker(Minecraft client, BlockPos redWoolAnchorPos) {
		return isBlock(client, redWoolAnchorPos, Blocks.RED_WOOL)
				&& isBlock(client, redWoolAnchorPos.offset(1, 0, 0), Blocks.RED_WOOL)
				&& isBlock(client, redWoolAnchorPos.offset(2, 0, 0), Blocks.RED_WOOL)
				&& isBlock(client, redWoolAnchorPos.offset(1, 0, -1), Blocks.RED_WOOL)
				&& isBlock(client, redWoolAnchorPos.offset(2, 0, -1), Blocks.RED_WOOL)
				&& isBlock(client, redWoolAnchorPos.offset(0, 1, -1), Blocks.DARK_OAK_STAIRS)
				&& isBlock(client, redWoolAnchorPos.offset(0, 2, -1), Blocks.DARK_OAK_STAIRS)
				&& isAir(client, redWoolAnchorPos.offset(1, 1, -1))
				&& isAir(client, redWoolAnchorPos.offset(1, 2, -1))
				&& isBlock(client, redWoolAnchorPos.offset(2, 1, -1), Blocks.DARK_OAK_STAIRS)
				&& isBlock(client, redWoolAnchorPos.offset(2, 2, -1), Blocks.DARK_OAK_STAIRS);
	}

	private static boolean matchesGoblinQueenMarker(Minecraft client, BlockPos headPos) {
		return isHeadBlock(client, headPos)
				&& isBlock(client, headPos.below(), Blocks.ORANGE_CARPET)
				&& isBlock(client, headPos.above(), Blocks.ORANGE_TERRACOTTA)
				&& isBlock(client, headPos.above(2), Blocks.GREEN_TERRACOTTA)
				&& isBlock(client, headPos.above(3), Blocks.BLACK_TERRACOTTA)
				&& isAir(client, headPos.above(4));
	}

	private static BlockPos findJungleTempleMarker(Minecraft client, BlockPos anchorPos, BlockState state) {
		if (state == null || state.getBlock() != Blocks.JUNGLE_PLANKS) {
			return null;
		}
		if (matchesJungleTempleSignatureAt(client, anchorPos)) {
			return anchorPos.immutable();
		}
		return null;
	}

	private static boolean matchesJungleTempleSignatureAt(Minecraft client, BlockPos centerPos) {
		return matchesJungleTempleSignatureOnZ(client, centerPos)
				|| matchesJungleTempleSignatureOnX(client, centerPos);
	}

	private static boolean matchesJungleTempleSignatureOnZ(Minecraft client, BlockPos centerPos) {
		if (!isBlock(client, centerPos, Blocks.JUNGLE_PLANKS)) {
			return false;
		}

		for (int[] offset : JUNGLE_TEMPLE_STAIR_SIGNATURE) {
			if (!isBlock(client, centerPos.offset(0, offset[0], offset[1]), Blocks.JUNGLE_STAIRS)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesJungleTempleSignatureOnX(Minecraft client, BlockPos centerPos) {
		if (!isBlock(client, centerPos, Blocks.JUNGLE_PLANKS)) {
			return false;
		}

		for (int[] offset : JUNGLE_TEMPLE_STAIR_SIGNATURE) {
			if (!isBlock(client, centerPos.offset(offset[1], offset[0], 0), Blocks.JUNGLE_STAIRS)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesCorleoneMarker(Minecraft client, BlockPos firePos) {
		return isBlock(client, firePos, Blocks.FIRE)
				&& isBlock(client, firePos.below(), Blocks.STONE)
				&& isBlock(client, firePos.below(2), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(-1, -2, 0), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(-1, -1, 0), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(1, -2, 0), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(1, -1, 0), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(0, -2, -1), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(0, -1, -1), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(0, -2, 1), Blocks.STONE_BRICK_STAIRS)
				&& isBlock(client, firePos.offset(0, -1, 1), Blocks.STONE_BRICK_STAIRS);
	}

	private static BlockPos findBalMarker(Minecraft client, BlockPos anchorPos, BlockState state) {
		if (state == null || state.getBlock() != Blocks.COBBLESTONE_WALL) {
			return null;
		}
		if (client == null || client.level == null || anchorPos == null) {
			return null;
		}

		for (int rotation = 0; rotation < 4; rotation++) {
			for (int[] anchorOffset : BAL_ANCHOR_OFFSETS) {
				BlockPos rotatedAnchorOffset = rotatedOffset(0, 0, 0, anchorOffset[0], anchorOffset[1], anchorOffset[2], rotation);
				int baseX = anchorPos.getX() - rotatedAnchorOffset.getX();
				int baseY = anchorPos.getY() - rotatedAnchorOffset.getY();
				int baseZ = anchorPos.getZ() - rotatedAnchorOffset.getZ();

				if (matchesBalSignatureAt(client, baseX, baseY, baseZ, rotation)) {
					return new BlockPos(baseX, baseY + 4, baseZ);
				}
			}
		}
		return null;
	}

	private static boolean matchesBalSignatureAt(Minecraft client, int baseX, int baseY, int baseZ, int rotation) {
		for (int[] offset : BAL_COBBLESTONE_WALL_OFFSETS) {
			if (!isBlock(client, rotatedOffset(baseX, baseY, baseZ, offset[0], offset[1], offset[2], rotation), Blocks.COBBLESTONE_WALL)) {
				return false;
			}
		}

		for (int[] offset : BAL_COBBLESTONE_OFFSETS) {
			if (!isBlock(client, rotatedOffset(baseX, baseY, baseZ, offset[0], offset[1], offset[2], rotation), Blocks.COBBLESTONE)) {
				return false;
			}
		}

		return true;
	}

	private static BlockPos rotatedOffset(int baseX, int baseY, int baseZ, int xOffset, int yOffset, int zOffset, int rotation) {
		return switch (rotation & 3) {
			case 1 -> new BlockPos(baseX - zOffset, baseY + yOffset, baseZ + xOffset);
			case 2 -> new BlockPos(baseX - xOffset, baseY + yOffset, baseZ - zOffset);
			case 3 -> new BlockPos(baseX + zOffset, baseY + yOffset, baseZ - xOffset);
			default -> new BlockPos(baseX + xOffset, baseY + yOffset, baseZ + zOffset);
		};
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

	private static boolean isBluePrecursorCityMarkerAnchorBlock(BlockState state) {
		return state != null && isHeadBlock(state.getBlock());
	}

	private static boolean isGoblinKingMarkerAnchorBlock(BlockState state) {
		return state != null && state.getBlock() == Blocks.RED_WOOL;
	}

	private static boolean isGoblinQueenMarkerAnchorBlock(BlockState state) {
		return state != null && isHeadBlock(state.getBlock());
	}

	private static boolean isJungleTempleMarkerAnchorBlock(BlockState state) {
		return state != null && state.getBlock() == Blocks.JUNGLE_PLANKS;
	}

	private static boolean isGrottoMarkerBlock(Block block) {
		return block == Blocks.MAGENTA_STAINED_GLASS_PANE
				|| block == Blocks.MAGENTA_STAINED_GLASS;
	}

	private static boolean isBalMarkerAnchorBlock(BlockState state) {
		return state != null && state.getBlock() == Blocks.COBBLESTONE_WALL;
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

	private static List<ScanChunk> orderedScanChunks(Minecraft client) {
		return orderedScanChunks(client, SCAN_CHUNKS);
	}

	private static List<ScanChunk> orderedScanChunks(Minecraft client, List<ScanChunk> scanChunks) {
		if (client == null || client.player == null) {
			return scanChunks;
		}

		int playerChunkX = Mth.floor(client.player.getX()) >> 4;
		int playerChunkZ = Mth.floor(client.player.getZ()) >> 4;
		List<ScanChunk> ordered = new ArrayList<>(scanChunks);
		ordered.sort((left, right) -> Long.compare(
				chunkDistanceSquared(left.chunkKey(), playerChunkX, playerChunkZ),
				chunkDistanceSquared(right.chunkKey(), playerChunkX, playerChunkZ)));
		return ordered;
	}

	private static long chunkDistanceSquared(long chunkKey, int centerChunkX, int centerChunkZ) {
		long dx = ChunkPos.getX(chunkKey) - centerChunkX;
		long dz = ChunkPos.getZ(chunkKey) - centerChunkZ;
		return dx * dx + dz * dz;
	}

	private static boolean containsChunk(List<ScanChunk> chunks, long chunkKey) {
		for (ScanChunk chunk : chunks) {
			if (chunk.chunkKey() == chunkKey) {
				return true;
			}
		}
		return false;
	}

	private static boolean allChunksScanned(List<ScanChunk> chunks) {
		for (ScanChunk chunk : chunks) {
			if (!SCANNED_CHUNKS.contains(chunk.chunkKey())) {
				return false;
			}
		}
		return true;
	}

	private static void renderWaypoints(WorldRenderContext context, Minecraft client) {
		for (StructureScan structure : STRUCTURES) {
			if (!structure.isEnabled() || structure.foundWaypoint == null) {
				continue;
			}
			renderWaypoint(context, client, structure);
		}
	}

	private static void renderGrottoWaypoints(WorldRenderContext context, Minecraft client) {
		if (!isGrottoScannerEnabled()) {
			return;
		}
		for (GrottoCluster cluster : GROTTO_CLUSTERS) {
			Vec3 waypoint = cluster.waypoint();
			WorldTextRenderer.drawText(
					context,
					new Vec3(waypoint.x, waypoint.y + 2.5D, waypoint.z),
					waypointLabel(client, "Grotto", waypoint),
					GROTTO_WAYPOINT_COLOR,
					waypointTextScale(client, waypoint),
					true,
					0.0F,
					WAYPOINT_BACKGROUND_COLOR);
		}
	}

	private static void renderCorleoneWaypoints(WorldRenderContext context, Minecraft client) {
		if (!isCorleoneScannerEnabled()) {
			return;
		}
		for (BlockPos marker : CORLEONE_MARKERS) {
			Vec3 waypoint = corleoneWaypoint(marker);
			WorldTextRenderer.drawText(
					context,
					new Vec3(waypoint.x, waypoint.y + 2.5D, waypoint.z),
					waypointLabel(client, "Corleone", waypoint),
					CORLEONE_WAYPOINT_COLOR,
					waypointTextScale(client, waypoint),
					true,
					0.0F,
					WAYPOINT_BACKGROUND_COLOR);
		}
	}

	private static Vec3 corleoneWaypoint(BlockPos marker) {
		return new Vec3(marker.getX() + 0.5D, marker.getY() + 0.5D, marker.getZ() + 0.5D);
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
		return waypointLabel(client, structure.name, structure.foundWaypoint);
	}

	private static String waypointLabel(Minecraft client, String name, Vec3 waypoint) {
		if (client == null || client.player == null || waypoint == null) {
			return name;
		}

		int distance = Math.max(0, Math.round((float) client.player.position().distanceTo(waypoint)));
		return name + " (" + distance + ")";
	}

	private static float waypointTextScale(Minecraft client, StructureScan structure) {
		return waypointTextScale(client, structure.foundWaypoint);
	}

	private static float waypointTextScale(Minecraft client, Vec3 waypoint) {
		if (client == null || client.player == null || waypoint == null) {
			return WAYPOINT_MIN_TEXT_SCALE;
		}

		float distance = (float) client.player.position().distanceTo(waypoint);
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
		SCANNED_CHUNKS.clear();
		GROTTO_CLUSTERS.clear();
		CORLEONE_MARKERS.clear();
		corleoneNotFoundReported = false;
	}

	private static List<StructureScan> createStructures() {
		return List.of(
				new StructureScan(
						"Dragon Lair",
						"No dragon lair was found.",
						"Dragon Lair found!",
						DRAGON_LAIR_WAYPOINT_COLOR,
						1.5D,
						List.of(MITHRIL_DEPOSIT),
						StructureScanner::isDragonLairScannerEnabled,
						StructureScanner::isDragonLairMarkerAnchorBlock,
						(client, pos, state) -> matchesDragonLairMarker(client, pos, state) ? pos.immutable() : null),
				new StructureScan(
						"Mines Of Divan",
						"No Mines Of Divan was found.",
						"",
						MINES_OF_DIVAN_WAYPOINT_COLOR,
						0.5D,
						List.of(MITHRIL_DEPOSIT),
						StructureScanner::isMinesOfDivanScannerEnabled,
						StructureScanner::isMinesOfDivanMarkerAnchorBlock,
						StructureScanner::findMinesOfDivanMarker),
				new StructureScan(
						"Precursor City",
						"No Precursor City was found.",
						"",
						PRECURSOR_CITY_WAYPOINT_COLOR,
						1.5D,
						List.of(PRECURSOR_REMNANTS),
						StructureScanner::isBluePrecursorCityScannerEnabled,
						StructureScanner::isBluePrecursorCityMarkerAnchorBlock,
						(client, pos, state) -> matchesBluePrecursorCityMarker(client, pos) ? pos.immutable() : null),
				new StructureScan(
						"Goblin King",
						"No Goblin King was found.",
						"",
						GOBLIN_KING_WAYPOINT_COLOR,
						0.0D,
						List.of(GOBLIN_HIDEOUT),
						StructureScanner::isGoblinKingScannerEnabled,
						StructureScanner::isGoblinKingMarkerAnchorBlock,
						StructureScanner::findGoblinKingMarker),
				new StructureScan(
						"Goblin Queen",
						"No Goblin Queen was found.",
						"",
						GOBLIN_QUEEN_WAYPOINT_COLOR,
						1.5D,
						List.of(GOBLIN_HIDEOUT),
						StructureScanner::isGoblinQueenScannerEnabled,
						StructureScanner::isGoblinQueenMarkerAnchorBlock,
						(client, pos, state) -> matchesGoblinQueenMarker(client, pos) ? pos.immutable() : null),
				new StructureScan(
						"Bal",
						"No Bal was found.",
						"",
						BAL_WAYPOINT_COLOR,
						2.0D,
						List.of(CRYSTAL_HOLLOWS),
						StructureScanner::isBalScannerEnabled,
						StructureScanner::isBalMarkerAnchorBlock,
						StructureScanner::findBalMarker),
				new StructureScan(
						"Jungle Temple",
						"No Jungle Temple was found.",
						"",
						JUNGLE_TEMPLE_WAYPOINT_COLOR,
						15.0D,
						List.of(JUNGLE),
						StructureScanner::isJungleTempleScannerEnabled,
						StructureScanner::isJungleTempleMarkerAnchorBlock,
						StructureScanner::findJungleTempleMarker));
	}

	private static List<ScanChunk> createScanChunks(List<ScanArea> scanAreas) {
		List<ScanChunk> chunks = new ArrayList<>();
		for (ScanArea area : scanAreas) {
			for (long chunkKey : area.chunkKeys()) {
				chunks.add(new ScanChunk(area, chunkKey));
			}
		}
		return List.copyOf(chunks);
	}

	private record ScanArea(int minX, int maxX, int minZ, int maxZ, List<Long> chunkKeys) {
		private static ScanArea between(int xA, int zA, int xB, int zB) {
			int minX = Math.min(xA, xB);
			int maxX = Math.max(xA, xB);
			int minZ = Math.min(zA, zB);
			int maxZ = Math.max(zA, zB);
			List<Long> chunkKeys = new ArrayList<>();
			for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
				for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
					chunkKeys.add(ChunkPos.asLong(chunkX, chunkZ));
				}
			}
			return new ScanArea(minX, maxX, minZ, maxZ, List.copyOf(chunkKeys));
		}

		private boolean contains(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}

	private record ScanChunk(ScanArea area, long chunkKey) {
	}

	private static final class GrottoCluster {
		private final Set<BlockPos> markers = new HashSet<>();

		private int minX;
		private int maxX;
		private int minY;
		private int maxY;
		private int minZ;
		private int maxZ;

		private boolean overlaps(BlockPos markerPos) {
			for (BlockPos marker : markers) {
				if (Math.abs(marker.getX() - markerPos.getX()) <= GROTTO_SEARCH_OVERLAP_DISTANCE
						&& Math.abs(marker.getY() - markerPos.getY()) <= GROTTO_SEARCH_OVERLAP_DISTANCE
						&& Math.abs(marker.getZ() - markerPos.getZ()) <= GROTTO_SEARCH_OVERLAP_DISTANCE) {
					return true;
				}
			}
			return false;
		}

		private boolean add(BlockPos markerPos) {
			if (!markers.add(markerPos)) {
				return false;
			}
			if (markers.size() == 1) {
				minX = markerPos.getX();
				maxX = markerPos.getX();
				minY = markerPos.getY();
				maxY = markerPos.getY();
				minZ = markerPos.getZ();
				maxZ = markerPos.getZ();
				return true;
			}

			minX = Math.min(minX, markerPos.getX());
			maxX = Math.max(maxX, markerPos.getX());
			minY = Math.min(minY, markerPos.getY());
			maxY = Math.max(maxY, markerPos.getY());
			minZ = Math.min(minZ, markerPos.getZ());
			maxZ = Math.max(maxZ, markerPos.getZ());
			return true;
		}

		private void merge(GrottoCluster other) {
			for (BlockPos marker : other.markers) {
				add(marker);
			}
		}

		private boolean couldExtendFrom(ScanChunk scanChunk) {
			int chunkMinX = Math.max(scanChunk.area().minX(), ChunkPos.getX(scanChunk.chunkKey()) << 4);
			int chunkMaxX = Math.min(scanChunk.area().maxX(), (ChunkPos.getX(scanChunk.chunkKey()) << 4) + 15);
			int chunkMinZ = Math.max(scanChunk.area().minZ(), ChunkPos.getZ(scanChunk.chunkKey()) << 4);
			int chunkMaxZ = Math.min(scanChunk.area().maxZ(), (ChunkPos.getZ(scanChunk.chunkKey()) << 4) + 15);

			return chunkMaxX >= minX - GROTTO_SEARCH_OVERLAP_DISTANCE
					&& chunkMinX <= maxX + GROTTO_SEARCH_OVERLAP_DISTANCE
					&& chunkMaxZ >= minZ - GROTTO_SEARCH_OVERLAP_DISTANCE
					&& chunkMinZ <= maxZ + GROTTO_SEARCH_OVERLAP_DISTANCE;
		}

		private Vec3 waypoint() {
			return new Vec3(
					(minX + maxX + 1.0D) / 2.0D,
					(minY + maxY + 1.0D) / 2.0D + GROTTO_WAYPOINT_Y_OFFSET,
					(minZ + maxZ + 1.0D) / 2.0D);
		}
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
		private final List<ScanArea> scanAreas;
		private final List<ScanChunk> scanChunks;
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
				List<ScanArea> scanAreas,
				BooleanSupplier enabled,
				AnchorMatcher anchorMatcher,
				StructureMatcher structureMatcher) {
			this.name = name;
			this.notFoundText = notFoundText;
			this.titleText = titleText;
			this.waypointColor = waypointColor;
			this.waypointYOffset = waypointYOffset;
			this.scanAreas = List.copyOf(scanAreas);
			this.scanChunks = createScanChunks(scanAreas);
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

		private boolean containsScanChunk(long chunkKey) {
			return containsChunk(scanChunks, chunkKey);
		}

		private boolean containsBlock(int x, int z) {
			for (ScanArea area : scanAreas) {
				if (area.contains(x, z)) {
					return true;
				}
			}
			return false;
		}

		private BlockPos matchPosition(Minecraft client, BlockPos pos, BlockState state) {
			return structureMatcher.matchPosition(client, pos, state);
		}

		private boolean allChunksScanned(Set<Long> scannedChunkKeys) {
			for (ScanChunk chunk : scanChunks) {
				if (!scannedChunkKeys.contains(chunk.chunkKey())) {
					return false;
				}
			}
			return true;
		}

		private void reset() {
			notFoundReported = false;
			foundMarkerPos = null;
			foundWaypoint = null;
			scannedChunks.clear();
		}
	}
}
