package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoglinAi.class)
public abstract class HoglinAiMixin {

    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void gentlemobs$removePlayerFromIdleTargetSelection(
            ServerLevel level,
            Hoglin hoglin,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> cir
    ) {
        if (GentleMobsFabric.getGlobalMode() != GentleMode.PASSIVE) {
            return;
        }

        Optional<? extends LivingEntity> target = cir.getReturnValue();
        if (target != null && target.orElse(null) instanceof Player) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void gentlemobs$preventPlayerAttackMemory(
            Hoglin hoglin,
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (GentleMobsFabric.getGlobalMode() == GentleMode.PASSIVE
                && target instanceof Player) {
            ci.cancel();
        }
    }
}
