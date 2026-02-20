package com.crussion.moissanite.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public final class ConfigAdapters {
	public static final ValueAdapter<Boolean> BOOLEAN = new ValueAdapter<>() {
		@Override
		public JsonElement toJson(Boolean value) {
			return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		@Override
		public Boolean fromJson(JsonElement element) {
			if (element == null || element.isJsonNull()) {
				return null;
			}
			try {
				return element.getAsBoolean();
			} catch (Exception ignored) {
				return null;
			}
		}

		@Override
		public boolean isNullable() {
			return false;
		}
	};

	public static final ValueAdapter<Integer> INTEGER = new ValueAdapter<>() {
		@Override
		public JsonElement toJson(Integer value) {
			return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		@Override
		public Integer fromJson(JsonElement element) {
			if (element == null || element.isJsonNull()) {
				return null;
			}
			try {
				return element.getAsInt();
			} catch (Exception ignored) {
				return null;
			}
		}

		@Override
		public boolean isNullable() {
			return false;
		}
	};

	public static final ValueAdapter<Double> DOUBLE = new ValueAdapter<>() {
		@Override
		public JsonElement toJson(Double value) {
			return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		@Override
		public Double fromJson(JsonElement element) {
			if (element == null || element.isJsonNull()) {
				return null;
			}
			try {
				return element.getAsDouble();
			} catch (Exception ignored) {
				return null;
			}
		}

		@Override
		public boolean isNullable() {
			return false;
		}
	};

	public static final ValueAdapter<String> STRING = new ValueAdapter<>() {
		@Override
		public JsonElement toJson(String value) {
			return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		@Override
		public String fromJson(JsonElement element) {
			if (element == null || element.isJsonNull()) {
				return null;
			}
			try {
				return element.getAsString();
			} catch (Exception ignored) {
				return null;
			}
		}

		@Override
		public boolean isNullable() {
			return true;
		}
	};

	private ConfigAdapters() {
	}
}
