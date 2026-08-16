package io.github.drcas.gentlemobs;

import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;

public final class DragonBehaviorListener implements Listener {

    private final GentleMobsConfig config;
    private final NeutralCombatTracker neutralCombatTracker;

    public DragonBehaviorListener(
            GentleMobsConfig config,
            NeutralCombatTracker neutralCombatTracker
    ) {
        this.config = config;
        this.neutralCombatTracker = neutralCombatTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDragonPhaseChange(
            EnderDragonChangePhaseEvent event
    ) {

        EnderDragon dragon = event.getEntity();

        GentleMode mode = config.getMode(
                dragon.getType()
        );

        /*
         * VANILLA:
         * GentleMobs does nothing.
         */
        if (mode == GentleMode.VANILLA) {
            return;
        }

        /*
         * NEUTRAL + ENGAGED:
         * Player started the fight.
         * Let vanilla Dragon combat operate normally.
         */
        if (mode == GentleMode.NEUTRAL &&
                neutralCombatTracker.isEngaged(dragon)) {
            return;
        }

        EnderDragon.Phase newPhase =
                event.getNewPhase();

        /*
         * PASSIVE or idle NEUTRAL:
         *
         * Block Dragon phases whose purpose is specifically
         * attacking a player.
         */
        if (isPlayerAttackPhase(newPhase)) {

            event.setNewPhase(
                    EnderDragon.Phase.CIRCLING
            );
        }
    }

    private boolean isPlayerAttackPhase(
            EnderDragon.Phase phase
    ) {

        return phase == EnderDragon.Phase.STRAFING ||
                phase == EnderDragon.Phase.BREATH_ATTACK ||
                phase == EnderDragon.Phase.SEARCH_FOR_BREATH_ATTACK_TARGET ||
                phase == EnderDragon.Phase.ROAR_BEFORE_ATTACK ||
                phase == EnderDragon.Phase.CHARGE_PLAYER;
    }
}