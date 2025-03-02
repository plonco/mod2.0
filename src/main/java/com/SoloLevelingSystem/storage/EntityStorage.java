package com.SoloLevelingSystem.storage;

import com.SoloLevelingSystem.configs.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.SoloLevelingSystem.events.EventHandler;
import net.minecraft.nbt.ListTag;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.Level;

@Mod.EventBusSubscriber
public class EntityStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStorage.class);
    private static Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities = new HashMap<>();
    private static Map<UUID, List<Entity>> spawnedEntities = new HashMap<>(); // Track spawned entities

    private static final int MAX_NORMAL_ENEMIES = 10;
    private static final int MAX_MINIBOSSES = 1;
    private static final int MAX_BOSSES = 1;
    private static final String DATA_NAME = "SoloLevelingEntityStorage";
    private static final Random RANDOM = new Random();
    private static final double SPAWN_RADIUS = 2.0;


    public static void storeEntity(UUID playerUUID, Entity entity, CompoundTag entityData) {
        ResourceLocation entityResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (entityResourceLocation == null) {
            LOGGER.error("Entity Resource Location is null for entity: {}", entity);
            return;
        }

        Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntities.computeIfAbsent(playerUUID, k -> new HashMap<>());
        List<CompoundTag> entityList = playerEntityMap.computeIfAbsent(entityResourceLocation, k -> new ArrayList<>());

        int maxEntities = 0;
        if (ConfigManager.isNormalEnemy(entityResourceLocation)) {
            maxEntities = MAX_NORMAL_ENEMIES;
        } else if (ConfigManager.isMinibossEnemy(entityResourceLocation)) {
            maxEntities = MAX_MINIBOSSES;
        } else if (ConfigManager.isBossEnemy(entityResourceLocation)) {
            maxEntities = MAX_BOSSES;
        }

        if (entityList.size() >= maxEntities) {
            LOGGER.warn("Player {} has reached the maximum entity storage limit for entity type {}.", playerUUID, entityResourceLocation);
            return;
        }

        entityList.add(entityData);
        LOGGER.debug("Storing entity data for player: {} - {}", playerUUID, entityResourceLocation);
        markDirty();
    }

    public static void spawnStoredEntities(Player player) {
        UUID playerUUID = player.getUUID();
        LOGGER.debug("Attempting to spawn entities for player: {}", playerUUID);

        ServerLevel serverLevel = (ServerLevel) player.level();

        if (spawnedEntities.containsKey(playerUUID) && !spawnedEntities.get(playerUUID).isEmpty()) {
            LOGGER.debug("Player {} has existing spawned entities. Removing and respawning.", playerUUID);

            // 1. Remove existing entities and save their NBT data
            List<Entity> existingEntities = spawnedEntities.get(playerUUID);
            List<CompoundTag> savedEntityData = new ArrayList<>();
            for (Entity entity : existingEntities) {
                CompoundTag entityData = new CompoundTag();
                entity.save(entityData);
                savedEntityData.add(entityData);
                entity.remove(Entity.RemovalReason.DISCARDED);
                LOGGER.debug("Removed existing entity and saved NBT data: {}", entity);
            }
            spawnedEntities.remove(playerUUID);

            // 2. Spawn new entities using the saved NBT data
            List<Entity> currentSpawnedEntities = new ArrayList<>();
            for (CompoundTag entityData : savedEntityData) {
                try {
                    if (!entityData.contains("id", 8)) {  // 8 is the Tag ID for a String
                        LOGGER.error("Entity NBT data missing 'id' tag! Data: {}", entityData);
                        continue;
                    }

                    String entityIdString = entityData.getString("id");
                    if (entityIdString == null || entityIdString.isEmpty()) {
                        LOGGER.error("Entity NBT data has empty or null 'id' tag! Data: {}", entityData);
                        continue;
                    }

                    ResourceLocation entityResourceLocation = ResourceLocation.tryParse(entityIdString);

                    if (entityResourceLocation == null) {
                        LOGGER.error("Could not parse ResourceLocation from id: {}", entityIdString);
                        continue;
                    }

                    EntityType<?> entityType =  BuiltInRegistries.ENTITY_TYPE.get(entityResourceLocation);

                    if (entityType == null) {
                        LOGGER.error("Could not find entity type {} in registry", entityResourceLocation);
                        continue;
                    }

                    Entity entity = entityType.create(serverLevel);

                    if (entity == null) {
                        LOGGER.error("Failed to create entity of type {}", entityType);
                        continue;
                    }

                    entity.load(entityData);

                    double x = player.getX() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
                    double y = player.getY();
                    double z = player.getZ() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;

                    entity.setPos(x, y, z);
                    serverLevel.addFreshEntity(entity);
                    LOGGER.debug("Respawned entity from saved NBT data: {}", entity);
                    currentSpawnedEntities.add(entity);

                } catch (Exception e) {
                    LOGGER.error("Failed to respawn entity", e);
                }
            }
            spawnedEntities.put(playerUUID, currentSpawnedEntities);

        } else {
            LOGGER.debug("Player {} has no existing spawned entities. Spawning from stored data.", playerUUID);

            if (playerEntities.containsKey(playerUUID) && !playerEntities.get(playerUUID).isEmpty()) {
                Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntities.get(playerUUID);
                List<Entity> currentSpawnedEntities = new ArrayList<>();

                for (Map.Entry<ResourceLocation, List<CompoundTag>> entry : playerEntityMap.entrySet()) {
                    ResourceLocation entityResourceLocation = entry.getKey();
                    List<CompoundTag> entitiesToSpawn = entry.getValue();
                    LOGGER.debug("Spawning entities of type {}: {}", entityResourceLocation, entitiesToSpawn.size());

                    for (CompoundTag entityData : entitiesToSpawn) {
                        try {
                            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityResourceLocation);
                            if (entityType == null) {
                                LOGGER.error("Could not find entity type {} in registry", entityResourceLocation);
                                continue;
                            }
                            Entity entity = entityType.create(serverLevel);
                            if (entity == null) {
                                LOGGER.error("Failed to create entity of type {}", entityType);
                                continue;
                            }
                            entity.load(entityData);

                            double x = player.getX() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
                            double y = player.getY();
                            double z = player.getZ() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;

                            entity.setPos(x, y, z);
                            serverLevel.addFreshEntity(entity);
                            LOGGER.debug("Spawned entity: {}", entity);
                            currentSpawnedEntities.add(entity);

                        } catch (Exception e) {
                            LOGGER.error("Failed to spawn entity", e);
                        }
                    }
                }
                spawnedEntities.put(playerUUID, currentSpawnedEntities);
            } else {
                LOGGER.debug("Player {} has no entities stored.", playerUUID);
            }
        }
    }


    public static boolean hasEntities(UUID playerUUID) {
        return playerEntities.containsKey(playerUUID) && !playerEntities.get(playerUUID).isEmpty();
    }

    public static void clearEntities(UUID playerUUID) {
        playerEntities.remove(playerUUID);
        clearSpawnedEntities(playerUUID); // Also clear spawned entities when clearing stored entities
        LOGGER.debug("Cleared entities for player: {}", playerUUID);
        markDirty();
    }

    public static List<Entity> getPlayerEntities(UUID playerUUID) {
        List<Entity> allEntities = new ArrayList<>();
        if (playerEntities.containsKey(playerUUID)) {
            Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntities.get(playerUUID);
            for (List<CompoundTag> entityList : playerEntityMap.values()) {
                // allEntities.addAll(entityList);
            }
        }
        return allEntities;
    }

    // New method to clear spawned entities
    public static void clearSpawnedEntities(UUID playerUUID) {
        if (spawnedEntities.containsKey(playerUUID)) {
            List<Entity> entitiesToRemove = spawnedEntities.get(playerUUID);
            for (Entity entity : entitiesToRemove) {
                entity.remove(Entity.RemovalReason.DISCARDED); // Remove the entity from the world
                LOGGER.debug("Removed entity: {}", entity);
            }
            spawnedEntities.remove(playerUUID);
            LOGGER.debug("Cleared spawned entities for player: {}", playerUUID);
        } else {
            LOGGER.debug("No spawned entities to clear for player: {}", playerUUID);
        }
    }

    // Persistent State Management
    private static EntityStorageState persistentState;

    private static void markDirty() {
        if (persistentState != null) {
            persistentState.setDirty();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        DimensionDataStorage storage = server.getLevel(server.overworld().dimension()).getDataStorage();
        persistentState = storage.computeIfAbsent(
                tag -> {
                    EntityStorageState state = EntityStorageState.create(tag, server.getLevel(server.overworld().dimension()));
                    playerEntities = state.getPlayerEntities();
                    return state;
                },
                () -> new EntityStorageState(new HashMap<>()),
                DATA_NAME
        );
        playerEntities = persistentState.getPlayerEntities();
        LOGGER.debug("Loaded entity storage from disk.");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.debug("Saving entity storage to disk.");
        markDirty();
    }

    public static class EntityStorageState extends SavedData {
        private Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities;

        public EntityStorageState(Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities) {
            this.playerEntities = playerEntities;
        }

        public Map<UUID, Map<ResourceLocation, List<CompoundTag>>> getPlayerEntities() {
            return playerEntities;
        }

        public static EntityStorageState create(CompoundTag tag, ServerLevel level) {
            Map<UUID, Map<ResourceLocation, List<CompoundTag>>> loadedEntities = new HashMap<>();
            CompoundTag allPlayers = tag.getCompound("playerEntities");

            for (String playerKey : allPlayers.getAllKeys()) {
                try {
                    UUID playerUUID = UUID.fromString(playerKey);
                    Map<ResourceLocation, List<CompoundTag>> playerEntityMap = new HashMap<>();
                    CompoundTag entityTypeMapTag = allPlayers.getCompound(playerKey);

                    for (String entityTypeKey : entityTypeMapTag.getAllKeys()) {
                        try {
                            ResourceLocation entityType = new ResourceLocation(entityTypeKey);
                            ListTag entityListTag = entityTypeMapTag.getList(entityTypeKey, 10); // 10 is the tag ID for CompoundTag
                            List<CompoundTag> entityList = new ArrayList<>();

                            for (int i = 0; i < entityListTag.size(); i++) {
                                CompoundTag entityTag = entityListTag.getCompound(i);
                                entityList.add(entityTag);
                            }
                            playerEntityMap.put(entityType, entityList);
                        } catch (Exception e) {
                            LOGGER.error("Error loading entity type {}: {}", entityTypeKey, e);
                        }
                    }
                    loadedEntities.put(playerUUID, playerEntityMap);

                } catch (IllegalArgumentException e) {
                    LOGGER.error("Failed to load UUID: {}", playerKey, e);
                }
            }

            return new EntityStorageState(loadedEntities);
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            CompoundTag allPlayers = new CompoundTag();
            for (Map.Entry<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntry : playerEntities.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntry.getValue();
                CompoundTag entityTypeMapTag = new CompoundTag();

                for (Map.Entry<ResourceLocation, List<CompoundTag>> entityTypeEntry : playerEntityMap.entrySet()) {
                    ResourceLocation entityType = entityTypeEntry.getKey();
                    List<CompoundTag> entityList = entityTypeEntry.getValue();
                    ListTag entityListTag = new ListTag();

                    for (CompoundTag entityTag : entityList) {
                        entityListTag.add(entityTag);
                    }
                    entityTypeMapTag.put(entityType.toString(), entityListTag);
                }
                allPlayers.put(playerUUID.toString(), entityTypeMapTag);
            }
            tag.put("playerEntities", allPlayers);
            return tag;
        }
    }
}