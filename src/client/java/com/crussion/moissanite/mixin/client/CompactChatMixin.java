package com.crussion.moissanite.mixin.client;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.MoissaniteOccurrenceContent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@Mixin(value = ChatComponent.class, priority = Integer.MAX_VALUE)
public abstract class CompactChatMixin {
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
	private static final Style MOISSANITE_OCCURRENCE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));

	@Unique
	private final Map<String, MoissaniteMessageTracker> moissanite$messages = new HashMap<>();

	@Unique
	private boolean moissanite$lastCompactEnabled;

	@Unique
	private String moissanite$previousMessage;

	@Unique
	private String moissanite$pendingDuplicateRemoval;

	@ModifyVariable(
			method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"),
			argsOnly = true
	)
	private Component moissanite$compactMessage(Component component) {
		boolean compactEnabled = Boolean.TRUE.equals(UiDefinitions.COMPACT_CHAT.get());

		if (compactEnabled != moissanite$lastCompactEnabled) {
			moissanite$lastCompactEnabled = compactEnabled;
			moissanite$messages.clear();
			moissanite$previousMessage = null;
			moissanite$pendingDuplicateRemoval = null;
		}

		if (!compactEnabled || component == null) {
			moissanite$pendingDuplicateRemoval = null;
			return component;
		}

		String message = moissanite$stripIgnoredComponents(component);
		MoissaniteMessageTracker tracker = moissanite$messages.computeIfAbsent(message, value -> new MoissaniteMessageTracker());
		boolean shouldIgnore = moissanite$shouldIgnore(component, message);

		moissanite$previousMessage = message;

		if (shouldIgnore) {
			if (tracker.occurrences() == 0) {
				tracker.incrementOccurrences();
			}

			return component;
		}

		tracker.incrementOccurrences();

		if (tracker.occurrences() <= 1) {
			return component;
		}

		MutableComponent mutableMessage = component.copy();
		moissanite$pendingDuplicateRemoval = message;

		MutableComponent occurrencesText = MoissaniteOccurrenceContent.create(tracker.occurrences())
				.setStyle(MOISSANITE_OCCURRENCE_STYLE);

		return mutableMessage.append(occurrencesText);
	}

	@Inject(
			method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("TAIL")
	)
	private void moissanite$removeCompactedDuplicate(Component component, MessageSignature signature, GuiMessageTag tag,
			CallbackInfo ci) {
		String message = moissanite$pendingDuplicateRemoval;
		moissanite$pendingDuplicateRemoval = null;
		if (message == null) {
			return;
		}

		boolean skippedNewest = false;
		boolean removedOlderDuplicate = false;
		ListIterator<GuiMessage> iterator = allMessages.listIterator();

		while (iterator.hasNext()) {
			GuiMessage guiMessage = iterator.next();

			if (guiMessage == null || guiMessage.content() == null) {
				continue;
			}

			MutableComponent contentWithoutOccurrences = guiMessage.content().copy();
			contentWithoutOccurrences.getSiblings().removeIf(sibling -> sibling.getContents() instanceof MoissaniteOccurrenceContent);

			String content = moissanite$stripIgnoredComponents(contentWithoutOccurrences);

			if (content.equals(message)) {
				if (!skippedNewest) {
					skippedNewest = true;
					continue;
				}
				iterator.remove();
				removedOlderDuplicate = true;
				break;
			}
		}

		if (removedOlderDuplicate) {
			moissanite$refreshTrimmedMessagesPreservingScroll();
		}
	}

	@Inject(method = "clearMessages", at = @At("HEAD"))
	private void moissanite$clearCompactChat(boolean clearHistory, CallbackInfo ci) {
		moissanite$messages.clear();
		moissanite$previousMessage = null;
		moissanite$pendingDuplicateRemoval = null;
	}

	@Unique
	private static boolean moissanite$shouldIgnore(Component originalComponent, String message) {
		if (originalComponent.getString().isBlank()) {
			return true;
		}

		return message.contains("-----") || message.contains("======");
	}

	@Unique
	private static String moissanite$stripIgnoredComponents(Component component) {
		return component.getString();
	}

	@Unique
	private void moissanite$refreshTrimmedMessagesPreservingScroll() {
		int previousScroll = chatScrollbarPos;
		boolean previousNewMessageSinceScroll = newMessageSinceScroll;

		if (previousScroll > 0) {
			chatScrollbarPos = 0;
		}

		refreshTrimmedMessages();

		if (previousScroll <= 0) {
			return;
		}

		int maxScroll = Math.max(0, trimmedMessages.size() - getLinesPerPage());
		chatScrollbarPos = Math.min(previousScroll, maxScroll);
		newMessageSinceScroll = previousNewMessageSinceScroll && chatScrollbarPos > 0;
	}

	@Unique
	private static final class MoissaniteMessageTracker {
		private static final int MAXIMUM_OCCURRENCES = 100;

		private int occurrences;

		private int occurrences() {
			return occurrences;
		}

		private void incrementOccurrences() {
			if (occurrences == MAXIMUM_OCCURRENCES) {
				return;
			}

			occurrences++;
		}
	}
}
