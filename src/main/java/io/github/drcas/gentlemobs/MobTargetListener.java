package io.github.drcas.gentlemobs;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public final class MobTargetListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEnemyTargetPlayer(EntityTargetLivingEntityEvent event) {

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

        // Only prevent targeting players.
        if (!(event.getTarget() instanceof Player)) {
            return;
        }


        // Remove the player as the target.
        event.setTarget(null);
    }
}