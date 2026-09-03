package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.BrainPlayerTargetController;
import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zoglin.class)
public abstract class ZoglinBehaviorMixin {

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void gentlemobs$clearPlayerBrainBeforeAi(ServerLevel level, CallbackInfo ci) {
        gentlemobs$clearPlayerCombatState();
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void gentlemobs$clearPlayerBrainAfterAi(ServerLevel level, CallbackInfo ci) {
        gentlemobs$clearPlayerCombatState();
    }

    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void gentlemobs$removePlayerFromIdleTargetSelection(
            ServerLevel level,
            Mob mob,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> cir
    ) {
        Optional<? extends LivingEntity> target = cir.getReturnValue();
        if (target != null && target.orElse(null) instanceof Player player
                && !GentleMobsFabric.canTargetPlayer(mob, player)) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$preventPlayerAttackMemory(
            LivingEntity target,
            CallbackInfo ci
    ) {
        Zoglin zoglin = (Zoglin) (Object) this;
        if (target instanceof Player player
                && !GentleMobsFabric.canTargetPlayer(zoglin, player)) {
            ci.cancel();
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$preventPlayerAttack(
            ServerLevel level,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Zoglin zoglin = (Zoglin) (Object) this;
        if (target instanceof Player player
                && !GentleMobsFabric.canTargetPlayer(zoglin, player)) {
            cir.setReturnValue(false);
        }
    }

    private void gentlemobs$clearPlayerCombatState() {
        Zoglin zoglin = (Zoglin) (Object) this;
        if (GentleMobsFabric.getGlobalMode() == GentleMode.VANILLA
                || GentleMobsFabric.isNeutralEngaged(zoglin)) {
            return;
        }

        BrainPlayerTargetController.clearPlayerCombatState(zoglin);
        zoglin.setTarget(null);
        zoglin.setAggressive(false);
    }
}
