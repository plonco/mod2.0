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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
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
    private static final double MAX_DISTANCE = 10.0;

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity(); // event.getEntity() ya es un Player
        Entity target = event.getTarget();

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
                        }
                    }
                }
            }
        }

        // Limpiar el registro del último atacante
        LastAttackerStorage.clearLastAttacker(victim);
    }
    @SubscribeEvent
    public static void onEntityDrop(LivingDropsEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getTags().contains("psummoned")) {
            // Cancela todos los drops
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            Entity source = event.getSource().getEntity();

            if (source != null) {
                UUID playerUUID = player.getUUID();
                List<Entity> spawnedEntities = EntityStorage.getPlayerEntities(playerUUID);

                if (spawnedEntities != null && spawnedEntities.contains(source)) {
                    LOGGER.debug("Preventing damage from spawned entity: {}", source);
                    event.setCanceled(true);
                }
            }
        }
    }
}