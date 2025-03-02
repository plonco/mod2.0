package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.storage.EntityStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.SoloLevelingSystem.SoloLevelingSystem;
import java.util.List;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class CombatEventHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        // Verificar que el objetivo es una entidad viva
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        // Usar el método getLivingPlayerEntities para obtener solo las entidades vivas
        List<Entity> playerEntities = EntityStorage.getLivingPlayerEntities(player.getUUID());

        if (!playerEntities.isEmpty()) {
            SoloLevelingSystem.getLogger().debug("Ordenando a {} entidades que ataquen a {}",
                    playerEntities.size(), target.getName().getString());
        }

        // Hacer que todas las entidades del jugador ataquen al objetivo
        for (Entity summonedEntity : playerEntities) {
            if (summonedEntity instanceof Mob mob && summonedEntity != target) {
                // Asegurarse de que la entidad esté viva y no sea el objetivo
                if (mob.isAlive()) {
                    mob.setTarget(livingTarget);
                    // Logging para debug
                    SoloLevelingSystem.getLogger().debug("Entidad {} atacando a {}",
                            mob.getName().getString(), target.getName().getString());
                }
            }
        }
    }
}