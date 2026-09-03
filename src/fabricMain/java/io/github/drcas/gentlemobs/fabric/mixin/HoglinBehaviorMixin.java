package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.BrainPlayerTargetController;
import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Hoglin.class)
public abstract class HoglinBehaviorMixin {

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void gentlemobs$clearPlayerBrainBeforeAi(ServerLevel level, CallbackInfo ci) {
        gentlemobs$clearPlayerCombatState();
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void gentlemobs$clearPlayerBrainAfterAi(ServerLevel level, CallbackInfo ci) {
        gentlemobs$clearPlayerCombatState();
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$preventPlayerAttack(
            ServerLevel level,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Hoglin hoglin = (Hoglin) (Object) this;
        if (target instanceof Player player
                && !GentleMobsFabric.canTargetPlayer(hoglin, player)) {
            cir.setReturnValue(false);
        }
    }

    private void gentlemobs$clearPlayerCombatState() {
        Hoglin hoglin = (Hoglin) (Object) this;
        if (GentleMobsFabric.getGlobalMode() == io.github.drcas.gentlemobs.fabric.GentleMode.VANILLA
                || GentleMobsFabric.isNeutralEngaged(hoglin)) {
            return;
        }

        BrainPlayerTargetController.clearPlayerCombatState(hoglin);
        hoglin.setTarget(null);
        hoglin.setAggressive(false);
    }
}
