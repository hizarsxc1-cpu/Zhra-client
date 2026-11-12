package com.zhra.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ZhraClient implements ClientModInitializer {
    private static final int NAVY = 0xD9101B35;
    private static final int PANEL = 0xE8172948;
    private static final int CYAN = 0xFF28D7F2;
    private static final int WHITE = 0xFFF2F7FF;
    private static final int MUTED = 0xFF9DB3CC;

    private static boolean hudEnabled = true;
    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zhra.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.zhra"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                hudEnabled = !hudEnabled;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudEnabled) {
                renderHud(MinecraftClient.getInstance(), drawContext);
            }
        });
    }

    private static void renderHud(MinecraftClient client,
                                  net.minecraft.client.gui.DrawContext context) {
        if (client.player == null || client.world == null) {
            return;
        }

        int x = 10;
        int y = 10;
        int width = 126;
        int height = 62;

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

        renderKeystrokes(client, context, x, y + height + 8);
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
