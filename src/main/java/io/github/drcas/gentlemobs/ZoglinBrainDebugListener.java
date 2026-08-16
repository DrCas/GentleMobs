package io.github.drcas.gentlemobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZoglinBrainDebugListener implements Listener {

    private final JavaPlugin plugin;

    public ZoglinBrainDebugListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onZoglinHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (!(event.getEntity() instanceof Zoglin zoglin)) {
            return;
        }

        plugin.getLogger().info("==================================");
        plugin.getLogger().info("ZOGLIN HIT - BEGINNING AI TRACE");
        plugin.getLogger().info("==================================");

        logState(zoglin, "IMMEDIATE");

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> logState(zoglin, "1 TICK"),
                1L
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> logState(zoglin, "5 TICKS"),
                5L
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> logState(zoglin, "10 TICKS"),
                10L
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> logState(zoglin, "20 TICKS"),
                20L
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> logState(zoglin, "40 TICKS"),
                40L
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> logState(zoglin, "60 TICKS"),
                60L
        );
    }

    private void logState(Zoglin zoglin, String time) {

        if (!zoglin.isValid() || zoglin.isDead()) {
            plugin.getLogger().info(
                    "[" + time + "] Zoglin no longer valid."
            );
            return;
        }

        plugin.getLogger().info(
                "---------- ZOGLIN @ " + time + " ----------"
        );

        plugin.getLogger().info(
                "Bukkit target: " +
                        (zoglin.getTarget() == null
                                ? "null"
                                : zoglin.getTarget().getType().toString())
        );

        plugin.getLogger().info(
                "Aggressive: " +
                        zoglin.isAggressive()
        );

        plugin.getLogger().info(
                "ANGRY_AT: " +
                        zoglin.getMemory(MemoryKey.ANGRY_AT)
        );

        plugin.getLogger().info(
                "PACIFIED: " +
                        zoglin.getMemory(MemoryKey.PACIFIED)
        );

        plugin.getLogger().info(
                "IS_PANICKING: " +
                        zoglin.getMemory(MemoryKey.IS_PANICKING)
        );

        plugin.getLogger().info(
                "UNIVERSAL_ANGER: " +
                        zoglin.getMemory(MemoryKey.UNIVERSAL_ANGER)
        );

        plugin.getLogger().info(
                "---------------------------------------"
        );
    }
}