package com.crussion.moissanite.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public final class CommandReplacer {
	private static final String FEATURE_NAME = "Command Replacer";
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("moissanite")
			.resolve("command-replacer.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, String> ALIASES = new LinkedHashMap<>();

	private CommandReplacer() {
	}

	public static void init() {
		load();
	}

	public static String normalizeUserCommand(String raw) {
		return normalizeCommand(raw);
	}

	public static String formatCommand(String command) {
		return command == null || command.isBlank() ? "/" : "/" + command;
	}

	public static List<AliasEntry> getAliases() {
		synchronized (ALIASES) {
			List<AliasEntry> snapshot = new ArrayList<>(ALIASES.size());
			for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
				snapshot.add(new AliasEntry(entry.getKey(), entry.getValue()));
			}
			return snapshot;
		}
	}

	public static String rewriteTypedCommand(String command) {
		String normalizedCommand = normalizeCommand(command);
		if (normalizedCommand == null) {
			return command;
		}

		AliasMatch match = findMatch(normalizedCommand);
		if (match == null) {
			return command;
		}

		String suffix = normalizedCommand.length() == match.alias().length()
				? ""
				: normalizedCommand.substring(match.alias().length());
		return match.replacement() + suffix;
	}

	public static OperationResult upsertAlias(String previousAliasRaw, String aliasRaw, String replacementRaw) {
		String previousAlias = normalizeCommand(previousAliasRaw);
		String alias = normalizeCommand(aliasRaw);
		String replacement = normalizeCommand(replacementRaw);
		if (alias == null) {
			return OperationResult.failure("Alias is required.");
		}
		if (replacement == null) {
			return OperationResult.failure("Replacement is required.");
		}
		if (alias.equals(replacement)) {
			return OperationResult.failure("Alias and replacement cannot be the same.");
		}

		synchronized (ALIASES) {
			if (previousAlias != null && !previousAlias.equals(alias)) {
				ALIASES.remove(previousAlias);
			}
			ALIASES.put(alias, replacement);
			save();
		}

		return OperationResult.success(
				"Saved " + formatCommand(alias) + " -> " + formatCommand(replacement),
				alias,
				replacement);
	}

	public static OperationResult removeAlias(String aliasRaw) {
		String alias = normalizeCommand(aliasRaw);
		if (alias == null) {
			return OperationResult.failure("Alias is required.");
		}

		String removed;
		synchronized (ALIASES) {
			removed = ALIASES.remove(alias);
			if (removed == null) {
				return OperationResult.failure("No alias found for " + formatCommand(alias));
			}
			save();
		}

		return OperationResult.success(
				"Removed " + formatCommand(alias) + " -> " + formatCommand(removed),
				alias,
				removed);
	}

	private static AliasMatch findMatch(String command) {
		AliasMatch best = null;
		synchronized (ALIASES) {
			for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
				String alias = entry.getKey();
				if (!matches(command, alias)) {
					continue;
				}
				if (best == null || alias.length() > best.alias().length()) {
					best = new AliasMatch(alias, entry.getValue());
				}
			}
		}
		return best;
	}

	private static boolean matches(String command, String alias) {
		return command.equals(alias) || command.startsWith(alias + " ");
	}

	private static void load() {
		synchronized (ALIASES) {
			ALIASES.clear();
			if (!Files.exists(CONFIG_PATH)) {
				return;
			}

			try {
				String content = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
				JsonElement parsed = GSON.fromJson(content, JsonElement.class);
				if (parsed == null || !parsed.isJsonObject()) {
					return;
				}

				JsonObject root = parsed.getAsJsonObject();
				JsonObject aliasesJson = root.getAsJsonObject("aliases");
				if (aliasesJson == null) {
					return;
				}

				for (Map.Entry<String, JsonElement> entry : aliasesJson.entrySet()) {
					String alias = normalizeCommand(entry.getKey());
					String replacement = normalizeCommand(entry.getValue().getAsString());
					if (alias == null || replacement == null || alias.equals(replacement)) {
						continue;
					}
					ALIASES.put(alias, replacement);
				}
			} catch (Exception ignored) {
			}
		}
	}

	private static void save() {
		JsonObject aliasesJson = new JsonObject();
		for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
			aliasesJson.addProperty(entry.getKey(), entry.getValue());
		}

		JsonObject root = new JsonObject();
		root.add("aliases", aliasesJson);

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}

	private static String normalizeCommand(String raw) {
		if (raw == null) {
			return null;
		}

		String trimmed = raw.trim();
		if (trimmed.length() >= 2) {
			char first = trimmed.charAt(0);
			char last = trimmed.charAt(trimmed.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
			}
		}

		if (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}

		trimmed = WHITESPACE.matcher(trimmed).replaceAll(" ").trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record AliasMatch(String alias, String replacement) {
	}

	public record AliasEntry(String alias, String replacement) {
		public String displayLabel() {
			return formatCommand(alias) + " -> " + formatCommand(replacement);
		}
	}

	public record OperationResult(boolean success, String message, String alias, String replacement) {
		private static OperationResult success(String message, String alias, String replacement) {
			return new OperationResult(true, prefixMessage(message), alias, replacement);
		}

		private static OperationResult failure(String message) {
			return new OperationResult(false, prefixMessage(message), null, null);
		}

		private static String prefixMessage(String message) {
			return FEATURE_NAME + ": " + (message == null ? "" : message);
		}
	}
}
