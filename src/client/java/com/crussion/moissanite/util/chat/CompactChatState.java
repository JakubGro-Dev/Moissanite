package com.crussion.moissanite.util.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public final class CompactChatState {
	private CompactChatState() {
	}

	public static final class TrackedMessage {
		public final Component baseComponent;
		public int seenCount;
		public int displayedCount;
		public long lastSeenMs;
		public GuiMessage displayedMessage;
		public boolean compactionDirty;
		public long nextCompactionFlushMs;
		public MessageSignature latestMessageSignature;
		public GuiMessageTag latestGuiMessageTag;

		public TrackedMessage(Component baseComponent, long lastSeenMs) {
			this.baseComponent = baseComponent;
			this.seenCount = 1;
			this.displayedCount = 0;
			this.lastSeenMs = lastSeenMs;
			this.compactionDirty = false;
			this.nextCompactionFlushMs = lastSeenMs;
		}
	}

	public static final class PendingCompaction {
		public final int targetCount;
		public final long sequence;
		public final MessageSignature messageSignature;
		public final GuiMessageTag guiMessageTag;

		public PendingCompaction(int targetCount, long sequence, MessageSignature messageSignature,
				GuiMessageTag guiMessageTag) {
			this.targetCount = targetCount;
			this.sequence = sequence;
			this.messageSignature = messageSignature;
			this.guiMessageTag = guiMessageTag;
		}
	}

	public static final class PendingInsert {
		public final String key;
		public final long sequence;
		public final MessageSignature messageSignature;
		public final GuiMessageTag guiMessageTag;

		public PendingInsert(String key, long sequence, MessageSignature messageSignature, GuiMessageTag guiMessageTag) {
			this.key = key;
			this.sequence = sequence;
			this.messageSignature = messageSignature;
			this.guiMessageTag = guiMessageTag;
		}
	}

	public static final class PendingApply {
		public final String key;
		public final GuiMessage previousMessage;
		public final Component compacted;
		public final int targetCount;
		public final long sequence;
		public final MessageSignature messageSignature;
		public final GuiMessageTag guiMessageTag;

		public PendingApply(String key, GuiMessage previousMessage, Component compacted, int targetCount,
				long sequence, MessageSignature messageSignature, GuiMessageTag guiMessageTag) {
			this.key = key;
			this.previousMessage = previousMessage;
			this.compacted = compacted;
			this.targetCount = targetCount;
			this.sequence = sequence;
			this.messageSignature = messageSignature;
			this.guiMessageTag = guiMessageTag;
		}
	}
}
