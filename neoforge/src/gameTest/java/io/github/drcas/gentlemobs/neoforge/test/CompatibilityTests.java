package io.github.drcas.gentlemobs.neoforge.test;

import com.mojang.authlib.GameProfile;
import io.github.drcas.gentlemobs.GentleMode;
import io.github.drcas.gentlemobs.neoforge.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.registries.RegisterEvent;

@GameTestHolder("gentlemobs")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "gentlemobs", bus = EventBusSubscriber.Bus.MOD)
public final class CompatibilityTests {
    private static EntityType<Zombie> MODDED;
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            MODDED = EntityType.Builder.<Zombie>of(Zombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("gentlemobs_test:monster");
            helper.register(ResourceLocation.parse("gentlemobs_test:monster"), MODDED);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) { event.put(MODDED, Zombie.createAttributes().build()); }

    @GameTest(template = "test_arena", timeoutTicks = 180)
    public static void behaviorAndCompatibility(GameTestHelper h) throws Exception {
        GentleConfig original = GentleMobsNeoForge.CONFIG;
        Path configPath = Path.of("gentlemobs-gametest.json").toAbsolutePath();
        Files.writeString(configPath, "{\"mode\":\"PASSIVE\",\"neutral-timeout-ticks\":20}");
        GentleMobsNeoForge.CONFIG = new GentleConfig(configPath);
        GentleMobsNeoForge.CONFIG.load();
        var level = h.getLevel();
        var server = level.getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "GentleMobsTest"), false);
        var player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(new Vec3(8, 2, 8));
        player.teleportTo(pos.x, pos.y, pos.z);

        Zombie zombie = h.spawn(EntityType.ZOMBIE, 9, 2, 8);
        zombie.setPersistenceRequired(); zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        zombie.setTarget(player);
        h.assertTrue(zombie.getTarget() == null && !zombie.canAttack(player), "PASSIVE zombie acquired a player");
        zombie.hurt(level.damageSources().playerAttack(player), 1);
        h.assertTrue(BehaviorEngine.fleeing(zombie), "Actual player damage did not start fleeing");
        h.assertTrue(zombie.getTarget() == null, "PASSIVE zombie retaliated");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        zombie.invulnerableTime = 0;
        zombie.hurt(level.damageSources().playerAttack(player), 1);
        h.assertTrue(BehaviorEngine.fleeing(zombie), "Creative player damage did not activate PASSIVE flee state");
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);

        var modded = h.spawn(MODDED, 2, 2, 2); modded.setNoAi(true);
        modded.setTarget(player);
        h.assertTrue(modded.getTarget() == player && BehaviorEngine.mode(modded) == GentleMode.VANILLA, "Modded Monster was modified by default");
        modded.hurt(level.damageSources().playerAttack(player), 1);
        h.assertTrue(!BehaviorEngine.fleeing(modded), "Attacking an unconfigured modded monster started fleeing");
        var cow = h.spawn(EntityType.COW, 3, 2, 2);
        h.assertTrue(BehaviorEngine.mode(cow) == GentleMode.VANILLA, "Vanilla animal affected");
        GentleMobsNeoForge.CONFIG.override("gentlemobs_test:monster", GentleMode.PASSIVE);
        BehaviorEngine.beforeTick(modded);
        h.assertTrue(modded.getTarget() == null, "Explicit modded opt-in failed");
        GentleMobsNeoForge.CONFIG.override("gentlemobs_test:monster", null);
        modded.setTarget(player);
        h.assertTrue(modded.getTarget() == player, "Removing modded override did not restore vanilla");

        var wither = h.spawn(EntityType.WITHER, 2, 5, 2); wither.setNoAi(true);
        wither.setAlternativeTarget(1, player.getId());
        h.assertTrue(wither.getAlternativeTarget(1) == 0, "Wither side head targeted player");
        var warden = h.spawn(EntityType.WARDEN, 4, 2, 2); warden.setNoAi(true);
        h.assertTrue(!warden.canTargetEntity(player), "Warden can become angry at idle player");
        var ghast = h.spawn(EntityType.GHAST, 4, 5, 4); ghast.setNoAi(true); ghast.setTarget(player);
        h.assertTrue(ghast.getTarget() == null, "Ghast targeted a player");
        var zoglin = h.spawn(EntityType.ZOGLIN, 5, 2, 2); zoglin.setNoAi(true);
        zoglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
        BehaviorEngine.beforeTick(zoglin);
        h.assertTrue(!zoglin.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && !zoglin.canAttack(player), "Zoglin brain retained player");
        var hoglin = h.spawn(EntityType.HOGLIN, 6, 2, 2); hoglin.setNoAi(true);
        var piglin = h.spawn(EntityType.PIGLIN, 7, 2, 2); piglin.setNoAi(true);
        var brute = h.spawn(EntityType.PIGLIN_BRUTE, 8, 2, 2); brute.setNoAi(true);
        // Constructing these brain entities also validates their AI mixins at runtime.
        h.assertTrue(!hoglin.canAttack(player) && !piglin.canAttack(player) && !brute.canAttack(player), "Brain mob targeting escaped policy");
        var dragon = h.spawn(EntityType.ENDER_DRAGON, 12, 7, 12); dragon.setNoAi(true);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
        h.assertTrue(dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.HOLDING_PATTERN, "Dragon attack phase allowed");
        float health = player.getHealth();
        player.hurt(level.damageSources().mobAttack(wither), 4);
        h.assertTrue(player.getHealth() == health, "Passive boss damage reached player");

        GentleMobsNeoForge.CONFIG.override("minecraft:zoglin", GentleMode.NEUTRAL);
        zoglin.hurt(level.damageSources().playerAttack(player), 1);
        h.assertTrue(BehaviorEngine.engaged(zoglin) && zoglin.canAttack(player), "NEUTRAL retaliation did not activate");
        GentleMobsNeoForge.CONFIG.override("minecraft:ghast", GentleMode.VANILLA);
        ghast.setTarget(player);
        h.assertTrue(ghast.getTarget() == player, "VANILLA override did not restore targeting");

        var dispatcher = server.getCommands().getDispatcher();
        var parsed = dispatcher.parse("gentlemobs override add minecraft:skeleton NEUTRAL", server.createCommandSourceStack());
        h.assertTrue(!parsed.getReader().canRead(), "Namespaced command failed to parse");
        dispatcher.execute(parsed);
        h.assertTrue(GentleMobsNeoForge.CONFIG.get().overrides().get("minecraft:skeleton") == GentleMode.NEUTRAL, "Override command failed");
        // Revision changes intentionally clear prior engagements; initiate after final config mutation.
        zoglin.invulnerableTime = 0;
        zoglin.hurt(level.damageSources().playerAttack(player), 1);
        zombie.invulnerableTime = 0;
        zombie.hurt(level.damageSources().playerAttack(player), 1);
        double startDistance = zombie.distanceToSqr(player);
        verifyRecipes(h);
        h.runAfterDelay(30, () -> {
            h.assertTrue(!BehaviorEngine.engaged(zoglin) && !zoglin.canAttack(player), "NEUTRAL did not time out");
            h.assertTrue(zombie.distanceToSqr(player) > startDistance, "Passive zombie did not move away");
            h.assertTrue(!BehaviorEngine.engaged(modded) && BehaviorEngine.mode(modded) == GentleMode.VANILLA, "Modded entity state changed");
            for (var mob : List.of(zombie, modded, cow, wither, warden, ghast, zoglin, hoglin, piglin, brute, dragon)) mob.discard();
            server.getPlayerList().remove(player);
            reloadRecipes(h, false, () -> {
                h.assertTrue(level.getRecipeManager().byKey(ResourceLocation.parse("gentlemobs:nether_star")).isEmpty(), "Disabled star recipe still loaded");
                h.assertTrue(level.getRecipeManager().byKey(ResourceLocation.parse("gentlemobs:dragons_breath")).isEmpty(), "Disabled breath recipe still loaded");
                reloadRecipes(h, true, () -> {
                    verifyRecipes(h);
                    GentleMobsNeoForge.CONFIG = original;
                    BehaviorEngine.clear();
                    h.succeed();
                });
            });
        });
    }
    private static void reloadRecipes(GameTestHelper h, boolean enabled, Runnable after) {
        try {
            Path path = Path.of("gentlemobs-gametest.json").toAbsolutePath();
            Files.writeString(path, "{\"recipes\":{\"nether-star\":{\"enabled\":" + enabled + "},\"dragons-breath\":{\"enabled\":" + enabled + "}}}");
            GentleMobsNeoForge.CONFIG.load();
            var server = h.getLevel().getServer();
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenCompleteAsync((unused, error) ->
                h.runAfterDelay(1, () -> { h.assertTrue(error == null, "Recipe reload failed: " + error); after.run(); }), server);
        } catch (Exception ex) { h.fail("Recipe config update failed: " + ex); }
    }
    private static void verifyRecipes(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        var star = manager.byKey(ResourceLocation.parse("gentlemobs:nether_star"));
        var breath = manager.byKey(ResourceLocation.parse("gentlemobs:dragons_breath"));
        h.assertTrue(star.isPresent() && breath.isPresent(), "Progression recipes failed to load");
        CraftingInput starInput = CraftingInput.of(3, 3, List.of(
            new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.WITHER_SKELETON_SKULL), new ItemStack(Items.NETHERITE_INGOT),
            new ItemStack(Items.WITHER_SKELETON_SKULL), new ItemStack(Items.SOUL_SAND), new ItemStack(Items.WITHER_SKELETON_SKULL),
            new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.WITHER_SKELETON_SKULL), new ItemStack(Items.NETHERITE_INGOT)));
        h.assertTrue(((CraftingRecipe)star.orElseThrow().value()).matches(starInput, h.getLevel()), "Original Nether Star pattern did not craft");
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.MUNDANE));
        CraftingInput input = CraftingInput.of(2, 2, List.of(new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.POPPED_CHORUS_FRUIT), potion, ItemStack.EMPTY));
        CraftingRecipe recipe = (CraftingRecipe)breath.orElseThrow().value();
        h.assertTrue(recipe.matches(input, h.getLevel()), "Mundane potion recipe does not craft");
        h.assertTrue(recipe.assemble(input, h.getLevel().registryAccess()).is(Items.DRAGON_BREATH), "Wrong breath result");
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        h.assertTrue(!recipe.matches(input, h.getLevel()), "Water potion incorrectly accepted");
    }
}
