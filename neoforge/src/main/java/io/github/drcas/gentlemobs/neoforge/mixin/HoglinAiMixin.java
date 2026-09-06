package io.github.drcas.gentlemobs.neoforge.mixin;
import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HoglinAi.class)
public abstract class HoglinAiMixin {
    @Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void gentlemobs$filter(Hoglin mob, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (BehaviorEngine.blocksTarget(mob, cir.getReturnValue().orElse(null))) cir.setReturnValue(Optional.empty());
    }
}
