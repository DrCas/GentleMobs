package io.github.drcas.gentlemobs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class GentleMobsConfig {

    private final JavaPlugin plugin;

    private GentleMode globalMode = GentleMode.PASSIVE;

    private final Map<EntityType, GentleMode> mobOverrides =
            new EnumMap<>(EntityType.class);

    private double fleeDistance = 12.0;
    private double fleeSpeed = 1.3;
    private int fleeDurationTicks = 60;

    public GentleMobsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        globalMode = parseMode(
                plugin.getConfig().getString(
                        "mode",
                        "PASSIVE"
                )
        );

        fleeDistance = plugin.getConfig().getDouble(
                "flee.distance",
                12.0
        );

        fleeSpeed = plugin.getConfig().getDouble(
                "flee.speed",
                1.3
        );

        fleeDurationTicks = plugin.getConfig().getInt(
                "flee.duration-ticks",
                60
        );

        mobOverrides.clear();

        ConfigurationSection section =
                plugin.getConfig()
                        .getConfigurationSection(
                                "mob-overrides"
                        );

        if (section != null) {

            for (String key : section.getKeys(false)) {

                EntityType entityType;

                try {
                    entityType = EntityType.valueOf(
                            key.toUpperCase(Locale.ROOT)
                    );
                } catch (IllegalArgumentException exception) {

                    plugin.getLogger().warning(
                            "Unknown mob type in config: " + key
                    );

                    continue;
                }

                GentleMode mode = parseMode(
                        section.getString(key)
                );

                mobOverrides.put(
                        entityType,
                        mode
                );
            }
        }
    }

    /*
     * Resolve the actual GentleMobs mode for an entity.
     *
     * Per-mob overrides take priority over the global mode.
     */
    public GentleMode getMode(
            EntityType entityType
    ) {

        return mobOverrides.getOrDefault(
                entityType,
                globalMode
        );
    }

    public GentleMode getGlobalMode() {
        return globalMode;
    }

    public boolean hasOverride(
            EntityType entityType
    ) {

        return mobOverrides.containsKey(
                entityType
        );
    }

    public GentleMode getOverride(
            EntityType entityType
    ) {

        return mobOverrides.get(
                entityType
        );
    }

    public Map<EntityType, GentleMode>
    getMobOverrides() {

        return Map.copyOf(
                mobOverrides
        );
    }

    /*
     * Permanently change the global mode.
     *
     * This writes to config.yml and immediately updates
     * the loaded configuration.
     */
    public void setGlobalMode(
            GentleMode mode
    ) {

        plugin.getConfig().set(
                "mode",
                mode.name()
        );

        plugin.saveConfig();

        globalMode = mode;
    }

    /*
     * Permanently create or replace a per-mob override.
     */
    public void setOverride(
            EntityType entityType,
            GentleMode mode
    ) {

        plugin.getConfig().set(
                "mob-overrides." +
                        entityType.name(),
                mode.name()
        );

        plugin.saveConfig();

        mobOverrides.put(
                entityType,
                mode
        );
    }

    /*
     * Permanently remove a per-mob override.
     *
     * The mob will immediately fall back to the global mode.
     */
    public void removeOverride(
            EntityType entityType
    ) {

        plugin.getConfig().set(
                "mob-overrides." +
                        entityType.name(),
                null
        );

        plugin.saveConfig();

        mobOverrides.remove(
                entityType
        );
    }

    public double getFleeDistance() {
        return fleeDistance;
    }

    public double getFleeSpeed() {
        return fleeSpeed;
    }

    public int getFleeDurationTicks() {
        return fleeDurationTicks;
    }

    private GentleMode parseMode(
            String value
    ) {

        if (value == null) {
            return GentleMode.PASSIVE;
        }

        try {

            return GentleMode.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {

            plugin.getLogger().warning(
                    "Unknown GentleMobs mode '" +
                            value +
                            "'. Using PASSIVE."
            );

            return GentleMode.PASSIVE;
        }
    }
}