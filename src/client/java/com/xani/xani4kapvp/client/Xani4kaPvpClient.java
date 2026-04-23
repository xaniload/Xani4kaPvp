package com.xani.xani4kapvp.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xani.xani4kapvp.Xani4kaPvp;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Xani4kaPvpClient implements ClientModInitializer {
	private final TargetArmorHudController hudController = new TargetArmorHudController();

	@Override
	public void onInitializeClient() {
		hudController.load();

		KeyMapping moveHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.xani4kapvp.move_hud",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			"category.xani4kapvp.general"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			hudController.tick(client);

			while (moveHudKey.consumeClick()) {
				if (client.player != null && client.level != null) {
					client.setScreen(new MoveHudScreen(hudController));
				}
			}
		});

		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.screen instanceof MoveHudScreen) {
				return;
			}

			hudController.render(client, drawContext);
		});

		Xani4kaPvp.LOGGER.info("Initialized {}", Xani4kaPvp.MOD_ID);
	}
}
