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
public class SummonedEntityLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation SUMMONED_TEXTURE =
            new ResourceLocation("solo_leveling_system", "textures/entity/summoned_overlay.png");

    public SummonedEntityLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isInvisible() && entity.getTags().contains("summoned")) {
            float alpha = 0.5F;

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.001D, 0.0D);

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(SUMMONED_TEXTURE));

            this.getParentModel().renderToBuffer(
                    poseStack,
                    vertexConsumer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    1.0F,  // Red
                    1.0F,  // Green
                    1.0F,  // Blue
                    alpha  // Alpha
            );

            poseStack.popPose();
        }
    }
}