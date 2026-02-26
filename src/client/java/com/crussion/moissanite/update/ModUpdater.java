package com.crussion.moissanite.update;

import com.crussion.moissanite.Moissanite;
import com.crussion.moissanite.definitions.UiDefinitions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModUpdater {
	public enum CheckTrigger {
		AUTO_JOIN,
		MANUAL,
		COMMAND
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("moissanite-updater");
	private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/JakubGro-Dev/Moissanite/releases";
	private static final String GITHUB_RELEASES_PAGE = "https://github.com/JakubGro-Dev/Moissanite/releases";
	private static final Pattern RELEASE_TAG_PATTERN = Pattern.compile("(?i)^release[-_\\s]*(\\d+(?:\\.\\d+)*)$");
	private static final Pattern LOOSE_VERSION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)*)");
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Moissanite-Updater");
		thread.setDaemon(true);
		return thread;
	});
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
	private static final AtomicBoolean AUTO_JOIN_CHECK_DONE = new AtomicBoolean(false);
	private static final AtomicBoolean CHECK_RUNNING = new AtomicBoolean(false);
	private static final AtomicBoolean DOWNLOAD_RUNNING = new AtomicBoolean(false);
	private static final AtomicBoolean INSTALL_HOOK_REGISTERED = new AtomicBoolean(false);

	private static volatile ReleaseInfo latestRelease;
	private static volatile ReleaseInfo latestUpdate;
	private static volatile PendingInstall pendingInstall;

	private static final String CURRENT_VERSION_TEXT = resolveCurrentVersionText();
	private static final Version CURRENT_VERSION = Version.parseLoose(CURRENT_VERSION_TEXT);

	private ModUpdater() {
	}

	public static void init() {
		if (!INITIALIZED.compareAndSet(false, true)) {
			return;
		}

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.getCurrentServer() == null) {
				return;
			}
			if (!Boolean.TRUE.equals(UiDefinitions.UPDATE_AUTO_CHECK_ON_JOIN.get())) {
				return;
			}
			if (AUTO_JOIN_CHECK_DONE.compareAndSet(false, true)) {
				checkForUpdatesAsync(CheckTrigger.AUTO_JOIN);
			}
		});
	}

	public static boolean isUpdateAvailable() {
		return latestUpdate != null;
	}

	public static boolean hasKnownRelease() {
		return latestRelease != null;
	}

	public static void openLatestReleasePage() {
		ReleaseInfo info = Optional.ofNullable(latestUpdate).orElse(latestRelease);
		String url = info == null ? GITHUB_RELEASES_PAGE : nonBlankOrDefault(info.htmlUrl(), GITHUB_RELEASES_PAGE);
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		client.execute(() -> Util.getPlatform().openUri(url));
	}

	public static void checkForUpdatesAsync(CheckTrigger trigger) {
		if (!CHECK_RUNNING.compareAndSet(false, true)) {
			if (trigger != CheckTrigger.AUTO_JOIN) {
				sendInfo("Update check already running.");
			}
			return;
		}

		CompletableFuture.runAsync(() -> {
			try {
				CheckResult result = fetchLatestRelease();
				latestRelease = result.latestRelease();
				latestUpdate = result.updateAvailable() ? result.latestRelease() : null;

				if (result.latestRelease() == null) {
					if (trigger != CheckTrigger.AUTO_JOIN) {
						sendInfo("No valid GitHub release tag found (expected tags like Release-1.0.1).");
					}
					return;
				}

				if (result.updateAvailable()) {
					sendUpdateAvailableMessage(result.latestRelease());
				} else if (trigger != CheckTrigger.AUTO_JOIN) {
					sendInfo("You are up to date (" + CURRENT_VERSION_TEXT + ").");
				}
			} catch (Exception exception) {
				LOGGER.warn("Update check failed", exception);
				if (trigger != CheckTrigger.AUTO_JOIN) {
					sendInfo("Update check failed. Try again later.");
				}
			} finally {
				CHECK_RUNNING.set(false);
			}
		}, EXECUTOR);
	}

	public static void downloadLatestAsync(CheckTrigger trigger) {
		if (!DOWNLOAD_RUNNING.compareAndSet(false, true)) {
			sendInfo("A download is already in progress.");
			return;
		}

		CompletableFuture.runAsync(() -> {
			try {
				CheckResult result = fetchLatestRelease();
				latestRelease = result.latestRelease();
				latestUpdate = result.updateAvailable() ? result.latestRelease() : null;

				if (result.latestRelease() == null) {
					sendInfo("No valid release found to download.");
					return;
				}
				if (!result.updateAvailable()) {
					if (trigger != CheckTrigger.AUTO_JOIN) {
						sendInfo("You are already on the latest version (" + CURRENT_VERSION_TEXT + ").");
					}
					return;
				}

				ReleaseInfo release = result.latestRelease();
				if (release.asset() == null || release.asset().downloadUrl().isBlank()) {
					sendInfo("No downloadable .jar asset found in the latest release.");
					sendOpenReleaseHint(release);
					return;
				}

				Path downloadedFile = downloadReleaseAsset(release);
				PendingInstall install = prepareInstall(release, downloadedFile);
				pendingInstall = install;
				registerInstallHook();

				sendDownloadCompleteMessage(release);
			} catch (Exception exception) {
				LOGGER.warn("Update download failed", exception);
				sendInfo("Download failed. Check logs for details.");
			} finally {
				DOWNLOAD_RUNNING.set(false);
			}
		}, EXECUTOR);
	}

	private static void registerInstallHook() {
		if (!INSTALL_HOOK_REGISTERED.compareAndSet(false, true)) {
			return;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(ModUpdater::launchPendingInstaller, "Moissanite-Update-Installer"));
	}

	private static void launchPendingInstaller() {
		PendingInstall install = pendingInstall;
		if (install == null) {
			return;
		}

		try {
			Files.createDirectories(install.modsDir());
			if (isWindows()) {
				launchWindowsInstaller(install);
			} else {
				launchPosixInstaller(install);
			}
		} catch (Exception exception) {
			LOGGER.error("Failed to launch update installer process", exception);
		}
	}

	private static void launchWindowsInstaller(PendingInstall install) throws IOException {
		Path script = install.downloadedFile().getParent().resolve("install-update-" + System.currentTimeMillis() + ".cmd");
		String currentPath = install.currentJar() == null ? install.targetJar().toString() : install.currentJar().toString();
		String content = """
				@echo off
				setlocal
				timeout /t 2 /nobreak >nul
				set "DOWNLOAD=%s"
				set "TARGET=%s"
				set "CURRENT=%s"
				if not exist "%s" mkdir "%s"
				copy /Y "%%DOWNLOAD%%" "%%TARGET%%" >nul && (if /I not "%%CURRENT%%"=="%%TARGET%%" if exist "%%CURRENT%%" del /F /Q "%%CURRENT%%" >nul)
				if exist "%%DOWNLOAD%%" del /F /Q "%%DOWNLOAD%%" >nul
				del /F /Q "%%~f0" >nul
				""".formatted(
				escapeWindowsValue(install.downloadedFile().toString()),
				escapeWindowsValue(install.targetJar().toString()),
				escapeWindowsValue(currentPath),
				escapeWindowsValue(install.modsDir().toString()),
				escapeWindowsValue(install.modsDir().toString())
		);
		Files.writeString(script, content, StandardCharsets.UTF_8);
		new ProcessBuilder("cmd.exe", "/c", script.toString()).start();
	}

	private static void launchPosixInstaller(PendingInstall install) throws IOException {
		Path script = install.downloadedFile().getParent().resolve("install-update-" + System.currentTimeMillis() + ".sh");
		String currentPath = install.currentJar() == null ? install.targetJar().toString() : install.currentJar().toString();
		String content = """
				#!/bin/sh
				sleep 2
				mkdir -p %s
				if cp -f %s %s; then
				  if [ %s != %s ] && [ -f %s ]; then rm -f %s; fi
				fi
				rm -f %s
				rm -f "$0"
				""".formatted(
				quotePosix(install.modsDir().toString()),
				quotePosix(install.downloadedFile().toString()),
				quotePosix(install.targetJar().toString()),
				quotePosix(currentPath),
				quotePosix(install.targetJar().toString()),
				quotePosix(currentPath),
				quotePosix(currentPath),
				quotePosix(install.downloadedFile().toString())
		);
		Files.writeString(script, content, StandardCharsets.UTF_8);
		script.toFile().setExecutable(true, true);
		new ProcessBuilder("sh", script.toString()).start();
	}

	private static CheckResult fetchLatestRelease() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(GITHUB_RELEASES_API))
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", "Moissanite-Updater")
				.timeout(REQUEST_TIMEOUT)
				.GET()
				.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("GitHub API returned status " + response.statusCode());
		}

		JsonElement root = JsonParser.parseString(response.body());
		if (root == null || !root.isJsonArray()) {
			return new CheckResult(null, false);
		}

		ReleaseInfo latest = selectLatestRelease(root.getAsJsonArray());
		boolean updateAvailable = latest != null && CURRENT_VERSION != null && latest.version().compareTo(CURRENT_VERSION) > 0;
		return new CheckResult(latest, updateAvailable);
	}

	private static ReleaseInfo selectLatestRelease(JsonArray releases) {
		List<ReleaseInfo> candidates = new ArrayList<>();
		for (JsonElement element : releases) {
			if (element == null || !element.isJsonObject()) {
				continue;
			}
			JsonObject releaseObject = element.getAsJsonObject();
			if (readBoolean(releaseObject, "draft")) {
				continue;
			}
			if (readBoolean(releaseObject, "prerelease")) {
				continue;
			}

			String tagName = readString(releaseObject, "tag_name");
			String releaseName = readString(releaseObject, "name");
			String htmlUrl = readString(releaseObject, "html_url");

			Version version = parseReleaseVersion(tagName, releaseName);
			if (version == null) {
				continue;
			}

			String versionText = version.asText();
			ReleaseAsset asset = selectAsset(releaseObject.getAsJsonArray("assets"));
			candidates.add(new ReleaseInfo(tagName, releaseName, htmlUrl, version, versionText, asset));
		}

		return candidates.stream()
				.max(Comparator.comparing(ReleaseInfo::version))
				.orElse(null);
	}

	private static ReleaseAsset selectAsset(JsonArray assets) {
		if (assets == null) {
			return null;
		}

		ReleaseAsset selected = null;
		int selectedScore = Integer.MIN_VALUE;
		for (JsonElement assetElement : assets) {
			if (assetElement == null || !assetElement.isJsonObject()) {
				continue;
			}
			JsonObject assetObject = assetElement.getAsJsonObject();
			String name = readString(assetObject, "name");
			String downloadUrl = readString(assetObject, "browser_download_url");
			if (name == null || downloadUrl == null) {
				continue;
			}
			if (!name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
				continue;
			}

			int score = 0;
			String lowerName = name.toLowerCase(Locale.ROOT);
			if (lowerName.contains("moissanite")) {
				score += 4;
			}
			if (!lowerName.contains("sources")) {
				score += 2;
			}
			if (!lowerName.contains("dev")) {
				score += 1;
			}

			if (score > selectedScore) {
				selectedScore = score;
				selected = new ReleaseAsset(name, downloadUrl);
			}
		}
		return selected;
	}

	private static Path downloadReleaseAsset(ReleaseInfo release) throws IOException, InterruptedException {
		ReleaseAsset asset = release.asset();
		Objects.requireNonNull(asset, "release asset");
		Objects.requireNonNull(asset.downloadUrl(), "release asset download URL");

		Path updateDir = FabricLoader.getInstance().getConfigDir().resolve("moissanite").resolve("updates");
		Files.createDirectories(updateDir);

		String safeName = sanitizeFilename(asset.name(), "moissanite-" + release.versionText() + ".jar");
		Path target = updateDir.resolve(safeName);
		Path temp = updateDir.resolve(safeName + ".part");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(asset.downloadUrl()))
				.header("Accept", "application/octet-stream")
				.header("User-Agent", "Moissanite-Updater")
				.timeout(REQUEST_TIMEOUT)
				.GET()
				.build();
		HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Asset download failed with status " + response.statusCode());
		}

		try (InputStream stream = response.body()) {
			Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
		}

		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		return target.toAbsolutePath().normalize();
	}

	private static PendingInstall prepareInstall(ReleaseInfo release, Path downloadedFile) {
		Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods").toAbsolutePath().normalize();
		Path resolvedCurrentJar = resolveCurrentJarPath();
		Path currentJar = null;
		if (resolvedCurrentJar != null
				&& resolvedCurrentJar.getParent() != null
				&& resolvedCurrentJar.getParent().toAbsolutePath().normalize().equals(modsDir)) {
			currentJar = resolvedCurrentJar;
		}
		Path targetJar = selectTargetJar(modsDir, currentJar, release, downloadedFile);
		return new PendingInstall(downloadedFile, targetJar, currentJar, modsDir);
	}

	private static Path selectTargetJar(Path modsDir, Path currentJar, ReleaseInfo release, Path downloadedFile) {
		if (currentJar != null && currentJar.getParent() != null && currentJar.getParent().toAbsolutePath().normalize().equals(modsDir)) {
			return currentJar;
		}
		String fallbackName = release.asset() != null ? release.asset().name() : downloadedFile.getFileName().toString();
		String safeName = sanitizeFilename(fallbackName, "moissanite-" + release.versionText() + ".jar");
		return modsDir.resolve(safeName).toAbsolutePath().normalize();
	}

	private static Path resolveCurrentJarPath() {
		try {
			if (ModUpdater.class.getProtectionDomain() == null
					|| ModUpdater.class.getProtectionDomain().getCodeSource() == null
					|| ModUpdater.class.getProtectionDomain().getCodeSource().getLocation() == null) {
				return null;
			}
			URI location = ModUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			Path path = Path.of(location).toAbsolutePath().normalize();
			if (!Files.isRegularFile(path)) {
				return null;
			}
			if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
				return null;
			}
			return path;
		} catch (URISyntaxException | IllegalArgumentException ignored) {
			return null;
		}
	}

	private static Version parseReleaseVersion(String tagName, String releaseName) {
		Version fromTag = Version.parseReleaseTag(tagName);
		if (fromTag != null) {
			return fromTag;
		}
		Version fromName = Version.parseReleaseTag(releaseName);
		if (fromName != null) {
			return fromName;
		}
		return Version.parseLoose(tagName);
	}

	private static String resolveCurrentVersionText() {
		return FabricLoader.getInstance()
				.getModContainer(Moissanite.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.filter(value -> value != null && !value.isBlank())
				.orElse("0.0.0");
	}

	private static void sendUpdateAvailableMessage(ReleaseInfo release) {
		MutableComponent title = prefix()
				.append(Component.literal("Update available ").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(CURRENT_VERSION_TEXT + " -> " + release.versionText()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		sendComponent(title);

		MutableComponent actions = prefix()
				.append(button(
						"[Open Release]",
						ChatFormatting.GOLD,
						new ClickEvent.OpenUrl(URI.create(nonBlankOrDefault(release.htmlUrl(), GITHUB_RELEASES_PAGE))),
						"Open GitHub release page"))
				.append(Component.literal(" "))
				.append(button(
						"[Click to Download]",
						ChatFormatting.AQUA,
						new ClickEvent.RunCommand("/moissanite update download"),
						"Download now and auto-install when game closes"));
		sendComponent(actions);
	}

	private static void sendDownloadCompleteMessage(ReleaseInfo release) {
		MutableComponent line = prefix()
				.append(Component.literal("Downloaded ").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(release.versionText()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
				.append(Component.literal(". Close the game to install automatically.").withStyle(ChatFormatting.WHITE));
		sendComponent(line);
	}

	private static void sendOpenReleaseHint(ReleaseInfo release) {
		MutableComponent line = prefix()
				.append(Component.literal("Open release page: ").withStyle(ChatFormatting.WHITE))
				.append(button(
						"[Open]",
						ChatFormatting.GOLD,
						new ClickEvent.OpenUrl(URI.create(nonBlankOrDefault(release.htmlUrl(), GITHUB_RELEASES_PAGE))),
						"Open GitHub release page"));
		sendComponent(line);
	}

	private static MutableComponent button(String text, ChatFormatting color, ClickEvent clickEvent, String hoverText) {
		return Component.literal(text).withStyle(style -> style
				.withColor(color)
				.withBold(true)
				.withUnderlined(true)
				.withClickEvent(clickEvent)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
	}

	private static MutableComponent prefix() {
		return Component.literal("[Moissanite Update] ").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
	}

	private static void sendInfo(String text) {
		sendComponent(prefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY)));
	}

	private static void sendComponent(Component component) {
		if (component == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		if (!client.isSameThread()) {
			client.execute(() -> sendComponent(component));
			return;
		}
		if (client.player != null) {
			client.player.displayClientMessage(component, false);
		}
	}

	private static String sanitizeFilename(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		String sanitized = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
		return sanitized.isBlank() ? fallback : sanitized;
	}

	private static String escapeWindowsValue(String value) {
		return value == null ? "" : value.replace("\"", "\"\"");
	}

	private static String quotePosix(String value) {
		String safe = value == null ? "" : value;
		return "'" + safe.replace("'", "'\"'\"'") + "'";
	}

	private static boolean readBoolean(JsonObject object, String key) {
		if (object == null || key == null || !object.has(key)) {
			return false;
		}
		try {
			return object.get(key).getAsBoolean();
		} catch (Exception ignored) {
			return false;
		}
	}

	private static String readString(JsonObject object, String key) {
		if (object == null || key == null || !object.has(key)) {
			return null;
		}
		try {
			String value = object.get(key).getAsString();
			return value == null || value.isBlank() ? null : value;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	private static String nonBlankOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private record CheckResult(ReleaseInfo latestRelease, boolean updateAvailable) {
	}

	private record ReleaseInfo(
			String tagName,
			String releaseName,
			String htmlUrl,
			Version version,
			String versionText,
			ReleaseAsset asset) {
	}

	private record ReleaseAsset(String name, String downloadUrl) {
	}

	private record PendingInstall(Path downloadedFile, Path targetJar, Path currentJar, Path modsDir) {
	}

	private static final class Version implements Comparable<Version> {
		private final List<Integer> parts;

		private Version(List<Integer> parts) {
			this.parts = parts;
		}

		private static Version parseReleaseTag(String raw) {
			if (raw == null || raw.isBlank()) {
				return null;
			}
			Matcher matcher = RELEASE_TAG_PATTERN.matcher(raw.trim());
			if (!matcher.matches()) {
				return null;
			}
			return parseParts(matcher.group(1));
		}

		private static Version parseLoose(String raw) {
			if (raw == null || raw.isBlank()) {
				return null;
			}
			Matcher matcher = LOOSE_VERSION_PATTERN.matcher(raw.trim());
			if (!matcher.find()) {
				return null;
			}
			return parseParts(matcher.group(1));
		}

		private static Version parseParts(String text) {
			if (text == null || text.isBlank()) {
				return null;
			}
			String[] split = text.split("\\.");
			List<Integer> numbers = new ArrayList<>();
			for (String part : split) {
				try {
					numbers.add(Integer.parseInt(part));
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
			while (numbers.size() > 1 && numbers.get(numbers.size() - 1) == 0) {
				numbers.remove(numbers.size() - 1);
			}
			return new Version(numbers);
		}

		private String asText() {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < parts.size(); i++) {
				if (i > 0) {
					builder.append('.');
				}
				builder.append(parts.get(i));
			}
			return builder.toString();
		}

		@Override
		public int compareTo(Version other) {
			if (other == null) {
				return 1;
			}
			int max = Math.max(this.parts.size(), other.parts.size());
			for (int index = 0; index < max; index++) {
				int left = index < this.parts.size() ? this.parts.get(index) : 0;
				int right = index < other.parts.size() ? other.parts.get(index) : 0;
				if (left != right) {
					return Integer.compare(left, right);
				}
			}
			return 0;
		}
	}
}
