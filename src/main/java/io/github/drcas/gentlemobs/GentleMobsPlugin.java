package io.github.drcas.gentlemobs;

import org.bukkit.plugin.java.JavaPlugin;

public final class GentleMobsPlugin extends JavaPlugin {

    private GentleMobsConfig gentleMobsConfig;
    private NeutralCombatTracker neutralCombatTracker;
    private WitherBehaviorListener witherBehaviorListener;

    @Override
    public void onEnable() {

        gentleMobsConfig = new GentleMobsConfig(this);
        gentleMobsConfig.load();

        neutralCombatTracker =
                new NeutralCombatTracker(this);

        neutralCombatTracker.start();

        getServer().getPluginManager().registerEvents(
                new MobTargetListener(
                        gentleMobsConfig,
                        neutralCombatTracker
                ),
                this
        );

        getServer().getPluginManager().registerEvents(
                new MobFleeListener(
                        this,
                        gentleMobsConfig,
                        neutralCombatTracker
                ),
                this
        );

        getServer().getPluginManager().registerEvents(
                new WardenBehaviorListener(
                        gentleMobsConfig,
                        neutralCombatTracker
                ),
                this
        );

        witherBehaviorListener =
                new WitherBehaviorListener(
                        this,
                        gentleMobsConfig,
                        neutralCombatTracker
                );

        getServer().getPluginManager().registerEvents(
                new DragonBehaviorListener(
                        gentleMobsConfig,
                        neutralCombatTracker
                ),
                this
        );

        getServer().getPluginManager().registerEvents(
                witherBehaviorListener,
                this
        );

        witherBehaviorListener.start();

        getLogger().info(
                "GentleMobs 0.1.0 enabled."
        );
    }

    @Override
    public void onDisable() {

        if (neutralCombatTracker != null) {
            neutralCombatTracker.stop();
        }

        if (witherBehaviorListener != null) {
            witherBehaviorListener.stop();
        }

        getLogger().info(
                "GentleMobs disabled."
        );
    }

    public GentleMobsConfig getGentleMobsConfig() {
        return gentleMobsConfig;
    }

    public NeutralCombatTracker getNeutralCombatTracker() {
        return neutralCombatTracker;
    }
}