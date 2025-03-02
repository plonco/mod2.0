package com.SoloLevelingSystem;

import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.entity.ai.CustomFollowPlayerGoal;
import com.SoloLevelingSystem.events.EntityDamageHandler;
import com.SoloLevelingSystem.events.EventHandler;
import com.SoloLevelingSystem.events.CombatEventHandler;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import com.SoloLevelingSystem.storage.EntityStorage;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

@Mod(SoloLevelingSystem.MODID)
public class SoloLevelingSystem {
    public static final String MODID = "solo_leveling_system";
    private static final Logger LOGGER = LogUtils.getLogger();
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

        // Registrar la configuración antes de cualquier otra cosa
        ConfigManager.register();

        // Registrar los event listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ConfigManager::onConfigLoad);
        modEventBus.addListener(ConfigManager::onConfigReload);

        // Registrar los manejadores de eventos
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler()); // Registramos el nuevo manejador de combate
        MinecraftForge.EVENT_BUS.register(EntityStorage.class);
        MinecraftForge.EVENT_BUS.register(new EntityDamageHandler());

        LOGGER.info("Solo Leveling System Mod initialized successfully");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Solo Leveling System - Common Setup");

        // Registrar mensajes de red
        CHANNEL.registerMessage(
                messageID++,
                SpawnEntitiesMessage.class,
                SpawnEntitiesMessage::encode,
                SpawnEntitiesMessage::new,
                SpawnEntitiesMessage::handle
        );

        LOGGER.info("Network messages registered successfully");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        LOGGER.info("Solo Leveling System - Server Starting");
    }

    // Eventos específicos del cliente
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Solo Leveling System - Client Setup");
        }
    }

    // Métodos de utilidad para el registro de mensajes de red
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