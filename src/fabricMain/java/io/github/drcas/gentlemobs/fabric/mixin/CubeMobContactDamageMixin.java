package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCubeMob.class)
public abstract class CubeMobContactDamageMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$controlPlayerContactDamage(Player player, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        GentleMode mode = GentleMobsFabric.getGlobalMode();

        if (mode == GentleMode.PASSIVE) {
            ci.cancel();
            return;
        }

        if (mode == GentleMode.NEUTRAL
                && !GentleMobsFabric.isNeutralEngagedWith(mob, player)) {
            ci.cancel();
        }
    }
}
