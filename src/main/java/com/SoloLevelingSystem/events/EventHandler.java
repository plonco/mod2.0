package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import com.SoloLevelingSystem.storage.EntityStorage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, initializing Solo Leveling System...");
        EntityStorage.clearAllStorage(); // Limpiar almacenamiento al iniciar el servidor
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, saving Solo Leveling System data...");
        EntityStorage.saveAllData(); // Guardar datos antes de detener el servidor
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged in", player.getGameProfile().getName());
            // Cargar datos del jugador si es necesario
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged out", player.getGameProfile().getName());
            // Guardar datos del jugador si es necesario
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LivingEntity target = event.getEntity();

        // Si la entidad muerta es una invocada, simplemente la removemos del registro
        if (EntityStorage.isSummonedEntity(target.getUUID())) {
            EntityStorage.removeSummonedEntity(target.getUUID());
            LOGGER.debug("Summoned entity {} was removed from tracking", target.getUUID());
            return;
        }

        // Si el atacante es una entidad viva (incluyendo jugadores)
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            // No almacenar si el atacante es una entidad invocada
            if (EntityStorage.isSummonedEntity(attacker.getUUID())) {
                return;
            }

            // Guardar datos completos de la entidad
            CompoundTag entityData = new CompoundTag();
            target.save(entityData);

            // Agregar datos adicionales si es necesario
            entityData.putFloat("MaxHealth", target.getMaxHealth());
            entityData.putFloat("LastHealth", target.getHealth());

            // Almacenar la entidad con el UUID del atacante
            EntityStorage.storeEntity(attacker.getUUID(), target, entityData);
            LOGGER.debug("Entity {} stored for attacker {}", target.getType().toString(), attacker.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // Si la entidad dañada es una invocada
        if (EntityStorage.isSummonedEntity(event.getEntity().getUUID())) {
            // Puedes modificar el daño o agregar efectos especiales aquí
            float newDamage = event.getAmount() * 0.8f; // Ejemplo: 20% menos de daño
            event.setAmount(newDamage);
        }

        // Si el atacante es una entidad invocada
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (EntityStorage.isSummonedEntity(attacker.getUUID())) {
                // Puedes modificar el daño que hacen las entidades invocadas
                float newDamage = event.getAmount() * 1.2f; // Ejemplo: 20% más de daño
                event.setAmount(newDamage);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player original = event.getOriginal();
        Player player = event.getEntity();

        // Transferir datos relevantes del jugador original al nuevo
        if (original instanceof ServerPlayer && player instanceof ServerPlayer) {
            // Aquí puedes transferir datos persistentes entre las instancias del jugador
            LOGGER.info("Transferring player data after death for {}", player.getGameProfile().getName());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Manejar el cambio de dimensión si es necesario
            LOGGER.info("Player {} changed dimension from {} to {}",
                    player.getGameProfile().getName(),
                    event.getFrom(),
                    event.getTo());
        }
    }
}