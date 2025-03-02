package com.SoloLevelingSystem.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class CustomFollowPlayerGoal extends Goal {
    private final Mob mob;
    private final Player owner;
    private final double speedModifier;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;  // Cambiado a float

    public CustomFollowPlayerGoal(Mob mob, Player owner, double speedModifier, float startDistance, float stopDistance) {
        this.mob = mob;
        this.owner = owner;
        this.speedModifier = speedModifier;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.owner == null || !this.owner.isAlive()) {
            return false;
        }
        return this.mob.distanceToSqr(this.owner) > (double)(this.startDistance * this.startDistance);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone() &&
                this.mob.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.oldWaterCost = this.mob.getPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.owner, 10.0F, (float)this.mob.getMaxHeadXRot());
        if (this.mob.distanceToSqr(this.owner) >= (double)(this.startDistance * this.startDistance)) {
            this.mob.getNavigation().moveTo(this.owner, this.speedModifier);
        }
    }
}