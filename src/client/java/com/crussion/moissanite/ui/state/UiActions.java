package com.crussion.moissanite.ui.state;

import java.util.function.UnaryOperator;

public final class UiActions {
	private UiActions() {
	}

	public static UnaryOperator<UiState> setSearchQuery(String searchQuery) {
		return state -> new UiState(searchQuery, state.selectedCategoryIndex(), state.showingSettings(), state.rightPanelScroll(), state.leftListScroll());
	}

	public static UnaryOperator<UiState> setSelectedCategoryIndex(int index) {
		return state -> new UiState(state.searchQuery(), index, state.showingSettings(), state.rightPanelScroll(), state.leftListScroll());
	}

	public static UnaryOperator<UiState> setShowingSettings(boolean showingSettings) {
		return state -> new UiState(state.searchQuery(), state.selectedCategoryIndex(), showingSettings, state.rightPanelScroll(), state.leftListScroll());
	}

	public static UnaryOperator<UiState> setRightPanelScroll(int rightPanelScroll) {
		return state -> new UiState(state.searchQuery(), state.selectedCategoryIndex(), state.showingSettings(), rightPanelScroll, state.leftListScroll());
	}

	public static UnaryOperator<UiState> setLeftListScroll(int leftListScroll) {
		return state -> new UiState(state.searchQuery(), state.selectedCategoryIndex(), state.showingSettings(), state.rightPanelScroll(), leftListScroll);
	}
}
