package com.crussion.moissanite.features.visual.storage;

import java.util.List;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class StorageOverviewScreen extends Screen {

    public static final Set<Item> EMPTY_STORAGE_SLOT_ITEMS = Set.of(
            Blocks.RED_STAINED_GLASS_PANE.asItem(),
            Blocks.BROWN_STAINED_GLASS_PANE.asItem(),
            Items.GRAY_DYE);
    public static final int PAGE_WIDTH = 19 * 9;

    private static int scroll = 0;
    private static int lastRenderedHeight = 0;

    private final StorageData content;
    public boolean isClosing = false;

    public StorageOverviewScreen() {
        super(Component.empty());
        StorageData data = StorageOverlayFeature.getData();
        this.content = data != null ? data : new StorageData();
    }

    @Override
    protected void init() {
        super.init();
        scroll = Math.min(scroll, getMaxScroll());
        scroll = Math.max(scroll, 0);
    }

    @Override
    public void onClose() {
        if (!StorageOverlayFeature.retainScroll())
            scroll = 0;
        super.onClose();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, 0x90000000);
        layoutedForEach((slot, inventory, offsetX, offsetY) -> {
            context.pose().pushMatrix();
            context.pose().translate((float) offsetX, (float) offsetY);
            renderStoragePage(context, inventory, mouseX - offsetX, mouseY - offsetY);
            context.pose().popMatrix();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        layoutedForEach((slot, inventory, x, y) -> {
            double rx = click.x() - x;
            double ry = click.y() - y;
            if (rx >= 0 && rx <= PAGE_WIDTH && ry >= 0 && ry <= getStorePageHeight(inventory)) {
                onClose();
                StorageOverlayFeature.lastStorageOverlay = this;
                slot.navigateTo();
            }
        });
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = (int) (scroll + StorageOverlayFeature.adjustScrollSpeed(verticalAmount));
        scroll = Math.min(scroll, getMaxScroll());
        scroll = Math.max(scroll, 0);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.input() == GLFW.GLFW_KEY_ESCAPE) {
            isClosing = true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public int getStorePageHeight(StorageData.StorageInventory page) {
        Minecraft client = Minecraft.getInstance();
        if (page.inventory() != null) {
            return page.inventory().rows() * 19 + client.font.lineHeight + 2;
        }
        return 60;
    }

    private int getMaxScroll() {
        return lastRenderedHeight - height + 2 * StorageOverlayFeature.margin();
    }

    private void layoutedForEach(LayoutCallback callback) {
        int offsetY = 0;
        int currentMaxHeight = StorageOverlayFeature.margin() - StorageOverlayFeature.padding() - scroll;
        int totalHeight = -currentMaxHeight;
        int columns = StorageOverlayFeature.columns();
        int padding = StorageOverlayFeature.padding();
        int index = 0;

        for (var entry : content.storageInventories().entrySet()) {
            StoragePageSlot slot = entry.getKey();
            StorageData.StorageInventory inventory = entry.getValue();

            int pageX = index % columns;
            if (pageX == 0) {
                currentMaxHeight += padding;
                offsetY += currentMaxHeight;
                totalHeight += currentMaxHeight;
                currentMaxHeight = 0;
            }
            int xPosition = width / 2
                    - (columns * (PAGE_WIDTH + padding) - padding) / 2
                    + pageX * (PAGE_WIDTH + padding);

            callback.accept(slot, inventory, xPosition, offsetY);

            int pageHeight = getStorePageHeight(inventory);
            currentMaxHeight = Math.max(currentMaxHeight, pageHeight);
            index++;
        }
        lastRenderedHeight = totalHeight + currentMaxHeight;
    }

    private void renderStoragePage(GuiGraphics context, StorageData.StorageInventory page, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        context.drawString(client.font, page.title(), 2, 2, -1, true);
        VirtualInventory inventory = page.inventory();
        if (inventory == null) {
            context.fill(0, 0, PAGE_WIDTH, 60, 0xFF331111);
            context.drawCenteredString(client.font, Component.literal("Not loaded yet"), PAGE_WIDTH / 2, 30, -1);
            return;
        }

        List<ItemStack> stacks = inventory.stacks();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int x = (i % 9) * 19;
            int y = (i / 9) * 19 + client.font.lineHeight + 2;
            boolean hovered = (mouseX - x) >= 0 && (mouseX - x) < 18
                    && (mouseY - y) >= 0 && (mouseY - y) < 18;
            context.fill(x, y, x + 18, y + 18, hovered ? 0x80808080 : 0x40808080);
            context.renderItem(stack, x + 1, y + 1);
            context.renderItemDecorations(client.font, stack, x + 1, y + 1);
        }
    }

    @FunctionalInterface
    private interface LayoutCallback {
        void accept(StoragePageSlot slot, StorageData.StorageInventory inventory, int offsetX, int offsetY);
    }
}
