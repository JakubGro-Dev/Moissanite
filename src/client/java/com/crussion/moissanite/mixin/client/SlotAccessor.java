package com.crussion.moissanite.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotAccessor {
	@Accessor("x")
	int moissanite$getX();

	@Mutable
	@Accessor("x")
	void moissanite$setX(int x);

	@Accessor("y")
	int moissanite$getY();

	@Mutable
	@Accessor("y")
	void moissanite$setY(int y);
}
