package com.crussion.moissanite.ui.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public abstract class UiValue<T> implements UiEntry<T> {
	private final String name;
	private T value;
	private BooleanSupplier visibilityPredicate = () -> true;
	private List<Consumer<T>> listeners;
	private List<Runnable> voidListeners;

	protected UiValue(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public T get() {
		return value;
	}

	@Override
	public void set(T value) {
		T previous = this.value;
		this.value = value;
		if (Objects.equals(previous, value)) {
			return;
		}
		notifyListeners(value);
	}

	@Override
	public boolean isVisible() {
		return visibilityPredicate.getAsBoolean();
	}

	public UiValue<T> visibleWhen(UiValue<Boolean> toggle) {
		return visibleWhen(() -> toggle != null && Boolean.TRUE.equals(toggle.get()));
	}

	public UiValue<T> visibleWhen(BooleanSupplier predicate) {
		visibilityPredicate = predicate == null ? () -> true : predicate;
		return this;
	}

	public UiValue<T> bind(Consumer<T> listener) {
		if (listener == null) {
			return this;
		}
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
		return this;
	}

	public UiValue<T> bind(Runnable listener) {
		if (listener == null) {
			return this;
		}
		if (voidListeners == null) {
			voidListeners = new ArrayList<>();
		}
		voidListeners.add(listener);
		return this;
	}

	private void notifyListeners(T value) {
		if (listeners != null) {
			for (Consumer<T> listener : listeners) {
				listener.accept(value);
			}
		}
		if (voidListeners != null) {
			for (Runnable listener : voidListeners) {
				listener.run();
			}
		}
	}
}
