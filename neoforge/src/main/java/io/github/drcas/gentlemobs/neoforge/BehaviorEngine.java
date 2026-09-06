package io.github.drcas.gentlemobs.neoforge;

import io.github.drcas.gentlemobs.GentleMode;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BehaviorEngine {
    private static final Map<Mob, State> STATES = new WeakHashMap<>();
    private static final class State {
        UUID opponent;
        long hitAt, combatUntil, fleeUntil;
        int revision;
        boolean goalInstalled;
    }
    private BehaviorEngine() {}
    public static String id(Mob mob) { return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(); }
    public static GentleMode mode(Mob mob) {
        var config = GentleMobsNeoForge.CONFIG;
        if (mob.level().isClientSide || config == null) return GentleMode.VANILLA;
        var s = config.get();
        return BehaviorPolicy.mode(id(mob), s.mode(), s.overrides());
    }
    public static boolean engaged(Mob mob) {
        if (mode(mob) != GentleMode.NEUTRAL) return false;
        State s = STATES.get(mob);
        Player opponent = s == null ? null : validPlayer(mob, s.opponent);
        return s != null
            && s.revision == GentleMobsNeoForge.CONFIG.revision()
            && mob.level().getGameTime() < s.combatUntil && opponent != null && !opponent.isCreative();
    }
    public static boolean blocksPlayers(Mob mob) { return BehaviorPolicy.blocksPlayers(mode(mob), engaged(mob)); }
    public static boolean blocksTarget(Mob mob, Entity target) { return target instanceof Player && blocksPlayers(mob); }
    public static boolean fleeing(Mob mob) {
        if (mode(mob) != GentleMode.PASSIVE) return false;
        State s = STATES.get(mob);
        return s != null
            && s.revision == GentleMobsNeoForge.CONFIG.revision()
            && mob.level().getGameTime() < s.fleeUntil && validPlayer(mob, s.opponent) != null;
    }
    private static Player validPlayer(Mob mob, UUID id) {
        if (id == null || !(mob.level() instanceof ServerLevel level)) return null;
        Player player = level.getPlayerByUUID(id);
        // Creative attackers still trigger PASSIVE fleeing. Combat eligibility is checked separately.
        return player != null && player.isAlive() && !player.isSpectator() ? player : null;
    }
    public static void hit(Mob mob, Player player) {
        GentleMode mode = mode(mob);
        if (mode == GentleMode.VANILLA) return;
        State s = STATES.computeIfAbsent(mob, m -> new State());
        s.revision = GentleMobsNeoForge.CONFIG.revision();
        s.opponent = player.getUUID();
        s.hitAt = mob.level().getGameTime();
        if (mode == GentleMode.NEUTRAL) {
            s.combatUntil = s.hitAt + GentleMobsNeoForge.CONFIG.get().neutralTimeout();
            s.fleeUntil = 0;
            // Mark combat before setting targets; event filters now permit vanilla retaliation.
            mob.setTarget(player);
        } else {
            s.combatUntil = 0;
            s.fleeUntil = s.hitAt + GentleMobsNeoForge.CONFIG.get().duration();
            clearPlayerCombat(mob);
        }
    }
    public static void beforeTick(Mob mob) {
        GentleMode mode = mode(mob);
        State s = STATES.get(mob);
        if (mode == GentleMode.VANILLA) {
            if (s != null) { s.opponent = null; s.combatUntil = 0; s.fleeUntil = 0; }
            return;
        }
        if (s == null) { s = new State(); STATES.put(mob, s); }
        if (!s.goalInstalled) {
            if (mob.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof FleeGoal))
                mob.goalSelector.addGoal(-1, new FleeGoal(mob));
            s.goalInstalled = true;
        }
        if (s.revision != GentleMobsNeoForge.CONFIG.revision()) {
            s.combatUntil = 0; s.fleeUntil = 0;
            s.revision = GentleMobsNeoForge.CONFIG.revision();
        }
        if (s.combatUntil > 0) {
            Player player = validPlayer(mob, s.opponent);
            long now = mob.level().getGameTime();
            LivingEntity brainTarget = memory(mob.getBrain(), MemoryModuleType.ATTACK_TARGET).orElse(null);
            // Bosses can fight without Mob.target. Ordinary mobs also calm when vanilla drops combat.
            boolean dropped = !(mob instanceof EnderDragon) && !(mob instanceof WitherBoss) && !(mob instanceof Warden)
                && now - s.hitAt > 40 && !(mob.getTarget() instanceof Player) && !(brainTarget instanceof Player);
            if (now >= s.combatUntil || player == null || player.isCreative() || player.distanceToSqr(mob) > 128 * 128 || dropped) {
                s.combatUntil = 0;
                if (mob instanceof Warden warden && player != null) warden.clearAnger(player);
            }
        }
        if (blocksPlayers(mob)) clearPlayerCombat(mob);
        if (mob instanceof EnderDragon dragon && blocksPlayers(mob)) {
            var phase = dragon.getPhaseManager().getCurrentPhase().getPhase();
            if (attackPhase(phase) || fleeing(mob) && phase != EnderDragonPhase.DYING && phase != EnderDragonPhase.HOLDING_PATTERN)
                dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }
    }
    public static void afterTick(Mob mob) {
        if (mode(mob) == GentleMode.VANILLA) return;
        if (blocksPlayers(mob)) clearPlayerCombat(mob);
        // Brain behaviors can replace paths after goals run; reassert the escape route afterwards.
        if (fleeing(mob)) fleeStep(mob);
    }
    public static void forget(Mob mob) { STATES.remove(mob); }
    public static void clear() { STATES.clear(); }
    public static boolean attackPhase(EnderDragonPhase<?> phase) {
        return phase == EnderDragonPhase.STRAFE_PLAYER || phase == EnderDragonPhase.SITTING_FLAMING
            || phase == EnderDragonPhase.SITTING_SCANNING || phase == EnderDragonPhase.SITTING_ATTACKING
            || phase == EnderDragonPhase.CHARGING_PLAYER;
    }
    public static <T> Optional<T> memory(Brain<?> brain, MemoryModuleType<T> type) {
        return brain.checkMemory(type, MemoryStatus.REGISTERED) ? brain.getMemory(type) : Optional.empty();
    }
    public static void clearPlayerCombat(Mob mob) {
        if (mob.getTarget() instanceof Player) { mob.setTarget(null); mob.setAggressive(false); }
        if (mob.getLastHurtByMob() instanceof Player) mob.setLastHurtByMob(null);
        Brain<?> brain = mob.getBrain();
        if (memory(brain, MemoryModuleType.ATTACK_TARGET).orElse(null) instanceof Player) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            mob.setAggressive(false);
        }
        if (memory(brain, MemoryModuleType.HURT_BY_ENTITY).orElse(null) instanceof Player) {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY); brain.eraseMemory(MemoryModuleType.HURT_BY);
        }
        UUID anger = memory(brain, MemoryModuleType.ANGRY_AT).orElse(null);
        State s = STATES.get(mob);
        if (anger != null && (mob.level().getPlayerByUUID(anger) != null || s != null && anger.equals(s.opponent)))
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        if (mob instanceof NeutralMob neutral && neutral.getPersistentAngerTarget() != null) {
            UUID target = neutral.getPersistentAngerTarget();
            if (mob.level().getPlayerByUUID(target) != null || s != null && target.equals(s.opponent)) neutral.stopBeingAngry();
        }
        if (mob instanceof WitherBoss wither) {
            for (int i = 0; i < 3; i++)
                if (mob.level().getEntity(wither.getAlternativeTarget(i)) instanceof Player) wither.setAlternativeTarget(i, 0);
        }
    }
    public static Vec3 fleeDestination(Mob mob) {
        State s = STATES.get(mob);
        Player player = s == null ? null : validPlayer(mob, s.opponent);
        if (player == null) return null;
        Vec3 away = mob.position().subtract(player.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        return mob.position().add(away.normalize().scale(GentleMobsNeoForge.CONFIG.get().distance()));
    }
    private static void fleeStep(Mob mob) {
        Vec3 destination = fleeDestination(mob);
        if (destination == null || mob instanceof EnderDragon) return;
        double speed = GentleMobsNeoForge.CONFIG.get().speed();
        if (mob instanceof Ghast || mob instanceof Phantom || mob instanceof Vex || mob instanceof WitherBoss || mob instanceof Blaze) {
            Vec3 away = destination.subtract(mob.position()).normalize().scale(0.08 * speed);
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.8).add(away));
            mob.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, speed);
        } else if (mob instanceof Slime) {
            Vec3 away = destination.subtract(mob.position()).normalize().scale(0.15 * speed);
            mob.setDeltaMovement(away.x, mob.onGround() ? 0.42 : mob.getDeltaMovement().y, away.z);
        } else if (mob.tickCount % 5 == 0) {
            Vec3 path = mob instanceof PathfinderMob pathfinder ? DefaultRandomPos.getPosAway(pathfinder,
                (int)Math.ceil(GentleMobsNeoForge.CONFIG.get().distance()), 7, validPlayer(mob, STATES.get(mob).opponent).position()) : null;
            if (path == null) path = destination;
            mob.getNavigation().moveTo(path.x, path.y, path.z, speed);
        }
    }
    private static final class FleeGoal extends Goal {
        private final Mob mob;
        FleeGoal(Mob mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return fleeing(mob); }
        @Override public boolean canContinueToUse() { return fleeing(mob); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { mob.getNavigation().stop(); }
        // EntityTick.Post applies movement once, after both goal and brain AI have run.
        @Override public void tick() {}
        @Override public void stop() { mob.getNavigation().stop(); }
    }
}
