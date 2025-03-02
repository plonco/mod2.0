package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.storage.EntityStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.SoloLevelingSystem.SoloLevelingSystem;
import java.util.List;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class CombatEventHandler {

    @SubscribeEvent
    public void onPlayerAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        // Log inicial para debug
        SoloLevelingSystem.getLogger().info("AttackEntityEvent disparado: Jugador {} atacando a {}",
                player.getName().getString(), target.getName().getString());

        // Verificar que el objetivo es una entidad viva
        if (!(target instanceof LivingEntity livingTarget)) {
            SoloLevelingSystem.getLogger().info("El objetivo no es una entidad viva");
            return;
        }

        // Usar el método getLivingPlayerEntities para obtener solo las entidades vivas
        List<Entity> playerEntities = EntityStorage.getLivingPlayerEntities(player.getUUID());

        if (playerEntities.isEmpty()) {
            SoloLevelingSystem.getLogger().info("No se encontraron entidades para el jugador");
            return;
        }

        SoloLevelingSystem.getLogger().info("Encontradas {} entidades del jugador", playerEntities.size());

        // Hacer que todas las entidades del jugador ataquen al objetivo
        for (Entity summonedEntity : playerEntities) {
            if (summonedEntity instanceof Mob mob && summonedEntity != target) {
                // Asegurarse de que la entidad esté viva y no sea el objetivo
                if (mob.isAlive()) {
                    // Log antes de establecer el objetivo
                    SoloLevelingSystem.getLogger().info("Configurando {} para atacar a {}",
                            mob.getName().getString(), livingTarget.getName().getString());

                    mob.setTarget(livingTarget);
                    mob.setAggressive(true);  // Hacer que la entidad sea agresiva

                    // Log después de establecer el objetivo
                    SoloLevelingSystem.getLogger().info("Objetivo establecido para {}: {}",
                            mob.getName().getString(), mob.getTarget() != null ? "éxito" : "fallido");
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Log inicial para debug
        SoloLevelingSystem.getLogger().info("LivingHurtEvent disparado: Jugador {} recibió daño",
                player.getName().getString());

        // Obtener el atacante
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            SoloLevelingSystem.getLogger().info("El atacante no es una entidad viva");
            return;
        }

        // Obtener las entidades del jugador
        List<Entity> playerEntities = EntityStorage.getLivingPlayerEntities(player.getUUID());

        if (playerEntities.isEmpty()) {
            SoloLevelingSystem.getLogger().info("No se encontraron entidades para defender al jugador");
            return;
        }

        SoloLevelingSystem.getLogger().info("Ordenando a {} entidades que defiendan contra {}",
                playerEntities.size(), attacker.getName().getString());

        // Hacer que todas las entidades del jugador ataquen al agresor
        for (Entity summonedEntity : playerEntities) {
            if (summonedEntity instanceof Mob mob && summonedEntity != attacker) {
                if (mob.isAlive()) {
                    mob.setTarget((LivingEntity) attacker);
                    mob.setAggressive(true);  // Hacer que la entidad sea agresiva

                    SoloLevelingSystem.getLogger().info("Entidad {} defendiendo contra {}",
                            mob.getName().getString(), attacker.getName().getString());
                }
            }
        }
    }
}