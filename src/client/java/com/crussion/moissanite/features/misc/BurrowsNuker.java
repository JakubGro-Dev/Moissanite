package com.crussion.moissanite.features.misc;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.hypixel.SkyBlockLocationTracker;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class BurrowsNuker {
	private static final Pattern BURROW_DUG_PATTERN =
			Pattern.compile(".*Griffin [Bb]urrow.*\\(\\d+/\\d+\\).*");
	private static final Map<BlockPos, Burrow> BURROWS = new ConcurrentHashMap<>();
	private static final Direction DIG_DIRECTION = Direction.UP;
	private static final int MINE_TICKS = 8;
	private static final int POST_ATTEMPT_COOLDOWN_TICKS = 4;

	private static boolean initialized;
	private static boolean attackHoldConsumed;
	private static State state = State.IDLE;
	private static ActiveTarget activeTarget;
	private static long cooldownUntilTick;
	private static BlockPos lastAttemptedPos;

	private BurrowsNuker() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(BurrowsNuker::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> resetRuntime(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetRuntime(client));
	}

	public static void onToggleChanged(Boolean enabled) {
		if (!Boolean.TRUE.equals(enabled)) {
			resetRuntime(Minecraft.getInstance());
		}
	}

	public static void onParticlePacket(ClientboundLevelParticlesPacket packet) {
		if (!isEnabled() || packet == null || !isInHub()) {
			return;
		}

		if (isBurrowRemoveParticle(packet)) {
			BURROWS.remove(blockBelow(packet.getX(), packet.getY(), packet.getZ()));
			return;
		}

		ParticleKind particleKind = particleKind(packet);
		if (particleKind == null) {
			return;
		}

		BlockPos pos = blockAt(packet.getX(), packet.getY() - 1.0D, packet.getZ());
		Burrow burrow = BURROWS.computeIfAbsent(pos, Burrow::new);
		switch (particleKind) {
			case FOOTSTEP -> burrow.hasFootstep = true;
			case ENCHANT -> burrow.hasEnchant = true;
			case EMPTY -> burrow.type = BurrowType.START;
			case MOB -> burrow.type = BurrowType.MOB;
			case TREASURE -> burrow.type = BurrowType.TREASURE;
		}
	}

	public static void onSystemChat(Component message) {
		if (!isEnabled()) {
			return;
		}

		String text = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString()).trim();
		if (!BURROW_DUG_PATTERN.matcher(text).matches()) {
			return;
		}

		if (lastAttemptedPos != null) {
			BURROWS.remove(lastAttemptedPos);
		}
		finishActiveAttempt(Minecraft.getInstance(), true);
	}

	private static void onClientTick(Minecraft client) {
		if (!isEnabled()) {
			resetRuntime(client);
			return;
		}
		if (!hasWorld(client)) {
			resetActive(client, true);
			attackHoldConsumed = false;
			return;
		}

		SkyBlockLocationTracker.requestRefreshIfNeeded();
		if (!isInHub()) {
			resetActive(client, true);
			attackHoldConsumed = false;
			return;
		}

		long nowTick = currentTick(client);
		boolean attackHeld = isAttackHeld(client);
		if (!attackHeld) {
			attackHoldConsumed = false;
			resetActive(client, true);
			return;
		}

		if (!hasEchoSpade(client.player.getMainHandItem())) {
			resetActive(client, true);
			return;
		}

		switch (state) {
			case MINING -> tickMining(client, nowTick);
			case IDLE -> {
				if (!attackHoldConsumed && nowTick >= cooldownUntilTick) {
					beginClosestBurrow(client, nowTick);
				}
			}
		}
	}

	private static void beginClosestBurrow(Minecraft client, long nowTick) {
		Candidate candidate = closestCandidate(client);
		if (candidate == null) {
			return;
		}

		attackHoldConsumed = true;
		lastAttemptedPos = candidate.burrow().pos;
		activeTarget = new ActiveTarget(candidate.burrow().pos, candidate.direction(), nowTick);
		boolean started = client.gameMode.startDestroyBlock(activeTarget.pos, activeTarget.direction);
		if (!started) {
			cooldownUntilTick = nowTick + POST_ATTEMPT_COOLDOWN_TICKS;
			resetActive(client, false);
			return;
		}

		client.player.swing(InteractionHand.MAIN_HAND);
		state = State.MINING;
	}

	private static void tickMining(Minecraft client, long nowTick) {
		if (activeTarget == null) {
			state = State.IDLE;
			return;
		}
		if (!isAttackHeld(client) || !hasEchoSpade(client.player.getMainHandItem())) {
			resetActive(client, true);
			return;
		}

		Candidate candidate = candidateFor(client, activeTarget.pos);
		if (candidate == null || !candidate.burrow().pos.equals(activeTarget.pos)) {
			resetActive(client, true);
			return;
		}

		if (nowTick - activeTarget.mineStartedTick >= MINE_TICKS) {
			finishActiveAttempt(client, true);
			return;
		}

		activeTarget.direction = candidate.direction();
		client.gameMode.continueDestroyBlock(activeTarget.pos, activeTarget.direction);
		if ((nowTick - activeTarget.mineStartedTick) % 4L == 0L) {
			client.player.swing(InteractionHand.MAIN_HAND);
		}
	}

	private static Candidate closestCandidate(Minecraft client) {
		Vec3 playerPos = client.player.position();
		Candidate best = null;
		double bestDistanceSqr = Double.MAX_VALUE;

		for (Burrow burrow : BURROWS.values()) {
			if (burrow == null || burrow.type == null) {
				continue;
			}
			if (client.level.isLoaded(burrow.pos) && !isGrassBlock(client, burrow.pos)) {
				BURROWS.remove(burrow.pos);
				continue;
			}

			Candidate candidate = candidateFor(client, burrow.pos);
			if (candidate == null) {
				continue;
			}

			double distanceSqr = playerPos.distanceToSqr(Vec3.atCenterOf(burrow.pos));
			if (distanceSqr < bestDistanceSqr) {
				bestDistanceSqr = distanceSqr;
				best = candidate;
			}
		}

		return best;
	}

	private static Candidate candidateFor(Minecraft client, BlockPos pos) {
		if (!isDirectMineTargetValid(client, pos)) {
			return null;
		}

		Burrow burrow = BURROWS.get(pos);
		if (burrow == null || burrow.type == null) {
			return null;
		}
		return new Candidate(burrow, DIG_DIRECTION);
	}

	private static boolean isDirectMineTargetValid(Minecraft client, BlockPos pos) {
		return hasWorld(client)
				&& pos != null
				&& isGrassBlock(client, pos)
				&& client.player.isWithinBlockInteractionRange(pos, 0.0D);
	}

	private static boolean hasEchoSpade(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		String displayName = TextNormalizer.normalize(stack.getDisplayName().getString());
		if (!displayName.contains("spade")) {
			return false;
		}

		String itemText = itemText(stack);
		return itemText.contains("ability: echo")
				|| itemText.contains("ability echo")
				|| (itemText.contains("ability") && itemText.contains("echo"));
	}

	private static String itemText(ItemStack stack) {
		StringBuilder builder = new StringBuilder(256);
		builder.append(stack.getHoverName().getString()).append(' ');
		builder.append(stack.getDisplayName().getString()).append(' ');
		builder.append(stack.getComponentsPatch()).append(' ');

		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				builder.append(line.getString()).append(' ');
			}
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null && !customData.isEmpty()) {
			builder.append(customData.copyTag());
		}
		return TextNormalizer.normalize(builder.toString());
	}

	private static boolean isGrassBlock(Minecraft client, BlockPos pos) {
		if (client == null || client.level == null || pos == null || !client.level.isLoaded(pos)) {
			return false;
		}
		BlockState state = client.level.getBlockState(pos);
		return state.is(Blocks.GRASS_BLOCK);
	}

	private static boolean isAttackHeld(Minecraft client) {
		return client != null
				&& client.screen == null
				&& client.options != null
				&& client.options.keyAttack != null
				&& client.options.keyAttack.isDown();
	}

	private static boolean hasWorld(Minecraft client) {
		return client != null
				&& client.player != null
				&& client.level != null
				&& client.gameMode != null;
	}

	private static boolean isEnabled() {
		return Boolean.TRUE.equals(UiDefinitions.BURROWS_NUKER.get());
	}

	private static boolean isInHub() {
		SkyBlockLocationTracker.Location location = SkyBlockLocationTracker.currentLocation();
		return location != null
				&& location.isSkyBlock()
				&& "hub".equalsIgnoreCase(location.mode());
	}

	private static boolean isBurrowRemoveParticle(ClientboundLevelParticlesPacket packet) {
		return packet.getParticle().getType() == ParticleTypes.LARGE_SMOKE
				&& Float.compare(packet.getMaxSpeed(), 0.01F) == 0
				&& Float.compare(packet.getXDist(), 0.0F) == 0
				&& Float.compare(packet.getYDist(), 0.0F) == 0
				&& Float.compare(packet.getZDist(), 0.0F) == 0;
	}

	private static ParticleKind particleKind(ClientboundLevelParticlesPacket packet) {
		if (isParticle(packet, ParticleTypes.ENCHANT, 5, 0.05F, 0.5F, 0.4F, 0.5F)) {
			return ParticleKind.ENCHANT;
		}
		if (isParticle(packet, ParticleTypes.ENCHANTED_HIT, 4, 0.01F, 0.5F, 0.1F, 0.5F)) {
			return ParticleKind.EMPTY;
		}
		if (isParticle(packet, ParticleTypes.CRIT, 3, 0.01F, 0.5F, 0.1F, 0.5F)) {
			return ParticleKind.MOB;
		}
		if (isParticle(packet, ParticleTypes.DRIPPING_LAVA, 2, 0.01F, 0.35F, 0.1F, 0.35F)) {
			return ParticleKind.TREASURE;
		}
		if (isParticle(packet, ParticleTypes.CRIT, 1, 0.0F, 0.05F, 0.0F, 0.05F)) {
			return ParticleKind.FOOTSTEP;
		}
		return null;
	}

	private static boolean isParticle(
			ClientboundLevelParticlesPacket packet,
			ParticleType<?> type,
			int count,
			float maxSpeed,
			float xDist,
			float yDist,
			float zDist) {
		return packet.getParticle().getType() == type
				&& packet.getCount() == count
				&& Float.compare(packet.getMaxSpeed(), maxSpeed) == 0
				&& Float.compare(packet.getXDist(), xDist) == 0
				&& Float.compare(packet.getYDist(), yDist) == 0
				&& Float.compare(packet.getZDist(), zDist) == 0;
	}

	private static BlockPos blockBelow(double x, double y, double z) {
		return blockAt(x, y, z).below();
	}

	private static BlockPos blockAt(double x, double y, double z) {
		return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}

	private static long currentTick(Minecraft client) {
		return client != null && client.player != null ? client.player.tickCount : 0L;
	}

	private static void finishActiveAttempt(Minecraft client, boolean stopDestroying) {
		if (activeTarget != null) {
			BURROWS.remove(activeTarget.pos);
			cooldownUntilTick = currentTick(client) + POST_ATTEMPT_COOLDOWN_TICKS;
		}
		resetActive(client, stopDestroying);
	}

	private static void resetActive(Minecraft client, boolean stopDestroying) {
		if (stopDestroying && state == State.MINING && client != null && client.gameMode != null) {
			client.gameMode.stopDestroyBlock();
		}
		activeTarget = null;
		state = State.IDLE;
	}

	private static void resetRuntime(Minecraft client) {
		resetActive(client, true);
		BURROWS.clear();
		attackHoldConsumed = false;
		cooldownUntilTick = 0L;
		lastAttemptedPos = null;
	}

	private enum State {
		IDLE,
		MINING
	}

	private enum ParticleKind {
		FOOTSTEP,
		ENCHANT,
		EMPTY,
		MOB,
		TREASURE
	}

	private enum BurrowType {
		START,
		MOB,
		TREASURE
	}

	private static final class Burrow {
		private final BlockPos pos;
		private boolean hasFootstep;
		private boolean hasEnchant;
		private BurrowType type;

		private Burrow(BlockPos pos) {
			this.pos = pos;
		}
	}

	private static final class ActiveTarget {
		private final BlockPos pos;
		private Direction direction;
		private final long mineStartedTick;

		private ActiveTarget(BlockPos pos, Direction direction, long mineStartedTick) {
			this.pos = pos;
			this.direction = direction;
			this.mineStartedTick = mineStartedTick;
		}
	}

	private record Candidate(Burrow burrow, Direction direction) {
	}
}
