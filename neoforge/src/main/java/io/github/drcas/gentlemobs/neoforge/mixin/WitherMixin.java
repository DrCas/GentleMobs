package io.github.drcas.gentlemobs.neoforge.mixin;

import io.github.drcas.gentlemobs.neoforge.BehaviorEngine;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(WitherBoss.class)
public abstract class WitherMixin {
    @ModifyVariable(method = "setAlternativeTarget", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int gentlemobs$headTarget(int targetId) {
        WitherBoss wither = (WitherBoss)(Object)this;
        return BehaviorEngine.blocksTarget(wither, wither.level().getEntity(targetId)) ? 0 : targetId;
    }
}
