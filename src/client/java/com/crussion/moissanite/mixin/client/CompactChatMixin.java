package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ChatComponent.class)
public abstract class CompactChatMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private List<GuiMessage> allMessages;

	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	private boolean newMessageSinceScroll;

	@Shadow
	protected abstract void refreshTrimmedMessages();

	@Shadow
	public abstract int getLinesPerPage();

	@Unique
	private static final String MOISSANITE_SERVER_SENDER_KEY = "__server__";

	@Unique
	private final ExecutorService moissanite$compactionExecutor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Moissanite-ChatCompactor");
		thread.setDaemon(true);
		return thread;
	});

	@Unique
	private final Object moissanite$trackedLock = new Object();

	@Unique
	private final List<String> moissanite$trackedKeys = new ArrayList<>();

	@Unique
	private final List<Component> moissanite$trackedBaseComponents = new ArrayList<>();

	@Unique
	private final List<String> moissanite$trackedBasePlainTexts = new ArrayList<>();

	@Unique
	private final List<Integer> moissanite$trackedCounts = new ArrayList<>();

	@Unique
	private final List<Long> moissanite$trackedTimestampsMs = new ArrayList<>();

	@Inject(method = "clearMessages", at = @At("HEAD"))
	private void moissanite$onClearMessages(boolean clearHistory, CallbackInfo ci) {
		moissanite$resetPipelineState();
	}

	@Unique
	private volatile long moissanite$pipelineGeneration = 0L;

	@Unique
	private boolean moissanite$pipelineEnabled;

	@Unique
	private boolean moissanite$bypassPipeline;

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
	private void moissanite$onAddMessage(Component component, MessageSignature messageSignature,
			GuiMessageTag guiMessageTag, CallbackInfo ci) {
		if (component == null || minecraft == null) {
			return;
		}
		if (moissanite$bypassPipeline) {
			return;
		}
		if (!minecraft.isSameThread()) {
			Component scheduledComponent = component.copy();
			minecraft.execute(() -> {
				if (minecraft.gui != null) {
					minecraft.gui.getChat().addMessage(scheduledComponent, messageSignature, guiMessageTag);
				}
			});
			ci.cancel();
			return;
		}

		boolean compactEnabled = Boolean.TRUE.equals(UiDefinitions.COMPACT_CHAT.get());
		if (!compactEnabled) {
			if (moissanite$pipelineEnabled) {
				moissanite$pipelineEnabled = false;
				moissanite$resetPipelineState();
			}
			return;
		}
		if (!moissanite$pipelineEnabled) {
			moissanite$pipelineEnabled = true;
			moissanite$resetPipelineState();
		}
		if (minecraft.gui == null) {
			return;
		}

		long generation = moissanite$pipelineGeneration;
		long maxAgeMs = moissanite$maxAgeMs();
		Component queuedComponent = component.copy();
		moissanite$compactionExecutor.execute(() -> moissanite$processQueuedMessage(
				generation,
				queuedComponent,
				messageSignature,
				guiMessageTag,
				maxAgeMs));
		ci.cancel();
	}

	@Unique
	private void moissanite$processQueuedMessage(long generation, Component component,
			MessageSignature messageSignature, GuiMessageTag guiMessageTag, long maxAgeMs) {
		if (generation != moissanite$pipelineGeneration) {
			return;
		}

		String previousRenderedText = null;
		Component compacted = null;
		long currentTimeMs = System.currentTimeMillis();

		synchronized (moissanite$trackedLock) {
			if (generation != moissanite$pipelineGeneration) {
				return;
			}

			moissanite$cleanupExpired(currentTimeMs, maxAgeMs);

			String[] parsed = moissanite$parseMessage(component);
			if (parsed == null) {
				moissanite$schedulePassThrough(generation, component, messageSignature, guiMessageTag);
				return;
			}

			String senderKey = moissanite$normalizeKey(parsed[0]);
			String bodyKey = moissanite$normalizeKey(parsed[1]);
			if (senderKey.isEmpty() || bodyKey.isEmpty()) {
				moissanite$schedulePassThrough(generation, component, messageSignature, guiMessageTag);
				return;
			}

			String key = senderKey + "\u0000" + bodyKey;
			int trackedIndex = moissanite$findTrackedIndex(key);
			if (trackedIndex < 0) {
				String basePlainText = moissanite$plainText(component.getString());
				moissanite$trackedKeys.add(key);
				moissanite$trackedBaseComponents.add(component.copy());
				moissanite$trackedBasePlainTexts.add(basePlainText);
				moissanite$trackedCounts.add(1);
				moissanite$trackedTimestampsMs.add(currentTimeMs);
				moissanite$schedulePassThrough(generation, component, messageSignature, guiMessageTag);
				return;
			}

			String basePlainText = moissanite$trackedBasePlainTexts.get(trackedIndex);
			int previousCount = moissanite$trackedCounts.get(trackedIndex);
			previousRenderedText = moissanite$renderedPlainText(basePlainText, previousCount);
			int nextCount = previousCount + 1;

			moissanite$trackedCounts.set(trackedIndex, nextCount);
			moissanite$trackedTimestampsMs.set(trackedIndex, currentTimeMs);

			compacted = moissanite$trackedBaseComponents.get(trackedIndex).copy()
					.append(Component.literal(" (" + nextCount + ")"));
		}

		moissanite$scheduleCompactReplace(generation, previousRenderedText, compacted, messageSignature, guiMessageTag);
	}

	@Unique
	private void moissanite$schedulePassThrough(long generation, Component component, MessageSignature messageSignature,
			GuiMessageTag guiMessageTag) {
		minecraft.execute(() -> {
			if (generation != moissanite$pipelineGeneration || minecraft.gui == null) {
				return;
			}

			moissanite$bypassPipeline = true;
			try {
				minecraft.gui.getChat().addMessage(component, messageSignature, guiMessageTag);
			} finally {
				moissanite$bypassPipeline = false;
			}
		});
	}

	@Unique
	private void moissanite$scheduleCompactReplace(long generation, String previousRenderedText, Component compacted,
			MessageSignature messageSignature, GuiMessageTag guiMessageTag) {
		minecraft.execute(() -> {
			if (generation != moissanite$pipelineGeneration || minecraft.gui == null || compacted == null) {
				return;
			}

			moissanite$applyCompactedReplace(previousRenderedText, compacted, messageSignature, guiMessageTag);
		});
	}

	@Unique
	private void moissanite$applyCompactedReplace(String previousRenderedText, Component compacted,
			MessageSignature messageSignature, GuiMessageTag guiMessageTag) {
		int previousScroll = chatScrollbarPos;
		boolean preserveScroll = previousScroll > 0;

		moissanite$removeLatestRendered(previousRenderedText);

		GuiMessage guiMessage = new GuiMessage(
				minecraft.gui.getGuiTicks(),
				compacted,
				messageSignature,
				guiMessageTag);
		allMessages.addFirst(guiMessage);
		if (allMessages.size() > 100) {
			allMessages.removeLast();
		}

		refreshTrimmedMessages();
		if (!preserveScroll) {
			return;
		}

		int maxScroll = Math.max(0, trimmedMessages.size() - getLinesPerPage());
		chatScrollbarPos = Math.min(previousScroll, maxScroll);
		newMessageSinceScroll = chatScrollbarPos > 0;
	}

	@Unique
	private void moissanite$resetPipelineState() {
		moissanite$pipelineGeneration++;
		moissanite$compactionExecutor.execute(() -> {
			synchronized (moissanite$trackedLock) {
				moissanite$clearTracked();
			}
		});
	}

	@Unique
	private void moissanite$cleanupExpired(long nowMs, long maxAgeMs) {
		for (int i = moissanite$trackedKeys.size() - 1; i >= 0; i--) {
			long ageMs = nowMs - moissanite$trackedTimestampsMs.get(i);
			if (ageMs > maxAgeMs) {
				moissanite$removeTrackedAt(i);
			}
		}
	}

	@Unique
	private void moissanite$removeTrackedAt(int index) {
		moissanite$trackedKeys.remove(index);
		moissanite$trackedBaseComponents.remove(index);
		moissanite$trackedBasePlainTexts.remove(index);
		moissanite$trackedCounts.remove(index);
		moissanite$trackedTimestampsMs.remove(index);
	}

	@Unique
	private int moissanite$findTrackedIndex(String key) {
		for (int i = 0; i < moissanite$trackedKeys.size(); i++) {
			if (moissanite$trackedKeys.get(i).equals(key)) {
				return i;
			}
		}
		return -1;
	}

	@Unique
	private static String moissanite$renderedPlainText(String base, int count) {
		return count <= 1 ? base : base + " (" + count + ")";
	}

	@Unique
	private void moissanite$removeLatestRendered(String renderedText) {
		if (renderedText == null || renderedText.isEmpty()) {
			return;
		}
		Iterator<GuiMessage> iterator = allMessages.iterator();
		while (iterator.hasNext()) {
			GuiMessage message = iterator.next();
			String plain = moissanite$plainText(message.content().getString());
			if (renderedText.equals(plain)) {
				iterator.remove();
				return;
			}
		}
	}

	@Unique
	private static String[] moissanite$parseMessage(Component component) {
		String text = moissanite$plainText(component.getString());
		if (text.isEmpty()) {
			return null;
		}

		if (text.startsWith("<")) {
			int end = text.indexOf("> ");
			if (end > 1 && end + 2 < text.length()) {
				String sender = text.substring(1, end);
				String body = text.substring(end + 2);
				if (!sender.isBlank() && !body.isBlank()) {
					return new String[] { sender, body };
				}
			}
		}

		int colonIndex = text.indexOf(": ");
		if (colonIndex > 0 && colonIndex + 2 < text.length()) {
			String sender = text.substring(0, colonIndex);
			String body = text.substring(colonIndex + 2);
			if (!sender.isBlank() && !body.isBlank()) {
				return new String[] { sender, body };
			}
		}

		int arrowIndex = text.indexOf(" > ");
		if (arrowIndex > 0 && arrowIndex + 3 < text.length()) {
			String sender = text.substring(0, arrowIndex);
			String body = text.substring(arrowIndex + 3);
			if (!sender.isBlank() && !body.isBlank()) {
				return new String[] { sender, body };
			}
		}

		return new String[] { MOISSANITE_SERVER_SENDER_KEY, text };
	}

	@Unique
	private static String moissanite$normalizeKey(String input) {
		return moissanite$plainText(input);
	}

	@Unique
	private static String moissanite$plainText(String input) {
		if (input == null) {
			return "";
		}
		String stripped = TextNormalizer.stripFormattingCodes(input)
				.replace('\u00A0', ' ')
				.trim();
		return stripped.replaceAll("\\s+", " ");
	}

	@Unique
	private static long moissanite$maxAgeMs() {
		Double configuredSeconds = UiDefinitions.COMPACT_CHAT_TIME.get();
		double seconds = configuredSeconds != null && Double.isFinite(configuredSeconds) ? configuredSeconds : 10.0D;
		return Math.max(1L, Math.round(seconds * 1000.0D));
	}

	@Unique
	private void moissanite$clearTracked() {
		moissanite$trackedKeys.clear();
		moissanite$trackedBaseComponents.clear();
		moissanite$trackedBasePlainTexts.clear();
		moissanite$trackedCounts.clear();
		moissanite$trackedTimestampsMs.clear();
	}
}
