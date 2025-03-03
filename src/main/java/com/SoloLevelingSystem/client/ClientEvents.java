package com.SoloLevelingSystem.client;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.client.render.SummonedEntityLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LOGGER.info("Adding Summoned Entity Layer to renderers...");

        ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(type -> type.getBaseClass() != null &&
                        LivingEntity.class.isAssignableFrom(type.getBaseClass()))
                .forEach(entityType -> {
                    try {
                        @SuppressWarnings("unchecked")
                        EntityType<? extends LivingEntity> livingEntityType =
                                (EntityType<? extends LivingEntity>) entityType;

                        var renderer = event.getRenderer(livingEntityType);
                        if (renderer instanceof LivingEntityRenderer) {
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer) renderer;
                            livingRenderer.addLayer(new SummonedEntityLayer(livingRenderer));
                            LOGGER.debug("Added layer to {}", entityType.getDescriptionId());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to add layer for {}: {}",
                                entityType.getDescriptionId(), e.getMessage());
                    }
                });

        LOGGER.info("Finished adding Summoned Entity Layer to renderers");
    }
}