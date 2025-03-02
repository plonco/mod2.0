package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.configs.ConfigManager;
import com.SoloLevelingSystem.storage.EntityStorage;
import com.SoloLevelingSystem.storage.LastAttackerStorage;
import com.SoloLevelingSystem.storage.PlayerSummons;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);
    private static final double MAX_DISTANCE = 10.0;

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        // Si el jugador ataca a una entidad, hacer que sus invocaciones también la ataquen
        if (target instanceof LivingEntity livingTarget && !(target instanceof Player)) {
            List<Entity> playerSummons = EntityStorage.getPlayerEntities(player.getUUID());
            if (playerSummons != null) {
                for (Entity summonedEntity : playerSummons) {
                    if (summonedEntity instanceof Mob mob) {
                        mob.setTarget(livingTarget);
                    }
                }
            }
        }

        // Prevenir que el jugador ataque a sus propias invocaciones
        UUID playerUUID = player.getUUID();
        List<Entity> spawnedEntities = EntityStorage.getPlayerEntities(playerUUID);

        if (spawnedEntities != null && spawnedEntities.contains(target)) {
            LOGGER.debug("Preventing player from attacking their own spawned entity: {}", target);
            event.setCanceled(true);
            return;
        }

        if (target instanceof LivingEntity entity) {
            LastAttackerStorage.setLastAttacker(entity, player);
        }
    }

    @SubscribeEvent
    public static void onLivingSetTarget(LivingChangeTargetEvent event) {
        Entity entity = event.getEntity();
        LivingEntity target = event.getNewTarget();

        // Si la entidad es una de nuestras invocaciones
        if (entity instanceof Mob && entity.getTags().contains("player_summon")) {
            // Solo cancelar si el objetivo es un jugador
            if (target instanceof Player) {
                event.setCanceled(true);
                ((Mob) entity).setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && entity.getTags().contains("player_summon")) {
            // Solo cancelar la agresión contra jugadores
            if (mob.getTarget() instanceof Player) {
                mob.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        UUID lastAttackerUUID = LastAttackerStorage.getLastAttacker(victim);
        if (lastAttackerUUID != null) {
            Player player = victim.level().getPlayerByUUID(lastAttackerUUID);

            if (player != null && player.isAlive() &&
                    player.level() instanceof ServerLevel serverLevel) {

                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
                LOGGER.debug("Entity died: {}", entityId);

                // Verificar si es un enemigo válido
                if (ConfigManager.isNormalEnemy(entityId) ||
                        ConfigManager.isMinibossEnemy(entityId) ||
                        ConfigManager.isBossEnemy(entityId)) {

                    // Verificar distancia
                    Vec3 entityPos = victim.position();
                    Vec3 playerPos = player.position();
                    double distance = entityPos.distanceTo(playerPos);

                    if (distance <= MAX_DISTANCE) {
                        // Guardar datos de la entidad
                        CompoundTag entityData = new CompoundTag();
                        victim.save(entityData);

                        // Intentar almacenar la entidad
                        boolean stored = EntityStorage.storeEntity(player.getUUID(), victim, entityData);

                        if (stored) {
                            // Solo registrar para invocación si se almacenó correctamente
                            PlayerSummons summons = PlayerSummons.get(serverLevel);
                            String cleanEntityId = entityId.toString().replace("_loot", "");
                            ResourceLocation cleanId = new ResourceLocation(cleanEntityId);

                            if (ConfigManager.isNormalEnemy(entityId)) {
                                if (summons.addSummon(player.getUUID(), cleanId, PlayerSummons.SummonType.NORMAL)) {
                                    player.sendSystemMessage(Component.literal("¡Nueva entidad normal añadida a tus invocaciones!"));
                                }
                            } else if (ConfigManager.isMinibossEnemy(entityId)) {
                                if (summons.addSummon(player.getUUID(), cleanId, PlayerSummons.SummonType.MINIBOSS)) {
                                    player.sendSystemMessage(Component.literal("¡Nueva entidad miniboss añadida a tus invocaciones!"));
                                }
                            } else if (ConfigManager.isBossEnemy(entityId)) {
                                if (summons.addSummon(player.getUUID(), cleanId, PlayerSummons.SummonType.BOSS)) {
                                    player.sendSystemMessage(Component.literal("¡Nueva entidad boss añadida a tus invocaciones!"));
                                }
                            }

                            LOGGER.debug("Stored entity for player: {} - {}", player.getUUID(), entityId);
                        } else if (!EntityStorage.hasShownLimitMessage(player.getUUID(), entityId)) {
                            // Solo mostrar el mensaje si no se ha mostrado antes
                            String entityName = entityId.toString().replace("minecraft:", "");
                            player.sendSystemMessage(Component.literal("Has alcanzado el límite de " +
                                    ConfigManager.getEntityLimit(entityId) + " para " + entityName));
                            EntityStorage.markLimitMessageShown(player.getUUID(), entityId);
                        }
                    }
                }
            }
        }
        // Limpiar el registro del último atacante
        LastAttackerStorage.clearLastAttacker(victim);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity target = event.getEntity();
        Entity source = event.getSource().getEntity();

        // Solo prevenir el daño entre invocaciones y su dueño
        if (source != null) {
            // Si el objetivo es un jugador y la fuente es una entidad invocada por ese jugador
            if (target instanceof Player player) {
                List<Entity> playerSummons = EntityStorage.getPlayerEntities(player.getUUID());
                if (playerSummons != null && playerSummons.contains(source)) {
                    event.setCanceled(true);
                }
            }
            // Si la fuente es un jugador y el objetivo es una de sus invocaciones
            else if (source instanceof Player player) {
                List<Entity> playerSummons = EntityStorage.getPlayerEntities(player.getUUID());
                if (playerSummons != null && playerSummons.contains(target)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}