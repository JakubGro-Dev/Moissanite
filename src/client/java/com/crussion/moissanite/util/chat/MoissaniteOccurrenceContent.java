package com.crussion.moissanite.util.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public final class MoissaniteOccurrenceContent implements ComponentContents {
	private static final int MAXIMUM_OCCURRENCES = 100;

	private static final MapCodec<MoissaniteOccurrenceContent> CODEC = Codec.INT
			.fieldOf("occurrences")
			.xmap(MoissaniteOccurrenceContent::new, content -> content.occurrences);

	private final int occurrences;

	public MoissaniteOccurrenceContent(int occurrences) {
		this.occurrences = occurrences;
	}

	public static MutableComponent create(int occurrences) {
		return MutableComponent.create(new MoissaniteOccurrenceContent(occurrences));
	}

	public String string() {
		if (occurrences >= MAXIMUM_OCCURRENCES) {
			return " (" + MAXIMUM_OCCURRENCES + "+)";
		}

		return " (" + occurrences + ")";
	}

	@Override
	public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
		return visitor.accept(style, string());
	}

	@Override
	public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
		return visitor.accept(string());
	}

	@Override
	public MapCodec<? extends ComponentContents> codec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "moissaniteCompactChatOccurrences{occurrences = " + occurrences + "}";
	}
}