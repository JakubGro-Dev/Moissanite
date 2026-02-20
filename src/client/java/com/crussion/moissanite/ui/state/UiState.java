package com.crussion.moissanite.ui.state;

public record UiState(String searchQuery,
					  int selectedCategoryIndex,
					  boolean showingSettings,
					  int rightPanelScroll,
					  int leftListScroll) {
}
