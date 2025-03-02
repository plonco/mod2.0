package com.SoloLevelingSystem.network;

import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(SoloLevelingSystem.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Registrar mensajes
        net.registerMessage(
                id(),
                SpawnEntitiesMessage.class,
                SpawnEntitiesMessage::encode,
                SpawnEntitiesMessage::new,
                SpawnEntitiesMessage::handle
        );
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}