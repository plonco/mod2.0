package com.SoloLevelingSystem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SummonedEntityLayer<T extends LivingEntity> extends RenderLayer<T, EntityModel<T>> {
    private static final ResourceLocation SUMMONED_TEXTURE =
            new ResourceLocation("solo_leveling_system", "textures/entity/summoned_overlay.png");

    private static final int ANIMATION_LENGTH = 32;
    private static final float MAX_ALPHA = 0.7F;

    public SummonedEntityLayer(RenderLayerParent<T, EntityModel<T>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (!entity.isInvisible() && entity.getTags().contains("summoned")) {
            // Animar la transparencia usando el tiempo del juego
            float alpha = 0.3F + (float)(Math.sin(entity.tickCount * 0.1F) + 1.0F) * 0.2F;

            // Color base azul claro con brillo
            float red = 0.4F;
            float green = 0.6F;
            float blue = 1.0F;

            poseStack.pushPose();

            // Pequeño offset para evitar z-fighting
            poseStack.translate(0.0D, 0.001D, 0.0D);

            // Aplicar una pequeña escala pulsante
            float scale = 1.0F + (float)(Math.sin(entity.tickCount * 0.05F) * 0.02F);
            poseStack.scale(scale, scale, scale);

            // Renderizar con modo translúcido
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentCull(SUMMONED_TEXTURE));

            // Renderizar el modelo con los efectos
            this.getParentModel().prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
            this.getParentModel().setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            this.getParentModel().renderToBuffer(
                    poseStack,
                    vertexConsumer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    red,    // Red
                    green,  // Green
                    blue,   // Blue
                    alpha   // Alpha - animado
            );

            poseStack.popPose();
        }
    }
}