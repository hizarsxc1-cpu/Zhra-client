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

    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();

    private static KeyBinding menuKey;
    private static boolean zoomApplied = false;
    private static int originalFov = 70;

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

    private static void renderHud(MinecraftClient client, DrawContext context) {
        if (client.player == null || client.world == null) return;

        int x = 10, y = 10, width = 142, height = 78;
        context.fill(x, y, x + width, y + height, NAVY);
        context.fill(x, y, x + width, y + 3, CYAN);
        context.fill(x, y + 3, x + width, y + height, PANEL);
        context.drawTextWithShadow(client.textRenderer, "ZHRA CLIENT", x + 8, y + 8, CYAN);
        context.drawTextWithShadow(client.textRenderer,
                "FPS  " + client.getCurrentFps(), x + 8, y + 23, WHITE);

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
        private static final int BUTTON_HEIGHT = 22;

        ZhraMenuScreen() {
            super(Text.literal("Zhra Client"));
        }

        @Override
        protected void init() {
            int left = width / 2 - BUTTON_WIDTH - 8;
            int right = width / 2 + 8;
            int top = height / 2 - 110;

            addToggle("HUD", () -> hudEnabled, value -> hudEnabled = value, left, top);
            addToggle("Keystrokes", () -> keystrokesEnabled, value -> keystrokesEnabled = value, right, top);
            addToggle("Armor Status", () -> armorEnabled, value -> armorEnabled = value, left, top + 29);
            addToggle("Zoom (30 FOV)", () -> zoomEnabled, value -> zoomEnabled = value, right, top + 29);
            addToggle("Toggle Sprint", () -> sprintToggle, value -> sprintToggle = value, left, top + 58);
            addToggle("Toggle Sneak", () -> sneakToggle, value -> sneakToggle = value, right, top + 58);
            addToggle("CPS Counter", () -> cpsEnabled, value -> cpsEnabled = value, left, top + 87);

            addDrawableChild(ButtonWidget.builder(Text.literal("CLOSE"), button -> close())
                    .dimensions(width / 2 - 75, top + 124, 150, 22).build());
        }

        private void addToggle(String label, java.util.function.BooleanSupplier getter,
                               java.util.function.Consumer<Boolean> setter, int x, int y) {
            ButtonWidget button = ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), b -> {
                setter.accept(!getter.getAsBoolean());
                b.setMessage(toggleText(label, getter.getAsBoolean()));
            }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            addDrawableChild(button);
        }

        private Text toggleText(String label, boolean enabled) {
            return Text.literal(label + "  [" + (enabled ? "ON" : "OFF") + "]");
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            int panelWidth = 344, panelHeight = 250;
            int x = width / 2 - panelWidth / 2, y = height / 2 - panelHeight / 2;
            context.fill(x, y, x + panelWidth, y + panelHeight, NAVY);
            context.fill(x, y, x + panelWidth, y + 3, CYAN);
            context.drawCenteredTextWithShadow(textRenderer, "ZHRA CLIENT", width / 2, y + 15, CYAN);
            context.drawCenteredTextWithShadow(textRenderer, "MIDNIGHT BLUE  /  ELECTRIC CYAN",
                    width / 2, y + 31, MUTED);
            context.drawCenteredTextWithShadow(textRenderer, "FEATURES", width / 2, y + 53, WHITE);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, "RIGHT SHIFT: OPEN / CLOSE MENU",
                    width / 2, y + panelHeight - 18, MUTED);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
