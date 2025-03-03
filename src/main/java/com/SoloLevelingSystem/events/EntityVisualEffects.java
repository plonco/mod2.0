package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EntityVisualEffects {
    private static final Random RANDOM = new Random();

    // Configuración de partículas
    private static final int PARTICLE_SPAWN_RATE = 5; // Cada cuántos ticks aparecen partículas
    private static final int PARTICLES_PER_SPAWN = 3; // Cantidad de partículas por spawn
    private static final float PARTICLE_SPREAD = 0.5F; // Radio de dispersión de las partículas
    private static final float PARTICLE_SPEED = 0.02F; // Velocidad de las partículas

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        Entity entity = event.getEntity();

        // Verificar si es una entidad invocada
        if (entity.getTags().contains("summoned") &&
                entity.level() instanceof ServerLevel serverLevel) {

            // Crear partículas cada PARTICLE_SPAWN_RATE ticks
            if (entity.tickCount % PARTICLE_SPAWN_RATE == 0) {
                spawnEntityParticles(entity, serverLevel);
            }
        }
    }

    private static void spawnEntityParticles(Entity entity, ServerLevel serverLevel) {
        double baseX = entity.getX();
        double baseY = entity.getY() + entity.getBbHeight() / 2;
        double baseZ = entity.getZ();

        // Generar múltiples partículas
        for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
            // Calcular offset aleatorio
            double offsetX = (RANDOM.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double offsetY = (RANDOM.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * PARTICLE_SPREAD;

            // Partículas de fuego de alma (azules)
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    baseX + offsetX,
                    baseY + offsetY,
                    baseZ + offsetZ,
                    1, // cantidad
                    0.0, // velocidadX
                    PARTICLE_SPEED, // velocidadY
                    0.0, // velocidadZ
                    0.01 // velocidad general
            );

            // Partículas de delfín (celestes)
            serverLevel.sendParticles(
                    ParticleTypes.DOLPHIN,
                    baseX + offsetX,
                    baseY + offsetY,
                    baseZ + offsetZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    PARTICLE_SPEED
            );
        }
    }

    // Método para crear un efecto de invocación intenso
    public static void createSummonEffect(Entity entity, ServerLevel serverLevel) {
        double baseX = entity.getX();
        double baseY = entity.getY() + entity.getBbHeight() / 2;
        double baseZ = entity.getZ();

        // Efecto más intenso para la invocación
        for (int i = 0; i < 20; i++) {
            double angle = i * (Math.PI * 2) / 20;
            double radius = 1.0;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Espiral ascendente de partículas
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    baseX + offsetX,
                    baseY + (i * 0.1),
                    baseZ + offsetZ,
                    2,
                    0.0,
                    0.1,
                    0.0,
                    0.02
            );

            // Partículas adicionales para efecto
            serverLevel.sendParticles(
                    ParticleTypes.DOLPHIN,
                    baseX + offsetX,
                    baseY + (i * 0.1),
                    baseZ + offsetZ,
                    1,
                    0.0,
                    0.05,
                    0.0,
                    0.01
            );
        }
    }
}