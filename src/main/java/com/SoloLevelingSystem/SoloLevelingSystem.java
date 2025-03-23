package com.SoloLevelingSystem;

import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.events.EntityDamageHandler;
import com.SoloLevelingSystem.events.EventHandler;
import com.SoloLevelingSystem.events.CombatEventHandler;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import com.SoloLevelingSystem.storage.EntityStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SoloLevelingSystem.MODID)
public class SoloLevelingSystem {
    public static final String MODID = "solo_leveling_system";
    private static final Logger LOGGER = LoggerFactory.getLogger(SoloLevelingSystem.class);
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int messageID = 0;

    public SoloLevelingSystem() {
        LOGGER.info("Initializing Solo Leveling System Mod");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ConfigManager.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ConfigManager::onConfigLoad);
        modEventBus.addListener(ConfigManager::onConfigReload);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler());
        MinecraftForge.EVENT_BUS.register(EntityStorage.class);
        MinecraftForge.EVENT_BUS.register(new EntityDamageHandler());


        LOGGER.info("Solo Leveling System Mod initialized successfully");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CHANNEL.registerMessage(
                messageID++,
                SpawnEntitiesMessage.class,
                SpawnEntitiesMessage::encode,
                SpawnEntitiesMessage::new,
                SpawnEntitiesMessage::handle
        );
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        LOGGER.info("Solo Leveling System - Server Starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        private static final Logger CLIENT_LOGGER = LoggerFactory.getLogger(ClientModEvents.class);

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            CLIENT_LOGGER.info("Solo Leveling System - Client Setup Starting");
            event.enqueueWork(() -> {
                try {
                    if (Minecraft.getInstance() != null) {
                        CLIENT_LOGGER.info("Client initialization successful");
                    }
                } catch (Exception e) {
                    CLIENT_LOGGER.error("Error during client setup: {}", e.getMessage());
                }
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.AddLayers event) {
            ForgeRegistries.ENTITY_TYPES.getValues().stream()
                    .filter(ClientEvents::isLivingEntityType)
                    .forEach(entityType -> {
                        try {
                            @SuppressWarnings("unchecked")
                            EntityType<? extends LivingEntity> livingEntityType =
                                    (EntityType<? extends LivingEntity>) entityType;

                            var renderer = event.getRenderer(livingEntityType);
                            if (renderer instanceof LivingEntityRenderer) {
                                @SuppressWarnings({"unchecked", "rawtypes"})
                                LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> livingRenderer =
                                        (LivingEntityRenderer) renderer;

                                // Aquí puedes agregar configuración adicional para el renderizado
                                // si es necesario, pero el efecto principal se manejará en EntityRenderHandler
                                LOGGER.debug("Registered shadow rendering capability for: {}",
                                        entityType.getDescriptionId());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to add layer for {}: {}",
                                    entityType.getDescriptionId(), e.getMessage());
                        }
                    });
        }

        private static boolean isLivingEntityType(EntityType<?> type) {
            return type.getBaseClass() != null &&
                    LivingEntity.class.isAssignableFrom(type.getBaseClass());
        }
    }

    public static SimpleChannel getChannel() {
        return CHANNEL;
    }

    public static String getModId() {
        return MODID;
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}