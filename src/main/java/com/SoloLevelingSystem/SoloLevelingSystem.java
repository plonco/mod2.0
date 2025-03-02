package com.SoloLevelingSystem;

import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.network.SpawnEntitiesMessage;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
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
import org.slf4j.Logger;

@Mod(SoloLevelingSystem.MODID)
public class SoloLevelingSystem {
    public static final String MODID = "solo_leveling_system";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "1";


    // Canal de red para comunicación entre cliente y servidor
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"), // Identificador único del canal
            () -> PROTOCOL_VERSION, // Versión del protocolo
            PROTOCOL_VERSION::equals, // Predicado para aceptar versiones del servidor
            PROTOCOL_VERSION::equals  // Predicado para aceptar versiones del cliente



    );

    private static int messageID = 0;

    public SoloLevelingSystem() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        modEventBus.addListener(this::commonSetup);

        // Registrar eventos de Forge
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Cargar la configuración
        ConfigManager.loadConfig();
        LOGGER.info("Configuration loaded successfully.");

        // Registrar mensajes en el canal de red
        CHANNEL.registerMessage(
                messageID++,
                SpawnEntitiesMessage.class,
                SpawnEntitiesMessage::encode,
                SpawnEntitiesMessage::new,
                SpawnEntitiesMessage::handle
        );
        LOGGER.info("Registered SpawnEntitiesMessage");
    }

    // Evento para cuando el servidor se inicia
    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        ConfigManager.loadConfig();
        LOGGER.info("Configuración recargada exitosamente");
    }

    // Eventos del cliente (opcional, si necesitas configuración específica del cliente)
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO from client setup");
        }
    }
}