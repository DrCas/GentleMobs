package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import io.github.drcas.gentlemobs.fabric.mixin.access.FleeingMob;
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
    private void gentlemobs$startFleeAfterPlayerHit(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (GentleMobsFabric.getGlobalMode() != GentleMode.PASSIVE) {
            return;
        }

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

        if (mob instanceof FleeingMob fleeingMob) {
            fleeingMob.gentlemobs$startFlee(player);
        }
    }
}
