package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.FleeingMob;
import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void gentlemobs$handlePlayerHit(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        if (!(source.getEntity() instanceof Player player)) {
            return;
        }

        Object self = this;
        if (!(self instanceof Mob mob)) {
            return;
        }

        GentleMode mode = GentleMobsFabric.getGlobalMode();

        if (mode == GentleMode.PASSIVE) {
            if (mob instanceof FleeingMob fleeingMob) {
                fleeingMob.gentlemobs$startFlee(player);
            }
            return;
        }

        if (mode == GentleMode.NEUTRAL) {
            GentleMobsFabric.engageNeutral(mob, player);
            mob.setTarget(player);
        }
    }
}
