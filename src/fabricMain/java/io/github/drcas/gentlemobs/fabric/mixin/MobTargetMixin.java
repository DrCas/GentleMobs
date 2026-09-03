package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$preventPlayerTarget(LivingEntity target, CallbackInfo ci) {
        if (!(target instanceof Player)) {
            return;
        }

        if (GentleMobsFabric.getGlobalMode() == GentleMode.VANILLA) {
            return;
        }

        Mob mob = (Mob) (Object) this;
        mob.setAggressive(false);
        ci.cancel();
    }
}
