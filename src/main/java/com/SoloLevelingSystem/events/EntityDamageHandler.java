package com.SoloLevelingSystem.events;

import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class EntityDamageHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Verificar si la entidad que recibe el daño y la entidad que causa el daño
        // (o la entidad que disparó el proyectil) son entidades invocadas.
        boolean targetIsSummoned = entity instanceof Mob && entity.getTags().contains("summoned");
        boolean sourceIsSummoned = false;

        if (attacker instanceof Mob && attacker.getTags().contains("summoned")) {
            sourceIsSummoned = true;
        } else if (source.getDirectEntity() instanceof Projectile) {
            // Si el daño proviene de un proyectil, verificar si la entidad que disparó el proyectil es una entidad invocada
            Entity owner = ((Projectile) source.getDirectEntity()).getOwner();
            if (owner instanceof Mob && owner.getTags().contains("summoned")) {
                sourceIsSummoned = true;
            }
        }

        // Si ambas entidades son invocadas, cancelar el evento de daño.
        if (targetIsSummoned && sourceIsSummoned) {
            // Intentar cancelar el evento
            event.setCanceled(true);

            // Si la cancelación no funciona, no hacer nada más.  Evitar la manipulación de la salud.
            return;
        }

        if (entity instanceof Mob mob && mob.getTags().contains("summoned")) {
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