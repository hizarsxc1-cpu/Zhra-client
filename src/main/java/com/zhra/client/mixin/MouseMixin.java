package com.zhra.client.mixin;

import com.zhra.client.ZhraClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void zhra$trackLeftClick(long window, int button, int action, int mods,
                                     CallbackInfo callbackInfo) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && action == GLFW.GLFW_PRESS
                && MinecraftClient.getInstance().currentScreen == null) {
            ZhraClient.recordLeftClick();
        }
    }
}
