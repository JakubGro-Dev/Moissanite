package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class IconButton extends AbstractWidget {
	public interface OnPress {
		void onPress(IconButton button);
	}

	private final Identifier icon;
	private final OnPress onPress;
	private final boolean drawFrame;
	private final int textureWidth;
	private final int textureHeight;

	public IconButton(int x, int y, int size, Identifier icon, Component narration, OnPress onPress) {
		this(x, y, size, icon, narration, onPress, true);
	}

	public IconButton(int x, int y, int size, Identifier icon, Component narration, OnPress onPress, boolean drawFrame) {
		this(x, y, size, icon, narration, onPress, drawFrame, 16, 16);
	}

	public IconButton(int x, int y, int size, Identifier icon, Component narration, OnPress onPress, boolean drawFrame, int textureWidth, int textureHeight) {
		super(x, y, size, size, narration);
		this.icon = icon;
		this.onPress = onPress;
		this.drawFrame = drawFrame;
		this.textureWidth = Math.max(1, textureWidth);
		this.textureHeight = Math.max(1, textureHeight);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		if (onPress != null && event.button() == 0) {
			onPress.onPress(this);
		}
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (drawFrame) {
			int radius = Math.max(6, Math.min(width, height) / 3);
			int bg = isHoveredOrFocused() ? Colors.BUTTON_HOVER : Colors.BUTTON_BG;
			int outline = isHoveredOrFocused() ? Colors.BUTTON_OUTLINE_HOVER : Colors.BUTTON_OUTLINE;
			UiShapes.fillRoundedOutline(graphics, getX(), getY(), width, height, radius, 1, outline, bg);
			UiShapes.fillRoundedRect(graphics, getX() + 2, getY() + 2, width - 4, 1, Math.max(3, radius - 2), Colors.PANEL_HIGHLIGHT);
		}

		if (icon != null) {
			int iconSize = Math.max(1, Math.min(width, height) - 5);
			int iconX = getX() + (width - iconSize) / 2;
			int iconY = getY() + (height - iconSize) / 2;
			graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					icon,
					iconX,
					iconY,
					0f,
					0f,
					iconSize,
					iconSize,
					textureWidth,
					textureHeight,
					textureWidth,
					textureHeight);
		} else {
			int centerX = getX() + width / 2;
			int centerY = getY() + height / 2;
			graphics.fill(centerX - 3, centerY - 1, centerX + 3, centerY + 1, Colors.TEXT_PRIMARY);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}
}
