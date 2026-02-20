package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.data.UiButton;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.render.UiTextRenderer;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class RoundedActionButton extends AbstractWidget {
	private final UiButton action;
	private final Font font;

	public RoundedActionButton(Font font, int x, int y, int width, int height, UiButton action) {
		super(x, y, width, height, UiText.uiTextStatic(action.buttonText()));
		this.action = action;
		this.font = font;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		if (event.button() == 0) {
			action.press();
		}
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int radius = Math.max(4, height / 3);
		int bg = isHoveredOrFocused() ? Colors.BUTTON_HOVER : Colors.BUTTON_BG;
		UiShapes.fillRoundedRect(graphics, getX(), getY(), width, height, radius, Colors.BUTTON_OUTLINE);
		UiShapes.fillRoundedRect(graphics, getX() + 1, getY() + 1, width - 2, height - 2, Math.max(3, radius - 1), bg);
		int textX = getX() + (width - font.width(getMessage())) / 2;
		int textY = getY() + (height - font.lineHeight) / 2;
		UiTextRenderer.drawBoldString(graphics, font, getMessage(), textX, textY, Colors.TEXT_PRIMARY, 1.02f);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}
}
