package io.github.drcas.gentlemobs;

import io.github.drcas.gentlemobs.internal.BrainTargetController;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class MobFleeListener implements Listener {

    private final JavaPlugin plugin;
    private final GentleMobsConfig config;
    private final NeutralCombatTracker neutralCombatTracker;

    public MobFleeListener(
            JavaPlugin plugin,
            GentleMobsConfig config,
            NeutralCombatTracker neutralCombatTracker
    ) {
        this.plugin = plugin;
        this.config = config;
        this.neutralCombatTracker = neutralCombatTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHitEnemy(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }


        // Only affect hostile enemies.
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        // We need a Mob for targeting/pathfinding control.
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        GentleMode mode = config.getMode(
                event.getEntity().getType()
        );

        /*
         * VANILLA:
         * GentleMobs does absolutely nothing.
         */
        if (mode == GentleMode.VANILLA) {
            return;
        }

        /*
         * NEUTRAL:
         *
         * The player started the fight.
         * Mark this mob as engaged and then get completely
         * out of Minecraft's way.
         *
         * From this point forward, vanilla AI decides how the
         * mob moves, attacks, navigates, explodes, jumps, etc.
         */
        if (mode == GentleMode.NEUTRAL) {

            neutralCombatTracker.engage(
                    mob,
                    player
            );

            return;
        }

        /*
         * PASSIVE:
         *
         * Clear combat state and make the mob flee.
         */
        clearCombatState(mob);

        final double fleeDistance =
                config.getFleeDistance();

        final double fleeSpeed =
                config.getFleeSpeed();

        final int fleeDurationTicks =
                config.getFleeDurationTicks();

        new BukkitRunnable() {

            private int ticksElapsed = 0;

            @Override
            public void run() {

                if (!mob.isValid() ||
                        mob.isDead() ||
                        !player.isOnline() ||
                        !mob.getWorld().equals(player.getWorld())) {

                    cancel();
                    return;
                }

                /*
                 * Passive mobs must never retain retaliation state.
                 */
                clearCombatState(mob);

                /*
                 * Recalculate the flee path every 5 ticks.
                 */
                if (ticksElapsed % 5 == 0) {

                    Location mobLocation =
                            mob.getLocation();

                    Location playerLocation =
                            player.getLocation();

                    Vector away = mobLocation.toVector()
                            .subtract(
                                    playerLocation.toVector()
                            );

                    away.setY(0);

                    if (away.lengthSquared() < 0.01) {

                        away = mobLocation
                                .getDirection()
                                .multiply(-1);

                        away.setY(0);
                    }

                    if (away.lengthSquared() < 0.01) {
                        away = new Vector(1, 0, 0);
                    }

                    away.normalize()
                            .multiply(fleeDistance);

                    Location fleeLocation =
                            mobLocation.clone()
                                    .add(away);

                    fleeLocation.setY(
                            mobLocation.getY()
                    );

                    mob.getPathfinder().moveTo(
                            fleeLocation,
                            fleeSpeed
                    );
                }

                ticksElapsed++;

                if (ticksElapsed >= fleeDurationTicks) {

                    clearCombatState(mob);

                    cancel();
                }
            }
        }.runTaskTimer(
                plugin,
                1L,
                1L
        );
    }

    private void clearCombatState(Mob mob) {

        mob.setTarget(null);

        BrainTargetController.clearCombatTarget(mob);
    }
}