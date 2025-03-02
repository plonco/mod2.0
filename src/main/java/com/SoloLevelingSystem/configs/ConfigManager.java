package com.SoloLevelingSystem.configs;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ConfigManager {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_STORED_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PARTICLE_EFFECTS;
    public static final ForgeConfigSpec.IntValue SUMMON_COOLDOWN;

    static {
        BUILDER.push("Solo Leveling System Configuration");

        MAX_STORED_ENTITIES = BUILDER
                .comment("Maximum number of entities that can be stored per player")
                .defineInRange("maxStoredEntities", 50, 1, 1000);

        ENABLE_PARTICLE_EFFECTS = BUILDER
                .comment("Enable particle effects when spawning stored entities")
                .define("enableParticleEffects", true);

        SUMMON_COOLDOWN = BUILDER
                .comment("Cooldown in seconds between summons")
                .defineInRange("summonCooldown", 30, 0, 3600);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "solo_leveling_system.toml");
    }

    public static void loadConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("solo_leveling_system.toml");
        if (java.nio.file.Files.exists(configPath)) {
            // La configuración se carga automáticamente por Forge
            System.out.println("Solo Leveling System config loaded from " + configPath);
        }
    }

    public static int getMaxStoredEntities() {
        return MAX_STORED_ENTITIES.get();
    }

    public static boolean areParticleEffectsEnabled() {
        return ENABLE_PARTICLE_EFFECTS.get();
    }

    public static int getSummonCooldown() {
        return SUMMON_COOLDOWN.get();
    }
}