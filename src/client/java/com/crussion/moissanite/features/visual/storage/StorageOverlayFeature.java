package com.crussion.moissanite.features.visual.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.customgui.CustomGui;
import com.crussion.moissanite.util.customgui.CustomGuiAccess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;


public final class StorageOverlayFeature {

	private static final Logger LOGGER = LoggerFactory.getLogger("moissanite-storage-overlay");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static StorageOverviewScreen lastStorageOverlay = null;
	public static boolean skipNextStorageOverlayBackflip = false;
	public static StorageBackingHandle currentHandler = null;

	private static StorageData data = null;
	private static boolean dirty = false;

	public static boolean isEnabled() {
		return UiDefinitions.STORAGE_OVERLAY.get();
	}

	public static boolean alwaysReplace() {
		return UiDefinitions.STORAGE_OVERLAY_ALWAYS_REPLACE.get();
	}

	public static boolean itemsBlockScrolling() {
		return UiDefinitions.STORAGE_OVERLAY_ITEMS_BLOCK_SCROLLING.get();
	}

	public static boolean retainScroll() {
		return UiDefinitions.STORAGE_OVERLAY_RETAIN_SCROLL.get();
	}

	public static boolean showInactivePageTooltips() {
		return UiDefinitions.STORAGE_OVERLAY_SHOW_TOOLTIPS.get();
	}

	public static int columns() {
		return UiDefinitions.STORAGE_OVERLAY_COLUMNS.get().intValue();
	}

	public static int panelHeight() {
		return UiDefinitions.STORAGE_OVERLAY_HEIGHT.get().intValue();
	}

	public static int scrollSpeed() {
		return UiDefinitions.STORAGE_OVERLAY_SCROLL_SPEED.get().intValue();
	}

	public static boolean inverseScroll() {
		return UiDefinitions.STORAGE_OVERLAY_INVERSE_SCROLL.get();
	}

	public static int padding() {
		return UiDefinitions.STORAGE_OVERLAY_PADDING.get().intValue();
	}

	public static int margin() {
		return UiDefinitions.STORAGE_OVERLAY_MARGIN.get().intValue();
	}

	public static double adjustScrollSpeed(double amount) {
		return amount * scrollSpeed() * (inverseScroll() ? 1 : -1);
	}

	public static StorageData getData() {
		return data;
	}

	public static void markDirty() {
		dirty = true;
		saveCache();
	}

	public static void init() {
		loadCache();
	}

