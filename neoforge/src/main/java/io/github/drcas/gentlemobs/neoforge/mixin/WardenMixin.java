package io.github.drcas.gentlemobs.neoforge.mixin;

import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public abstract class WardenMixin {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$angerTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (BehaviorEngine.blocksTarget((Warden)(Object)this, target)) cir.setReturnValue(false);
    }
}
