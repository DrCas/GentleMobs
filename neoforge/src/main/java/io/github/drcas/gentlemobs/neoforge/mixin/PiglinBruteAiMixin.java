package io.github.drcas.gentlemobs.neoforge.mixin;
import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PiglinBruteAi.class)
public abstract class PiglinBruteAiMixin {
    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void gentlemobs$filter(AbstractPiglin mob, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (BehaviorEngine.blocksTarget(mob, cir.getReturnValue().orElse(null))) cir.setReturnValue(Optional.empty());
    }
}
