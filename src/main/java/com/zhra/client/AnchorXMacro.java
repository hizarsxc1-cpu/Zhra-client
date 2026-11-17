package com.zhra.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.RespawnAnchorBlock;
import org.lwjgl.glfw.GLFW;

public final class AnchorXMacro implements ClientModInitializer {
    private static KeyBinding activateKey;
    private static int step = 0;
    private static int waitTicks = 0;
    private static BlockPos anchorPos;
    private static int anchorSlot = -1;
    private static int glowstoneSlot = -1;
    private static int triggerSlot = -1;

    @Override
    public void onInitializeClient() {
        activateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zhra.anchorx",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.zhra"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            reset();
            return;
        }

        while (activateKey.wasPressed()) {
            if (step == 0) start(client);
        }

        if (step == 0) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        switch (step) {
            case 1 -> placeAnchor(client);
            case 2 -> chargeAnchor(client);
            case 3 -> detonateIfSafe(client);
            default -> reset();
        }
    }

    private static void start(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) return;
        anchorSlot = findHotbar(client, Items.RESPAWN_ANCHOR);
        glowstoneSlot = findHotbar(client, Items.GLOWSTONE);
        if (anchorSlot < 0 || glowstoneSlot < 0) return;

        anchorPos = hit.getBlockPos().offset(hit.getSide());
        // Use the actual crosshair hit so Minecraft validates the placement normally.
        anchorPos = hit.getBlockPos();
        step = 1;
        waitTicks = 0;
    }

    private static void placeAnchor(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            reset();
            return;
        }
        client.player.getInventory().setSelectedSlot(anchorSlot);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        step = 2;
        waitTicks = 1;
    }

    private static void chargeAnchor(MinecraftClient client) {
        if (anchorPos == null || !client.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            reset();
            return;
        }
        client.player.getInventory().setSelectedSlot(glowstoneSlot);
        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(anchorPos), Direction.UP, anchorPos, false
        );
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        step = 3;
        waitTicks = 1;
    }

    private static void detonateIfSafe(MinecraftClient client) {
        if (anchorPos == null || !client.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            reset();
            return;
        }

        // Only perform the final use when a Totem is already in the offhand.
        if (!client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            reset();
            return;
        }

        int charges = client.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);
        if (charges <= 0) {
            reset();
            return;
        }

        triggerSlot = findTriggerSlot(client);
        if (triggerSlot < 0) {
            reset();
            return;
        }

        client.player.getInventory().setSelectedSlot(triggerSlot);
        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(anchorPos), Direction.UP, anchorPos, false
        );
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        reset();
    }

    private static int findHotbar(MinecraftClient client, Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private static int findTriggerSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            Item item = client.player.getInventory().getStack(i).getItem();
            if (!client.player.getInventory().getStack(i).isEmpty()
                    && item != Items.RESPAWN_ANCHOR && item != Items.GLOWSTONE) return i;
        }
        return -1;
    }

    private static void reset() {
        step = 0;
        waitTicks = 0;
        anchorPos = null;
        anchorSlot = -1;
        glowstoneSlot = -1;
        triggerSlot = -1;
    }
}
