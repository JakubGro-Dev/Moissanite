package com.crussion.moissanite.features.visual.storage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.crussion.moissanite.mixin.client.SlotAccessor;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public final class StorageOverlayScreen extends Screen {

	public static final int PLAYER_WIDTH = 184;
	public static final int PLAYER_HEIGHT = 91;
	public static final int PLAYER_Y_INSET = 3;
	public static final int SLOT_SIZE = 18;
	public static final int PADDING = 10;
	public static final int PAGE_SLOTS_WIDTH = SLOT_SIZE * 9;
	public static final int PAGE_WIDTH = PAGE_SLOTS_WIDTH + 4;
	public static final int HOTBAR_X = 12;
	public static final int HOTBAR_Y = 67;
	public static final int MAIN_INVENTORY_Y = 9;
	public static final int SCROLL_BAR_WIDTH = 8;
	public static final int SCROLL_BAR_HEIGHT = 16;
	public static final int CONTROL_X_INSET = 3;
	public static final int CONTROL_Y_INSET = 5;
	public static final int CONTROL_WIDTH = 70;
	public static final int CONTROL_BACKGROUND_WIDTH = CONTROL_WIDTH + CONTROL_X_INSET + 1;
	public static final int CONTROL_HEIGHT = 50;

	public static float scroll = 0F;
	public static int lastRenderedInnerHeight = 0;
	private static String searchText = "";

	public boolean isExiting = false;
	private int pageWidthCount;

	public int innerScrollPanelWidth;
	public int overviewWidth;
	public int mX;
	public int overviewHeight;
	public int innerScrollPanelHeight;
	public int mY;
	public int playerX;
	public int playerY;
	public int controlX;
	public int controlY;
	public int totalWidth;
	public int totalHeight;

	private EditBox searchField;
	private Button editButton;

	private boolean knobGrabbed = false;

	private String searchCache = null;
	private Set<StoragePageSlot> filteredPagesCache = Set.of();

	public StorageOverlayScreen() {
		super(Component.literal(""));
		this.pageWidthCount = StorageOverlayFeature.columns();
	}

	public static void resetScroll() {
		if (!StorageOverlayFeature.retainScroll())
			scroll = 0F;
	}

	private void recalcMeasurements() {
		innerScrollPanelWidth = PAGE_WIDTH * pageWidthCount + (pageWidthCount - 1) * PADDING;
		overviewWidth = innerScrollPanelWidth + 3 * PADDING + SCROLL_BAR_WIDTH;
		mX = width / 2 - overviewWidth / 2;
		overviewHeight = Math.min(
				height - PLAYER_HEIGHT - Math.min(80, height / 10),
				StorageOverlayFeature.panelHeight());
		innerScrollPanelHeight = overviewHeight - PADDING * 2;
		mY = height / 2 - (overviewHeight + PLAYER_HEIGHT) / 2;
		playerX = width / 2 - PLAYER_WIDTH / 2;
		playerY = mY + overviewHeight - PLAYER_Y_INSET;
		controlX = playerX - CONTROL_WIDTH + CONTROL_X_INSET;
		controlY = playerY - CONTROL_Y_INSET;
		totalWidth = overviewWidth;
		totalHeight = overviewHeight - PLAYER_Y_INSET + PLAYER_HEIGHT;
	}

	public void setupDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void init() {
		super.init();
		pageWidthCount = Math.max(1,
				Math.min(StorageOverlayFeature.columns(),
						(width - PADDING) / (PAGE_WIDTH + PADDING)));
		recalcMeasurements();
		scroll = Math.min(scroll, getMaxScroll());
		scroll = Math.max(scroll, 0F);

		this.searchField = new EditBox(this.font, controlX + 4, controlY + 4, CONTROL_WIDTH - 8, 16, Component.empty());
		this.searchField.setHint(Component.literal("Search..."));
		this.searchField.setMaxLength(100);
		this.searchField.setValue(searchText);
		this.searchField.setResponder(value -> {
			searchText = value;
			searchCache = null;
			layoutedForEach(StorageOverlayFeature.getData(), (rect, page, inventory) -> {
			});
			coerceScroll(0F);
		});
		this.addRenderableWidget(this.searchField);

		this.editButton = Button.builder(Component.literal("Edit Pages"), button -> editPages())
				.bounds(controlX + 4, controlY + 24, CONTROL_WIDTH - 8, 20)
				.build();
		this.addRenderableWidget(this.editButton);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		coerceScroll((float) StorageOverlayFeature.adjustScrollSpeed(verticalAmount));
		return true;
	}

	public void coerceScroll(float offset) {
		scroll = Math.min(scroll + offset, getMaxScroll());
		scroll = Math.max(scroll, 0F);
	}

	public float getMaxScroll() {
		return (float) lastRenderedInnerHeight - getScrollPanelInner().height();
	}

	public float getScrollbarPercentage() {
		float max = getMaxScroll();
		return max <= 0 ? 0 : scroll / max;
	}

	@Override
	public void onClose() {
		isExiting = true;
		resetScroll();
		super.onClose();
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		drawBackgrounds(context);
		drawPages(context, mouseX, mouseY, delta, null, null, 0, 0);
		drawScrollBar(context);
		drawPlayerInventory(context, mouseX, mouseY, delta);
		drawControls(context, mouseX, mouseY);
	}

	public void drawBackgrounds(GuiGraphics context) {
		context.fill(mX, mY, mX + overviewWidth, mY + overviewHeight, 0xC0101010);
		context.fill(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT, 0xC0101010);
	}

	public void drawScrollBar(GuiGraphics context) {
		ScreenRectangle sbRect = getScrollBarRect();
		context.fill(sbRect.left(), sbRect.top(), sbRect.right(), sbRect.bottom(), 0x80333333);
		float maxScroll = getMaxScroll();
		int knobY = sbRect.top();
		if (maxScroll > 0) {
			knobY += (int) (getScrollbarPercentage() * (sbRect.height() - SCROLL_BAR_HEIGHT));
		}
		context.fill(sbRect.left(), knobY, sbRect.right(), knobY + SCROLL_BAR_HEIGHT, 0xFFAAAAAA);
	}

	public void drawControls(GuiGraphics context, int mouseX, int mouseY) {
		context.fill(controlX, controlY, controlX + CONTROL_BACKGROUND_WIDTH, controlY + CONTROL_HEIGHT, 0xC0101010);
	}

	public void drawPlayerInventory(GuiGraphics context, int mouseX, int mouseY, float delta) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null)
			return;
		var items = client.player.getInventory().getNonEquipmentItems();
		for (int index = 0; index < items.size(); index++) {
			ItemStack item = items.get(index);
			int[] pos = getPlayerInventorySlotPosition(index);
			context.renderItem(item, pos[0], pos[1], 0);
			context.renderItemDecorations(font, item, pos[0], pos[1]);
		}
	}

	public int[] getPlayerInventorySlotPosition(int slotIndex) {
		if (slotIndex < 9) {
			return new int[] { playerX + slotIndex * SLOT_SIZE + HOTBAR_X, HOTBAR_Y + playerY };
		}
		return new int[] {
				playerX + (slotIndex % 9) * SLOT_SIZE + HOTBAR_X,
				playerY + (slotIndex / 9 - 1) * SLOT_SIZE + MAIN_INVENTORY_Y
		};
	}

	public ScreenRectangle getScrollBarRect() {
		return new ScreenRectangle(
				mX + PADDING + innerScrollPanelWidth + PADDING,
				mY + PADDING,
				SCROLL_BAR_WIDTH,
				innerScrollPanelHeight);
	}

	public ScreenRectangle getScrollPanelInner() {
		return new ScreenRectangle(
				mX + PADDING,
				mY + PADDING,
				innerScrollPanelWidth,
				innerScrollPanelHeight);
	}

	public void createScissors(GuiGraphics context, int transX, int transY) {
		ScreenRectangle rect = getScrollPanelInner();
		context.enableScissor(rect.left() - transX, rect.top() - transY, rect.right() - transX, rect.bottom() - transY);
	}

	public void drawPages(
			GuiGraphics context, int mouseX, int mouseY, float delta,
			StoragePageSlot excluding, List<Slot> slots,
			int slotOffsetX, int slotOffsetY) {
		createScissors(context, 0, 0);
		StorageData data = StorageOverlayFeature.getData();
		if (data == null)
			data = new StorageData();
		StorageData finalData = data;
		layoutedForEach(finalData, (rect, page, inventory) -> {
			drawPage(context, rect.left(), rect.top(), page, inventory,
					page.equals(excluding) ? slots : null,
					slotOffsetX, slotOffsetY,
					mouseX, mouseY);
		});
		context.disableScissor();
	}

	public int drawPage(
			GuiGraphics context,
			int x, int y,
			StoragePageSlot page,
			StorageData.StorageInventory inventory,
			List<Slot> slots,
			int slotOffsetX, int slotOffsetY,
			int mouseX, int mouseY) {
		VirtualInventory inv = inventory.inventory();
		if (inv == null) {
			context.fill(x, y, x + PAGE_WIDTH, y + 18, 0xC0202020);
			context.drawString(font, Component.literal("TODO: open this page"), x + 4, y + 4, -1, true);
			return 18;
		}
		if (slots != null && slots.size() != inv.stacks().size()) {
			return 0;
		}
		String name = inventory.title();
		int pageHeight = inv.rows() * SLOT_SIZE + 8 + font.lineHeight;

		context.drawString(font, Component.literal(name), x + 6, y + 3,
				slots == null ? 0xFFFFFFFF : 0xFFFFFF00, true);

		context.fill(x + 2, y + 5 + font.lineHeight, x + 2 + PAGE_SLOTS_WIDTH,
				y + 5 + font.lineHeight + inv.rows() * SLOT_SIZE, 0x80222222);

		ScreenRectangle scrollPanel = getScrollPanelInner();
		List<ItemStack> stacks = inv.stacks();
		for (int index = 0; index < stacks.size(); index++) {
			ItemStack stack = stacks.get(index);
			int slotX = (index % 9) * SLOT_SIZE + x + 3;
			int slotY = (index / 9) * SLOT_SIZE + y + 5 + font.lineHeight + 1;
			if (slots == null) {
				// Inactive page — render items directly
				context.renderItem(stack, slotX, slotY);
				context.renderItemDecorations(font, stack, slotX, slotY);
				if (StorageOverlayFeature.showInactivePageTooltips() && !stack.isEmpty()
						&& mouseX >= slotX && mouseY >= slotY
						&& mouseX <= slotX + 16 && mouseY <= slotY + 16
						&& scrollPanel.containsPoint(mouseX, mouseY)) {
					try {
						context.setTooltipForNextFrame(font, stack, mouseX, mouseY);
					} catch (IllegalStateException e) {
						context.setComponentTooltipForNextFrame(font, List.of(
								Component.nullToEmpty(ChatFormatting.RED + "Error Getting Tooltip!"),
								Component.nullToEmpty(
										ChatFormatting.YELLOW + "Open page to fix" + ChatFormatting.RESET)),
								mouseX, mouseY);
					}
				}
			} else {
				Slot slot = slots.get(index);
				SlotAccessor slotAccessor = (SlotAccessor) (Object) slot;
				slotAccessor.moissanite$setX(slotX - slotOffsetX);
				slotAccessor.moissanite$setY(slotY - slotOffsetY);
			}
		}
		return pageHeight + 6;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		return mouseClicked(click, doubled, null);
	}

	public boolean mouseClicked(MouseButtonEvent click, boolean doubled, StoragePageSlot activePage) {
		this.setFocused(null);
		double mouseX = click.x();
		double mouseY = click.y();

		ScreenRectangle scrollPanel = getScrollPanelInner();
		if (scrollPanel.containsPoint((int) mouseX, (int) mouseY)) {
			StorageData data = StorageOverlayFeature.getData();
			if (data != null) {
				final boolean[] handled = { false };
				layoutedForEach(data, (rect, page, inv) -> {
					if (!handled[0] && rect.containsPoint((int) mouseX, (int) mouseY)
							&& !page.equals(activePage) && click.button() == 0) {
						page.navigateTo();
						handled[0] = true;
					}
				});
				if (handled[0])
					return true;
			}
			return false;
		}

		ScreenRectangle sbRect = getScrollBarRect();
		if (sbRect.containsPoint((int) mouseX, (int) mouseY)) {
			float percentage = (float) ((mouseY - sbRect.top()) / sbRect.height());
			scroll = getMaxScroll() * percentage;
			mouseScrolled(0, 0, 0, 0);
			knobGrabbed = true;
			return true;
		}

		if (super.mouseClicked(click, doubled)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (knobGrabbed) {
			knobGrabbed = false;
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (knobGrabbed) {
			ScreenRectangle sbRect = getScrollBarRect();
			float percentage = (float) ((click.y() - sbRect.top()) / sbRect.height());
			scroll = getMaxScroll() * percentage;
			mouseScrolled(0, 0, 0, 0);
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (super.charTyped(input))
			return true;
		return false;
	}

	@Override
	public boolean keyReleased(KeyEvent input) {
		if (super.keyReleased(input))
			return true;
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return this == Minecraft.getInstance().screen;
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (super.keyPressed(input))
			return true;
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public Set<StoragePageSlot> getFilteredPages() {
		StorageData data = StorageOverlayFeature.getData();
		if (data == null)
			return filteredPagesCache;
		if (searchText.equals(searchCache))
			return filteredPagesCache;

		Set<StoragePageSlot> result = new LinkedHashSet<>();
		for (var entry : data.storageInventories().entrySet()) {
			VirtualInventory inv = entry.getValue().inventory();
			if (inv == null || inv.stacks().stream().anyMatch(s -> matchesSearch(s, searchText))) {
				result.add(entry.getKey());
			}
		}
		searchCache = searchText;
		filteredPagesCache = result;
		return result;
	}

	public boolean matchesSearch(ItemStack itemStack, String search) {
		if (search == null || search.isBlank())
			return true;
		TreeSet<String> searchWords = new TreeSet<>();
		for (String w : search.trim().split("\\s+")) {
			if (!w.isBlank())
				searchWords.add(w.toLowerCase());
		}

		String displayName = itemStack.getHoverName().getString();
		for (String word : displayName.split("\\s+")) {
			String cleaned = TextNormalizer.stripFormattingCodes(word);
			String lower = cleaned.toLowerCase();
			searchWords.removeIf(sw -> lower.contains(sw));
		}
		if (searchWords.isEmpty())
			return true;
		return searchWords.isEmpty();
	}

	private void layoutedForEach(StorageData data, LayoutCallback func) {
		if (data == null)
			data = new StorageData();
		int yOffset = -(int) scroll;
		int xOffset = 0;
		int maxHeight = 0;
		Set<StoragePageSlot> filter = getFilteredPages();

		for (var entry : data.storageInventories().entrySet()) {
			StoragePageSlot page = entry.getKey();
			StorageData.StorageInventory inventory = entry.getValue();
			if (!filter.contains(page))
				continue;

			VirtualInventory inv = inventory.inventory();
			int currentHeight = inv != null
					? inv.rows() * SLOT_SIZE + 6 + font.lineHeight
					: 18;
			maxHeight = Math.max(maxHeight, currentHeight);

			ScreenRectangle rect = new ScreenRectangle(
					mX + PADDING + (PAGE_WIDTH + PADDING) * xOffset,
					yOffset + mY + PADDING,
					PAGE_WIDTH,
					currentHeight);
			func.accept(rect, page, inventory);
			xOffset++;
			if (xOffset >= pageWidthCount) {
				yOffset += maxHeight;
				xOffset = 0;
				maxHeight = 0;
			}
		}
		lastRenderedInnerHeight = maxHeight + yOffset + (int) scroll;
	}

	public void editPages() {
		isExiting = true;
		StorageOverlayFeature.editPages();
	}

	public List<ScreenRectangle> getBounds() {
		return List.of(
				new ScreenRectangle(mX, mY, overviewWidth, overviewHeight),
				new ScreenRectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT),
				new ScreenRectangle(controlX, controlY, CONTROL_WIDTH, CONTROL_HEIGHT));
	}

	@FunctionalInterface
	private interface LayoutCallback {
		void accept(ScreenRectangle rect, StoragePageSlot page, StorageData.StorageInventory inventory);
	}
}
