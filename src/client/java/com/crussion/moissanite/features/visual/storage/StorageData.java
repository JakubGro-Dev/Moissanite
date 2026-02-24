package com.crussion.moissanite.features.visual.storage;

import java.util.NavigableMap;
import java.util.TreeMap;

public final class StorageData {
    private final NavigableMap<StoragePageSlot, StorageInventory> storageInventories;

    public StorageData() {
        this.storageInventories = new TreeMap<>();
    }

    public StorageData(NavigableMap<StoragePageSlot, StorageInventory> storageInventories) {
        this.storageInventories = storageInventories;
    }

    public NavigableMap<StoragePageSlot, StorageInventory> storageInventories() {
        return storageInventories;
    }

    public static final class StorageInventory {
        private String title;
        private final StoragePageSlot slot;
        private VirtualInventory inventory;

        public StorageInventory(String title, StoragePageSlot slot, VirtualInventory inventory) {
            this.title = title;
            this.slot = slot;
            this.inventory = inventory;
        }

        public String title() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public StoragePageSlot slot() {
            return slot;
        }

        public VirtualInventory inventory() {
            return inventory;
        }

        public void setInventory(VirtualInventory inventory) {
            this.inventory = inventory;
        }
    }
}
