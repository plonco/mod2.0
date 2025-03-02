package com.SoloLevelingSystem.configs;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.*;

public class ConfigManager {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> NORMAL_ENEMIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINIBOSS_ENEMIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOSS_ENEMIES;

    private static final Set<ResourceLocation> normalEnemies = new HashSet<>();
    private static final Set<ResourceLocation> minibossEnemies = new HashSet<>();
    private static final Set<ResourceLocation> bossEnemies = new HashSet<>();

    static {
        BUILDER.comment("Solo Leveling System Configuration");

        BUILDER.push("enemies");

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
}