package io.github.drcas.gentlemobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class NeutralCombatTracker {

    private final JavaPlugin plugin;

    private final Map<UUID, UUID> engagedMobs =
            new HashMap<>();

    private BukkitTask cleanupTask;

    public NeutralCombatTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {

        cleanupTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::cleanupEngagements,
                20L,
                20L
        );
    }

    public void stop() {

        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        engagedMobs.clear();
    }

    public void engage(Mob mob, Player player) {

        engagedMobs.put(
                mob.getUniqueId(),
                player.getUniqueId()
        );
    }

    public void disengage(Mob mob) {

        engagedMobs.remove(
                mob.getUniqueId()
        );
    }

    public boolean isEngaged(Mob mob) {

        return engagedMobs.containsKey(
                mob.getUniqueId()
        );
    }

    public boolean isEngagedWith(
            Mob mob,
            Player player
    ) {

        UUID playerId = engagedMobs.get(
                mob.getUniqueId()
        );

        return playerId != null &&
                playerId.equals(player.getUniqueId());
    }

    public UUID getEngagedPlayer(Mob mob) {

        return engagedMobs.get(
                mob.getUniqueId()
        );
    }

    private void cleanupEngagements() {

        Iterator<Map.Entry<UUID, UUID>> iterator =
                engagedMobs.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<UUID, UUID> entry =
                    iterator.next();

            Entity entity =
                    Bukkit.getEntity(entry.getKey());

            /*
             * Mob disappeared, died, unloaded, etc.
             */
            if (!(entity instanceof Mob mob) ||
                    !mob.isValid() ||
                    mob.isDead()) {

                iterator.remove();
                continue;
            }

            Player player =
                    Bukkit.getPlayer(entry.getValue());

            /*
             * Player logged out, died, or is no longer
             * available as a combat target.
             */
            if (player == null ||
                    !player.isOnline() ||
                    player.isDead()) {

                iterator.remove();
                continue;
            }

            /*
             * Player and mob are no longer in the same world.
             */
            if (!mob.getWorld().equals(player.getWorld())) {

                iterator.remove();
                continue;
            }

            /*
             * Most important rule:
             *
             * If vanilla Minecraft has naturally dropped
             * this player as the mob's target, combat is over.
             *
             * GentleMobs can now resume Neutral behavior.
             */
            if (mob.getTarget() == null ||
                    !mob.getTarget().getUniqueId()
                            .equals(player.getUniqueId())) {

                iterator.remove();
            }
        }
    }
}