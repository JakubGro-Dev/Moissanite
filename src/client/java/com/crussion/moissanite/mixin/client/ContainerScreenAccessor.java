package com.crussion.moissanite.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Nullable
    @Accessor("hoveredSlot")
    Slot moissanite$getHoveredSlot();

    @Accessor("leftPos")
    int moissanite$getLeftPos();

    @Mutable
    @Accessor("leftPos")
    void moissanite$setLeftPos(int leftPos);

    @Accessor("topPos")
    int moissanite$getTopPos();

    @Mutable
    @Accessor("topPos")
    void moissanite$setTopPos(int topPos);

    @Accessor("imageWidth")
    int moissanite$getImageWidth();

    @Mutable
    @Accessor("imageWidth")
    void moissanite$setImageWidth(int imageWidth);

    @Accessor("imageHeight")
    int moissanite$getImageHeight();

    @Mutable
    @Accessor("imageHeight")
    void moissanite$setImageHeight(int imageHeight);
}
