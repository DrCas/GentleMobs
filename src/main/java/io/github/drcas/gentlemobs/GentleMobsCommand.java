package io.github.drcas.gentlemobs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GentleMobsCommand
        implements CommandExecutor, TabCompleter {

    private final GentleMobsPlugin plugin;
    private final GentleMobsConfig config;
    private final NeutralCombatTracker neutralCombatTracker;
    private final GentleRecipeManager recipeManager;

    public GentleMobsCommand(
            GentleMobsPlugin plugin,
            GentleMobsConfig config,
            NeutralCombatTracker neutralCombatTracker,
            GentleRecipeManager recipeManager

    ) {
        this.plugin = plugin;
        this.config = config;
        this.neutralCombatTracker = neutralCombatTracker;
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "help" -> showHelp(sender);

            case "reload" -> handleReload(sender);

            case "version" -> handleVersion(sender);

            case "mode" -> handleMode(sender, args);

            case "inspect" -> handleInspect(sender);

            case "override" -> handleOverride(
                    sender,
                    args
            );

            default -> {
                sender.sendMessage(
                        "Unknown GentleMobs command."
                );

                sender.sendMessage(
                        "Use /gentlemobs help"
                );
            }
        }

        return true;
    }

    private void showHelp(
            CommandSender sender
    ) {

        sender.sendMessage(
                "----- GentleMobs Commands -----"
        );

        sender.sendMessage(
                "/gentlemobs help"
        );

        sender.sendMessage(
                "/gentlemobs reload"
        );

        sender.sendMessage(
                "/gentlemobs version"
        );

        sender.sendMessage(
                "/gentlemobs mode [mode]"
        );

        sender.sendMessage(
                "/gentlemobs inspect"
        );

        sender.sendMessage(
                "/gentlemobs override add <mob> <mode>"
        );

        sender.sendMessage(
                "/gentlemobs override remove <mob>"
        );

        sender.sendMessage(
                "/gentlemobs override list"
        );

        sender.sendMessage(
                "Modes: Passive, Neutral, Vanilla"
        );
    }

    private void handleReload(
            CommandSender sender
    ) {

        config.load();
        recipeManager.reloadRecipes();

        sender.sendMessage(
                "GentleMobs configuration and recipes reloaded."
        );
    }

    private void handleVersion(
            CommandSender sender
    ) {

        sender.sendMessage(
                "GentleMobs " +
                        plugin.getPluginMeta()
                                .getVersion()
        );
    }

    private void handleMode(
            CommandSender sender,
            String[] args
    ) {

        if (args.length == 1) {

            sender.sendMessage(
                    "Global GentleMobs mode: " +
                            displayMode(
                                    config.getGlobalMode()
                            )
            );

            return;
        }

        GentleMode mode =
                parseMode(args[1]);

        if (mode == null) {

            sender.sendMessage(
                    "Invalid mode. Use Passive, Neutral, or Vanilla."
            );

            return;
        }

        config.setGlobalMode(mode);

        sender.sendMessage(
                "Global GentleMobs mode changed to " +
                        displayMode(mode) +
                        "."
        );
    }

    private void handleInspect(
            CommandSender sender
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Only players can inspect mobs."
            );

            return;
        }

        RayTraceResult result =
                player.getWorld()
                        .rayTraceEntities(
                                player.getEyeLocation(),
                                player.getEyeLocation()
                                        .getDirection(),
                                10.0,
                                0.25,
                                entity ->
                                        entity instanceof Mob
                        );

        if (result == null ||
                result.getHitEntity() == null) {

            sender.sendMessage(
                    "Look directly at a mob within 10 blocks."
            );

            return;
        }

        Entity entity =
                result.getHitEntity();

        if (!(entity instanceof Mob mob)) {

            sender.sendMessage(
                    "That entity is not a supported mob."
            );

            return;
        }

        EntityType type =
                mob.getType();

        GentleMode resolvedMode =
                config.getMode(type);

        sender.sendMessage(
                "----- GentleMobs Inspection -----"
        );

        sender.sendMessage(
                "Mob: " +
                        displayEntityType(type)
        );

        sender.sendMessage(
                "Resolved Mode: " +
                        displayMode(resolvedMode)
        );

        if (config.hasOverride(type)) {

            sender.sendMessage(
                    "Override: Yes (" +
                            displayMode(
                                    config.getOverride(type)
                            ) +
                            ")"
            );

        } else {

            sender.sendMessage(
                    "Override: No"
            );

            sender.sendMessage(
                    "Global Mode: " +
                            displayMode(
                                    config.getGlobalMode()
                            )
            );
        }

        sender.sendMessage(
                "Neutral Engaged: " +
                        (neutralCombatTracker
                                .isEngaged(mob)
                                ? "Yes"
                                : "No")
        );
    }

    private void handleOverride(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 2) {

            sender.sendMessage(
                    "Usage: /gentlemobs override <add|remove|list>"
            );

            return;
        }

        switch (args[1]
                .toLowerCase(Locale.ROOT)) {

            case "add" ->
                    handleOverrideAdd(
                            sender,
                            args
                    );

            case "remove" ->
                    handleOverrideRemove(
                            sender,
                            args
                    );

            case "list" ->
                    handleOverrideList(sender);

            default ->
                    sender.sendMessage(
                            "Usage: /gentlemobs override <add|remove|list>"
                    );
        }
    }

    private void handleOverrideAdd(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 4) {

            sender.sendMessage(
                    "Usage: /gentlemobs override add <mob> <mode>"
            );

            return;
        }

        EntityType entityType =
                parseEntityType(args[2]);

        if (entityType == null ||
                !isRelevantMobType(entityType)) {

            sender.sendMessage(
                    "Unknown or unsupported hostile mob: " +
                            args[2]
            );

            return;
        }

        GentleMode mode =
                parseMode(args[3]);

        if (mode == null) {

            sender.sendMessage(
                    "Invalid mode. Use Passive, Neutral, or Vanilla."
            );

            return;
        }

        config.setOverride(
                entityType,
                mode
        );

        sender.sendMessage(
                displayEntityType(entityType) +
                        " override set to " +
                        displayMode(mode) +
                        "."
        );
    }

    private void handleOverrideRemove(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 3) {

            sender.sendMessage(
                    "Usage: /gentlemobs override remove <mob>"
            );

            return;
        }

        EntityType entityType =
                parseEntityType(args[2]);

        if (entityType == null) {

            sender.sendMessage(
                    "Unknown mob type: " +
                            args[2]
            );

            return;
        }

        if (!config.hasOverride(entityType)) {

            sender.sendMessage(
                    displayEntityType(entityType) +
                            " does not currently have an override."
            );

            return;
        }

        config.removeOverride(entityType);

        sender.sendMessage(
                displayEntityType(entityType) +
                        " override removed."
        );
    }

    private void handleOverrideList(
            CommandSender sender
    ) {

        Map<EntityType, GentleMode> overrides =
                config.getMobOverrides();

        if (overrides.isEmpty()) {

            sender.sendMessage(
                    "No mob overrides are configured."
            );

            return;
        }

        sender.sendMessage(
                "----- GentleMobs Overrides -----"
        );

        overrides.entrySet()
                .stream()
                .sorted(
                        Map.Entry.comparingByKey(
                                Comparator.comparing(
                                        EntityType::name
                                )
                        )
                )
                .forEach(entry ->
                        sender.sendMessage(
                                displayEntityType(
                                        entry.getKey()
                                ) +
                                        ": " +
                                        displayMode(
                                                entry.getValue()
                                        )
                        )
                );
    }

    private GentleMode parseMode(
            String value
    ) {

        try {

            return GentleMode.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {

            return null;
        }
    }

    private EntityType parseEntityType(
            String value
    ) {

        try {

            return EntityType.valueOf(
                    value.trim()
                            .replace(" ", "_")
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {

            return null;
        }
    }

    private String displayMode(
            GentleMode mode
    ) {

        String name =
                mode.name()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return Character.toUpperCase(
                name.charAt(0)
        ) + name.substring(1);
    }

    private String displayEntityType(
            EntityType type
    ) {

        String[] words =
                type.name()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .split("_");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {

            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );

            result.append(
                    word.substring(1)
            );
        }

        return result.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {

        if (args.length == 1) {

            return filter(
                    List.of(
                            "help",
                            "reload",
                            "version",
                            "mode",
                            "inspect",
                            "override"
                    ),
                    args[0]
            );
        }

        if (args[0].equalsIgnoreCase("mode") &&
                args.length == 2) {

            return filter(
                    modeNames(),
                    args[1]
            );
        }

        if (args[0].equalsIgnoreCase("override")) {

            if (args.length == 2) {

                return filter(
                        List.of(
                                "add",
                                "remove",
                                "list"
                        ),
                        args[1]
                );
            }

            if (args.length == 3 &&
                    args[1].equalsIgnoreCase("add")) {

                return filter(
                        mobNames(),
                        args[2]
                );
            }

            if (args.length == 3 &&
                    args[1].equalsIgnoreCase("remove")) {

                List<String> overrideNames =
                        config.getMobOverrides()
                                .keySet()
                                .stream()
                                .map(type ->
                                        type.name()
                                                .toLowerCase(
                                                        Locale.ROOT
                                                )
                                )
                                .sorted()
                                .toList();

                return filter(
                        overrideNames,
                        args[2]
                );
            }

            if (args.length == 4 &&
                    args[1].equalsIgnoreCase("add")) {

                return filter(
                        modeNames(),
                        args[3]
                );
            }
        }

        return List.of();
    }

    private List<String> modeNames() {

        return Arrays.stream(
                        GentleMode.values()
                )
                .map(mode ->
                        mode.name()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                )
                .toList();
    }

    private List<String> mobNames() {

        return Arrays.stream(
                        EntityType.values()
                )
                .filter(this::isRelevantMobType)
                .map(type ->
                        type.name()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                )
                .sorted()
                .toList();
    }

    private boolean isRelevantMobType(
            EntityType type
    ) {

        Class<? extends Entity> entityClass =
                type.getEntityClass();

        return entityClass != null &&
                Enemy.class.isAssignableFrom(
                        entityClass
                );
    }

    private List<String> filter(
            List<String> values,
            String input
    ) {

        String lower =
                input.toLowerCase(
                        Locale.ROOT
                );

        List<String> matches =
                new ArrayList<>();

        for (String value : values) {

            if (value.toLowerCase(
                            Locale.ROOT
                    )
                    .startsWith(lower)) {

                matches.add(value);
            }
        }

        return matches;
    }
}