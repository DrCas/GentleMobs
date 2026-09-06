package io.github.drcas.gentlemobs.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.drcas.gentlemobs.GentleMode;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import static net.minecraft.commands.Commands.*;

public final class GentleCommands {
    private GentleCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("gentlemobs").requires(s -> s.hasPermission(2))
            .executes(c -> help(c.getSource()))
            .then(literal("help").executes(c -> help(c.getSource())))
            .then(literal("version").executes(c -> reply(c.getSource(), "GentleMobs " + ModList.get().getModContainerById("gentlemobs").orElseThrow().getModInfo().getVersion() + " / NeoForge 1.21.1")))
            .then(literal("mode").executes(c -> reply(c.getSource(), "Global mode: " + GentleMobsNeoForge.CONFIG.get().mode()))
                .then(argument("mode", StringArgumentType.word()).suggests((c,b) -> SharedSuggestionProvider.suggest(List.of("PASSIVE", "NEUTRAL", "VANILLA"),b))
                    .executes(c -> change(c, () -> GentleMobsNeoForge.CONFIG.setMode(mode(c)), "Global mode saved."))))
            .then(literal("reload").executes(c -> reload(c.getSource())))
            .then(literal("inspect").executes(c -> inspect(c.getSource())))
            .then(literal("recipes").executes(c -> reply(c.getSource(), "Nether Star: " + GentleMobsNeoForge.CONFIG.get().netherStar() + "; Dragon's Breath: " + GentleMobsNeoForge.CONFIG.get().dragonsBreath())))
            .then(literal("override")
                .then(literal("list").executes(c -> reply(c.getSource(), "Overrides: " + new TreeMap<>(GentleMobsNeoForge.CONFIG.get().overrides()))))
                .then(literal("add").then(argument("mob", ResourceLocationArgument.id())
                    .suggests((c,b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b))
                    .then(argument("mode", StringArgumentType.word()).suggests((c,b) -> SharedSuggestionProvider.suggest(List.of("PASSIVE", "NEUTRAL", "VANILLA"),b))
                        .executes(c -> change(c, () -> GentleMobsNeoForge.CONFIG.override(entityId(c, true), mode(c)), "Mob override saved. Explicit overrides opt modded entities in.")))))
                .then(literal("remove").then(argument("mob", ResourceLocationArgument.id())
                    .suggests((c,b) -> SharedSuggestionProvider.suggest(GentleMobsNeoForge.CONFIG.get().overrides().keySet(),b))
                    .executes(c -> change(c, () -> GentleMobsNeoForge.CONFIG.override(entityId(c, false), null), "Override removed; vanilla scope rules restored."))))));
    }
    private static GentleMode mode(CommandContext<CommandSourceStack> c) { return GentleConfig.parseMode(StringArgumentType.getString(c,"mode")); }
    private static String entityId(CommandContext<CommandSourceStack> c, boolean validate) {
        String id = ResourceLocationArgument.getId(c,"mob").toString();
        if (validate && !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(id))) throw new IllegalArgumentException("Unknown entity ID: " + id);
        return id;
    }
    @FunctionalInterface private interface Change { void run() throws Exception; }
    private static int change(CommandContext<CommandSourceStack> c, Change change, String success) {
        try { change.run(); return reply(c.getSource(), success); }
        catch (Exception ex) { c.getSource().sendFailure(Component.literal(ex.getMessage())); return 0; }
    }
    private static int reload(CommandSourceStack source) {
        try { GentleMobsNeoForge.CONFIG.load(); GentleMobsNeoForge.validateOverrides(); }
        catch (Exception ex) { source.sendFailure(Component.literal(ex.getMessage() + "; previous configuration remains active.")); return 0; }
        source.getServer().reloadResources(source.getServer().getPackRepository().getSelectedIds())
            .whenComplete((unused, error) -> source.getServer().execute(() -> {
                if (error == null) reply(source, "GentleMobs configuration and recipes reloaded.");
                else { source.sendFailure(Component.literal("Config loaded, but recipe reload failed; check the server log.")); GentleMobsNeoForge.LOGGER.error("Recipe reload failed", error); }
            }));
        return reply(source, "Configuration loaded; reloading datapacks for recipe changes...");
    }
    private static int inspect(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        Vec3 eye = player.getEyePosition();
        Vec3 end = player.pick(32, 0, false).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player, eye, end,
            player.getBoundingBox().expandTowards(player.getViewVector(0).scale(32)).inflate(1),
            e -> e instanceof Mob || e instanceof EnderDragonPart, eye.distanceToSqr(end));
        if (hit == null) return reply(source, "Look at a mob within 32 blocks.");
        Mob mob = hit.getEntity() instanceof EnderDragonPart part ? part.parentMob : (Mob)hit.getEntity();
        String id = BehaviorEngine.id(mob);
        return reply(source, id + " | mode=" + BehaviorEngine.mode(mob) + " | default-scope=" + BehaviorPolicy.VANILLA_HOSTILES.contains(id)
            + " | override=" + GentleMobsNeoForge.CONFIG.get().overrides().get(id) + " | engaged=" + BehaviorEngine.engaged(mob) + " | fleeing=" + BehaviorEngine.fleeing(mob));
    }
    private static int help(CommandSourceStack source) {
        return reply(source, "/gentlemobs mode [PASSIVE|NEUTRAL|VANILLA], reload, version, inspect, recipes, override add <mob> <mode>, override remove <mob>, override list. Use lowercase entity IDs (e.g. minecraft:zombie or zombie).");
    }
    private static int reply(CommandSourceStack source, String message) { source.sendSuccess(() -> Component.literal(message), false); return 1; }
}
