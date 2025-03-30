package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EntityVisualEffects {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityVisualEffects.class);
    private static final Random RANDOM = ThreadLocalRandom.current();

    // Configuración base de partículas
    private static final ParticleConfig BASE_CONFIG = new ParticleConfig(
            5,    // spawnRate
            3,    // particlesPerSpawn
            0.5F, // spread
            0.02F // speed
    );

    // Tipos de efectos visuales
    public enum EffectType {
        NORMAL,
        MINIBOSS,
        BOSS,
        SUMMON,
        DEATH
    }

    // Configuración de partículas por tipo
    private static final EnumMap<EffectType, ParticleEffect> EFFECT_CONFIGS = new EnumMap<>(EffectType.class);

    static {
        // Inicializar configuraciones
        EFFECT_CONFIGS.put(EffectType.NORMAL, new ParticleEffect(
                ParticleTypes.SOUL_FIRE_FLAME,
                BASE_CONFIG,
                new int[]{0x3498db},
                1.0F
        ));

        EFFECT_CONFIGS.put(EffectType.MINIBOSS, new ParticleEffect(
                ParticleTypes.DRAGON_BREATH,
                new ParticleConfig(4, 5, 0.6F, 0.03F),
                new int[]{0x9b59b6, 0x8e44ad},
                1.5F
        ));

        EFFECT_CONFIGS.put(EffectType.BOSS, new ParticleEffect(
                ParticleTypes.END_ROD,
                new ParticleConfig(3, 7, 0.7F, 0.04F),
                new int[]{0xe74c3c, 0xc0392b},
                2.0F
        ));
    }

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        Entity entity = event.getEntity();

        if (!entity.getTags().contains("psummoned") ||
                !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Determinar tipo de efecto basado en tags o NBT
        EffectType effectType = determineEffectType(entity);
        ParticleEffect effect = EFFECT_CONFIGS.get(effectType);

        if (effect != null && entity.tickCount % effect.config.spawnRate == 0) {
            spawnEntityParticles(entity, serverLevel, effect);
        }
    }

    public static void createSummonEffect(Entity entity, ServerLevel serverLevel) {
        if (!(entity instanceof LivingEntity)) return;

        EffectType effectType = determineEffectType(entity);
        ParticleEffect effect = EFFECT_CONFIGS.get(effectType);

        if (effect == null) return;

        Vec3 position = entity.position();
        double height = entity.getBbHeight();

        // Efecto de portal ascendente
        createPortalEffect(serverLevel, position, height, effect);

        // Efecto de explosión
        createExplosionEffect(serverLevel, position, height, effect);

        // Efecto de aura
        createAuraEffect(serverLevel, position, height, effect);
    }

    private static void createPortalEffect(ServerLevel level, Vec3 pos, double height, ParticleEffect effect) {
        double baseY = pos.y + height / 2;
        int particles = 30;

        for (int i = 0; i < particles; i++) {
            double angle = i * (Math.PI * 2) / particles;
            double radius = effect.scale * (1.0 + Math.sin(i * 0.2));

            for (int y = 0; y < 3; y++) {
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;

                level.sendParticles(
                        effect.particleType,
                        pos.x + offsetX,
                        baseY + (y * 0.5),
                        pos.z + offsetZ,
                        1,
                        0.0,
                        0.1 * effect.scale,
                        0.0,
                        0.02 * effect.scale
                );
            }
        }
    }

    private static void createExplosionEffect(ServerLevel level, Vec3 pos, double height, ParticleEffect effect) {
        double baseY = pos.y + height / 2;

        for (int i = 0; i < 50; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = RANDOM.nextDouble() * Math.PI;
            double radius = effect.scale * (0.5 + RANDOM.nextDouble() * 0.5);

            double offsetX = Math.sin(phi) * Math.cos(theta) * radius;
            double offsetY = Math.cos(phi) * radius;
            double offsetZ = Math.sin(phi) * Math.sin(theta) * radius;

            level.sendParticles(
                    effect.particleType,
                    pos.x,
                    baseY,
                    pos.z,
                    0,
                    offsetX * 0.1,
                    offsetY * 0.1,
                    offsetZ * 0.1,
                    0.1 * effect.scale
            );
        }
    }

    private static void createAuraEffect(ServerLevel level, Vec3 pos, double height, ParticleEffect effect) {
        double baseY = pos.y + height / 2;

        for (int i = 0; i < 20; i++) {
            double angle = i * (Math.PI * 2) / 20;
            double verticalOffset = Math.sin(angle * 2) * 0.2;
            double radius = effect.scale * 0.8;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            level.sendParticles(
                    effect.particleType,
                    pos.x + offsetX,
                    baseY + verticalOffset,
                    pos.z + offsetZ,
                    1,
                    0.0,
                    0.05 * effect.scale,
                    0.0,
                    0.01 * effect.scale
            );
        }
    }

    private static void spawnEntityParticles(Entity entity, ServerLevel serverLevel, ParticleEffect effect) {
        Vec3 position = entity.position();
        double height = entity.getBbHeight();

        for (int i = 0; i < effect.config.particlesPerSpawn; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * effect.config.spread;
            double offsetY = (RANDOM.nextDouble() - 0.5) * effect.config.spread;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * effect.config.spread;

            serverLevel.sendParticles(
                    effect.particleType,
                    position.x + offsetX,
                    position.y + (height / 2) + offsetY,
                    position.z + offsetZ,
                    1,
                    0.0,
                    effect.config.speed * effect.scale,
                    0.0,
                    0.01 * effect.scale
            );
        }
    }

    private static EffectType determineEffectType(Entity entity) {
        if (entity.getTags().contains("boss_summon")) {
            return EffectType.BOSS;
        } else if (entity.getTags().contains("miniboss_summon")) {
            return EffectType.MINIBOSS;
        }
        return EffectType.NORMAL;
    }

    private static class ParticleConfig {
        final int spawnRate;
        final int particlesPerSpawn;
        final float spread;
        final float speed;

        ParticleConfig(int spawnRate, int particlesPerSpawn, float spread, float speed) {
            this.spawnRate = spawnRate;
            this.particlesPerSpawn = particlesPerSpawn;
            this.spread = spread;
            this.speed = speed;
        }
    }

    private static class ParticleEffect {
        final ParticleOptions particleType;
        final ParticleConfig config;
        final int[] colors;
        final float scale;

        ParticleEffect(ParticleOptions particleType, ParticleConfig config, int[] colors, float scale) {
            this.particleType = particleType;
            this.config = config;
            this.colors = colors;
            this.scale = scale;
        }
    }
}