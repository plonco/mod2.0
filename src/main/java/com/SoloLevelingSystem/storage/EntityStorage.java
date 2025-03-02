package com.SoloLevelingSystem.storage;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.configs.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EntityStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStorage.class);
    private static final String DATA_NAME = "entity_storage";
    private static final Random RANDOM = new Random();
    private static final double SPAWN_RADIUS = 3.0;

    // Límites de almacenamiento por tipo
    private static final int MAX_NORMAL_ENTITIES = 5;
    private static final int MAX_MINIBOSS_ENTITIES = 2;
    private static final int MAX_BOSS_ENTITIES = 1;

    // Almacenamiento principal de entidades por jugador
    private static Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities = new HashMap<>();

    // Entidades actualmente invocadas
    private static final Map<UUID, List<Entity>> spawnedEntities = new HashMap<>();

    public static boolean storeEntity(UUID playerUUID, LivingEntity entity, CompoundTag entityData) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        // Verificar si la entidad está configurada
        if (!ConfigManager.isNormalEnemy(entityId) &&
                !ConfigManager.isMinibossEnemy(entityId) &&
                !ConfigManager.isBossEnemy(entityId)) {
            LOGGER.debug("Entity {} is not configured for storage", entityId);
            return false;
        }

        // Obtener el límite para esta entidad específica
        int maxEntities = ConfigManager.getEntityLimit(entityId);
        if (maxEntities <= 0) {
            LOGGER.debug("Entity {} has no storage limit configured", entityId);
            return false;
        }

        // Inicializar el mapa para el jugador si no existe
        playerEntities.computeIfAbsent(playerUUID, k -> new HashMap<>());
        Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntities.get(playerUUID);

        // Inicializar la lista para esta entidad específica si no existe
        playerEntityMap.computeIfAbsent(entityId, k -> new ArrayList<>());
        List<CompoundTag> entityList = playerEntityMap.get(entityId);

        // Verificar si se alcanzó el límite
        if (entityList.size() >= maxEntities) {
            LOGGER.warn("Player {} has reached the limit of {} entities for {}",
                    playerUUID, maxEntities, entityId);
            return false;
        }

        // Almacenar la entidad
        entityList.add(entityData);
        markDirty();
        LOGGER.debug("Stored entity {} for player {} ({}/{})",
                entityId, playerUUID, entityList.size(), maxEntities);
        return true;
    }

    private static int getMaxEntitiesForType(ResourceLocation entityId) {
        if (ConfigManager.isNormalEnemy(entityId)) {
            return MAX_NORMAL_ENTITIES;
        } else if (ConfigManager.isMinibossEnemy(entityId)) {
            return MAX_MINIBOSS_ENTITIES;
        } else if (ConfigManager.isBossEnemy(entityId)) {
            return MAX_BOSS_ENTITIES;
        }
        return 0;
    }

    public static boolean spawnStoredEntities(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            LOGGER.error("Cannot spawn entities in non-server level");
            return false;
        }

        UUID playerUUID = player.getUUID();
        LOGGER.debug("Attempting to spawn stored entities for player: {}", playerUUID);

        // Limpiar entidades previamente invocadas
        clearSpawnedEntities(playerUUID);

        if (!playerEntities.containsKey(playerUUID) || playerEntities.get(playerUUID).isEmpty()) {
            LOGGER.debug("No stored entities found for player: {}", playerUUID);
            return false;
        }

        Map<ResourceLocation, List<CompoundTag>> entityMap = playerEntities.get(playerUUID);
        List<Entity> currentSpawnedEntities = new ArrayList<>();
        boolean spawnedAny = false;

        for (Map.Entry<ResourceLocation, List<CompoundTag>> entry : entityMap.entrySet()) {
            ResourceLocation entityId = entry.getKey();
            List<CompoundTag> entityDataList = entry.getValue();

            LOGGER.debug("Attempting to spawn {} entities of type {}", entityDataList.size(), entityId);

            for (CompoundTag entityData : entityDataList) {
                try {
                    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                    if (entityType == null) {
                        LOGGER.error("Could not find entity type: {}", entityId);
                        continue;
                    }

                    Entity entity = entityType.create(serverLevel);
                    if (entity == null) {
                        LOGGER.error("Failed to create entity of type: {}", entityType);
                        continue;
                    }

                    // Cargar datos y configurar la entidad
                    entity.load(entityData);

                    // Posicionar cerca del jugador
                    double x = player.getX() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
                    double y = player.getY();
                    double z = player.getZ() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
                    entity.setPos(x, y, z);

                    // Hacer la entidad amistosa
                    if (entity instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.setPersistenceRequired(); // Evitar que desaparezca
                        mob.setAggressive(false);
                        // Si es domesticable, hacerla del jugador
                        if (mob instanceof TamableAnimal tamable) {
                            tamable.tame(player);
                        }
                    }

                    // Añadir la entidad al mundo
                    if (serverLevel.addFreshEntity(entity)) {
                        currentSpawnedEntities.add(entity);
                        spawnedAny = true;
                        LOGGER.debug("Successfully spawned entity: {} at ({}, {}, {})",
                                entity, x, y, z);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error spawning entity: ", e);
                }
            }
        }

        if (!currentSpawnedEntities.isEmpty()) {
            spawnedEntities.put(playerUUID, currentSpawnedEntities);
        }

        return spawnedAny;
    }

    public static List<Entity> getPlayerEntities(UUID playerUUID) {
        return spawnedEntities.getOrDefault(playerUUID, new ArrayList<>());
    }

    public static void clearSpawnedEntities(UUID playerUUID) {
        if (spawnedEntities.containsKey(playerUUID)) {
            List<Entity> entitiesToRemove = spawnedEntities.get(playerUUID);
            for (Entity entity : entitiesToRemove) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                LOGGER.debug("Removed spawned entity: {}", entity);
            }
            spawnedEntities.remove(playerUUID);
            LOGGER.debug("Cleared all spawned entities for player: {}", playerUUID);
        }
    }

    public static void clearEntities(UUID playerUUID) {
        playerEntities.remove(playerUUID);
        clearSpawnedEntities(playerUUID);
        LOGGER.debug("Cleared all stored entities for player: {}", playerUUID);
        markDirty();
    }

    public static boolean hasEntities(UUID playerUUID) {
        return playerEntities.containsKey(playerUUID) &&
                !playerEntities.get(playerUUID).isEmpty();
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
                EntityStorageState::create,
                () -> new EntityStorageState(new HashMap<>()),
                DATA_NAME
        );
        playerEntities = persistentState.getPlayerEntities();
        LOGGER.info("Loaded entity storage from disk");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Saving entity storage to disk");
        markDirty();
    }

    public static class EntityStorageState extends SavedData {
        private final Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities;

        public EntityStorageState(Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities) {
            this.playerEntities = playerEntities;
        }

        public Map<UUID, Map<ResourceLocation, List<CompoundTag>>> getPlayerEntities() {
            return playerEntities;
        }

        public static EntityStorageState create(CompoundTag tag) {
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
                            ListTag entityListTag = entityTypeMapTag.getList(entityTypeKey, CompoundTag.TAG_COMPOUND);
                            List<CompoundTag> entityList = new ArrayList<>();

                            for (int i = 0; i < entityListTag.size(); i++) {
                                entityList.add(entityListTag.getCompound(i));
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
                CompoundTag playerTag = new CompoundTag();

                for (Map.Entry<ResourceLocation, List<CompoundTag>> entityTypeEntry : playerEntry.getValue().entrySet()) {
                    ListTag entityListTag = new ListTag();
                    for (CompoundTag entityData : entityTypeEntry.getValue()) {
                        entityListTag.add(entityData);
                    }
                    playerTag.put(entityTypeEntry.getKey().toString(), entityListTag);
                }

                allPlayers.put(playerEntry.getKey().toString(), playerTag);
            }

            tag.put("playerEntities", allPlayers);
            return tag;
        }
    }
}