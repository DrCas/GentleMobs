package io.github.drcas.gentlemobs.fabric;

import java.util.Optional;
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

        LivingEntity attackTarget = safeGet(brain, MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (attackTarget instanceof Player) {
            safeErase(brain, MemoryModuleType.ATTACK_TARGET);
            safeErase(brain, MemoryModuleType.WALK_TARGET);
            safeErase(brain, MemoryModuleType.LOOK_TARGET);
        }

        LivingEntity hurtByEntity = safeGet(brain, MemoryModuleType.HURT_BY_ENTITY).orElse(null);
        if (hurtByEntity instanceof Player) {
            safeErase(brain, MemoryModuleType.HURT_BY_ENTITY);
            safeErase(brain, MemoryModuleType.HURT_BY);
        }

        UUID angryAt = safeGet(brain, MemoryModuleType.ANGRY_AT).orElse(null);
        if (angryAt != null
                && entity.level() instanceof ServerLevel serverLevel
                && serverLevel.getPlayerByUUID(angryAt) != null) {
            safeErase(brain, MemoryModuleType.ANGRY_AT);
        }
    }

    private static <T> Optional<T> safeGet(Brain<?> brain, MemoryModuleType<T> memoryType) {
        try {
            return brain.getMemory(memoryType);
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    private static void safeErase(Brain<?> brain, MemoryModuleType<?> memoryType) {
        try {
            brain.eraseMemory(memoryType);
        } catch (IllegalStateException ignored) {
            // Not every mob brain registers every memory module.
        }
    }
}
