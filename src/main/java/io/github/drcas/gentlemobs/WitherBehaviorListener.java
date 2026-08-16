package io.github.drcas.gentlemobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class WitherBehaviorListener implements Listener {

    private final JavaPlugin plugin;
    private final GentleMobsConfig config;
    private final NeutralCombatTracker neutralCombatTracker;

    private BukkitTask cleanupTask;

    public WitherBehaviorListener(
            JavaPlugin plugin,
            GentleMobsConfig config,
            NeutralCombatTracker neutralCombatTracker
    ) {
        this.plugin = plugin;
        this.config = config;
        this.neutralCombatTracker = neutralCombatTracker;
    }

    public void start() {

        cleanupTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::clearPassiveHeadTargets,
                1L,
                1L
        );
    }

    public void stop() {

        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWitherTargetPlayer(
            EntityTargetLivingEntityEvent event
    ) {

        if (!(event.getEntity() instanceof Wither wither)) {
            return;
        }

        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        if (shouldAllowVanillaCombat(wither)) {
            return;
        }

        event.setTarget(null);

        clearPlayerHeadTargets(wither);
    }

    private void clearPassiveHeadTargets() {

        for (Wither wither :
                plugin.getServer().getWorlds().stream()
                        .flatMap(world ->
                                world.getEntitiesByClass(
                                        Wither.class
                                ).stream()
                        )
                        .toList()) {

            if (shouldAllowVanillaCombat(wither)) {
                continue;
            }

            clearPlayerHeadTargets(wither);
        }
    }

    private void clearPlayerHeadTargets(Wither wither) {

        /*
         * Center head / normal Mob target.
         */
        if (wither.getTarget() instanceof Player) {
            wither.setTarget(null);
        }

        /*
         * The Wither's individual heads maintain
         * independent targets.
         */
        for (Wither.Head head : Wither.Head.values()) {

            if (wither.getTarget(head) instanceof Player) {
                wither.setTarget(head, null);
            }
        }
    }

    private boolean shouldAllowVanillaCombat(
            Wither wither
    ) {

        GentleMode mode =
                config.getMode(wither.getType());

        if (mode == GentleMode.VANILLA) {
            return true;
        }

        return mode == GentleMode.NEUTRAL &&
                neutralCombatTracker.isEngaged(wither);
    }
}