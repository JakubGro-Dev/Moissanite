package com.crussion.moissanite.features.dungeons.terminals;

import net.minecraft.world.item.ItemStack;

record TerminalItem(int windowId, int slot, int id, int meta, int size, String name, boolean enchanted) {
	static TerminalItem from(int windowId, int slot, ItemStack stack) {
		int id = TerminalSupport.legacyItemId(stack);
		int meta = TerminalSupport.legacyMetadata(stack);
		int size = stack == null ? 0 : stack.getCount();
		String name = stack == null ? "" : TerminalSupport.stripFormatting(stack.getHoverName());
		boolean enchanted = stack != null && stack.hasFoil();
		return new TerminalItem(windowId, slot, id, meta, size, name, enchanted);
	}
}
