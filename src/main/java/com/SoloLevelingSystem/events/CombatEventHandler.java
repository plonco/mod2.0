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

        // Verificar que el objetivo es una entidad viva
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        // Usar el método getLivingPlayerEntities para obtener solo las entidades vivas
        List<Entity> playerEntities = EntityStorage.getLivingPlayerEntities(player.getUUID());

        // Hacer que todas las entidades del jugador ataquen al objetivo
        for (Entity summonedEntity : playerEntities) {
            if (summonedEntity instanceof Mob mob) {
                // Asegurarse de que la entidad esté viva
                if (mob.isAlive()) {

                    // *** NUEVA CONDICIÓN: No atacar a otras entidades invocadas ***
                    if (target.getTags().contains("summoned")) {
                        continue; // Saltar a la siguiente entidad
                    }

                    mob.setTarget(livingTarget);
                    mob.setAggressive(true);  // Hacer que la entidad sea agresiva
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Obtener el atacante
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return;
        }

        // Obtener las entidades del jugador
        List<Entity> playerEntities = EntityStorage.getLivingPlayerEntities(player.getUUID());

        // Hacer que todas las entidades del jugador ataquen al agresor
        for (Entity summonedEntity : playerEntities) {
            if (summonedEntity instanceof Mob mob) {
                if (mob.isAlive()) {
                    // *** NUEVA CONDICIÓN: No atacar a otras entidades invocadas ***
                    if (attacker.getTags().contains("summoned")) {
                        continue; // Saltar a la siguiente entidad
                    }

                    mob.setTarget((LivingEntity) attacker);
                    mob.setAggressive(true);  // Hacer que la entidad sea agresiva
                }
            }
        }
    }
}