package io.github.drcas.gentlemobs.internal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;

public final class BrainTargetController {

    private BrainTargetController() {
        // Utility class
    }

    public static void clearCombatTarget(
            org.bukkit.entity.LivingEntity bukkitEntity
    ) {

        if (!(bukkitEntity instanceof CraftLivingEntity craftEntity)) {
            return;
        }

        LivingEntity nmsEntity = craftEntity.getHandle();

        var brain = nmsEntity.getBrain();

        // Current combat target.
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);

        // Anger / hostility target.
        brain.eraseMemory(MemoryModuleType.ANGRY_AT);

        // Recent damage information.
        brain.eraseMemory(MemoryModuleType.HURT_BY);
        brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
    }
}