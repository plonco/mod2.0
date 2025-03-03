package com.SoloLevelingSystem;

import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.entity.ai.CustomFollowPlayerGoal;
import com.SoloLevelingSystem.events.EntityDamageHandler;
import com.SoloLevelingSystem.events.EventHandler;
import com.SoloLevelingSystem.events.CombatEventHandler;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import com.SoloLevelingSystem.storage.EntityStorage;
import com.SoloLevelingSystem.client.render.SummonedEntityLayer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        private static final Set<Class<?>> PROCESSED_RENDERERS = new HashSet<>();

        public static final ModelLayerLocation SUMMONED_LAYER = new ModelLayerLocation(
                new ResourceLocation(MODID, "summoned_overlay"), "main");

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    SUMMONED_LAYER,
                    () -> LayerDefinition.create(new MeshDefinition(), 64, 64)
            );
        }

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
            // Obtenemos todos los tipos de entidades registrados en Forge
            ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
                EntityType<?> entityType = entry.getValue();

                // Verificamos si el tipo de entidad puede ser renderizado como LivingEntity
                if (isLivingEntityType(entityType)) {
                    EntityRenderer<?> renderer = event.getRenderer((EntityType)entityType);
                    if (renderer instanceof LivingEntityRenderer) {
                        addLayerToRenderer((LivingEntityRenderer<?, ?>)renderer);
                    }
                }
            });
        }

        private static boolean isLivingEntityType(EntityType<?> type) {
            try {
                // Verificamos si la clase de la entidad es una subclase de LivingEntity
                return LivingEntity.class.isAssignableFrom(type.getBaseClass());
            } catch (Exception e) {
                return false;
            }
        }

        private static void addLayerToRenderer(LivingEntityRenderer<?, ?> renderer) {
            try {
                @SuppressWarnings("unchecked")
                LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> livingRenderer =
                        (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) renderer;
                livingRenderer.addLayer(new SummonedEntityLayer<>(livingRenderer));
            } catch (Exception e) {
                LOGGER.error("Failed to add layer to renderer: {}", e.getMessage());
            }
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