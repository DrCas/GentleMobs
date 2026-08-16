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

    private static final double FLEE_DISTANCE = 12.0;
    private static final double FLEE_SPEED = 1.3;

    // 60 ticks = 3 seconds
    private static final int FLEE_DURATION_TICKS = 60;

    public MobFleeListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHitEnemy(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // Boss/special encounters remain vanilla.
        if (event.getEntity() instanceof EnderDragon ||
                event.getEntity() instanceof Wither ||
                event.getEntity() instanceof Warden) {
            return;
        }

        // Only affect hostile enemies.
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        // We need a Mob so we can control its target and pathfinder.
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        /*
         * Clear any combat state that already exists at the moment
         * the mob is hit.
         */
        clearCombatState(mob);

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
                 * Clear combat targeting EVERY tick while fleeing.
                 *
                 * mob.setTarget(null) handles the Bukkit-facing target.
                 *
                 * BrainTargetController clears Minecraft's internal
                 * Brain memories such as ATTACK_TARGET and ANGRY_AT.
                 *
                 * This is especially important for mobs like Zoglins,
                 * which may establish retaliation state shortly after
                 * taking damage.
                 */
                clearCombatState(mob);

                /*
                 * Recalculate the flee path every 5 ticks.
                 *
                 * This lets the mob continue moving away from the
                 * player's current position without replacing the
                 * path every single server tick.
                 */
                if (ticksElapsed % 5 == 0) {

                    Location mobLocation = mob.getLocation();
                    Location playerLocation = player.getLocation();

                    Vector away = mobLocation.toVector()
                            .subtract(playerLocation.toVector());

                    // Keep the flee direction horizontal.
                    away.setY(0);

                    /*
                     * If the player and mob are almost exactly on top
                     * of each other, there is no useful "away" vector.
                     * Fall back to the opposite of the mob's facing
                     * direction.
                     */
                    if (away.lengthSquared() < 0.01) {
                        away = mobLocation.getDirection()
                                .multiply(-1);

                        away.setY(0);
                    }

                    /*
                     * Last-resort fallback so normalize() always has
                     * a valid direction.
                     */
                    if (away.lengthSquared() < 0.01) {
                        away = new Vector(1, 0, 0);
                    }

                    away.normalize()
                            .multiply(FLEE_DISTANCE);

                    Location fleeLocation =
                            mobLocation.clone().add(away);

                    fleeLocation.setY(mobLocation.getY());

                    mob.getPathfinder().moveTo(
                            fleeLocation,
                            FLEE_SPEED
                    );
                }

                ticksElapsed++;

                if (ticksElapsed >= FLEE_DURATION_TICKS) {

                    /*
                     * Make absolutely sure the mob does not leave
                     * flee mode with a retaliation target still set.
                     */
                    clearCombatState(mob);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void clearCombatState(Mob mob) {

        // Clear the Bukkit-facing target.
        mob.setTarget(null);

        // Clear Minecraft Brain combat memories when available.
        BrainTargetController.clearCombatTarget(mob);
    }
}