	public static void openOverlayScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.schedule(() -> client.setScreen(new StorageOverviewScreen()));
		}
	}

	public static void requestStorageRefresh() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null && client.player.connection != null) {
			client.player.connection.sendCommand("storage");
		}
	}

	public static void clearCache() {
		data = new StorageData();
		markDirty();
		saveCache();
	}

	public static void onChestContentUpdate() {
		if (!isEnabled())
			return;
		LOGGER.info("[onChestContentUpdate] currentHandler={}", currentHandler);
		rememberContent(currentHandler);
	}

	public static void onSlotClick(net.minecraft.world.inventory.Slot slot, ItemStack stack) {
		if (!isEnabled())
			return;
		if (lastStorageOverlay != null
				&& !(slot.container instanceof Inventory)
				&& slot.getContainerSlot() < 9
				&& stack.getItem() != Items.BLACK_STAINED_GLASS_PANE) {
			skipNextStorageOverlayBackflip = true;
		}
	}

	public static Screen onScreenChange(Screen oldScreen, Screen newScreen) {
		if (!isEnabled())
			return newScreen;
		if (oldScreen == null && newScreen == null)
			return newScreen;

		StorageOverlayScreen storageOverlayScreen = null;
		if (oldScreen instanceof StorageOverlayScreen sos) {
			storageOverlayScreen = sos;
		} else if (oldScreen instanceof AbstractContainerScreen<?> acs) {
			CustomGui gui = CustomGuiAccess.get(acs);
			if (gui instanceof StorageOverlayCustom soc) {
				storageOverlayScreen = soc.overview;
			}
		}

		StorageOverviewScreen storageOverviewScreen = null;
		if (oldScreen instanceof StorageOverviewScreen sovs) {
			storageOverviewScreen = sovs;
		}

		ContainerScreen screen = (newScreen instanceof ContainerScreen cs) ? cs : null;
		rememberContent(currentHandler);

		StorageBackingHandle oldHandler = currentHandler;
		currentHandler = StorageBackingHandle.fromScreen(screen);

		if (storageOverviewScreen != null && oldHandler instanceof StorageBackingHandle.HasBackingScreen hbs) {
			var player = Minecraft.getInstance().player;
			assert player != null;
			if (player != null && player.connection != null) {
				player.connection.send(new ServerboundContainerClosePacket(hbs.handler().containerId));
			}
			if (player != null && player.containerMenu == hbs.handler()) {
				player.containerMenu = player.inventoryMenu;
			}
		}

		if (storageOverviewScreen == null) {
			storageOverviewScreen = lastStorageOverlay;
		}

		if (newScreen == null && storageOverlayScreen != null && !storageOverlayScreen.isExiting) {
			return storageOverlayScreen;
		}

		if (storageOverviewScreen != null
				&& !storageOverviewScreen.isClosing
				&& (currentHandler instanceof StorageBackingHandle.Overview || currentHandler == null)) {
			if (skipNextStorageOverlayBackflip) {
				skipNextStorageOverlayBackflip = false;
			} else {
				lastStorageOverlay = null;
				return storageOverviewScreen;
			}
			return newScreen;
		}

		if (screen == null)
			return newScreen;
		if (storageOverlayScreen != null && storageOverlayScreen.isExiting)
			return newScreen;

		if (currentHandler == null)
			return newScreen;
		StorageOverlayScreen overlayScreen = storageOverlayScreen != null
				? storageOverlayScreen
				: (alwaysReplace() ? new StorageOverlayScreen() : null);
		if (overlayScreen == null)
			return newScreen;

		CustomGuiAccess.set(screen, new StorageOverlayCustom(currentHandler, screen, overlayScreen));
		return newScreen;
	}

	public static void editPages() {
		Minecraft client = Minecraft.getInstance();
		if (client == null)
			return;
		client.schedule(() -> {
			Screen screen = client.screen;
			if (screen instanceof ContainerScreen containerScreen) {
				StorageBackingHandle handle = StorageBackingHandle.fromScreen(containerScreen);
				if (handle instanceof StorageBackingHandle.Overview) {
					CustomGuiAccess.set(containerScreen, null);
					containerScreen.init(containerScreen.width, containerScreen.height);
					return;
				}
			}
			if (client.player != null && client.player.connection != null) {
				client.player.connection.sendCommand("storage");
			}
		});
	}

	public static void rememberContent(StorageBackingHandle handler) {
		if (handler == null)
			return;
		if (data == null)
			data = new StorageData();
		NavigableMap<StoragePageSlot, StorageData.StorageInventory> inventories = data.storageInventories();

		if (handler instanceof StorageBackingHandle.Overview overview) {
			rememberStorageOverview(overview, inventories);
		} else if (handler instanceof StorageBackingHandle.Page page) {
			rememberPage(page, inventories);
		}
	}

	private static void rememberStorageOverview(
			StorageBackingHandle.Overview handler,
			NavigableMap<StoragePageSlot, StorageData.StorageInventory> inventories) {
		var items = handler.handler().getItems();
		for (int index = 0; index < items.size(); index++) {
			ItemStack stack = items.get(index);
			if (stack.isEmpty())
				continue;
			StoragePageSlot slot = StoragePageSlot.fromOverviewSlotIndex(index);
			if (slot == null)
				continue;
			boolean isEmpty = StorageOverviewScreen.EMPTY_STORAGE_SLOT_ITEMS.contains(stack.getItem());
			if (inventories.containsKey(slot)) {
				if (isEmpty)
					inventories.remove(slot);
				continue;
			}
			if (!isEmpty) {
				inventories.put(slot, new StorageData.StorageInventory(slot.defaultName(), slot, null));
			}
		}
		markDirty();
	}

	private static void rememberPage(
			StorageBackingHandle.Page handler,
			NavigableMap<StoragePageSlot, StorageData.StorageInventory> inventories) {
		int rowCount = handler.handler().getRowCount();
		var allItems = handler.handler().getItems();
		int end = Math.min(allItems.size(), rowCount * 9);
		int start = Math.min(9, end);
		List<ItemStack> pageItems = new ArrayList<>();
		for (int i = start; i < end; i++) {
			pageItems.add(allItems.get(i).copy());
		}
		VirtualInventory newStacks = new VirtualInventory(pageItems);

		inventories.compute(handler.storagePageSlot(), (slot, existing) -> {
			StorageData.StorageInventory inv = existing != null
					? existing
					: new StorageData.StorageInventory(slot.defaultName(), slot, null);
			inv.setInventory(newStacks);
			return inv;
		});
		markDirty();
	}

	private static File getCacheFile() {
		Minecraft client = Minecraft.getInstance();
		File configDir = new File(client.gameDirectory, "config/moissanite");
		if (!configDir.exists())
			configDir.mkdirs();
		return new File(configDir, "storage-data.json");
	}

	public static void loadCache() {
		try {
			File file = getCacheFile();
			if (!file.exists()) {
				data = new StorageData();
				return;
			}
			try (FileReader reader = new FileReader(file)) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
				if (json == null) {
					data = new StorageData();
					return;
				}
				NavigableMap<StoragePageSlot, StorageData.StorageInventory> inventories = new TreeMap<>();
				JsonObject invObj = json.has("inventories") ? json.getAsJsonObject("inventories") : null;
				if (invObj != null) {
					for (var entry : invObj.entrySet()) {
						int index = Integer.parseInt(entry.getKey());
						StoragePageSlot slot = new StoragePageSlot(index);
						JsonObject pageJson = entry.getValue().getAsJsonObject();
						String title = pageJson.has("title") ? pageJson.get("title").getAsString() : slot.defaultName();
						VirtualInventory vi = null;
						if (pageJson.has("inventory")) {
							String b64 = pageJson.get("inventory").getAsString();
							vi = deserializeInventory(b64);
						}
						inventories.put(slot, new StorageData.StorageInventory(title, slot, vi));
					}
				}
				data = new StorageData(inventories);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load storage overlay cache", e);
			data = new StorageData();
		}
	}

	public static void saveCache() {
		if (!dirty)
			return;
		dirty = false;

		final StorageData snapshotData = data;

		CompletableFuture.runAsync(() -> {
			try {
				JsonObject json = new JsonObject();
				JsonObject invObj = new JsonObject();
				if (snapshotData != null) {
					for (var entry : snapshotData.storageInventories().entrySet()) {
						JsonObject pageJson = new JsonObject();
						pageJson.addProperty("title", entry.getValue().title());
						if (entry.getValue().inventory() != null) {
							try {
								String serialized = entry.getValue().inventory().getSerializationCache().get();
								if (serialized != null) {
									pageJson.addProperty("inventory", serialized);
								}
							} catch (Exception e) {
								LOGGER.error("Failed to get serialized inventory for page {}", entry.getKey().index(),
										e);
							}
						}
						invObj.add(String.valueOf(entry.getKey().index()), pageJson);
					}
				}
				json.add("inventories", invObj);
				File file = getCacheFile();
				try (FileWriter writer = new FileWriter(file)) {
					GSON.toJson(json, writer);
				}
			} catch (Exception e) {
				LOGGER.error("Failed to save storage overlay cache", e);
			}
		});
	}

	private static VirtualInventory deserializeInventory(String b64) {
		try {
			Minecraft client = Minecraft.getInstance();
			byte[] bytes = Base64.getDecoder().decode(b64);
			CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.create(100_000_000));
			var listOpt = root.getList("INVENTORY");
			if (listOpt.isEmpty())
				return null;
			ListTag list = listOpt.get();
			var ops = client.level != null
					? client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE)
					: NbtOps.INSTANCE;
			List<ItemStack> stacks = new ArrayList<>();
			for (int i = 0; i < list.size(); i++) {
				CompoundTag tag = list.getCompound(i).orElse(null);
				if (tag == null || tag.isEmpty()) {
					stacks.add(ItemStack.EMPTY);
				} else {
					try {
						stacks.add(ItemStack.CODEC.parse(ops, tag).getOrThrow());
					} catch (Exception e) {
						stacks.add(ItemStack.EMPTY);
					}
				}
			}
			if (stacks.isEmpty() || stacks.size() % 9 != 0)
				return null;
			return new VirtualInventory(stacks);
		} catch (Exception e) {
			LOGGER.error("Failed to deserialize inventory", e);
			return null;
		}
	}
}
