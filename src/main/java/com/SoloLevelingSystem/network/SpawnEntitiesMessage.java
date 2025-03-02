package com.SoloLevelingSystem.network;

import com.SoloLevelingSystem.storage.EntityStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpawnEntitiesMessage {
    public SpawnEntitiesMessage() {
    }

    public SpawnEntitiesMessage(FriendlyByteBuf buffer) {
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Llamar al método de spawneo con logging
                boolean spawned = EntityStorage.spawnStoredEntities(player);
                if (!spawned) {
                    player.sendSystemMessage(Component.literal("No hay entidades para invocar."));
                }
            }
        });
        context.setPacketHandled(true);
    }
}