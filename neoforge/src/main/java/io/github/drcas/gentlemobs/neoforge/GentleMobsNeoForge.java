package io.github.drcas.gentlemobs.neoforge;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

@Mod(GentleMobsNeoForge.MOD_ID)
public final class GentleMobsNeoForge {
    public static final String MOD_ID = "gentlemobs";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static volatile GentleConfig CONFIG;
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
        DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, MOD_ID);
    static { CONDITIONS.register("recipe_enabled", () -> RecipeEnabledCondition.CODEC); }
    public GentleMobsNeoForge(IEventBus modBus) {
        CONFIG = new GentleConfig(FMLPaths.CONFIGDIR.get().resolve("gentlemobs.json"));
        try { CONFIG.load(); }
        catch (java.io.IOException ex) { throw new IllegalStateException("Unable to load GentleMobs configuration", ex); }
        CONDITIONS.register(modBus);
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST, this::target);
        bus.addListener(EventPriority.LOWEST, this::incoming);
        bus.addListener(this::damaged);
        bus.addListener(this::beforeTick);
        bus.addListener(this::afterTick);
        bus.addListener(this::leave);
        bus.addListener(this::commands);
        bus.addListener(this::stopped);
        bus.addListener(this::started);
        LOGGER.info("GentleMobs NeoForge loaded; vanilla hostile registry IDs only by default.");
    }
    private void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && BehaviorEngine.blocksTarget(mob, event.getNewAboutToBeSetTarget()))
            event.setNewAboutToBeSetTarget(null);
    }
    private void incoming(LivingIncomingDamageEvent event) {
        // Owner-attributed damage covers melee, arrows/fireballs, sonic boom and slime contact.
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof Mob mob
                && BehaviorEngine.blocksPlayers(mob)) event.setCanceled(true);
    }
    private void damaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && event.getSource().getEntity() instanceof Player player
                && event.getOriginalDamage() > 0) BehaviorEngine.hit(mob, player);
    }
    private void beforeTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide) BehaviorEngine.beforeTick(mob);
    }
    private void afterTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide) BehaviorEngine.afterTick(mob);
    }
    private void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide) BehaviorEngine.forget(mob);
    }
    private void commands(RegisterCommandsEvent event) { GentleCommands.register(event.getDispatcher()); }
    private void stopped(ServerStoppedEvent event) { BehaviorEngine.clear(); }
    private void started(ServerStartedEvent event) { validateOverrides(); }
    public static void validateOverrides() {
        CONFIG.get().overrides().keySet().stream().filter(id -> !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(id)))
            .forEach(id -> LOGGER.warn("Unknown mob override '{}' is inactive (Creaking is not in Minecraft 1.21.1).", id));
    }
}
