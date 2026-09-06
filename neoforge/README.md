# GentleMobs for NeoForge 1.21.1

## Install on a server

1. Use a Minecraft **1.21.1** server with **NeoForge 21.1.249 or a newer 21.1.x patch** and **Java 21**. This release is built and integration-tested against 21.1.249, with additional live singleplayer checks on 21.1.250. Keep the server and players on the modpack's matching Minecraft/NeoForge versions.
2. Stop the server.
3. Copy `GentleMobs-NeoForge-1.21.1-1.0.0.jar` from `neoforge/build/libs/` into the server's `mods/` directory. Use only the NeoForge jar for this server.
4. Start the server. GentleMobs creates `config/gentlemobs.json` beside the other server configs.
5. As an operator, run `/gentlemobs version`, then `/gentlemobs mode` to verify the installation. Default behavior is PASSIVE.

For All the Mons, use its normal NeoForge server pack and add the jar to that pack's `mods/` folder. Check its NeoForge version against the requirement above. GentleMobs adds no blocks, items, entity types or custom network payloads in production; the mod is designed to be server-only and does not require a GentleMobs installation on players' NeoForge clients. It can also be installed in a client modpack for singleplayer; behavior changes run only on the logical server.

Do not install this mod in Paper's `plugins/` folder. The existing Paper plugin and its configuration are independent.

## Configuration

```json
{
  "mode": "PASSIVE",
  "flee": {
    "distance": 12.0,
    "speed": 1.3,
    "duration-ticks": 60
  },
  "neutral-timeout-ticks": 600,
  "mob-overrides": {},
  "recipes": {
    "nether-star": { "enabled": true },
    "dragons-breath": { "enabled": true }
  }
}
```

The familiar Paper configuration concepts are retained in JSON. Paper's YAML file is not imported automatically. Mode names are case-insensitive. In the config, Bukkit-style keys such as `ZOMBIE` normalize to `minecraft:zombie`; namespaced IDs are recommended.

At 20 ticks/second, default fleeing lasts 3 seconds and neutral combat expires 30 seconds after the last player hit. Flee distance accepts 1–64 blocks, speed 0.1–4, duration 1–12,000 ticks, and neutral timeout 20–72,000 ticks. Invalid settings reject the whole reload and leave the previous configuration active. An invalid startup config produces an explicit load error instead of silently changing behavior. Unknown entity IDs are logged and remain inactive until that entity exists.

Example per-mob overrides:

```json
"mob-overrides": {
  "minecraft:zombie": "NEUTRAL",
  "minecraft:wither": "VANILLA",
  "minecraft:ender_dragon": "VANILLA"
}
```

Run `/gentlemobs reload` after editing the file. Behavior settings apply immediately; this command also reloads server datapacks so recipe toggles and recipe-book unlocks update. Mode and override commands save their changes to disk. Config changes clear previous flee/combat sessions on the next relevant tick. Engagement is not persisted across chunk unload, server restart or dimension changes.

## Modpack compatibility boundary

Default scope is a fixed list of **39 vanilla hostile entity IDs**, including vanilla conditionally hostile mobs such as Endermen, Piglins and Zombified Piglins. It does not infer eligibility from `Monster`, `Enemy`, `MobCategory.MONSTER`, or `Mob` inheritance. The complete list is in `BehaviorPolicy.VANILLA_HOSTILES`.

`cobblemon:pokemon` and every other modded entity ID resolve to VANILLA by default. Normal vanilla animals, wolves and golems are also excluded. The server tick handlers do not install goals or allocate combat state for excluded mobs. Shared targeting/damage hooks check the same registry-ID policy before changing anything.

To intentionally opt a modded mob in, add its exact registered ID to `mob-overrides`, for example `"examplemod:guard": "NEUTRAL"`. Only that ID is opted in. Removing the override excludes it again. No namespace wildcard or class-wide option is provided. Explicitly opting in a custom mob uses the generic targeting/fleeing integration; unusual custom AI may require dedicated future support. There is no dedicated Cobblemon integration, and none is needed to exclude its Pokémon by default.

GentleMobs still modifies vanilla mobs if another mod changes their AI while retaining their vanilla registry ID. Other AI mods or server datapacks can conflict with those modifications; full All the Mons/Cobblemon runtime testing has not been performed.

## Commands

All commands require permission level 2 (operator), and are available from the server console except `inspect`.

```text
/gentlemobs help
/gentlemobs version
/gentlemobs mode [PASSIVE|NEUTRAL|VANILLA]
/gentlemobs reload
/gentlemobs inspect
/gentlemobs recipes
/gentlemobs override add minecraft:zombie NEUTRAL
/gentlemobs override remove minecraft:zombie
/gentlemobs override list
```

