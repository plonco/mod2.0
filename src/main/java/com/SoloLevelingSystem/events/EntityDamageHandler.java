package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.damagesource.DamageSource;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EntityDamageHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingHurtEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getTags().contains("summoned")) {
            DamageSource source = event.getSource();
            String damageType = source.getMsgId();

            // Agregamos "sun_damage" y "inSunlight" para cubrir diferentes versiones de Minecraft
            if (damageType.equals("drown") ||
                    damageType.equals("cactus") ||
                    damageType.equals("dryout") ||
                    damageType.equals("sun_damage") ||
                    damageType.equals("inSunlight") ||
                    damageType.equals("indaylight")) {

                event.setCanceled(true);
                mob.clearFire();

                // Asegurarnos de que no esté "quemándose" por el sol
                if (mob.isOnFire()) {
                    mob.setRemainingFireTicks(0);
                }
            }
        }
    }

    // Agregar un evento adicional específicamente para el daño del sol
    @SubscribeEvent
    public static void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getTags().contains("summoned")) {
            // Prevenir que la entidad se queme por el sol
            if (mob.isOnFire()) {
                mob.clearFire();
                mob.setRemainingFireTicks(0);
            }
        }
    }
}