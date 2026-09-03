package io.github.drcas.gentlemobs.fabric;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

public final class BrainPlayerTargetController {

    private BrainPlayerTargetController() {
    }

    public static void clearPlayerCombatState(LivingEntity entity) {
        Brain<?> brain = entity.getBrain();

        LivingEntity attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (attackTarget instanceof Player) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        }

        LivingEntity hurtByEntity = brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null);
        if (hurtByEntity instanceof Player) {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            brain.eraseMemory(MemoryModuleType.HURT_BY);
        }

        UUID angryAt = brain.getMemory(MemoryModuleType.ANGRY_AT).orElse(null);
        if (angryAt != null
                && entity.level() instanceof ServerLevel serverLevel
                && serverLevel.getPlayerByUUID(angryAt) != null) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }
}
