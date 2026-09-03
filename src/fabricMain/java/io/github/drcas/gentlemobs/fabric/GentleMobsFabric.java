package io.github.drcas.gentlemobs.fabric;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GentleMobsFabric implements ModInitializer {
    public static final String MOD_ID = "gentlemobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GentleMode globalMode = GentleMode.PASSIVE;
    private static final Map<UUID, UUID> NEUTRAL_ENGAGEMENTS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        GentleMobsCommands.register();
        LOGGER.info("GentleMobs Fabric initialized in {} mode.", globalMode);
    }

    public static GentleMode getGlobalMode() {
        return globalMode;
    }

    public static void setGlobalMode(GentleMode mode) {
        globalMode = mode;
        if (mode != GentleMode.NEUTRAL) {
            NEUTRAL_ENGAGEMENTS.clear();
        }
    }

    public static void engageNeutral(Mob mob, Player player) {
        if (globalMode == GentleMode.NEUTRAL) {
            NEUTRAL_ENGAGEMENTS.put(mob.getUUID(), player.getUUID());
        }
    }

    public static void disengageNeutral(Mob mob) {
        NEUTRAL_ENGAGEMENTS.remove(mob.getUUID());
    }

    public static boolean isNeutralEngaged(Mob mob) {
        return NEUTRAL_ENGAGEMENTS.containsKey(mob.getUUID());
    }

    public static boolean isNeutralEngagedWith(Mob mob, Player player) {
        UUID playerId = NEUTRAL_ENGAGEMENTS.get(mob.getUUID());
        return playerId != null && playerId.equals(player.getUUID());
    }

    public static boolean canTargetPlayer(Mob mob, Player player) {
        return switch (globalMode) {
            case VANILLA -> true;
            case PASSIVE -> false;
            case NEUTRAL -> isNeutralEngagedWith(mob, player);
        };
    }
}
