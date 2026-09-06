package io.github.drcas.gentlemobs.neoforge.mixin;

import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonHoldingPatternPhase.class)
public abstract class DragonFlightMixin extends AbstractDragonPhaseInstance {
    protected DragonFlightMixin(EnderDragon dragon) { super(dragon); }
    @Inject(method = "getFlyTargetLocation", at = @At("HEAD"), cancellable = true)
    private void gentlemobs$flee(CallbackInfoReturnable<Vec3> cir) {
        if (BehaviorEngine.fleeing(dragon)) cir.setReturnValue(BehaviorEngine.fleeDestination(dragon));
    }
}
