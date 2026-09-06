package io.github.drcas.gentlemobs.neoforge;

import io.github.drcas.gentlemobs.GentleMode;
import java.util.Map;
import java.util.Set;

/** Registry IDs, never Java inheritance, define the default compatibility boundary. */
public final class BehaviorPolicy {
    private BehaviorPolicy() {}
    public static final Set<String> VANILLA_HOSTILES = Set.of(
        "minecraft:blaze", "minecraft:bogged", "minecraft:breeze", "minecraft:cave_spider",
        "minecraft:creeper", "minecraft:drowned", "minecraft:elder_guardian", "minecraft:ender_dragon",
        "minecraft:enderman", "minecraft:endermite", "minecraft:evoker", "minecraft:ghast",
        "minecraft:giant", "minecraft:guardian", "minecraft:hoglin", "minecraft:husk",
        "minecraft:illusioner", "minecraft:magma_cube", "minecraft:phantom", "minecraft:piglin",
        "minecraft:piglin_brute", "minecraft:pillager", "minecraft:ravager", "minecraft:shulker",
        "minecraft:silverfish", "minecraft:skeleton", "minecraft:slime", "minecraft:spider",
        "minecraft:stray", "minecraft:vex", "minecraft:vindicator", "minecraft:warden",
        "minecraft:witch", "minecraft:wither", "minecraft:wither_skeleton", "minecraft:zoglin",
        "minecraft:zombie", "minecraft:zombie_villager", "minecraft:zombified_piglin");

    public static GentleMode mode(String id, GentleMode global, Map<String, GentleMode> overrides) {
        if (overrides.containsKey(id)) return overrides.get(id);
        return VANILLA_HOSTILES.contains(id) ? global : GentleMode.VANILLA;
    }

    public static boolean blocksPlayers(GentleMode mode, boolean engaged) {
        return mode == GentleMode.PASSIVE || mode == GentleMode.NEUTRAL && !engaged;
    }
}
