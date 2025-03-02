package com.SoloLevelingSystem.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpawnEntitiesMessage {
    public SpawnEntitiesMessage() {
    }

    public SpawnEntitiesMessage(FriendlyByteBuf buf) {
        // Read message data from buffer
    }

    public void encode(FriendlyByteBuf buf) {
        // Write message data to buffer
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle message on the receiving side
        });
        return true;
    }
}