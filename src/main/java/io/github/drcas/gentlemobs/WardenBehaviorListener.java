package io.github.drcas.gentlemobs;

import io.papermc.paper.event.entity.WardenAngerChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class WardenBehaviorListener implements Listener {

    private final GentleMobsConfig config;
    private final NeutralCombatTracker neutralCombatTracker;

    public WardenBehaviorListener(
            GentleMobsConfig config,
            NeutralCombatTracker neutralCombatTracker
    ) {
        this.config = config;
        this.neutralCombatTracker = neutralCombatTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onWardenAngerChange(
            WardenAngerChangeEvent event
    ) {

        Warden warden = event.getEntity();

        /*
         * Only interfere with anger caused by a player.
         */
        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        GentleMode mode = config.getMode(
                warden.getType()
        );

        /*
         * VANILLA:
         * Warden behaves completely normally.
         */
        if (mode == GentleMode.VANILLA) {
            return;
        }

        /*
         * NEUTRAL + ENGAGED:
         * Player started the fight, so vanilla Warden
         * anger mechanics are allowed to operate normally.
         */
        if (mode == GentleMode.NEUTRAL &&
                neutralCombatTracker.isEngaged(warden)) {
            return;
        }

        /*
         * PASSIVE:
         * Do not allow player anger to increase.
         *
         * NEUTRAL + NOT ENGAGED:
         * Do not allow the Warden to start the fight.
         */
        if (event.getNewAnger() > event.getOldAnger()) {
            event.setCancelled(true);
        }
    }
}