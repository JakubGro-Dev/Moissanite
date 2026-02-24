package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.util.customgui.CoordRememberingSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.inventory.Slot;

@Mixin(Slot.class)
public class SlotOriginalCoordsMixin implements CoordRememberingSlot {
	@Unique
	private int moissanite$originalX;
	@Unique
	private int moissanite$originalY;

	@Override
	public void moissanite$rememberCoords() {
		SlotAccessor accessor = (SlotAccessor) (Object) this;
		this.moissanite$originalX = accessor.moissanite$getX();
		this.moissanite$originalY = accessor.moissanite$getY();
	}

	@Override
	public void moissanite$restoreCoords() {
		SlotAccessor accessor = (SlotAccessor) (Object) this;
		accessor.moissanite$setX(this.moissanite$originalX);
		accessor.moissanite$setY(this.moissanite$originalY);
	}
}
