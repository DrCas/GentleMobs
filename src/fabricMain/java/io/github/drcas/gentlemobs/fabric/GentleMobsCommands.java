package io.github.drcas.gentlemobs.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class GentleMobsCommands {

    private GentleMobsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("gentlemobs")
                                .executes(context -> showHelp(context.getSource()))
                                .then(Commands.literal("help")
                                        .executes(context -> showHelp(context.getSource())))
                                .then(Commands.literal("version")
                                        .executes(context -> {
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("GentleMobs Fabric"),
                                                    false
                                            );
                                            return 1;
                                        }))
                                .then(Commands.literal("mode")
                                        .executes(context -> {
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Global GentleMobs mode: " + displayMode(GentleMobsFabric.getGlobalMode())
                                                    ),
                                                    false
                                            );
                                            return 1;
                                        })
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("passive");
                                                    builder.suggest("neutral");
                                                    builder.suggest("vanilla");
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    GentleMode mode = parseMode(StringArgumentType.getString(context, "mode"));
                                                    if (mode == null) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Invalid mode. Use Passive, Neutral, or Vanilla.")
                                                        );
                                                        return 0;
                                                    }

                                                    GentleMobsFabric.setGlobalMode(mode);
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal(
                                                                    "Global GentleMobs mode changed to " + displayMode(mode) + "."
                                                            ),
                                                            true
                                                    );
                                                    return 1;
                                                })))
                )
        );
    }

    private static int showHelp(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("----- GentleMobs Commands -----"), false);
        source.sendSuccess(() -> Component.literal("/gentlemobs help"), false);
        source.sendSuccess(() -> Component.literal("/gentlemobs version"), false);
        source.sendSuccess(() -> Component.literal("/gentlemobs mode [passive|neutral|vanilla]"), false);
        source.sendSuccess(() -> Component.literal("Passive: mobs ignore players and flee when hit."), false);
        source.sendSuccess(() -> Component.literal("Neutral: mobs ignore players until attacked, then retaliate."), false);
        source.sendSuccess(() -> Component.literal("Vanilla: normal Minecraft behavior."), false);
        return 1;
    }

    private static GentleMode parseMode(String value) {
        try {
            return GentleMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String displayMode(GentleMode mode) {
        String name = mode.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
