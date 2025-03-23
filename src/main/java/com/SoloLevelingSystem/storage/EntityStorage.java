package com.SoloLevelingSystem.storage;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.entity.ai.CustomFollowPlayerGoal;
import com.SoloLevelingSystem.events.EntityVisualEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
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
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EntityStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStorage.class);
    private static final String DATA_NAME = "entity_storage";
    private static final Random RANDOM = new Random();
    private static final double SPAWN_RADIUS = 3.0;

    private static Map<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntities = new HashMap<>();
    private static final Map<UUID, List<Entity>> spawnedEntities = new HashMap<>();
    private static final Map<UUID, Set<ResourceLocation>> shownLimitMessages = new HashMap<>();

    public static void clearLimitMessage(UUID playerUUID, ResourceLocation entityId) {
        shownLimitMessages.computeIfAbsent(playerUUID, k -> new HashSet<>()).remove(entityId);
    }

    public static boolean hasShownLimitMessage(UUID playerUUID, ResourceLocation entityId) {
        return shownLimitMessages.computeIfAbsent(playerUUID, k -> new HashSet<>()).contains(entityId);
    }

    private static void markLimitMessageShown(UUID playerUUID, ResourceLocation entityId) {
        shownLimitMessages.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(entityId);
    }

    public static boolean storeEntity(UUID playerUUID, LivingEntity entity, CompoundTag entityData) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (!ConfigManager.isNormalEnemy(entityId) &&
                !ConfigManager.isMinibossEnemy(entityId) &&
                !ConfigManager.isBossEnemy(entityId)) {
            LOGGER.debug("Entity {} is not configured for storage", entityId);
            return false;
        }

        int maxEntities = ConfigManager.getEntityLimit(entityId);
        if (maxEntities <= 0) {
            LOGGER.debug("Entity {} has no storage limit configured", entityId);
            return false;
        }

        playerEntities.computeIfAbsent(playerUUID, k -> new HashMap<>());
        Map<ResourceLocation, List<CompoundTag>> playerEntityMap = playerEntities.get(playerUUID);

        playerEntityMap.computeIfAbsent(entityId, k -> new ArrayList<>());
        List<CompoundTag> entityList = playerEntityMap.get(entityId);

        if (entityList.size() >= maxEntities) {
            markLimitMessageShown(playerUUID, entityId);
            LOGGER.warn("Player {} has reached the limit of {} entities for {}",
                    playerUUID, maxEntities, entityId);
            return false;
        }

        CompoundTag cleanData = new CompoundTag();
        cleanData.putString("id", entityId.toString());

        if (entityData.contains("Attributes")) {
            cleanData.put("Attributes", entityData.get("Attributes"));
        }
        if (entityData.contains("ArmorItems")) {
            cleanData.put("ArmorItems", entityData.get("ArmorItems"));
        }
        if (entityData.contains("HandItems")) {
            cleanData.put("HandItems", entityData.get("HandItems"));
        }
        if (entityData.contains("Tags")) {
            cleanData.put("Tags", entityData.get("Tags"));
        }
        if (entityData.contains("CustomName")) {
            cleanData.put("CustomName", entityData.get("CustomName"));
        }

        clearLimitMessage(playerUUID, entityId);
        entityList.add(cleanData);
        markDirty();
        LOGGER.debug("Stored entity {} for player {} ({}/{})",
                entityId, playerUUID, entityList.size(), maxEntities);
        return true;
    }

    public static boolean spawnStoredEntities(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            LOGGER.error("Cannot spawn entities in non-server level");
            return false;
        }

        UUID playerUUID = player.getUUID();
        LOGGER.debug("Attempting to spawn stored entities for player: {}", playerUUID);

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

                    CompoundTag modifiedData = entityData.copy();
                    if (entity instanceof LivingEntity) {
                        modifiedData.remove("DeathTime");
                        modifiedData.remove("DeathLootTable");
                        modifiedData.remove("Health");
                        modifiedData.remove("HurtTime");
                        modifiedData.remove("HurtByTimestamp");
                        modifiedData.remove("Brain");
                        modifiedData.putBoolean("Dead", false);
                    }

                    // Preparar los tags antes de cargar la entidad
                    ListTag tagsListPre = new ListTag();
                    tagsListPre.add(StringTag.valueOf("friendly"));
                    tagsListPre.add(StringTag.valueOf("psummoned"));
                    modifiedData.put("Tags", tagsListPre);

                    entity.load(modifiedData);

                    if (entity instanceof LivingEntity living) {
                        living.setHealth(living.getMaxHealth());
                    }

                    double x = player.getX() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
                    double y = player.getY();
                    double z = player.getZ() + (RANDOM.nextDouble() * 2 - 1) * SPAWN_RADIUS;
                    entity.setPos(x, y, z);

                    if (entity instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.setPersistenceRequired();
                        mob.setNoAi(false);
                        mob.removeAllEffects();
                        mob.clearFire();
                        mob.setRemainingFireTicks(0);
                        mob.setInvulnerable(false);
                        mob.setAirSupply(mob.getMaxAirSupply());

                        mob.goalSelector.getAvailableGoals().clear();
                        mob.targetSelector.getAvailableGoals().clear();

                        mob.goalSelector.addGoal(1, new CustomFollowPlayerGoal(
                                mob,
                                player,
                                1.0D,
                                20.0F,
                                5.0F
                        ));

                        if (mob instanceof PathfinderMob pathfinderMob) {
                            pathfinderMob.goalSelector.addGoal(3, new MeleeAttackGoal(pathfinderMob, 1.2D, true));
                            pathfinderMob.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(pathfinderMob, 1.0D));
                            pathfinderMob.goalSelector.addGoal(5, new RandomLookAroundGoal(pathfinderMob));

                            pathfinderMob.targetSelector.addGoal(2, new HurtByTargetGoal(pathfinderMob));
                            pathfinderMob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(pathfinderMob,
                                    LivingEntity.class, 10, true, false, (target) -> {

                                if (mob instanceof RangedAttackMob rangedAttackMob) {
                                    pathfinderMob.goalSelector.addGoal(2, new RangedAttackGoal(rangedAttackMob, 1.0D, 20, 15.0F));
                                }
                                if (mob instanceof Creeper) {
                                    mob.goalSelector.addGoal(2, new SwellGoal((Creeper) mob));
                                }



                                if (target instanceof Mob targetMob && targetMob.getTarget() == player) return true;
                                return target == player.getLastHurtMob() || target == player.getLastHurtByMob();
                            }));
                        }

                        // Verificar y sincronizar tags
                        if (serverLevel.addFreshEntity(entity)) {
                            // Asegurar que los tags se apliquen después de añadir la entidad
                            entity.addTag("psummoned");

                            // Sincronizar con todos los jugadores cercanos
                            for (ServerPlayer nearbyPlayer : serverLevel.players()) {
                                if (nearbyPlayer.distanceTo(entity) < 64.0D) {
                                    serverLevel.getChunkSource().broadcastAndSend(entity,
                                            new ClientboundSetEntityDataPacket(entity.getId(),
                                                    entity.getEntityData().getNonDefaultValues()));
                                }
                            }

                            EntityVisualEffects.createSummonEffect(entity, serverLevel);

                            currentSpawnedEntities.add(entity);
                            spawnedAny = true;

                            LOGGER.debug("Successfully spawned entity: {} at ({}, {}, {})",
                                    entity, x, y, z);
                            System.out.println("Tags after spawn: " + entity.getTags()); // Debug log
                        }
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
        shownLimitMessages.remove(playerUUID);
        LOGGER.debug("Cleared all stored entities for player: {}", playerUUID);
        markDirty();
    }

    public static boolean hasEntities(UUID playerUUID) {
        return playerEntities.containsKey(playerUUID) &&
                !playerEntities.get(playerUUID).isEmpty();
    }

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

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();
        LOGGER.debug("Player {} disconnected, removing their spawned entities", player.getName().getString());
        clearSpawnedEntities(playerUUID);
    }

    public static List<Entity> getLivingPlayerEntities(UUID playerUUID) {
        return spawnedEntities.getOrDefault(playerUUID, new ArrayList<>()).stream()
                .filter(entity -> entity != null && entity.isAlive())
                .collect(Collectors.toList());
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
                            LOGGER.error("Error loading entity type {}: {}", entityTypeKey, e.getMessage());
                        }
                    }

                    loadedEntities.put(playerUUID, playerEntityMap);
                } catch (Exception e) {
                    LOGGER.error("Error loading player data {}: {}", playerKey, e.getMessage());
                }
            }

            return new EntityStorageState(loadedEntities);
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            CompoundTag allPlayers = new CompoundTag();

            for (Map.Entry<UUID, Map<ResourceLocation, List<CompoundTag>>> playerEntry : playerEntities.entrySet()) {
                CompoundTag entityTypeMap = new CompoundTag();

                for (Map.Entry<ResourceLocation, List<CompoundTag>> entityTypeEntry : playerEntry.getValue().entrySet()) {
                    ListTag entityList = new ListTag();
                    entityTypeEntry.getValue().forEach(entityList::add);
                    entityTypeMap.put(entityTypeEntry.getKey().toString(), entityList);
                }
                allPlayers.put(playerEntry.getKey().toString(), entityTypeMap);
            }

            tag.put("playerEntities", allPlayers);
            return tag;
        }
    }
}