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
		int radius = Math.max(6, height / 3);
		int bg = isHoveredOrFocused() ? Colors.BUTTON_HOVER : Colors.BUTTON_BG;
		int outline = isHoveredOrFocused() ? Colors.BUTTON_OUTLINE_HOVER : Colors.BUTTON_OUTLINE;
		UiShapes.fillRoundedOutline(graphics, getX(), getY(), width, height, radius, 1, outline, bg);
		UiShapes.fillRoundedRect(graphics, getX() + 2, getY() + 2, width - 4, 1, Math.max(3, radius - 2), Colors.PANEL_HIGHLIGHT);
		int textX = getX() + (width - font.width(getMessage())) / 2;
		int textY = getY() + (height - font.lineHeight) / 2;
		UiTextRenderer.drawBoldString(graphics, font, getMessage(), textX, textY, Colors.TEXT_PRIMARY, 1.0f);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}
}
