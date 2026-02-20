package com.crussion.moissanite.config;

import com.google.gson.JsonElement;

public interface ValueAdapter<T> {
	JsonElement toJson(T value);

	T fromJson(JsonElement element);

	boolean isNullable();
}
