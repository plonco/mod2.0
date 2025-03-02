package com.SoloLevelingSystem.configs;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moandjiezana.toml.Toml;

import java.io.InputStream;
import java.util.*;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Set<ResourceLocation> normalEnemies = new HashSet<>();
    private static final Set<ResourceLocation> minibossEnemies = new HashSet<>();
    private static final Set<ResourceLocation> bossEnemies = new HashSet<>();

    private static class ModConfig {
        Enemies enemies = new Enemies();

        static class Enemies {
            List<String> normal = new ArrayList<>();
            List<String> miniboss = new ArrayList<>();
            List<String> boss = new ArrayList<>();
        }
    }

    public static void loadConfig() {
        LOGGER.debug("Cargando configuración...");
        normalEnemies.clear();
        minibossEnemies.clear();
        bossEnemies.clear();

        try (InputStream configStream = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config/solo_leveling_system/config.toml")) {

            if (configStream == null) {
                LOGGER.error("Archivo de configuración no encontrado!");
                return;
            }

            Toml toml = new Toml().read(configStream);
            ModConfig config = toml.to(ModConfig.class);

            processList(config.enemies.normal, normalEnemies);
            processList(config.enemies.miniboss, minibossEnemies);
            processList(config.enemies.boss, bossEnemies);

            LOGGER.info("=== Configuración Cargada ===");
            LOGGER.info("Normales: {}", normalEnemies);
            LOGGER.info("Minibosses: {}", minibossEnemies);
            LOGGER.info("Bosses: {}", minibossEnemies);

        } catch (Exception e) {
            LOGGER.error("Error cargando configuración: {}", e.getMessage());
        }
    }

    private static void processList(List<String> entries, Set<ResourceLocation> targetSet) {
        for (String entry : entries) {
            try {
                ResourceLocation location = ResourceLocation.tryParse(entry.trim());
                if (location == null) {
                    LOGGER.warn("Entrada inválida: '{}'", entry);
                    continue;
                }
                targetSet.add(location);
                LOGGER.debug("Registrado: {}", location);
            } catch (Exception e) {
                LOGGER.warn("Error procesando entrada: '{}'", entry);
            }
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