package com.crussion.moissanite.ui.state;

import java.util.function.UnaryOperator;

public final class UiStore {
	private static final UiStore INSTANCE = new UiStore();

	private UiState state = new UiState("", -1, false, 0, 0);

	private UiStore() {
	}

	public static UiStore get() {
		return INSTANCE;
	}

	public UiState getState() {
		return state;
	}

	public void setState(UiState state) {
		this.state = state;
	}

	public void update(UnaryOperator<UiState> reducer) {
		this.state = reducer.apply(this.state);
	}
}
