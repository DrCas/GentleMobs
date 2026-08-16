package io.github.drcas.gentlemobs;

import org.bukkit.plugin.java.JavaPlugin;

public final class GentleMobsPlugin extends JavaPlugin {

    private GentleMobsConfig gentleMobsConfig;
    private NeutralCombatTracker neutralCombatTracker;
    private WitherBehaviorListener witherBehaviorListener;
    private GentleRecipeManager recipeManager;

    @Override
    public void onEnable() {

        /*
         * Load configuration.
         */
        gentleMobsConfig =
                new GentleMobsConfig(this);

        gentleMobsConfig.load();

        /*
         * Register custom GentleMobs recipes.
         */
        recipeManager =
                new GentleRecipeManager(this);

        recipeManager.reloadRecipes();

        /*
         * Start Neutral combat tracking.
         */
        neutralCombatTracker =
                new NeutralCombatTracker(this);

        neutralCombatTracker.start();

        /*
         * Register GentleMobs command system.
         */
        GentleMobsCommand gentleMobsCommand =
                new GentleMobsCommand(
                        this,
                        gentleMobsConfig,
                        neutralCombatTracker,
                        recipeManager
                );

        if (getCommand("gentlemobs") != null) {

            getCommand("gentlemobs")
                    .setExecutor(
                            gentleMobsCommand
                    );

            getCommand("gentlemobs")
                    .setTabCompleter(
                            gentleMobsCommand
                    );
        }

        /*
         * Generic mob behavior.
         */
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

        /*
         * Special mob compatibility.
         */
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
                witherBehaviorListener,
                this
        );

        witherBehaviorListener.start();

        getServer().getPluginManager().registerEvents(
                new DragonBehaviorListener(
                        gentleMobsConfig,
                        neutralCombatTracker
                ),
                this
        );

        getLogger().info(
                "GentleMobs " +
                        getPluginMeta().getVersion() +
                        " enabled."
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

        if (recipeManager != null) {
            recipeManager.removeRecipes();
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