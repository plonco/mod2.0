package com.SoloLevelingSystem.client;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.client.render.SummonedEntityLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(type -> type.getBaseClass() != null && LivingEntity.class.isAssignableFrom(type.getBaseClass()))
                .forEach(entityType -> {
                    @SuppressWarnings("unchecked")
                    var livingEntityType = (EntityType<? extends LivingEntity>) entityType;

                    var renderer = event.getRenderer(livingEntityType);
                    if (renderer instanceof LivingEntityRenderer) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> livingRenderer =
                                (LivingEntityRenderer) renderer;

                        @SuppressWarnings({"unchecked", "rawtypes"})
                        SummonedEntityLayer<LivingEntity, EntityModel<LivingEntity>> layer =
                                new SummonedEntityLayer(livingRenderer);

                        livingRenderer.addLayer(layer);
                    }
                });
    }
}