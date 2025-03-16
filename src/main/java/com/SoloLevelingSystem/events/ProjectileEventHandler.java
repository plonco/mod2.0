package com.SoloLevelingSystem.events;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.SoloLevelingSystem.SoloLevelingSystem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

@Mod.EventBusSubscriber(modid = SoloLevelingSystem.MODID)
public class ProjectileEventHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
            if (event.getEntity() instanceof Projectile projectile) {
                Entity target = entityHitResult.getEntity();

                // Check if the target is a summoned entity
                if (target instanceof Mob && target.getTags().contains("summoned")) {
                    // Check if the projectile was fired by a summoned entity
                    if (projectile.getOwner() instanceof Mob && projectile.getOwner().getTags().contains("summoned")) {
                        // Get the original target (if available)
                        LivingEntity originalTarget = null;
                        if (projectile.getPersistentData().contains("originalTargetId")) {
                            Level level = null;
                            try {
                                // Attempt to use getWorld() first
                                java.lang.reflect.Method getWorldMethod = projectile.getClass().getMethod("getWorld");
                                level = (Level) getWorldMethod.invoke(projectile);
                            } catch (NoSuchMethodException e) {
                                // If getWorld() doesn't exist, try getLevel()
                                try {
                                    java.lang.reflect.Method getLevelMethod = projectile.getClass().getMethod("getLevel");
                                    level = (Level) getLevelMethod.invoke(projectile);
                                } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ex) {
                                    SoloLevelingSystem.getLogger().error("Could not get Level/World from projectile", ex);
                                    return; // If we can't get the level, exit the event
                                }
                            } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                                SoloLevelingSystem.getLogger().error("Could not get Level/World from projectile", e);
                                return; // If we can't get the level, exit the event
                            }

                            if (level != null) {
                                Entity originalTargetEntity = level.getEntity(projectile.getPersistentData().getInt("originalTargetId"));
                                if (originalTargetEntity instanceof LivingEntity) {
                                    originalTarget = (LivingEntity) originalTargetEntity;
                                }
                            }
                        }

                        if (originalTarget != null) {
                            // Calculate the redirection vector
                            Vec3 projectilePos = projectile.position();
                            Vec3 targetPos = originalTarget.position().add(0, originalTarget.getEyeHeight() / 2, 0); // Aim for the head
                            Vec3 direction = targetPos.subtract(projectilePos).normalize();

                            // Add a small random offset
                            double randomOffset = 0.1;
                            direction = direction.add(
                                    (RANDOM.nextDouble() - 0.5) * randomOffset,
                                    (RANDOM.nextDouble() - 0.5) * randomOffset,
                                    (RANDOM.nextDouble() - 0.5) * randomOffset
                            ).normalize();

                            // Set the new velocity
                            double speed = 1.0; // Adjust as needed
                            projectile.setDeltaMovement(direction.scale(speed));

                            // Mark the projectile as changed
                            projectile.hasImpulse = true;

                            SoloLevelingSystem.getLogger().debug("Redirected projectile to original target.");
                            event.setCanceled(true); // Cancel the original impact
                        } else {
                            // If we don't have an original target, cancel the event
                            event.setCanceled(true);
                            SoloLevelingSystem.getLogger().debug("Cancelled projectile impact on summoned entity (no original target).");
                        }
                    }
                }
            }
        }
    }
}