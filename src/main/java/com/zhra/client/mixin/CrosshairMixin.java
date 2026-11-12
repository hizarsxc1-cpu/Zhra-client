package com.zhra.client.mixin;

import com.zhra.client.ZhraClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void zhra$replaceCrosshair(DrawContext context, RenderTickCounter tickCounter,
                                       CallbackInfo callbackInfo) {
        ZhraClient.renderCustomCrosshair(context);
        callbackInfo.cancel();
    }
}
