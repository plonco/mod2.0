package com.SoloLevelingSystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SpawnEntitiesMessage {
    public SpawnEntitiesMessage() {}

    public SpawnEntitiesMessage(FriendlyByteBuf buffer) {}

    public void encode(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.getServer() != null) {
                com.SoloLevelingSystem.storage.EntityStorage.spawnStoredEntities(player);
            }
        });
        return true;
    }
}