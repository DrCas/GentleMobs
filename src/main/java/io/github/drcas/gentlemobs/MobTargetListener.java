package io.github.drcas.gentlemobs;

import io.github.drcas.gentlemobs.internal.BrainTargetController;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public final class MobTargetListener implements Listener {

    private final GentleMobsConfig config;
    private final NeutralCombatTracker neutralCombatTracker;

    public MobTargetListener(
            GentleMobsConfig config,
            NeutralCombatTracker neutralCombatTracker
    ) {
        this.config = config;
        this.neutralCombatTracker = neutralCombatTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnemyTargetPlayer(
            EntityTargetLivingEntityEvent event
    ) {

        // Only affect hostile enemies.
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        // We need a Mob to check Neutral combat state.
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        // Only care about targeting players.
        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        GentleMode mode = config.getMode(
                event.getEntity().getType()
        );

        /*
         * VANILLA:
         * GentleMobs never interferes.
         */
        if (mode == GentleMode.VANILLA) {
            return;
        }

        /*
         * NEUTRAL + ENGAGED:
         *
         * The player already started this fight.
         * Vanilla Minecraft owns the mob's AI until
         * combat naturally ends.
         */
        if (mode == GentleMode.NEUTRAL &&
                neutralCombatTracker.isEngaged(mob)) {
            return;
        }

        /*
         * PASSIVE:
         * Block player targeting.
         *
         * NEUTRAL + NOT ENGAGED:
         * Block the mob from starting a fight.
         */
        event.setTarget(null);

        BrainTargetController.clearCombatTarget(mob);
    }
}