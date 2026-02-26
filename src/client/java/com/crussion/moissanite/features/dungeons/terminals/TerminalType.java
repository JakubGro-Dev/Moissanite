package com.crussion.moissanite.features.dungeons.terminals;

import java.util.regex.Pattern;

enum TerminalType {
	NUMBERS(0, 35, Pattern.compile("^Click in order!$")),
	COLORS(1, 53, Pattern.compile("^Select all the (.+?) items!$")),
	STARTSWITH(2, 44, Pattern.compile("^What starts with: '(.+?)'\\?$")),
	RUBIX(3, 44, Pattern.compile("^Change all to same color!$")),
	REDGREEN(4, 44, Pattern.compile("^Correct all the panes!$")),
	MELODY(5, 44, Pattern.compile("^Click the button on time!$"));

	private final int id;
	private final int slotCount;
	private final Pattern titlePattern;

	TerminalType(int id, int slotCount, Pattern titlePattern) {
		this.id = id;
		this.slotCount = slotCount;
		this.titlePattern = titlePattern;
	}

	int id() {
		return id;
	}

	int slotCount() {
		return slotCount;
	}

	boolean matches(String title) {
		return title != null && titlePattern.matcher(title).matches();
	}

	static TerminalType match(String title) {
		for (TerminalType type : values()) {
			if (type.matches(title)) {
				return type;
			}
		}
		return null;
	}
}
