package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.util.customgui.CoordRememberingSlot;
import com.crussion.moissanite.util.customgui.CustomGui;
import com.crussion.moissanite.util.customgui.HasCustomGui;
import com.crussion.moissanite.features.dungeons.terminals.DungeonsTerminals;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenCustomGuiMixin<T extends AbstractContainerMenu> extends Screen implements HasCustomGui {
	@Shadow
	@Final
	protected T menu;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;
	@Shadow
	protected abstract void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY);
	@Unique
	private CustomGui moissanite$customGui;
	@Unique
	private boolean moissanite$hasRememberedSlots;

	protected AbstractContainerScreenCustomGuiMixin(Component title) {
		super(title);
	}

	@Nullable
	@Override
	public CustomGui moissanite$getCustomGui() {
		return moissanite$customGui;
	}

	@Override
	public void moissanite$setCustomGui(@Nullable CustomGui customGui) {
		this.moissanite$customGui = customGui;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void moissanite$onInit(CallbackInfo ci) {
		if (moissanite$customGui != null) {
			moissanite$customGui.onInit(this.width, this.height);
		}
	}

	@Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
	private void moissanite$renderLabels(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (moissanite$customGui != null && !moissanite$customGui.shouldDrawForeground()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderSlot", at = @At("HEAD"))
	private void moissanite$beforeSlotRender(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (moissanite$customGui != null) {
			moissanite$customGui.beforeSlotRender(graphics, slot);
		}
	}

	@Inject(method = "renderSlot", at = @At("TAIL"))
	private void moissanite$afterSlotRender(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (moissanite$customGui != null) {
			moissanite$customGui.afterSlotRender(graphics, slot);
		}
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void moissanite$hasClickedOutside(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> cir) {
		if (moissanite$customGui != null) {
			cir.setReturnValue(moissanite$customGui.isClickOutsideBounds(mouseX, mouseY));
		}
	}

	@Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
	private void moissanite$isHoveringBounds(int x, int y, int width, int height, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
		if (moissanite$customGui != null) {
			cir.setReturnValue(moissanite$customGui.isPointWithinBounds(x + this.leftPos, y + this.topPos, width, height, pointX, pointY));
		}
	}

	@Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
	private void moissanite$isHoveringSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
		if (moissanite$customGui != null) {
			cir.setReturnValue(moissanite$customGui.isPointOverSlot(slot, this.leftPos, this.topPos, pointX, pointY));
		}
	}

	@Inject(method = "renderBackground", at = @At("HEAD"))
	private void moissanite$moveSlots(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (moissanite$customGui == null) {
			if (moissanite$hasRememberedSlots) {
				for (Slot slot : menu.slots) {
					if (slot instanceof CoordRememberingSlot rememberingSlot) {
						rememberingSlot.moissanite$restoreCoords();
					}
				}
				moissanite$hasRememberedSlots = false;
			}
			return;
		}

		for (Slot slot : menu.slots) {
			if (!moissanite$hasRememberedSlots && slot instanceof CoordRememberingSlot rememberingSlot) {
				rememberingSlot.moissanite$rememberCoords();
			}
			moissanite$customGui.moveSlot(slot, this.leftPos, this.topPos);
		}
		moissanite$hasRememberedSlots = true;
	}

	@Redirect(
			method = "renderBackground",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
	private void moissanite$renderCustomGui(AbstractContainerScreen<?> instance, GuiGraphics graphics, float delta, int mouseX, int mouseY) {
		if (moissanite$customGui != null) {
			moissanite$customGui.render(graphics, delta, mouseX, mouseY);
			return;
		}
		this.renderBg(graphics, delta, mouseX, mouseY);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void moissanite$mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (DungeonsTerminals.onContainerScreenMouseClicked(click)) {
			cir.setReturnValue(true);
			return;
		}
		if (moissanite$customGui != null && moissanite$customGui.mouseClick(click, doubled)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void moissanite$mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
			CallbackInfoReturnable<Boolean> cir) {
		if (moissanite$customGui != null && moissanite$customGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void moissanite$mouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
		if (moissanite$customGui != null && moissanite$customGui.mouseDragged(click, offsetX, offsetY)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void moissanite$mouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
		if (moissanite$customGui != null && moissanite$customGui.mouseReleased(click)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void moissanite$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
		if (DungeonsTerminals.onContainerScreenKeyPressed(input)) {
			cir.setReturnValue(true);
			return;
		}
		if (moissanite$customGui != null && moissanite$customGui.keyPressed(input)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
	private void moissanite$onClose(CallbackInfo ci) {
		if (moissanite$customGui != null && !moissanite$customGui.onVoluntaryExit()) {
			ci.cancel();
		}
	}
}
