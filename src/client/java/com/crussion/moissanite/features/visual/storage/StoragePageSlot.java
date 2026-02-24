package com.crussion.moissanite.features.visual.storage;

import net.minecraft.client.Minecraft;

public record StoragePageSlot(int index) implements Comparable<StoragePageSlot> {

	public StoragePageSlot {
		assert index >= 0 && index < (3 * 9);
	}

	public boolean isEnderChest() {
		return index < 9;
	}

	public boolean isBackPack() {
		return !isEnderChest();
	}

	public int slotIndexInOverviewPage() {
		return isEnderChest() ? index + 9 : index + 18;
	}

	public String defaultName() {
		return isEnderChest()
				? "Ender Chest #" + (index + 1)
				: "Backpack #" + (index - 9 + 1);
	}

	public void navigateTo() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.player.connection == null) {
			return;
		}
		if (isBackPack()) {
			client.player.connection.sendCommand("backpack " + (index - 9 + 1));
		} else {
			client.player.connection.sendCommand("enderchest " + (index + 1));
		}
	}

	public static StoragePageSlot fromOverviewSlotIndex(int slot) {
		if (slot >= 9 && slot < 18)
			return new StoragePageSlot(slot - 9);
		if (slot >= 27 && slot < 45)
			return new StoragePageSlot(slot - 27 + 9);
		return null;
	}

	public static StoragePageSlot ofEnderChestPage(int slot) {
		assert slot >= 1 && slot <= 9;
		return new StoragePageSlot(slot - 1);
	}

	public static StoragePageSlot ofBackPackPage(int slot) {
		assert slot >= 1 && slot <= 18;
		return new StoragePageSlot(slot - 1 + 9);
	}

	@Override
	public int compareTo(StoragePageSlot other) {
		return this.index - other.index;
	}
}
