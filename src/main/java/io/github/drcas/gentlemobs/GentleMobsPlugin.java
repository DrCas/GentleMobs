package io.github.drcas.gentlemobs;

import org.bukkit.plugin.java.JavaPlugin;

public final class GentleMobsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(
                new MobTargetListener(),
                this
        );

        getServer().getPluginManager().registerEvents(
                new MobFleeListener(this),
                this
        );

        getLogger().info("GentleMobs 0.1.0 enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("GentleMobs disabled.");
    }
}