package io.github.drcas.gentlemobs;

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

        var section = plugin.getConfig()
                .getConfigurationSection("mob-overrides");

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

                mobOverrides.put(entityType, mode);
            }
        }
    }

    public GentleMode getMode(EntityType entityType) {

        return mobOverrides.getOrDefault(
                entityType,
                globalMode
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

    private GentleMode parseMode(String value) {

        if (value == null) {
            return GentleMode.PASSIVE;
        }

        try {
            return GentleMode.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
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