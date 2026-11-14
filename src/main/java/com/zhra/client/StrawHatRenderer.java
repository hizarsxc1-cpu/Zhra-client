package com.zhra.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public final class StrawHatRenderer implements ClientModInitializer {
    private static final EntityModelLayer HAT_LAYER =
            new EntityModelLayer(Identifier.of("zhra", "straw_hat"), "main");
    private static final Identifier HAT_TEXTURE =
            Identifier.of("zhra", "textures/entity/straw_hat.png");

    private static ModelPart hatModel;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(HAT_LAYER, StrawHatRenderer::createModelData);

        ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
            if (slot != EquipmentSlot.HEAD) {
                return;
            }

            if (hatModel == null) {
                hatModel = MinecraftClient.getInstance()
                        .getEntityModelLoader()
                        .getModelPart(HAT_LAYER)
                        .getChild("hat");
            }

            matrices.push();
            contextModel.head.rotate(matrices);
            VertexConsumer vertices = vertexConsumers.getBuffer(
                    RenderLayer.getEntityCutoutNoCull(HAT_TEXTURE)
            );
            hatModel.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }, Items.NETHERITE_HELMET);
    }

    private static TexturedModelData createModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartBuilder hat = ModelPartBuilder.create()
                // Wide, flat straw brim
                .uv(0, 0).cuboid(-7.0F, -9.0F, -7.0F, 14.0F, 1.0F, 14.0F)
                // Rounded-looking blocky crown
                .uv(0, 0).cuboid(-4.0F, -13.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                // Red band around the crown
                .uv(32, 0).cuboid(-4.05F, -10.0F, -4.05F, 8.1F, 1.0F, 8.1F);

        root.addChild("hat", hat, ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 32);
    }
}
