package com.crussion.moissanite.features.cheats;

import java.util.function.Consumer;

import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.util.CheatAutomationRuntime;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.HotbarDebugReporter;
import com.crussion.moissanite.util.inventory.HotbarItemSearch;
import com.crussion.moissanite.util.kuudra.KuudraTriggerArea;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.tick.TickTaskScheduler;

import net.minecraft.world.item.ItemStack;

public final class AutoRendHelper {
	private AutoRendHelper() {
	}

	static {
		CheatAutomationRuntime.init();
	}

	// Upper-case aliases to match requested helper names.
	public static boolean SwapHeldItem(int hotbarSlot) {
		return swapHeldItem(hotbarSlot);
	}

	public static boolean SwapHeldItem(String itemQuery) {
		return swapHeldItem(itemQuery);
	}

	public static int FindItem(String itemQuery) {
		return findItem(itemQuery);
	}

	public static int FindFirstHotbarSlotByNBT(String nbtSuffix) {
		return findFirstHotbarSlotByNBT(nbtSuffix);
	}

	public static ItemStack GetHeldItem() {
		return getHeldItem();
	}

	public static void Area(String areaNameInScoreboard, Runnable action) {
		area(areaNameInScoreboard, action);
	}

	public static void WithTick(int amountOfTicksToWaitBeforeRunning, Runnable action) {
		withTick(amountOfTicksToWaitBeforeRunning, action);
	}

	public static boolean IfPlayerInTriggerArea() {
		return ifPlayerInTriggerArea();
	}

	public static boolean Jump() {
		return jump();
	}

	public static boolean LC() {
		return lc();
	}

	public static boolean RC() {
		return rc();
	}

	public static boolean Rotate(double x, double y, double z) {
		return rotate(x, y, z);
	}

	public static boolean Rotate(double yaw, double pitch) {
		return rotate(yaw, pitch);
	}

	public static boolean RotateYawPitch(double yaw, double pitch, double multiplier) {
		return rotateYawPitch(yaw, pitch, multiplier);
	}

	public static boolean Rotate(double x, double y, double z, double multiplier) {
		return rotate(x, y, z, multiplier);
	}

	public static boolean WDSwapSlot(int slot) {
		return wdSwapSlot(slot);
	}

	public static boolean WDSwapSlot(int slot, Consumer<Boolean> onCompleted) {
		return wdSwapSlot(slot, onCompleted);
	}

	public static void DumpHotbarItemsToChat() {
		dumpHotbarItemsToChat();
	}

	public static boolean swapHeldItem(int hotbarSlot) {
		return HotbarItemSearch.swapHeldItem(hotbarSlot);
	}

	public static boolean swapHeldItem(String itemQuery) {
		return HotbarItemSearch.swapHeldItem(itemQuery);
	}

	public static int findItem(String itemQuery) {
		return HotbarItemSearch.findItem(itemQuery);
	}

	public static int findFirstHotbarSlotByNBT(String nbtSuffix) {
		return HotbarItemSearch.findFirstHotbarSlotByNbt(nbtSuffix);
	}

	public static ItemStack getHeldItem() {
		return HotbarItemSearch.getHeldItem();
	}

	public static void area(String areaNameInScoreboard, Runnable action) {
		if (action == null) {
			return;
		}
		if (isInScoreboardArea(areaNameInScoreboard)) {
			action.run();
		}
	}

	public static void withTick(int amountOfTicksToWaitBeforeRunning, Runnable action) {
		TickTaskScheduler.schedule(amountOfTicksToWaitBeforeRunning, action);
	}

	public static boolean ifPlayerInTriggerArea() {
		return KuudraTriggerArea.isPlayerInTriggerArea();
	}

	public static boolean jump() {
		return PlayerInputActions.jump();
	}

	public static boolean lc() {
		return PlayerInputActions.leftClick();
	}

	public static boolean rc() {
		return PlayerInputActions.rightClick();
	}

	public static boolean rotate(double x, double y, double z) {
		return RotationController.rotateTo(x, y, z);
	}

	public static boolean rotate(double x, double y, double z, double multiplier) {
		return RotationController.rotateTo(x, y, z, multiplier);
	}

	public static boolean rotate(double yaw, double pitch) {
		return RotationController.rotateYawPitch(yaw, pitch);
	}

	public static boolean rotateYawPitch(double yaw, double pitch, double multiplier) {
		return RotationController.rotateYawPitch(yaw, pitch, multiplier);
	}

	public static boolean wdSwapSlot(int slot) {
		return wdSwapSlot(slot, null);
	}

	public static boolean wdSwapSlot(int slot, Consumer<Boolean> onCompleted) {
		return WardrobeKeybinds.requestSwapSlot(slot, onCompleted);
	}

	public static void dumpHotbarItemsToChat() {
		HotbarDebugReporter.dumpHotbarItemsToChat();
	}

	public static boolean isInTriggerArea(double x, double y, double z) {
		return KuudraTriggerArea.isInTriggerArea(x, y, z);
	}

	public static boolean isInScoreboardArea(String areaNameInScoreboard) {
		return ScoreboardAreaMatcher.isInArea(areaNameInScoreboard);
	}

	public static void setSliderFromSlot(UiSlider slider, int slot) {
		HotbarItemSearch.setSliderFromSlot(slider, slot);
	}
}
