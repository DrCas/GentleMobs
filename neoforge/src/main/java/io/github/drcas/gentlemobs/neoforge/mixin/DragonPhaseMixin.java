package io.github.drcas.gentlemobs.neoforge.mixin;

import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EnderDragonPhaseManager.class)
public abstract class DragonPhaseMixin {
    @Shadow @Final private EnderDragon dragon;
    @ModifyVariable(method = "setPhase", at = @At("HEAD"), argsOnly = true)
    private EnderDragonPhase<?> gentlemobs$phase(EnderDragonPhase<?> phase) {
        if (BehaviorEngine.fleeing(dragon) && phase != EnderDragonPhase.DYING) return EnderDragonPhase.HOLDING_PATTERN;
        return BehaviorEngine.attackPhase(phase) && BehaviorEngine.blocksPlayers(dragon) ? EnderDragonPhase.HOLDING_PATTERN : phase;
    }
}
