package com.zhra.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class ZhraClient implements ClientModInitializer {
    private static final int NAVY = 0xE9101B35;
    private static final int PANEL = 0xE8172948;
    private static final int CYAN = 0xFF28D7F2;
    private static final int WHITE = 0xFFF2F7FF;
    private static final int MUTED = 0xFF9DB3CC;
    private static final int GREEN = 0xFF55E6A5;
    private static final int ORANGE = 0xFFFFC36B;

    private static boolean hudEnabled = true;
    private static boolean settingsOpen = false;
    private static boolean sprintToggle = false;
    private static boolean sneakToggle = false;
    private static boolean zoomActive = false;
    private static int originalFov = 70;

    private static KeyBinding toggleHudKey;
    private static KeyBinding settingsKey;
    private static KeyBinding sprintToggleKey;
    private static KeyBinding sneakToggleKey;
    private static KeyBinding zoomKey;

    @Override
    public void onInitializeClient() {
        toggleHudKey = register("key.zhra.toggle_hud", GLFW.GLFW_KEY_H);
        settingsKey = register("key.zhra.settings", GLFW.GLFW_KEY_M);
        sprintToggleKey = register("key.zhra.toggle_sprint", GLFW.GLFW_KEY_G);
        sneakToggleKey = register("key.zhra.toggle_sneak", GLFW.GLFW_KEY_N);
        zoomKey = register("key.zhra.zoom", GLFW.GLFW_KEY_C);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                hudEnabled = !hudEnabled;
            }
            while (settingsKey.wasPressed()) {
                settingsOpen = !settingsOpen;
            }
            while (sprintToggleKey.wasPressed()) {
                sprintToggle = !sprintToggle;
            }
            while (sneakToggleKey.wasPressed()) {
                sneakToggle = !sneakToggle;
            }

            updateZoom(client);

            if (client.player != null) {
                if (sprintToggle) {
                    client.player.setSprinting(true);
                }
                if (sneakToggle) {
                    client.player.setSneaking(true);
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudEnabled) {
                renderHud(MinecraftClient.getInstance(), drawContext);
            }
        });
    }

    private static KeyBinding register(String translationKey, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                InputUtil.Type.KEYSYM,
                defaultKey,
                "category.zhra"
        ));
    }

    private static void updateZoom(MinecraftClient client) {
        if (client.options == null) {
            return;
        }

        boolean pressed = zoomKey.isPressed();
        if (pressed && !zoomActive) {
            originalFov = client.options.getFov().getValue();
            zoomActive = true;
        }

        if (pressed) {
            client.options.getFov().setValue(Math.min(originalFov, 30));
        } else if (zoomActive) {
            client.options.getFov().setValue(originalFov);
            zoomActive = false;
        }
    }

    private static void renderHud(MinecraftClient client,
                                  net.minecraft.client.gui.DrawContext context) {
        if (client.player == null || client.world == null) {
            return;
        }

        int x = 10;
        int y = 10;
        int width = 142;
        int height = 78;

        context.fill(x, y, x + width, y + height, NAVY);
        context.fill(x, y, x + width, y + 3, CYAN);
        context.fill(x, y + 3, x + width, y + height, PANEL);

        context.drawTextWithShadow(client.textRenderer, "ZHRA CLIENT", x + 8, y + 8, CYAN);
        context.drawTextWithShadow(client.textRenderer,
                "FPS  " + client.getCurrentFps(), x + 8, y + 23, WHITE);

        int ping = getPing(client);
        String pingText = ping >= 0 ? ping + " ms" : "--";
        context.drawTextWithShadow(client.textRenderer,
                "PING  " + pingText, x + 8, y + 36, WHITE);

        String coordinates = String.format("XYZ  %d %d %d",
                (int) client.player.getX(),
                (int) client.player.getY(),
                (int) client.player.getZ());
        context.drawTextWithShadow(client.textRenderer,
                coordinates, x + 8, y + 49, MUTED);

        String movement = "SPRINT " + (sprintToggle ? "ON" : "OFF")
                + "  SNEAK " + (sneakToggle ? "ON" : "OFF");
        context.drawTextWithShadow(client.textRenderer, movement, x + 8, y + 63,
                sprintToggle || sneakToggle ? GREEN : MUTED);

        renderKeystrokes(client, context, x, y + height + 7);
        renderArmor(client, context, x, y + height + 62);

        if (settingsOpen) {
            renderSettings(client, context, x + 151, y);
        }
    }

    private static int getPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) {
            return -1;
        }

        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private static void renderKeystrokes(MinecraftClient client,
                                         net.minecraft.client.gui.DrawContext context,
                                         int x, int y) {
        int cell = 22;
        int gap = 3;
        int totalWidth = cell * 3 + gap * 2;
        context.fill(x, y, x + totalWidth, y + cell * 2 + gap, NAVY);

        drawKey(context, client, "W", client.options.forwardKey.isPressed(),
                x + cell + gap, y, cell);
        drawKey(context, client, "A", client.options.leftKey.isPressed(),
                x, y + cell + gap, cell);
        drawKey(context, client, "S", client.options.backKey.isPressed(),
                x + cell + gap, y + cell + gap, cell);
        drawKey(context, client, "D", client.options.rightKey.isPressed(),
                x + (cell + gap) * 2, y + cell + gap, cell);
    }

    private static void renderArmor(MinecraftClient client,
                                    net.minecraft.client.gui.DrawContext context,
                                    int x, int y) {
        context.fill(x, y, x + 142, y + 34, NAVY);
        context.drawTextWithShadow(client.textRenderer, "ARMOR", x + 6, y + 4, CYAN);

        var armor = client.player.getInventory().armor;
        String[] slots = {"BO", "LE", "CH", "HE"};
        int slotX = x + 6;
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            int color = stack.isEmpty() ? MUTED : WHITE;
            String value = stack.isEmpty() ? slots[i] + " --"
                    : slots[i] + " " + getDurability(stack) + "%";
            context.drawTextWithShadow(client.textRenderer, value,
                    slotX, y + 18, color);
            slotX += 34;
        }
    }

    private static int getDurability(ItemStack stack) {
        if (!stack.isDamageable()) {
            return 100;
        }
        int remaining = stack.getMaxDamage() - stack.getDamage();
        return Math.max(0, Math.round(remaining * 100.0f / stack.getMaxDamage()));
    }

    private static void renderSettings(MinecraftClient client,
                                       net.minecraft.client.gui.DrawContext context,
                                       int x, int y) {
        int width = 126;
        int height = 112;
        context.fill(x, y, x + width, y + height, NAVY);
        context.fill(x, y, x + width, y + 3, CYAN);
        context.drawTextWithShadow(client.textRenderer, "SETTINGS", x + 7, y + 9, CYAN);
        context.drawTextWithShadow(client.textRenderer, "HUD  " + (hudEnabled ? "ON" : "OFF"),
                x + 7, y + 27, hudEnabled ? GREEN : MUTED);
        context.drawTextWithShadow(client.textRenderer, "ZOOM  HOLD C",
                x + 7, y + 42, zoomActive ? GREEN : WHITE);
        context.drawTextWithShadow(client.textRenderer, "SPRINT  G",
                x + 7, y + 57, sprintToggle ? GREEN : WHITE);
        context.drawTextWithShadow(client.textRenderer, "SNEAK  N",
                x + 7, y + 72, sneakToggle ? GREEN : WHITE);
        context.drawTextWithShadow(client.textRenderer, "H HUD  |  M CLOSE",
                x + 7, y + 94, ORANGE);
    }

    private static void drawKey(net.minecraft.client.gui.DrawContext context,
                                 MinecraftClient client, String label,
                                 boolean pressed, int x, int y, int size) {
        int background = pressed ? 0xFF087F9C : 0xE8172948;
        int foreground = pressed ? 0xFFFFFFFF : MUTED;
        context.fill(x, y, x + size, y + size, background);
        int textWidth = client.textRenderer.getWidth(label);
        context.drawTextWithShadow(client.textRenderer, label,
                x + (size - textWidth) / 2, y + 7, foreground);
    }
}
