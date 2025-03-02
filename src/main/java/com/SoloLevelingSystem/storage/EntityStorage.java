package com.SoloLevelingSystem.storage;

import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStorage.class);
    private static final String STORAGE_KEY = SoloLevelingSystem.MODID + "_stored_entities";

    // Almacenamiento principal de entidades por dimensión y UUID del invocador
    private static final Map<String, Map<UUID, List<StoredEntityData>>> playerStoredEntities = new ConcurrentHashMap<>();

    // Registro de entidades invocadas
    private static final Set<UUID> summonedEntities = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Registro de propietarios de entidades
    private static final Map<UUID, UUID> entityOwners = new ConcurrentHashMap<>();

    public static class StoredEntityData {
        private final ResourceLocation entityType;
        private final CompoundTag entityData;
        private final Vec3 position;
        private final UUID ownerUUID;
        private final long timestamp;

        public StoredEntityData(ResourceLocation entityType, CompoundTag entityData, Vec3 position, UUID ownerUUID) {
            this.entityType = entityType;
            this.entityData = entityData;
            this.position = position;
            this.ownerUUID = ownerUUID;
            this.timestamp = System.currentTimeMillis();
        }

        public ResourceLocation getEntityType() { return entityType; }
        public CompoundTag getEntityData() { return entityData; }
        public Vec3 getPosition() { return position; }
        public UUID getOwnerUUID() { return ownerUUID; }
        public long getTimestamp() { return timestamp; }
    }

    // Método para verificar si una entidad es invocada
    public static boolean isSummonedEntity(UUID entityUUID) {
        if (entityUUID == null) return false;
        return summonedEntities.contains(entityUUID);
    }

    // Método para marcar una entidad como invocada
    public static void markAsSummoned(Entity entity, UUID ownerUUID) {
        if (entity == null || entity.getUUID() == null) return;

        summonedEntities.add(entity.getUUID());
        if (ownerUUID != null) {
            entityOwners.put(entity.getUUID(), ownerUUID);
        }
        LOGGER.debug("Entity {} marked as summoned by {}", entity.getUUID(), ownerUUID);
    }

    // Método para remover una entidad invocada
    public static void removeSummonedEntity(UUID entityUUID) {
        if (entityUUID == null) return;

        summonedEntities.remove(entityUUID);
        entityOwners.remove(entityUUID);
        LOGGER.debug("Entity {} removed from summoned tracking", entityUUID);
    }

    // Método para obtener el propietario de una entidad
    public static UUID getEntityOwner(UUID entityUUID) {
        return entityOwners.get(entityUUID);
    }

    // Método principal para almacenar una entidad
    public static void storeEntity(UUID ownerUUID, Entity entity, CompoundTag additionalData) {
        if (!(entity.level() instanceof ServerLevel) || ownerUUID == null) {
            LOGGER.warn("Invalid conditions for storing entity");
            return;
        }

        ServerLevel serverLevel = (ServerLevel) entity.level();
        String dimensionKey = serverLevel.dimension().location().toString();

        ResourceLocation entityId = serverLevel.registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(entity.getType());

        if (entityId == null) {
            LOGGER.error("Failed to get entity ID for type: {}", entity.getType());
            return;
        }

        CompoundTag entityData = new CompoundTag();
        if (!entity.save(entityData)) {
            LOGGER.error("Failed to save entity data for: {}", entity);
            return;
        }

        // Combinar datos adicionales si existen
        if (additionalData != null) {
            entityData.merge(additionalData);
        }

        Vec3 position = entity.position();
        StoredEntityData storedData = new StoredEntityData(entityId, entityData, position, ownerUUID);

        // Almacenar la entidad
        playerStoredEntities
                .computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(ownerUUID, k -> new ArrayList<>())
                .add(storedData);

        LOGGER.info("Stored entity {} for owner {} in dimension {}", entityId, ownerUUID, dimensionKey);
    }

    // Método para hacer spawn de las entidades almacenadas
    public static void spawnStoredEntities(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        String dimensionKey = serverLevel.dimension().location().toString();
        UUID playerUUID = player.getUUID();

        Map<UUID, List<StoredEntityData>> dimensionStorage = playerStoredEntities.get(dimensionKey);
        if (dimensionStorage == null || !dimensionStorage.containsKey(playerUUID)) {
            LOGGER.debug("No stored entities found for player {} in dimension {}", playerUUID, dimensionKey);
            return;
        }

        List<StoredEntityData> storedEntities = dimensionStorage.get(playerUUID);
        List<StoredEntityData> failedSpawns = new ArrayList<>();

        for (StoredEntityData storedData : storedEntities) {
            try {
                Entity entity = createEntity(storedData.getEntityType(), serverLevel);
                if (entity == null) continue;

                // Cargar datos y posición
                entity.load(storedData.getEntityData().copy());
                Vec3 pos = storedData.getPosition();
                entity.setPos(pos.x, pos.y, pos.z);

                // Marcar como entidad invocada
                markAsSummoned(entity, playerUUID);

                if (serverLevel.tryAddFreshEntityWithPassengers(entity)) {
                    LOGGER.info("Successfully spawned entity {} for player {}",
                            storedData.getEntityType(), playerUUID);
                } else {
                    failedSpawns.add(storedData);
                }
            } catch (Exception e) {
                LOGGER.error("Error spawning entity: {}", e.getMessage());
                failedSpawns.add(storedData);
            }
        }

        // Actualizar almacenamiento con las entidades que fallaron
        if (!failedSpawns.isEmpty()) {
            dimensionStorage.put(playerUUID, failedSpawns);
        } else {
            dimensionStorage.remove(playerUUID);
            if (dimensionStorage.isEmpty()) {
                playerStoredEntities.remove(dimensionKey);
            }
        }
    }

    private static Entity createEntity(ResourceLocation entityTypeId, ServerLevel world) {
        EntityType<?> type = world.registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .get(entityTypeId);

        if (type == null) {
            LOGGER.error("Unknown entity type: {}", entityTypeId);
            return null;
        }

        try {
            return type.create(world);
        } catch (Exception e) {
            LOGGER.error("Failed to create entity of type {}: {}", entityTypeId, e.getMessage());
            return null;
        }
    }

    // Método para guardar todos los datos
    public static void saveAllData() {
        LOGGER.info("Saving all entity storage data...");
        // Implementar lógica de guardado persistente si es necesario
    }

    // Método para limpiar todo el almacenamiento
    public static void clearAllStorage() {
        playerStoredEntities.clear();
        summonedEntities.clear();
        entityOwners.clear();
        LOGGER.info("Cleared all entity storage data");
    }

    // Métodos de utilidad
    public static int getStoredEntityCount(UUID playerUUID, String dimensionKey) {
        return Optional.ofNullable(playerStoredEntities.get(dimensionKey))
                .map(dim -> dim.getOrDefault(playerUUID, Collections.emptyList()).size())
                .orElse(0);
    }

    public static boolean hasStoredEntities(UUID playerUUID, String dimensionKey) {
        return getStoredEntityCount(playerUUID, dimensionKey) > 0;
    }
}