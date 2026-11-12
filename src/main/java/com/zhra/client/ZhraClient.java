package com.zhra.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ZhraClient implements ClientModInitializer {
    static final int NAVY = 0xF0101B35;
    static final int PANEL = 0xE8172948;
    static final int CYAN = 0xFF28D7F2;
    static final int WHITE = 0xFFF2F7FF;
    static final int MUTED = 0xFF9DB3CC;
    static final int GREEN = 0xFF55E6A5;

    static boolean hudEnabled = true;
    static boolean keystrokesEnabled = true;
    static boolean armorEnabled = true;
    static boolean zoomEnabled = false;
    static boolean sprintToggle = false;
    static boolean sneakToggle = false;
    static boolean cpsEnabled = true;
    static boolean customCrosshair = true;
    static boolean performanceMode = false;

    static int hudX = 10;
    static int hudY = 10;
    static float hudScale = 1.0f;
    static int crosshairSize = 5;
    static int crosshairGap = 3;
    static int crosshairColor = 0xFF28D7F2;

    private static final int[] CROSSHAIR_COLORS = {
            0xFF28D7F2, 0xFFFFFFFF, 0xFFFF5555, 0xFF55E6A5, 0xFFFFFF55
    };
    private static int colorIndex = 0;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();

    private static KeyBinding menuKey;
    private static boolean zoomApplied = false;
    private static int originalFov = 70;
    private static int originalViewDistance = -1;
    private static ParticlesMode originalParticles = null;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zhra.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.zhra"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen instanceof ZhraMenuScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new ZhraMenuScreen());
                }
            }

            if (client.player != null) {
                client.player.setSprinting(sprintToggle || client.options.sprintKey.isPressed());
                client.player.setSneaking(sneakToggle || client.options.sneakKey.isPressed());
            }
            updateZoom(client);
            updatePerformance(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudEnabled) renderHud(MinecraftClient.getInstance(), drawContext);
        });
    }

    public static void recordLeftClick() {
        long now = System.currentTimeMillis();
        LEFT_CLICKS.addLast(now);
        pruneClicks(now);
    }

    private static void pruneClicks(long now) {
        while (!LEFT_CLICKS.isEmpty() && now - LEFT_CLICKS.peekFirst() > 1000L) {
            LEFT_CLICKS.removeFirst();
        }
    }

    private static int getCps() {
        pruneClicks(System.currentTimeMillis());
        return LEFT_CLICKS.size();
    }

    private static void updateZoom(MinecraftClient client) {
        if (client.options == null) return;
        if (zoomEnabled && !zoomApplied) {
            originalFov = client.options.getFov().getValue();
            zoomApplied = true;
        }
        if (zoomEnabled) {
            client.options.getFov().setValue(Math.min(originalFov, 30));
        } else if (zoomApplied) {
            client.options.getFov().setValue(originalFov);
            zoomApplied = false;
        }
    }

    private static void updatePerformance(MinecraftClient client) {
        if (client.options == null) return;
        if (performanceMode && originalViewDistance < 0) {
            originalViewDistance = client.options.getViewDistance().getValue();
            originalParticles = client.options.getParticles().getValue();
        }
        if (performanceMode) {
            client.options.getViewDistance().setValue(Math.min(originalViewDistance, 6));
            client.options.getParticles().setValue(ParticlesMode.MINIMAL);
        } else if (originalViewDistance >= 0) {
            client.options.getViewDistance().setValue(originalViewDistance);
            if (originalParticles != null) client.options.getParticles().setValue(originalParticles);
            originalViewDistance = -1;
            originalParticles = null;
        }
    }

    private static void renderHud(MinecraftClient client, DrawContext context) {
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(hudScale, hudScale, 1.0f);

        int x = 0, y = 0, width = 142, height = 78;
        context.fill(x, y, x + width, y + height, NAVY);
        context.fill(x, y, x + width, y + 3, CYAN);
        context.fill(x, y + 3, x + width, y + height, PANEL);
        context.drawTextWithShadow(client.textRenderer, "ZHRA CLIENT", x + 8, y + 8, CYAN);
        context.drawTextWithShadow(client.textRenderer, "FPS  " + client.getCurrentFps(), x + 8, y + 23, WHITE);

        int ping = getPing(client);
        context.drawTextWithShadow(client.textRenderer,
                "PING  " + (ping >= 0 ? ping + " ms" : "--"), x + 8, y + 36, WHITE);
        if (cpsEnabled) {
            context.drawTextWithShadow(client.textRenderer, "CPS  " + getCps(), x + 8, y + 49, GREEN);
        }
        context.drawTextWithShadow(client.textRenderer,
                String.format("XYZ  %d %d %d", (int) client.player.getX(),
                        (int) client.player.getY(), (int) client.player.getZ()),
                x + 8, y + 62, MUTED);

        if (keystrokesEnabled) renderKeystrokes(client, context, x, y + height + 7);
        if (armorEnabled) renderArmor(client, context, x, y + height + 62);
        context.getMatrices().pop();
    }

    public static void renderCustomCrosshair(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!customCrosshair || client.player == null || client.currentScreen != null || client.options.hudHidden) return;
        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;
        int s = crosshairSize;
        int g = crosshairGap;
        int t = 2;
        context.fill(centerX - t / 2, centerY - g - s, centerX + (t + 1) / 2, centerY - g, crosshairColor);
        context.fill(centerX - t / 2, centerY + g + 1, centerX + (t + 1) / 2, centerY + g + s + 1, crosshairColor);
        context.fill(centerX - g - s, centerY - t / 2, centerX - g, centerY + (t + 1) / 2, crosshairColor);
        context.fill(centerX + g + 1, centerY - t / 2, centerX + g + s + 1, centerY + (t + 1) / 2, crosshairColor);
    }

    private static int getPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return -1;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private static void renderKeystrokes(MinecraftClient client, DrawContext context, int x, int y) {
        int cell = 22, gap = 3;
        context.fill(x, y, x + cell * 3 + gap * 2, y + cell * 2 + gap, NAVY);
        drawKey(context, client, "W", client.options.forwardKey.isPressed(), x + cell + gap, y, cell);
        drawKey(context, client, "A", client.options.leftKey.isPressed(), x, y + cell + gap, cell);
        drawKey(context, client, "S", client.options.backKey.isPressed(), x + cell + gap, y + cell + gap, cell);
        drawKey(context, client, "D", client.options.rightKey.isPressed(), x + (cell + gap) * 2, y + cell + gap, cell);
    }

    private static void renderArmor(MinecraftClient client, DrawContext context, int x, int y) {
        context.fill(x, y, x + 142, y + 34, NAVY);
        context.drawTextWithShadow(client.textRenderer, "ARMOR", x + 6, y + 4, CYAN);
        var armor = client.player.getInventory().armor;
        String[] slots = {"BO", "LE", "CH", "HE"};
        int slotX = x + 6;
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            String value = stack.isEmpty() ? slots[i] + " --" : slots[i] + " " + getDurability(stack) + "%";
            context.drawTextWithShadow(client.textRenderer, value, slotX, y + 18,
                    stack.isEmpty() ? MUTED : WHITE);
            slotX += 34;
        }
    }

    private static int getDurability(ItemStack stack) {
        if (!stack.isDamageable()) return 100;
        int remaining = stack.getMaxDamage() - stack.getDamage();
        return Math.max(0, Math.round(remaining * 100.0f / stack.getMaxDamage()));
    }

    private static void drawKey(DrawContext context, MinecraftClient client, String label,
                                boolean pressed, int x, int y, int size) {
        int background = pressed ? 0xFF087F9C : PANEL;
        int foreground = pressed ? 0xFFFFFFFF : MUTED;
        context.fill(x, y, x + size, y + size, background);
        int textWidth = client.textRenderer.getWidth(label);
        context.drawTextWithShadow(client.textRenderer, label,
                x + (size - textWidth) / 2, y + 7, foreground);
    }

    static final class ZhraMenuScreen extends Screen {
        private static final int BUTTON_WIDTH = 150;
        private static final int BUTTON_HEIGHT = 20;

        ZhraMenuScreen() {
            super(Text.literal("Zhra Client"));
        }

        @Override
        protected void init() {
            int left = width / 2 - BUTTON_WIDTH - 8;
            int right = width / 2 + 8;
            int top = height / 2 - 116;

            addToggle("HUD", () -> hudEnabled, value -> hudEnabled = value, left, top);
            addToggle("Keystrokes", () -> keystrokesEnabled, value -> keystrokesEnabled = value, right, top);
            addToggle("Armor Status", () -> armorEnabled, value -> armorEnabled = value, left, top + 25);
            addToggle("Zoom (30 FOV)", () -> zoomEnabled, value -> zoomEnabled = value, right, top + 25);
            addToggle("Toggle Sprint", () -> sprintToggle, value -> sprintToggle = value, left, top + 50);
            addToggle("Toggle Sneak", () -> sneakToggle, value -> sneakToggle = value, right, top + 50);
            addToggle("CPS Counter", () -> cpsEnabled, value -> cpsEnabled = value, left, top + 75);
            addToggle("Custom Crosshair", () -> customCrosshair, value -> customCrosshair = value, right, top + 75);
            addToggle("Performance Mode", () -> performanceMode, value -> performanceMode = value, left, top + 100);

            addAction("Crosshair Color", "CHANGE", () -> {
                colorIndex = (colorIndex + 1) % CROSSHAIR_COLORS.length;
                crosshairColor = CROSSHAIR_COLORS[colorIndex];
            }, right, top + 100);
            addAction("Crosshair Size", crosshairSize + " px", () -> {
                crosshairSize = crosshairSize >= 9 ? 3 : crosshairSize + 2;
                clearAndInit();
            }, left, top + 125);
            addAction("HUD Scale", Math.round(hudScale * 100) + "%", () -> {
                hudScale = hudScale >= 1.4f ? 0.8f : hudScale + 0.2f;
                clearAndInit();
            }, right, top + 125);
            addAction("HUD Position", "LEFT", () -> {
                hudX = hudX > 20 ? 10 : Math.min(120, hudX + 30);
                clearAndInit();
            }, left, top + 150);
            addAction("HUD Position", "UP / DOWN", () -> {
                hudY = hudY > 20 ? 10 : Math.min(120, hudY + 30);
                clearAndInit();
            }, right, top + 150);

            addDrawableChild(ButtonWidget.builder(Text.literal("CLOSE"), button -> close())
                    .dimensions(width / 2 - 75, top + 178, 150, 20).build());
        }

        private void clearAndInit() {
            clearChildren();
            init();
        }

        private void addToggle(String label, java.util.function.BooleanSupplier getter,
                               java.util.function.Consumer<Boolean> setter, int x, int y) {
            ButtonWidget button = ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), b -> {
                setter.accept(!getter.getAsBoolean());
                b.setMessage(toggleText(label, getter.getAsBoolean()));
            }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            addDrawableChild(button);
        }

        private void addAction(String label, String value, Runnable action, int x, int y) {
            addDrawableChild(ButtonWidget.builder(Text.literal(label + "  [" + value + "]"), b -> action.run())
                    .dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        private Text toggleText(String label, boolean enabled) {
            return Text.literal(label + "  [" + (enabled ? "ON" : "OFF") + "]");
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            int panelWidth = 344, panelHeight = 228;
            int x = width / 2 - panelWidth / 2, y = height / 2 - panelHeight / 2;
            context.fill(x, y, x + panelWidth, y + panelHeight, NAVY);
            context.fill(x, y, x + panelWidth, y + 3, CYAN);
            context.drawCenteredTextWithShadow(textRenderer, "ZHRA CLIENT", width / 2, y + 8, CYAN);
            context.drawCenteredTextWithShadow(textRenderer, "MIDNIGHT BLUE  /  ELECTRIC CYAN",
                    width / 2, y + 22, MUTED);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, "RIGHT SHIFT: OPEN / CLOSE MENU",
                    width / 2, y + panelHeight - 12, MUTED);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
