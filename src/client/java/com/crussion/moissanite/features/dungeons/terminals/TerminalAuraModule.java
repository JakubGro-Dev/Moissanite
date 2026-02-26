package com.crussion.moissanite.features.dungeons.terminals;

import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

final class TerminalAuraModule {
	private static final Pattern TERMINAL_COOLDOWN_CHAT =
			Pattern.compile("This Terminal doesn't seem to be responsive at the moment\\.", Pattern.CASE_INSENSITIVE);

	private final TerminalCore terminalCore;
	private int lastClickTicks;

	TerminalAuraModule(TerminalCore terminalCore) {
		this.terminalCore = terminalCore;
	}

	void onTick(Minecraft client) {
		if (lastClickTicks > 0) {
			lastClickTicks--;
		}
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (!TerminalSupport.terminalAuraEnabled()) {
			return;
		}
		if (terminalCore.isInTerminal()) {
			return;
		}
		if (client.screen != null) {
			return;
		}
		if (lastClickTicks > 0) {
			return;
		}
		if (client.player.containerMenu != client.player.inventoryMenu) {
			return;
		}

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof ArmorStand armorStand) || !armorStand.isAlive()) {
				continue;
			}
			String name = TerminalSupport.stripFormatting(armorStand.getName().getString());
			if (!"Inactive Terminal".equals(name)) {
				continue;
			}
			if (distanceToPlayerEyes(client, armorStand) > 4.0D) {
				continue;
			}
			if (client.getConnection() != null) {
				client.getConnection().send(ServerboundInteractPacket.createInteractionPacket(armorStand, false, InteractionHand.MAIN_HAND));
			}
		}
	}

	void onOpenWindow(ClientboundOpenScreenPacket packet) {
		String title = TerminalSupport.stripFormatting(packet.getTitle());
		if (TerminalType.MELODY.matches(title)) {
			lastClickTicks = 0;
		}
	}

	void onSystemChat(Component content) {
		String message = TerminalSupport.stripFormatting(content);
		if (TERMINAL_COOLDOWN_CHAT.matcher(message).find()) {
			lastClickTicks = 0;
		}
	}

	void onContainerSetData(ClientboundContainerSetDataPacket packet) {
		if (lastClickTicks > 0) {
			lastClickTicks--;
		}
	}

	void onWorldReset() {
		lastClickTicks = 0;
	}

	boolean onPacketSent(Packet<?> packet) {
		if (!(packet instanceof ServerboundInteractPacket interactPacket)) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return false;
		}

		int entityId = interactEntityId(interactPacket);
		Entity entity = client.level.getEntity(entityId);
		if (entity == null) {
			return false;
		}

		String name = TerminalSupport.stripFormatting(entity.getName().getString());
		if (!"Inactive Terminal".equals(name)) {
			return false;
		}
		if (lastClickTicks > 0 || terminalCore.isInTerminal()) {
			return true;
		}

		lastClickTicks = 10;
		return false;
	}

	private int interactEntityId(ServerboundInteractPacket packet) {
		try {
			var field = ServerboundInteractPacket.class.getDeclaredField("entityId");
			field.setAccessible(true);
			return field.getInt(packet);
		} catch (ReflectiveOperationException ignored) {
			return -1;
		}
	}

	private double distanceToPlayerEyes(Minecraft client, Entity entity) {
		double dx = entity.getX() - client.player.getX();
		double dy = entity.getY() - (client.player.getY() + client.player.getEyeHeight(client.player.getPose()));
		double dz = entity.getZ() - client.player.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
