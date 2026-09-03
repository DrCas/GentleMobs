package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.FleeingMob;
import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobTargetMixin implements FleeingMob {

    @Unique
    private static final double GENTLEMOBS_FLEE_DISTANCE = 12.0D;

    @Unique
    private static final double GENTLEMOBS_FLEE_SPEED = 1.3D;

    @Unique
    private static final int GENTLEMOBS_FLEE_DURATION_TICKS = 60;

    @Unique
    private Player gentlemobs$fleeFrom;

    @Unique
    private int gentlemobs$fleeTicksRemaining;

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$preventPlayerTarget(LivingEntity target, CallbackInfo ci) {
        if (!(target instanceof Player player)) {
            return;
        }

        Mob mob = (Mob) (Object) this;
        if (GentleMobsFabric.canTargetPlayer(mob, player)) {
            return;
        }

        mob.setAggressive(false);
        ci.cancel();
    }

    @Override
    public void gentlemobs$startFlee(Player player) {
        if (GentleMobsFabric.getGlobalMode() != GentleMode.PASSIVE) {
            return;
        }

        Mob mob = (Mob) (Object) this;
        mob.setTarget(null);
        mob.setAggressive(false);

        gentlemobs$fleeFrom = player;
        gentlemobs$fleeTicksRemaining = GENTLEMOBS_FLEE_DURATION_TICKS;
        gentlemobs$updateFleePath(mob, player);
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void gentlemobs$afterServerAiStep(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (GentleMobsFabric.getGlobalMode() == GentleMode.NEUTRAL
                && GentleMobsFabric.isNeutralEngaged(mob)) {
            LivingEntity target = mob.getTarget();
            if (!(target instanceof Player player)
                    || !player.isAlive()
                    || mob.level() != player.level()
                    || !GentleMobsFabric.isNeutralEngagedWith(mob, player)) {
                GentleMobsFabric.disengageNeutral(mob);
            }
        }

        if (GentleMobsFabric.getGlobalMode() != GentleMode.PASSIVE) {
            gentlemobs$stopFlee();
            return;
        }

        if (gentlemobs$fleeTicksRemaining <= 0 || gentlemobs$fleeFrom == null) {
            return;
        }

        Player player = gentlemobs$fleeFrom;

        if (!mob.isAlive()
                || !player.isAlive()
                || mob.level() != player.level()) {
            gentlemobs$stopFlee();
            return;
        }

        mob.setTarget(null);
        mob.setAggressive(false);

        if (gentlemobs$fleeTicksRemaining % 5 == 0) {
            gentlemobs$updateFleePath(mob, player);
        }

        gentlemobs$fleeTicksRemaining--;

        if (gentlemobs$fleeTicksRemaining <= 0) {
            gentlemobs$stopFlee();
        }
    }

    @Unique
    private void gentlemobs$updateFleePath(Mob mob, Player player) {
        double dx = mob.getX() - player.getX();
        double dz = mob.getZ() - player.getZ();
        double horizontalLength = Math.sqrt(dx * dx + dz * dz);

        if (horizontalLength < 0.001D) {
            double angle = Math.toRadians(mob.getYRot() + 180.0F);
            dx = -Math.sin(angle);
            dz = Math.cos(angle);
            horizontalLength = 1.0D;
        }

        double scale = GENTLEMOBS_FLEE_DISTANCE / horizontalLength;
        double fleeX = mob.getX() + dx * scale;
        double fleeZ = mob.getZ() + dz * scale;

        mob.getNavigation().moveTo(
                fleeX,
                mob.getY(),
                fleeZ,
                GENTLEMOBS_FLEE_SPEED
        );
    }

    @Unique
    private void gentlemobs$stopFlee() {
        gentlemobs$fleeFrom = null;
        gentlemobs$fleeTicksRemaining = 0;
    }
}
