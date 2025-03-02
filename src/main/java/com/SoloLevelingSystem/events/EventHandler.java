package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.storage.EntityStorage;
import com.SoloLevelingSystem.storage.LastAttackerStorage;
import com.SoloLevelingSystem.storage.PlayerTargetStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);
    private static final Map<UUID, CompoundTag> preDeathEntityData = new HashMap<>();

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getTarget();
            if (event.getEntity() instanceof Player) {
                // Verificar si el objetivo es una entidad convocada
                if (EntityStorage.isSummonedEntity(target.getUUID())) {
                    event.setCanceled(true); // Cancelar el ataque
                    LOGGER.debug("Prevented attack on summoned entity: {}", target.getUUID());
                    return;
                }
                LastAttackerStorage.setLastAttacker(target, (Player) event.getEntity());
                LOGGER.debug("Set last attacker for entity: {} - {}", target, event.getEntity().getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player && event.getEntity() instanceof Mob) {
            Player player = (Player) event.getSource().getEntity();
            Mob target = (Mob) event.getEntity();
            PlayerTargetStorage.setPlayerTarget(player.getUUID(), target);

            // Capturar datos NBT antes de la muerte
            CompoundTag entityData = new CompoundTag();
            target.save(entityData);
            preDeathEntityData.put(target.getUUID(), entityData);

            LOGGER.debug("Set player target: {} - {}", player.getUUID(), target);
            LOGGER.debug("Stored pre-death data for entity: {}", target.getUUID());
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

        // Verificar si la entidad fue descartada (por ejemplo, al limpiar entidades)
        if (entity.isRemoved()) {
            LOGGER.debug("Entity was discarded, not storing it again.");
            return;
        }

        // Verificar si la muerte fue causada por un jugador
        DamageSource damageSource = event.getSource();
        if (damageSource != null && damageSource.getEntity() instanceof Player) {
            // Forzar la ejecución en el hilo del servidor
            if (!entity.level().isClientSide) {
                ServerLevel serverLevel = (ServerLevel) entity.level();
                MinecraftServer server = serverLevel.getServer();
                server.execute(() -> {
                    // Obtener el último atacante
                    UUID lastAttackerUUID = LastAttackerStorage.getLastAttacker(entity);

                    if (lastAttackerUUID != null) {
                        // Recuperar datos NBT almacenados
                        CompoundTag entityData = preDeathEntityData.get(entity.getUUID());
                        if (entityData != null) {
                            LOGGER.debug("Retrieved pre-death data for entity: {}", entity.getUUID());
                            EntityStorage.storeEntity(lastAttackerUUID, entity, entityData);
                            LOGGER.debug("Stored entity for player: {} - {}", lastAttackerUUID, entityId);
                            LastAttackerStorage.clearLastAttacker(entity);
                            PlayerTargetStorage.clearPlayerTarget(lastAttackerUUID);

                            // Eliminar de almacenamiento temporal
                            preDeathEntityData.remove(entity.getUUID());
                            LOGGER.debug("Removed pre-death data for entity: {}", entity.getUUID());
                        } else {
                            LOGGER.warn("No pre-death entity data found for {}", entityId);
                        }
                    } else {
                        LOGGER.debug("Entity not killed by player, no last attacker found.");
                    }
                });
            } else {
                LOGGER.debug("Skipping on client side.");
            }
        } else {
            LOGGER.debug("Entity died from non-player source: {}", damageSource);
        }
    }
}