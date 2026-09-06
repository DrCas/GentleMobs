package io.github.drcas.gentlemobs.neoforge.mixin;

import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingTargetMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$canAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Mob mob && BehaviorEngine.blocksTarget(mob, target)) cir.setReturnValue(false);
    }
}
