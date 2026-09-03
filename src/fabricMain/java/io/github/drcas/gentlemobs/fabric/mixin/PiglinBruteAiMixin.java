package io.github.drcas.gentlemobs.fabric.mixin;

import io.github.drcas.gentlemobs.fabric.GentleMobsFabric;
import io.github.drcas.gentlemobs.fabric.GentleMode;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinBruteAi.class)
public abstract class PiglinBruteAiMixin {

    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void gentlemobs$filterPlayerAttackTarget(
            ServerLevel level,
            AbstractPiglin body,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> cir
    ) {
        if (GentleMobsFabric.getGlobalMode() != GentleMode.PASSIVE) {
            return;
        }

        Optional<? extends LivingEntity> target = cir.getReturnValue();
        if (target != null && target.isPresent() && target.get() instanceof Player) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
