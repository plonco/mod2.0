package com.SoloLevelingSystem.storage;

import com.SoloLevelingSystem.configs.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber
public class EntityStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStorage.class);
    private static final Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities = new HashMap<>();
    private static final Map<UUID, List<Entity>> spawnedEntities = new HashMap<>();
    private static final Set<UUID> summonedEntities = new HashSet<>();
    private static final int MAX_NORMAL_ENEMIES = 10;
    private static final int MAX_MINIBOSSES = 1;
    private static final int MAX_BOSSES = 1;
    private static final Random RANDOM = new Random();
    private static final double SPAWN_RADIUS = 2.0;

    // Método para obtener el archivo de datos del jugador
    private static File getPlayerDataFile(ServerLevel level, UUID playerUUID) {
        // Obtener el directorio de datos del mundo (normalmente en "saves/<nombre_mundo>/playerdata")
        File worldDirectory = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();

        // Crear un subdirectorio para los datos del mod (por ejemplo, "SoloLevelingSystem")
        File modDataDirectory = new File(worldDirectory, "SoloLevelingSystem");
        if (!modDataDirectory.exists()) {
            modDataDirectory.mkdirs(); // Crear el directorio si no existe
        }

        // Crear un archivo para los datos del jugador usando su UUID como nombre
        return new File(modDataDirectory, playerUUID.toString() + ".dat");
    }

    // Método para guardar los datos del jugador
    public static void savePlayerData(ServerLevel level, UUID playerUUID) {
        File dataFile = getPlayerDataFile(level, playerUUID);
        CompoundTag playerData = new CompoundTag();

        // Guardar las entidades convocadas
        if (spawnedEntities.containsKey(playerUUID)) {
            ListTag entityListTag = new ListTag();
            for (Entity entity : spawnedEntities.get(playerUUID)) {
                CompoundTag entityData = new CompoundTag();
                entity.save(entityData); // Guardar la entidad en NBT
                LOGGER.debug("Saving entity data: {}", entityData);
                entityListTag.add(entityData);
            }
            playerData.put("summonedEntities", entityListTag);
        }

        // Guardar las entidades almacenadas
        if (playerEntities.containsKey(playerUUID)) {
            CompoundTag storedEntitiesTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, List<CompoundTag>> entry : playerEntities.get(playerUUID).entrySet()) {
                ListTag entityListTag = new ListTag();
                entityListTag.addAll(entry.getValue()); // Agregar todas las entidades de este tipo
                storedEntitiesTag.put(entry.getKey().toString(), entityListTag);
            }
            playerData.put("storedEntities", storedEntitiesTag);
        }

        // Escribir los datos en el archivo
        try (FileOutputStream fileOutputStream = new FileOutputStream(dataFile)) {
            net.minecraft.nbt.NbtIo.writeCompressed(playerData, fileOutputStream);
            LOGGER.debug("Saved player data to file: {}", dataFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Error saving player data for player: {}", playerUUID, e);
        }
    }

    // Método para cargar los datos del jugador
    public static void loadPlayerData(ServerLevel level, UUID playerUUID) {
        File dataFile = getPlayerDataFile(level, playerUUID);

        // Estructuras temporales para carga segura
        List<Entity> tempSpawned = new ArrayList<>();
        Map<ResourceLocation, List<CompoundTag>> tempStored = new HashMap<>();

        try {
            if (dataFile.exists() && dataFile.length() > 0) {
                try (FileInputStream fileInputStream = new FileInputStream(dataFile)) {
                    CompoundTag playerData = NbtIo.readCompressed(fileInputStream);

                    // 1. Cargar entidades convocadas (spawned)
                    if (playerData.contains("summonedEntities", Tag.TAG_LIST)) {
                        ListTag entityListTag = playerData.getList("summonedEntities", Tag.TAG_COMPOUND);
                        LOGGER.debug("Loading {} summoned entities for {}", entityListTag.size(), playerUUID);

                        for (int i = 0; i < entityListTag.size(); i++) {
                            CompoundTag entityTag = entityListTag.getCompound(i);
                            ResourceLocation entityTypeId = new ResourceLocation(entityTag.getString("id"));
                            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);

                            if (type != null) {
                                Entity entity = type.create(level);
                                if (entity != null) {
                                    entity.load(entityTag);
                                    tempSpawned.add(entity);
                                    LOGGER.debug("Loaded summoned entity: {}", entityTypeId);
                                }
                            } else {
                                LOGGER.warn("Unknown entity type: {}", entityTypeId);
                            }
                        }
                    }

                    // 2. Cargar entidades almacenadas (stored)
                    if (playerData.contains("storedEntities", Tag.TAG_COMPOUND)) {
                        CompoundTag storedEntitiesTag = playerData.getCompound("storedEntities");
                        LOGGER.debug("Loading stored entities for {}", playerUUID);

                        for (String key : storedEntitiesTag.getAllKeys()) {
                            ResourceLocation entityTypeId = new ResourceLocation(key);
                            ListTag entityListTag = storedEntitiesTag.getList(key, Tag.TAG_COMPOUND);
                            List<CompoundTag> entities = new ArrayList<>();

                            for (int i = 0; i < entityListTag.size(); i++) {
                                entities.add(entityListTag.getCompound(i));
                            }

                            tempStored.put(entityTypeId, entities);
                            LOGGER.debug("Loaded {} stored entities of type {}", entities.size(), entityTypeId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load data for {}: {}", playerUUID, e.getMessage());
            // Mantener datos existentes en caso de error
            tempSpawned = spawnedEntities.getOrDefault(playerUUID, new ArrayList<>());
            tempStored = playerEntities.getOrDefault(playerUUID, new HashMap<>());
        } finally {
            // Actualizar estructuras principales de manera atómica
            spawnedEntities.put(playerUUID, tempSpawned);
            playerEntities.put(playerUUID, tempStored);

            // Inicializar si está vacío
            if (!spawnedEntities.containsKey(playerUUID)) {
                spawnedEntities.put(playerUUID, new ArrayList<>());
            }
            if (!playerEntities.containsKey(playerUUID)) {
                playerEntities.put(playerUUID, new HashMap<>());
            }
        }

        LOGGER.debug("Finished loading data for {}", playerUUID);
    }

    // Método para spawnear entidades almacenadas
    public static void spawnStoredEntities(Player player) {
        UUID playerUUID = player.getUUID();
        if (!spawnedEntities.containsKey(playerUUID) || spawnedEntities.get(playerUUID).isEmpty()) {
            LOGGER.debug("No entities to spawn for player: {}", playerUUID);
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        List<Entity> entitiesToSpawn = new ArrayList<>(spawnedEntities.get(playerUUID));

        for (Entity entity : entitiesToSpawn) {
            if (!entity.isAlive()) { // Solo respawnear si la entidad no está viva
                Entity newEntity = EntityType.loadEntityRecursive(entity.saveWithoutId(new CompoundTag()), level, (e) -> e);
                if (newEntity != null) {
                    level.addFreshEntity(newEntity);
                    LOGGER.debug("Respawned entity: {}", newEntity);
                }
            }
        }
    }

    // Método para almacenar una entidad
    public static void storeEntity(UUID playerUUID, Entity entity, CompoundTag entityData) {
        ResourceLocation entityResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (entityResourceLocation == null) {
            LOGGER.error("Entity Resource Location is null for entity: {}", entity);
            return;
        }

        // Debug: Verificar clasificación
        LOGGER.debug("Clasificando {} - Normal: {}, Miniboss: {}, Boss: {}",
                entityResourceLocation,
                ConfigManager.isNormalEnemy(entityResourceLocation),
                ConfigManager.isMinibossEnemy(entityResourceLocation),
                ConfigManager.isBossEnemy(entityResourceLocation));

        Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntities.computeIfAbsent(playerUUID, k -> new HashMap<>());
        List<CompoundTag> entityList = playerEntityMap.computeIfAbsent(entityResourceLocation, k -> new ArrayList<>());

        int maxEntities = 0;
        String type = "Desconocido";

        if (ConfigManager.isNormalEnemy(entityResourceLocation)) {
            maxEntities = MAX_NORMAL_ENEMIES;
            type = "Normal";
        } else if (ConfigManager.isMinibossEnemy(entityResourceLocation)) {
            maxEntities = MAX_MINIBOSSES;
            type = "Miniboss";
        } else if (ConfigManager.isBossEnemy(entityResourceLocation)) {
            maxEntities = MAX_BOSSES;
            type = "Boss";
        }

        LOGGER.debug("Límite configurado para {}: {} (Tipo: {})", entityResourceLocation, maxEntities, type);

        if (entityList.size() >= maxEntities) {
            LOGGER.warn("Límite alcanzado - Jugador: {}, Entidad: {}, Almacenadas: {}/{}",
                    playerUUID, entityResourceLocation, entityList.size(), maxEntities);
            return;
        }

        entityList.add(entityData);
        LOGGER.debug("Entidad almacenada - Jugador: {}, Tipo: {}, Total: {}",
                playerUUID, entityResourceLocation, entityList.size());

        // Agregar a spawnedEntities
        List<Entity> entities = spawnedEntities.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        entities.add(entity);
        LOGGER.debug("Entidad añadida a spawnedEntities: {}", entity);

        // Guardar datos
        if (entity.level() instanceof ServerLevel level) {
            savePlayerData(level, playerUUID);
            LOGGER.debug("Datos guardados inmediatamente para {}", playerUUID);
        } else {
            LOGGER.error("No se pudo obtener ServerLevel para guardar");
        }
    }

    // Método para verificar si un jugador tiene entidades almacenadas
    public static boolean hasEntities(UUID playerUUID) {
        return playerEntities.containsKey(playerUUID) && !playerEntities.get(playerUUID).isEmpty();
    }

    // Método para limpiar las entidades de un jugador
    public static void clearEntities(UUID playerUUID) {
        playerEntities.remove(playerUUID);
        summonedEntities.removeIf(uuid -> spawnedEntities.values().stream()
                .flatMap(List::stream)
                .noneMatch(entity -> entity.getUUID().equals(uuid)));
        clearSpawnedEntities(playerUUID);
        LOGGER.debug("Cleared entities for player: {}", playerUUID);
    }

    // Método para obtener las entidades de un jugador
    public static List<Entity> getPlayerEntities(UUID playerUUID) {
        List<Entity> allEntities = new ArrayList<>();
        if (spawnedEntities.containsKey(playerUUID)) {
            allEntities.addAll(spawnedEntities.get(playerUUID));
        }
        return allEntities;
    }

    // Método para limpiar las entidades spawnadas de un jugador
    public static void clearSpawnedEntities(UUID playerUUID) {
        if (spawnedEntities.containsKey(playerUUID)) {
            List<Entity> entitiesToRemove = spawnedEntities.get(playerUUID);
            for (Entity entity : entitiesToRemove) {
                summonedEntities.remove(entity.getUUID());
                entity.remove(Entity.RemovalReason.DISCARDED);
                LOGGER.debug("Removed entity: {}", entity);
            }
            spawnedEntities.remove(playerUUID);
            LOGGER.debug("Cleared spawned entities for player: {}", playerUUID);
        } else {
            LOGGER.debug("No spawned entities to clear for player: {}", playerUUID);
        }
    }

    // Método para verificar si una entidad es convocada
    public static boolean isSummonedEntity(UUID entityUUID) {
        return summonedEntities.contains(entityUUID);
    }

    // Evento para guardar datos al detener el servidor
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID playerUUID : spawnedEntities.keySet()) {
                savePlayerData(level, playerUUID);
            }
        }
        LOGGER.info("Saved all summoned entities before server shutdown.");
    }

    // Evento para cargar datos al iniciar el servidor
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            for (Player player : level.players()) {
                loadPlayerData(level, player.getUUID());
                spawnStoredEntities(player);
            }
        }
        LOGGER.info("Loaded all summoned entities after server startup.");
    }

    // Evento para cargar datos al conectar un jugador
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerLevel level = (ServerLevel) player.level();
            UUID playerUUID = player.getUUID();

            // Cargar datos solo si el archivo existe y no está vacío
            loadPlayerData(level, playerUUID);
            LOGGER.debug("Loaded player data on login for player: {}", playerUUID);

            // Spawnear entidades almacenadas
            spawnStoredEntities(player);
        }
    }

    // Evento para guardar datos al desconectar un jugador
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerLevel level = (ServerLevel) player.level();
            UUID playerUUID = player.getUUID();

            // Guardar datos al desconectar
            savePlayerData(level, playerUUID);
            LOGGER.debug("Saved player data on logout for player: {}", playerUUID);
        }
    }
}