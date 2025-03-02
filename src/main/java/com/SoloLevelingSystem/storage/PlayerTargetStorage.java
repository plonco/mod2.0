package com.SoloLevelingSystem.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

public class PlayerTargetStorage {
    private static final Map<UUID, LivingEntity> playerTargets = new ConcurrentHashMap<>(); // Usar ConcurrentHashMap

    public static void setPlayerTarget(UUID playerUUID, LivingEntity target) {
        playerTargets.put(playerUUID, target);
    }

    public static LivingEntity getPlayerTarget(UUID playerUUID) {
        return playerTargets.get(playerUUID); // Manejo de nulos ya implícito en ConcurrentHashMap
    }

    public static void clearPlayerTarget(UUID playerUUID) {
        playerTargets.remove(playerUUID);
    }

    // Ejemplo de limpieza cuando el jugador se desconecta
    public static void onPlayerDisconnect(UUID playerUUID) {
        clearPlayerTarget(playerUUID);
    }

    // Ejemplo de limpieza cuando la entidad objetivo muere
    public static void onEntityDeath(LivingEntity entity) {
        playerTargets.entrySet().removeIf(entry -> entry.getValue().equals(entity));
    }
}