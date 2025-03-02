package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.storage.EntityStorage;
import com.SoloLevelingSystem.storage.LastAttackerStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;
import java.util.List;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);

    private static final double MAX_DISTANCE = 10.0; // Maximum distance to consider the player as the killer

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Entity target = event.getTarget();

            UUID playerUUID = player.getUUID();
            List<Entity> spawnedEntities = EntityStorage.getPlayerEntities(playerUUID);

            if (spawnedEntities != null && spawnedEntities.contains(target)) {
                LOGGER.debug("Preventing player from attacking their own spawned entity: {}", target);
                event.setCanceled(true);
                return;
            }

            if (event.getTarget() instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) event.getTarget();
                LastAttackerStorage.setLastAttacker(entity, player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity entity = (LivingEntity) event.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        LOGGER.debug("Entity died: {}", entityId);
        if (ConfigManager.isNormalEnemy(entityId)) {
            LOGGER.debug("Entity is a normal enemy: {}", entityId);
            // ...
        } else {
            LOGGER.debug("Entity is NOT a normal enemy: {}", entityId);
        }

        // Check if the entity is a target enemy
        if (ConfigManager.isNormalEnemy(entityId) || ConfigManager.isMinibossEnemy(entityId) || ConfigManager.isBossEnemy(entityId)) {
            // Check if the killer is a player
            UUID lastAttackerUUID = LastAttackerStorage.getLastAttacker(entity);
            if (lastAttackerUUID != null) {
                Player player = event.getEntity().level().getPlayerByUUID(lastAttackerUUID);
                if (player != null && player.isAlive()) {
                    // Check if the player is close enough to the entity
                    Vec3 entityPos = entity.position();
                    Vec3 playerPos = player.position();
                    double distance = entityPos.distanceTo(playerPos);

                    if (distance <= MAX_DISTANCE) {
                        // Store a *new* copy of the entity in the player's storage
                        CompoundTag entityData = new CompoundTag();
                        entity.save(entityData);
                        EntityStorage.storeEntity(player.getUUID(), entity, entityData);
                        LOGGER.debug("Stored entity for player: {} - {}", player.getUUID(), entityId);
                    } else {
                        LOGGER.debug("Entity not killed by player, player is too far away.");
                    }
                } else {
                    LOGGER.debug("Entity not killed by player, last attacker is not a valid player.");
                }
            } else {
                LOGGER.debug("Entity not killed by player, no last attacker found.");
            }

            LastAttackerStorage.clearLastAttacker(entity); // Clear the last attacker after death
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Entity source = event.getSource().getEntity();

            if (source != null) {
                UUID playerUUID = player.getUUID();
                List<Entity> spawnedEntities = EntityStorage.getPlayerEntities(playerUUID);

                if (spawnedEntities != null && spawnedEntities.contains(source)) {
                    LOGGER.debug("Preventing damage from spawned entity: {}", source);
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
