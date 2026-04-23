package com.xani.xani4kapvp.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.xani.xani4kapvp.Xani4kaPvp;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class TargetArmorHudController {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int PANEL_WIDTH = 116;
	private static final int PANEL_HEIGHT = 90;
	private static final int PANEL_MARGIN = 12;
	private static final int TARGET_MEMORY_TICKS = 40;

	private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve(Xani4kaPvp.MOD_ID + ".json");
	private HudConfig config = new HudConfig();
	private UUID rememberedTargetId;
	private int rememberedTargetTicks;

	public void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			rememberedTargetId = null;
			rememberedTargetTicks = 0;
			return;
		}

		Player directTarget = getCrosshairTarget(client);
		if (directTarget != null) {
			rememberedTargetId = directTarget.getUUID();
			rememberedTargetTicks = TARGET_MEMORY_TICKS;
		} else if (rememberedTargetTicks > 0) {
			rememberedTargetTicks--;
		} else {
			rememberedTargetId = null;
		}
	}

	public void render(Minecraft client, GuiGraphics context) {
		if (client.player == null || client.level == null) {
			return;
		}

		Player target = getActiveTarget(client);
		if (target == null) {
			return;
		}

		renderPanel(context, client, target, target.getName(), false);
	}

	public void renderPreview(Minecraft client, GuiGraphics context) {
		Player previewTarget = getActiveTarget(client);
		if (previewTarget == null) {
			previewTarget = client.player;
		}

		if (previewTarget != null) {
			renderPanel(context, client, previewTarget, Component.translatable("text.xani4kapvp.preview"), true);
		}
	}

	public void setPosition(int x, int y, int screenWidth, int screenHeight) {
		config.x = Math.max(0, Math.min(x, Math.max(0, screenWidth - PANEL_WIDTH)));
		config.y = Math.max(0, Math.min(y, Math.max(0, screenHeight - PANEL_HEIGHT)));
	}

	public void resetPosition() {
		config.x = -1;
		config.y = -1;
	}

	public int getRenderX(int screenWidth) {
		return config.x >= 0 ? config.x : screenWidth - PANEL_WIDTH - PANEL_MARGIN;
	}

	public int getRenderY() {
		return config.y >= 0 ? config.y : PANEL_MARGIN;
	}

	public int getPanelWidth() {
		return PANEL_WIDTH;
	}

	public int getPanelHeight() {
		return PANEL_HEIGHT;
	}

	public void load() {
		if (!Files.exists(configPath)) {
			config = new HudConfig();
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath)) {
			HudConfig loaded = GSON.fromJson(reader, HudConfig.class);
			config = loaded == null ? new HudConfig() : loaded;
		} catch (IOException | JsonParseException exception) {
			Xani4kaPvp.LOGGER.warn("Failed to load HUD config from {}", configPath, exception);
			config = new HudConfig();
		}
	}

	public void save() {
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			Xani4kaPvp.LOGGER.warn("Failed to save HUD config to {}", configPath, exception);
		}
	}

	private void renderPanel(GuiGraphics context, Minecraft client, Player target, Component title, boolean preview) {
		int x = getRenderX(context.guiWidth());
		int y = getRenderY();
		int background = preview ? 0xC0202020 : 0x90000000;

		context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, background);
		context.renderOutline(x, y, PANEL_WIDTH, PANEL_HEIGHT, 0x80FFFFFF);
		context.drawString(client.font, title, x + 8, y + 8, 0xFFFFFF, false);

		List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
		for (int index = 0; index < slots.size(); index++) {
			int rowY = y + 24 + index * 15;
			renderArmorRow(context, client.font, target.getItemBySlot(slots.get(index)), x + 8, rowY);
		}
	}

	private void renderArmorRow(GuiGraphics context, Font textRenderer, ItemStack stack, int x, int y) {
		context.fill(x, y, x + 16, y + 16, 0x35222222);
		context.renderOutline(x, y, 16, 16, 0x60FFFFFF);

		if (!stack.isEmpty()) {
			context.renderItem(stack, x, y);
		}

		int textColor = 0xB0B0B0;
		String durabilityText = "-";
		if (!stack.isEmpty() && stack.isDamageableItem()) {
			int max = stack.getMaxDamage();
			int remaining = max - stack.getDamageValue();
			float ratio = max <= 0 ? 1.0F : (float) remaining / max;
			textColor = getDurabilityColor(ratio);
			durabilityText = remaining + "/" + max;
		}

		context.drawString(textRenderer, durabilityText, x + 22, y + 4, textColor, false);
	}

	private int getDurabilityColor(float ratio) {
		if (ratio > 0.66F) {
			return 0x55FF55;
		}
		if (ratio > 0.33F) {
			return 0xFFAA00;
		}
		return 0xFF5555;
	}

	private Player getActiveTarget(Minecraft client) {
		Player directTarget = getCrosshairTarget(client);
		if (directTarget != null) {
			return directTarget;
		}

		if (rememberedTargetId == null || client.level == null) {
			return null;
		}

		for (Player player : client.level.players()) {
			if (player.getUUID().equals(rememberedTargetId) && player.isAlive() && player != client.player) {
				return player;
			}
		}

		return null;
	}

	private Player getCrosshairTarget(Minecraft client) {
		HitResult hitResult = client.hitResult;
		if (!(hitResult instanceof EntityHitResult entityHitResult)) {
			return null;
		}

		if (entityHitResult.getEntity() instanceof Player player && player != client.player && player.isAlive()) {
			return player;
		}

		return null;
	}
}