Command mob arguments use lowercase Minecraft resource IDs; `zombie` also means `minecraft:zombie`. Tab completion includes installed entity IDs. `inspect` looks up to 32 blocks along the player's sight line, respecting blocks, and reports registry ID, mode, default scope, override, combat and fleeing state.

## Behavior and differences from Paper

| Case | NeoForge implementation |
| --- | --- |
| PASSIVE | Blocks player targeting, clears player combat/anger memories, and starts a temporary escape response after player damage. Other non-player targets are preserved where practical. |
| NEUTRAL | Blocks unprovoked player targeting; after an actual player hit, vanilla combat is permitted. Like Paper's general listener, an engaged mob can follow normal vanilla target selection, including other players. Ordinary mobs calm after vanilla loses the player target, with a short acquisition grace period. All mobs also calm on the configurable timeout, player death/logout/dimension change, or separation over 128 blocks. |
| VANILLA | No target, damage, anger, phase or flee modification for that mob. |
| Wither | Filters all three head targets individually and clears existing player head targets. Flight escape uses movement control/velocity. Normal attacks on non-players and random skulls remain possible. |
| Ender Dragon | Replaces player attack phases with holding pattern; passive hits provide an escape flight destination. The death phase remains intact. Bosses use the neutral timeout because they do not reliably maintain ordinary Mob targets. |
| Warden | Rejects players in native anger/target eligibility while passive or idle neutral; clears player combat memories. Hit-driven neutral combat permits vanilla anger. Ambient darkness, emerging and digging remain vanilla. |
| Ghast | Standard player target filtering plus flight escape control; does not depend on ground navigation. |
| Zoglin | Filters attack eligibility and NeoForge brain target events, and clears player combat memories before/after ticks. |
| Hoglin / Piglin / Piglin Brute | Native target-selection filters complement brain cleanup and target events. Bartering and non-player interactions are preserved. |
| Slime / Magma Cube | Blocks owner-attributed contact damage to players while passive/idle neutral; fleeing uses horizontal movement and jumps. |
| Creaking | Not present in vanilla Minecraft 1.21.1. No 1.21.4+ class is referenced. A modded Creaking backport remains excluded unless its own ID is explicitly configured. |

Ground mobs use native pathfinding with reachable escape candidates. Flying mobs use flight controls, and the Dragon uses its phase flight destination; these cannot exactly reproduce Paper's `Pathfinder.moveTo` speed/path semantics. Blocked terrain can prevent escape. Immobile Shulkers retain their native teleport-on-hit behavior rather than gaining ground movement. Fleeing does not disable all AI or make the entity invulnerable.

Player-owned projectile hits count as player attacks in NeoForge. Paper's existing flee listener only checked direct Player damagers. Owner-attributed outgoing damage is blocked for passive/idle-neutral mobs, covering projectiles, explosions, sonic boom and contact damage that can bypass normal targeting. This is additional protection relative to the original Paper listeners. It is not a general environment-protection mod: body pushing, terrain damage, Wither spawn explosions, lingering fire or effects without a mob owner can still have indirect consequences.

## Alternate recipes

The original ingredients, arrangement and output counts are preserved:

- **Nether Star ×1:** `NWN / WSW / NWN`, with `N` = Netherite Ingot, `W` = Wither Skeleton Skull, `S` = Soul Sand. Total: four ingots, four skulls and one soul sand.
- **Dragon's Breath ×1:** shapeless Ender Pearl + Popped Chorus Fruit + **Mundane Potion**. A water bottle, other potion, splash potion or potion with different components does not substitute for the mundane potion.

Recipes are datapack resources with NeoForge load conditions driven by the two config toggles. Recipe-book unlocks are provided when acquiring Netherite Ingots or Ender Pearls. Operators may also use `/recipe give @s gentlemobs:nether_star` and `/recipe give @s gentlemobs:dragons_breath` when those recipes are enabled. Existing modpack datapacks can replace the recipes by ID.

Live testing on NeoForge 21.1.250 confirmed both recipes can be crafted and collected. The Dragon's Breath recipe-book button fills the pearl and chorus fruit but does not automatically place the component-specific Mundane Potion. Place that potion into the crafting grid manually to complete the recipe.

Version `1.0.0` includes the fix for PASSIVE fleeing after damage from Creative players. Creative players remain excluded from NEUTRAL combat engagement. The fix is covered by a regression test and was verified in a running Minecraft client.

## Developer references

- [NeoForge 1.21.1 getting started / Java 21](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [Official 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)

Minecraft 1.21.1 and NeoForge 21.1.249 generated sources were inspected for the specific AI, damage, recipe and phase hooks. The implementation deliberately avoids Bukkit reflection and Fabric's 26.2 method signatures.
