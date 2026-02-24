package com.crussion.moissanite.util.customgui;

import org.jetbrains.annotations.Nullable;

public interface HasCustomGui {
	@Nullable
	CustomGui moissanite$getCustomGui();

	void moissanite$setCustomGui(@Nullable CustomGui customGui);
}
