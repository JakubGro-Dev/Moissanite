package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.CompactChatState;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	private static final long MOISSANITE_COMPACTION_UPDATE_INTERVAL_MS = 500L;

	@Unique
	private final ExecutorService moissanite$compactionExecutor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Moissanite-ChatCompactor");
		thread.setDaemon(true);
		return thread;
	});

	@Unique
	private final Object moissanite$trackedLock = new Object();

	@Unique
	private final Map<String, CompactChatState.TrackedMessage> moissanite$trackedMessages = new LinkedHashMap<>();

	@Unique
	private final List<CompactChatState.PendingInsert> moissanite$pendingInserts = new ArrayList<>();

	@Unique
	private final Map<String, CompactChatState.PendingCompaction> moissanite$pendingCompactions = new LinkedHashMap<>();

	@Unique
	private long moissanite$messageSequence;

	@Unique
	private boolean moissanite$flushScheduled;

	@Unique
	private volatile boolean moissanite$flushRequested;

	@Unique
	private volatile long moissanite$flushRequestedGeneration = -1L;

	@Inject(method = "clearMessages", at = @At("HEAD"))
	private void moissanite$onClearMessages(boolean clearHistory, CallbackInfo ci) {
		moissanite$resetPipelineState();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void moissanite$onTick(CallbackInfo ci) {
		long requestedGeneration;
		synchronized (moissanite$trackedLock) {
			moissanite$queueDueDirtyCompactions(System.currentTimeMillis());
			if (!moissanite$flushRequested) {
				return;
			}
			requestedGeneration = moissanite$flushRequestedGeneration;
			moissanite$flushRequested = false;
			moissanite$flushRequestedGeneration = -1L;
		}
		moissanite$flushPendingCompactions(requestedGeneration);
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

		long currentTimeMs = System.currentTimeMillis();
		boolean scheduleFlush = false;
		boolean passThrough = false;

		synchronized (moissanite$trackedLock) {
			if (generation != moissanite$pipelineGeneration) {
				return;
			}

			moissanite$cleanupExpired(currentTimeMs, maxAgeMs);

			String[] parsed = moissanite$parseMessage(component);
			if (parsed == null) {
				passThrough = true;
			} else {
				String senderKey = moissanite$normalizeKey(parsed[0]);
				String bodyKey = moissanite$normalizeKey(parsed[1]);
				if (senderKey.isEmpty() || bodyKey.isEmpty()) {
					passThrough = true;
				} else {
					String trackedKey = senderKey + "\u0000" + bodyKey;
					CompactChatState.TrackedMessage trackedMessage = moissanite$trackedMessages.get(trackedKey);
					if (trackedMessage == null) {
						moissanite$trackedMessages.put(trackedKey,
								new CompactChatState.TrackedMessage(component.copy(), currentTimeMs));
						moissanite$pendingInserts.add(new CompactChatState.PendingInsert(
								trackedKey,
								++moissanite$messageSequence,
								messageSignature,
								guiMessageTag));
						if (!moissanite$flushScheduled) {
							moissanite$flushScheduled = true;
							scheduleFlush = true;
						}
					} else {
						boolean queuedCompaction = false;
						trackedMessage.seenCount++;
						trackedMessage.lastSeenMs = currentTimeMs;
						trackedMessage.latestMessageSignature = messageSignature;
						trackedMessage.latestGuiMessageTag = guiMessageTag;
						if (trackedMessage.displayedCount > 0) {
							trackedMessage.compactionDirty = true;
							if (currentTimeMs >= trackedMessage.nextCompactionFlushMs) {
								moissanite$queuePendingCompaction(
										trackedKey,
										trackedMessage,
										currentTimeMs,
										messageSignature,
										guiMessageTag);
								queuedCompaction = true;
							}
						}
						if (queuedCompaction && !moissanite$flushScheduled) {
							moissanite$flushScheduled = true;
							scheduleFlush = true;
						}
					}
				}
			}
		}

		if (passThrough) {
			moissanite$schedulePassThrough(generation, component, messageSignature, guiMessageTag);
			return;
		}
		if (scheduleFlush) {
			moissanite$requestPendingFlush(generation);
		}
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
	private void moissanite$requestPendingFlush(long generation) {
		synchronized (moissanite$trackedLock) {
			if (generation != moissanite$pipelineGeneration) {
				return;
			}
			moissanite$flushRequested = true;
			moissanite$flushRequestedGeneration = generation;
		}
	}

	@Unique
	private void moissanite$flushPendingCompactions(long generation) {
		if (generation != moissanite$pipelineGeneration || minecraft.gui == null) {
			synchronized (moissanite$trackedLock) {
				if (generation == moissanite$pipelineGeneration) {
					moissanite$flushScheduled = false;
				}
			}
			return;
		}

		List<CompactChatState.PendingApply> pendingApplies = new ArrayList<>();
		synchronized (moissanite$trackedLock) {
			if (generation != moissanite$pipelineGeneration) {
				return;
			}

			moissanite$flushScheduled = false;

			for (CompactChatState.PendingInsert pendingInsert : moissanite$pendingInserts) {
				CompactChatState.TrackedMessage trackedMessage = moissanite$trackedMessages.get(pendingInsert.key);
				if (trackedMessage == null) {
					continue;
				}

				int targetCount = trackedMessage.seenCount;
				trackedMessage.nextCompactionFlushMs = System.currentTimeMillis() + MOISSANITE_COMPACTION_UPDATE_INTERVAL_MS;
				Component displayComponent = targetCount <= 1
						? trackedMessage.baseComponent.copy()
						: trackedMessage.baseComponent.copy().append(Component.literal(" (" + targetCount + ")"));
				pendingApplies.add(new CompactChatState.PendingApply(
						pendingInsert.key,
						null,
						displayComponent,
						targetCount,
						pendingInsert.sequence,
						pendingInsert.messageSignature,
						pendingInsert.guiMessageTag));
			}
			moissanite$pendingInserts.clear();

			Iterator<Map.Entry<String, CompactChatState.PendingCompaction>> iterator = moissanite$pendingCompactions.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, CompactChatState.PendingCompaction> entry = iterator.next();
				CompactChatState.TrackedMessage trackedMessage = moissanite$trackedMessages.get(entry.getKey());
				if (trackedMessage == null) {
					iterator.remove();
					continue;
				}

				CompactChatState.PendingCompaction pendingCompaction = entry.getValue();
				if (pendingCompaction.targetCount <= trackedMessage.displayedCount) {
					iterator.remove();
					continue;
				}
				if (trackedMessage.displayedMessage == null || trackedMessage.displayedCount <= 0) {
					continue;
				}

				pendingApplies.add(new CompactChatState.PendingApply(
						entry.getKey(),
						trackedMessage.displayedMessage,
						trackedMessage.baseComponent.copy().append(Component.literal(" (" + pendingCompaction.targetCount + ")")),
						pendingCompaction.targetCount,
						pendingCompaction.sequence,
						pendingCompaction.messageSignature,
						pendingCompaction.guiMessageTag));
				iterator.remove();
			}
		}

		if (pendingApplies.isEmpty()) {
			return;
		}

		pendingApplies.sort(Comparator.comparingLong(pendingApply -> pendingApply.sequence));
		for (int i = 0; i < pendingApplies.size(); i++) {
			if (generation != moissanite$pipelineGeneration || minecraft.gui == null) {
				return;
			}

			CompactChatState.PendingApply pendingApply = pendingApplies.get(i);
			if (pendingApply.previousMessage == null) {
				GuiMessage appliedMessage = moissanite$applyQueuedInsert(
						pendingApply.compacted,
						pendingApply.messageSignature,
						pendingApply.guiMessageTag);
				synchronized (moissanite$trackedLock) {
					if (generation != moissanite$pipelineGeneration) {
						return;
					}

					CompactChatState.TrackedMessage trackedMessage = moissanite$trackedMessages.get(pendingApply.key);
					if (trackedMessage == null) {
						continue;
					}
					trackedMessage.displayedMessage = appliedMessage;
					trackedMessage.displayedCount = pendingApply.targetCount;
				}
				continue;
			}

			int replacementEnd = i + 1;
			while (replacementEnd < pendingApplies.size() && pendingApplies.get(replacementEnd).previousMessage != null) {
				replacementEnd++;
			}
			List<CompactChatState.PendingApply> replacementBatch = pendingApplies.subList(i, replacementEnd);
			List<GuiMessage> appliedMessages = moissanite$applyQueuedReplacements(replacementBatch);
			synchronized (moissanite$trackedLock) {
				if (generation != moissanite$pipelineGeneration) {
					return;
				}

				for (int batchIndex = 0; batchIndex < replacementBatch.size() && batchIndex < appliedMessages.size(); batchIndex++) {
					CompactChatState.PendingApply replacement = replacementBatch.get(batchIndex);
					CompactChatState.TrackedMessage trackedMessage = moissanite$trackedMessages.get(replacement.key);
					if (trackedMessage == null) {
						continue;
					}
					trackedMessage.displayedMessage = appliedMessages.get(batchIndex);
					trackedMessage.displayedCount = replacement.targetCount;
				}
			}
			i = replacementEnd - 1;
		}
	}

	@Unique
	private GuiMessage moissanite$applyQueuedInsert(Component component, MessageSignature messageSignature,
			GuiMessageTag guiMessageTag) {
		moissanite$bypassPipeline = true;
		try {
			minecraft.gui.getChat().addMessage(component, messageSignature, guiMessageTag);
		} finally {
			moissanite$bypassPipeline = false;
		}
		return allMessages.isEmpty() ? null : allMessages.getFirst();
	}

	@Unique
	private List<GuiMessage> moissanite$applyQueuedReplacements(List<CompactChatState.PendingApply> replacements) {
		List<GuiMessage> appliedMessages = new ArrayList<>(replacements.size());
		int previousScroll = chatScrollbarPos;
		boolean preserveScroll = previousScroll > 0;

		for (CompactChatState.PendingApply replacement : replacements) {
			moissanite$removeTrackedMessage(replacement.previousMessage);

			GuiMessage guiMessage = new GuiMessage(
					minecraft.gui.getGuiTicks(),
					replacement.compacted,
					replacement.messageSignature,
					replacement.guiMessageTag);
			allMessages.addFirst(guiMessage);
			if (allMessages.size() > 100) {
				allMessages.removeLast();
			}
			appliedMessages.add(guiMessage);
		}

		refreshTrimmedMessages();
		if (!preserveScroll) {
			return appliedMessages;
		}

		int maxScroll = Math.max(0, trimmedMessages.size() - getLinesPerPage());
		chatScrollbarPos = Math.min(previousScroll, maxScroll);
		newMessageSinceScroll = chatScrollbarPos > 0;
		return appliedMessages;
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
		Iterator<Map.Entry<String, CompactChatState.TrackedMessage>> iterator = moissanite$trackedMessages.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, CompactChatState.TrackedMessage> entry = iterator.next();
			long ageMs = nowMs - entry.getValue().lastSeenMs;
			if (ageMs > maxAgeMs) {
				iterator.remove();
				moissanite$pendingCompactions.remove(entry.getKey());
			}
		}
	}

	@Unique
	private void moissanite$queueDueDirtyCompactions(long nowMs) {
		for (Map.Entry<String, CompactChatState.TrackedMessage> entry : moissanite$trackedMessages.entrySet()) {
			CompactChatState.TrackedMessage trackedMessage = entry.getValue();
			if (!trackedMessage.compactionDirty || trackedMessage.displayedCount <= 0) {
				continue;
			}
			if (nowMs < trackedMessage.nextCompactionFlushMs) {
				continue;
			}
			moissanite$queuePendingCompaction(
					entry.getKey(),
					trackedMessage,
					nowMs,
					trackedMessage.latestMessageSignature,
					trackedMessage.latestGuiMessageTag);
			if (!moissanite$flushScheduled) {
				moissanite$flushScheduled = true;
				moissanite$flushRequested = true;
				moissanite$flushRequestedGeneration = moissanite$pipelineGeneration;
			}
		}
	}

	@Unique
	private void moissanite$queuePendingCompaction(String trackedKey, CompactChatState.TrackedMessage trackedMessage,
			long nowMs, MessageSignature messageSignature, GuiMessageTag guiMessageTag) {
		trackedMessage.compactionDirty = false;
		trackedMessage.nextCompactionFlushMs = nowMs + MOISSANITE_COMPACTION_UPDATE_INTERVAL_MS;
		moissanite$pendingCompactions.put(
				trackedKey,
				new CompactChatState.PendingCompaction(
						trackedMessage.seenCount,
						++moissanite$messageSequence,
						messageSignature,
						guiMessageTag));
	}

	@Unique
	private void moissanite$removeTrackedMessage(GuiMessage trackedMessage) {
		if (trackedMessage == null) {
			return;
		}

		Iterator<GuiMessage> iterator = allMessages.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == trackedMessage) {
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
		return moissanite$collapseWhitespace(TextNormalizer.stripFormattingCodes(input));
	}

	@Unique
	private static String moissanite$collapseWhitespace(String input) {
		if (input == null || input.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder(input.length());
		boolean pendingSpace = false;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			char normalized = c == '\u00A0' ? ' ' : c;
			if (Character.isWhitespace(normalized)) {
				pendingSpace = builder.length() > 0;
				continue;
			}
			if (pendingSpace) {
				builder.append(' ');
				pendingSpace = false;
			}
			builder.append(normalized);
		}
		return builder.toString();
	}

	@Unique
	private static long moissanite$maxAgeMs() {
		Double configuredSeconds = UiDefinitions.COMPACT_CHAT_TIME.get();
		double seconds = configuredSeconds != null && Double.isFinite(configuredSeconds) ? configuredSeconds : 10.0D;
		return Math.max(1L, Math.round(seconds * 1000.0D));
	}

	@Unique
	private void moissanite$clearTracked() {
		moissanite$trackedMessages.clear();
		moissanite$pendingInserts.clear();
		moissanite$pendingCompactions.clear();
		moissanite$messageSequence = 0L;
		moissanite$flushScheduled = false;
		moissanite$flushRequested = false;
		moissanite$flushRequestedGeneration = -1L;
	}
}
