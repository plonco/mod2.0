package com.SoloLevelingSystem.configs;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> NORMAL_ENEMIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINIBOSS_ENEMIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOSS_ENEMIES;

    // Límites por tipo
    private static final ForgeConfigSpec.IntValue NORMAL_LIMIT;
    private static final ForgeConfigSpec.IntValue MINIBOSS_LIMIT;
    private static final ForgeConfigSpec.IntValue BOSS_LIMIT;

    private static final Set<ResourceLocation> normalEnemies = new HashSet<>();
    private static final Set<ResourceLocation> minibossEnemies = new HashSet<>();
    private static final Set<ResourceLocation> bossEnemies = new HashSet<>();

    static {
        BUILDER.comment("Solo Leveling System Configuration");

        BUILDER.push("enemies");

        // Configuración de límites
        NORMAL_LIMIT = BUILDER
                .comment("Maximum number of normal enemies that can be stored of each type")
                .defineInRange("normal_limit", 5, 1, 100);

        MINIBOSS_LIMIT = BUILDER
                .comment("Maximum number of miniboss enemies that can be stored of each type")
                .defineInRange("miniboss_limit", 2, 1, 100);

        BOSS_LIMIT = BUILDER
                .comment("Maximum number of boss enemies that can be stored of each type")
                .defineInRange("boss_limit", 1, 1, 100);

        // Listas de enemigos
        NORMAL_ENEMIES = BUILDER
                .comment("List of normal enemies")
                .defineList("normal",
                        Arrays.asList(
                                "minecraft:zombie",
                                "minecraft:skeleton",
                                "minecraft:spider",
                                "minecraft:creeper"
                        ),
                        s -> s instanceof String
                );

        MINIBOSS_ENEMIES = BUILDER
                .comment("List of miniboss enemies")
                .defineList("miniboss",
                        Arrays.asList(
                                "minecraft:witch",
                                "minecraft:enderman",
                                "minecraft:ravager"
                        ),
                        s -> s instanceof String
                );

        BOSS_ENEMIES = BUILDER
                .comment("List of boss enemies")
                .defineList("boss",
                        Arrays.asList(
                                "minecraft:ender_dragon",
                                "minecraft:wither",
                                "minecraft:warden"
                        ),
                        s -> s instanceof String
                );

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            updateEnemyLists();
        }
    }

    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            updateEnemyLists();
        }
    }

    private static void updateEnemyLists() {
        normalEnemies.clear();
        minibossEnemies.clear();
        bossEnemies.clear();

        if (NORMAL_ENEMIES.get() != null) {
            NORMAL_ENEMIES.get().forEach(s -> normalEnemies.add(new ResourceLocation(s)));
        }
        if (MINIBOSS_ENEMIES.get() != null) {
            MINIBOSS_ENEMIES.get().forEach(s -> minibossEnemies.add(new ResourceLocation(s)));
        }
        if (BOSS_ENEMIES.get() != null) {
            BOSS_ENEMIES.get().forEach(s -> bossEnemies.add(new ResourceLocation(s)));
        }
    }

    public static boolean isNormalEnemy(ResourceLocation entity) {
        return normalEnemies.contains(entity);
    }

    public static boolean isMinibossEnemy(ResourceLocation entity) {
        return minibossEnemies.contains(entity);
    }

    public static boolean isBossEnemy(ResourceLocation entity) {
        return bossEnemies.contains(entity);
    }

    public static int getEntityLimit(ResourceLocation entityId) {
        if (isNormalEnemy(entityId)) {
            return NORMAL_LIMIT.get();
        } else if (isMinibossEnemy(entityId)) {
            return MINIBOSS_LIMIT.get();
        } else if (isBossEnemy(entityId)) {
            return BOSS_LIMIT.get();
        }
        return 0;
    }
}