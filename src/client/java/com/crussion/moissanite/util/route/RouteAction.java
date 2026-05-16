package com.crussion.moissanite.util.route;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class RouteAction {
	public String type = RouteActionType.TP.name();
	public int x;
	public int y;
	public int z;
	public Integer targetX;
	public Integer targetY;
	public Integer targetZ;
	public Double aimX;
	public Double aimY;
	public Double aimZ;

	public RouteAction() {
	}

	public static RouteAction capture(RouteActionType type, LocalPlayer player) {
		return capture(type, player, player.blockPosition());
	}

	public static RouteAction capture(RouteActionType type, LocalPlayer player, BlockPos pos) {
		if (pos == null) {
			pos = player.blockPosition();
		}
		RouteAction action = new RouteAction();
		action.type = type.name();
		action.x = pos.getX();
		action.y = pos.getY();
		action.z = pos.getZ();
		return action;
	}

	public RouteActionType actionType() {
		return RouteActionType.fromSerialized(type);
	}

	public BlockPos triggerBlock() {
		return new BlockPos(x, y, z);
	}

	public BlockPos targetBlock() {
		BlockPos target = explicitTargetBlock();
		return target == null ? triggerBlock() : target;
	}

	public BlockPos explicitTargetBlock() {
		if (targetX == null || targetY == null || targetZ == null) {
			return null;
		}
		return new BlockPos(targetX, targetY, targetZ);
	}

	public void setTarget(BlockPos target) {
		if (target == null) {
			clearTarget();
			return;
		}
		targetX = target.getX();
		targetY = target.getY();
		targetZ = target.getZ();
	}

	public void clearTarget() {
		targetX = null;
		targetY = null;
		targetZ = null;
	}

	public Vec3 aimPoint(BlockPos baseBlock) {
		if (baseBlock == null || aimX == null || aimY == null || aimZ == null) {
			return null;
		}
		if (!Double.isFinite(aimX) || !Double.isFinite(aimY) || !Double.isFinite(aimZ)) {
			return null;
		}
		return new Vec3(baseBlock.getX() + aimX, baseBlock.getY() + aimY, baseBlock.getZ() + aimZ);
	}

	public void setAimPoint(BlockPos baseBlock, Vec3 aimPoint) {
		if (baseBlock == null || aimPoint == null) {
			clearAimPoint();
			return;
		}
		aimX = aimPoint.x - baseBlock.getX();
		aimY = aimPoint.y - baseBlock.getY();
		aimZ = aimPoint.z - baseBlock.getZ();
	}

	public void clearAimPoint() {
		aimX = null;
		aimY = null;
		aimZ = null;
	}

	public boolean hasWalkTarget() {
		return actionType() == RouteActionType.WALK && hasTarget();
	}

	public boolean hasTarget() {
		BlockPos target = explicitTargetBlock();
		return target != null && !target.equals(triggerBlock());
	}
}
