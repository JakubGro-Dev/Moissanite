package com.crussion.moissanite.ui.data;

public interface UiEntry<T> {
	String name();
	UiEntryType type();
	T get();
	void set(T value);

	default boolean isVisible() {
		return true;
	}
}
