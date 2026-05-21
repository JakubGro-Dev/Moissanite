package com.crussion.moissanite.features.general;

import com.crussion.moissanite.definitions.UiDefinitions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GeneralTweaks {
	private GeneralTweaks() {
	}

	public static void init() {
		UiDefinitions.NO_GRASS_RENDER.bind(value -> rebuildWorldRenderer());
	}

	public static boolean noGrassRenderEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.NO_GRASS_RENDER.get());
	}

	public static boolean noMobLeftoversEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.NO_MOB_LEFTOVERS.get());
	}

	public static boolean isGrassLike(BlockState state) {
		if (state == null) {
			return false;
		}
		return isGrassLike(state.getBlock());
	}

	public static boolean shouldIgnorePickableEntity(Entity entity) {
		if (!noMobLeftoversEnabled() || entity == null) {
			return false;
		}
		if (entity instanceof ArmorStand armorStand && armorStand.isInvisible()) {
			return true;
		}
		if (entity instanceof LivingEntity livingEntity) {
			return livingEntity.isDeadOrDying();
		}
		return false;
	}

	private static boolean isGrassLike(Block block) {
		return block == Blocks.SHORT_GRASS
				|| block == Blocks.TALL_GRASS
				|| block == Blocks.SHORT_DRY_GRASS
				|| block == Blocks.TALL_DRY_GRASS
				|| block == Blocks.FERN
				|| block == Blocks.LARGE_FERN
				|| block == Blocks.SEAGRASS
				|| block == Blocks.TALL_SEAGRASS;
	}

	private static void rebuildWorldRenderer() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		Runnable rebuild = () -> {
			if (client.level != null && client.levelRenderer != null) {
				client.levelRenderer.allChanged();
			}
		};

		if (client.isSameThread()) {
			rebuild.run();
		} else {
			client.execute(rebuild);
		}
	}
}
