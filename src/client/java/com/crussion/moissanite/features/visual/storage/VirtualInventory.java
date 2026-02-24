package com.crussion.moissanite.features.visual.storage;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import java.util.concurrent.CompletableFuture;

public final class VirtualInventory {
    private final List<ItemStack> stacks;
    private final int rows;
    private final CompletableFuture<String> serializationCache;

    public VirtualInventory(List<ItemStack> stacks) {
        assert stacks.size() % 9 == 0;
        assert stacks.size() / 9 >= 1 && stacks.size() / 9 <= 5;
        this.stacks = List.copyOf(stacks);
        this.rows = stacks.size() / 9;

        this.serializationCache = CompletableFuture.supplyAsync(() -> serializeInventory(this));
    }

    public CompletableFuture<String> getSerializationCache() {
        return serializationCache;
    }

    public List<ItemStack> stacks() {
        return stacks;
    }

    public int rows() {
        return rows;
    }

    private static String serializeInventory(VirtualInventory inventory) {
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();

            com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops = net.minecraft.nbt.NbtOps.INSTANCE;
            if (client.level != null) {
                ops = client.level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            }

            for (ItemStack stack : inventory.stacks()) {
                if (stack.isEmpty()) {
                    list.add(new net.minecraft.nbt.CompoundTag());
                } else {
                    try {
                        list.add(ItemStack.CODEC.encode(stack, ops, new net.minecraft.nbt.CompoundTag()).getOrThrow());
                    } catch (Exception e) {
                        list.add(new net.minecraft.nbt.CompoundTag());
                    }
                }
            }
            net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
            root.put("INVENTORY", list);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(root, baos);
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("moissanite-storage-overlay")
                    .error("Failed to serialize inventory", e);
            return null;
        }
    }
}
