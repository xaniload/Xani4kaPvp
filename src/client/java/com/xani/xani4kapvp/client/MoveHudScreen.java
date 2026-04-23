package com.xani.xani4kapvp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MoveHudScreen extends Screen {
	private final TargetArmorHudController controller;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public MoveHudScreen(TargetArmorHudController controller) {
		super(Component.translatable("screen.xani4kapvp.move_hud"));
		this.controller = controller;
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xB0000000);
		super.render(context, mouseX, mouseY, delta);
		controller.renderPreview(Minecraft.getInstance(), context);
		context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
		context.drawCenteredString(font, Component.translatable("text.xani4kapvp.move_hint"), width / 2, 38, 0xD0D0D0);
		context.drawCenteredString(font, Component.translatable("text.xani4kapvp.reset_hint"), width / 2, 50, 0xD0D0D0);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		int panelX = controller.getRenderX(width);
		int panelY = controller.getRenderY();
		boolean hovered = mouseX >= panelX && mouseX <= panelX + controller.getPanelWidth()
			&& mouseY >= panelY && mouseY <= panelY + controller.getPanelHeight();

		if (hovered) {
			dragging = true;
			dragOffsetX = (int) mouseX - panelX;
			dragOffsetY = (int) mouseY - panelY;
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}

		controller.setPosition((int) mouseX - dragOffsetX, (int) mouseY - dragOffsetY, width, height);
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			dragging = false;
			controller.save();
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_R) {
			controller.resetPosition();
			controller.save();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
