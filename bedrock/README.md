# GentleMobs for Bedrock — mob preview

**0.2.0 development preview**, targeting **Bedrock 26.40+**, using stable
`@minecraft/server` 2.0.0. This is an expanded test build, not a complete port or
a Marketplace release. Paper, Fabric and NeoForge builds remain unchanged.

## Included mobs

The explicit allowlist in `mobs.json` contains **38 Bedrock entity types**:

| Group | Included |
| --- | --- |
| Zombies | Zombie, husk, drowned, both Bedrock zombie-villager definitions |
| Skeletons | Skeleton, stray, bogged, parched, wither skeleton |
| Other ground mobs | Creeper, spider, cave spider, silverfish, endermite, Enderman, slime, magma cube |
| Nether mobs | Piglin, piglin brute, zombified piglin (`zombie_pigman`), hoglin, zoglin |
| Raiders | Pillager, vindicator, evoker (`evocation_illager`), ravager, witch |
| Flying and aquatic | Blaze, ghast, phantom, vex, guardian, elder guardian |
| Special | Breeze, shulker, Warden, Creaking |

Creaking defaults to **VANILLA**, matching the existing Paper configuration.
An explicit `creaking` override enables its experimental handling. All other
listed mobs follow the global mode unless overridden. Other add-on entities,
passive animals and unlisted Minecraft IDs are not modified.

**Wither and Ender Dragon are not supported yet and remain vanilla.** Their
engine-controlled boss attacks need a separate implementation and validation.
Their reviewed definitions are retained in `vendor/` but are not packaged as
entity overrides. Java-only giants/illusioners have no Bedrock equivalent here.
Inclusion in this list does not mean a mob has passed gameplay testing.

## Modes and commands

| Mode | Intended behavior toward players |
| --- | --- |
| PASSIVE | Ignores players; flees after a player hit for 60 ticks (3 seconds at 20 TPS). |
| NEUTRAL | Ignores players until attacked; permits retaliation for 600 ticks (30 seconds at 20 TPS). Another player hit refreshes that period. |
| VANILLA | Allows normal targeting and combat. |

Configuration persists in the world, including overrides from the 0.1.0 zombie
prototype. Changing modes or loading an entity clears temporary combat/flee
state. NEUTRAL permits targeting nearby eligible players, not exclusively the
attacker. Non-player targets and interactions retain vanilla behavior except
where special handling below affects them.

Run these separately, with cheats enabled and operator permissions:

```mcfunction
/scriptevent gentlemobs:mode PASSIVE
/scriptevent gentlemobs:mode NEUTRAL
/scriptevent gentlemobs:mode VANILLA
/scriptevent gentlemobs:override creeper PASSIVE
/scriptevent gentlemobs:override creeper CLEAR
/scriptevent gentlemobs:status
/scriptevent gentlemobs:status minecraft:skeleton
/scriptevent gentlemobs:list
```

Short Bedrock names or full `minecraft:` IDs work. Modes are case-insensitive.
Unknown IDs are rejected. The list command shows exact IDs; use
`evocation_illager`, not Java's `evoker`. Clearing the Creaking override restores
its VANILLA default.

## Install or update

Installable file: `dist/GentleMobs-Bedrock-0.2.0-preview.mcpack`.

1. Save and leave your test world before updating.
2. Double-click the `.mcpack` to import it, or use `npm run deploy` for a local
   development copy. Avoid importing a second copy alongside the development pack.
3. In world settings, activate **GentleMobs - Mob Preview** under **Behavior Packs**.
   It retains the 0.1.0 pack UUID and saved settings. If Bedrock still shows the
   old pack/version, restart the game and reactivate it.
4. Use **Easy, Normal or Hard** difficulty. Peaceful still applies vanilla
   spawning/despawning rules. No resource pack or experiments are required.
5. Enable cheats for commands and use Survival for targeting tests. An old test
   world may still have global VANILLA selected: explicitly set PASSIVE.

For a dedicated Bedrock server, copy `dist/GentleMobs_BP` to `behavior_packs`,
then add/update this entry in the desired world's `world_behavior_packs.json`
array while the server is stopped. Preserve other pack entries:

```json
{
  "pack_id": "5c385238-6e64-43d2-bcde-105360b7d06b",
  "version": [0, 2, 0]
}
```

Dedicated servers, console/Realm play and multiplayer have not yet been tested.

## Special handling and limits

- Target filters apply to base components and every native component-group
  variant, including skeleton weapons/difficulty, spiders' daylight states and
  piglin gold/chest targeting. A native sensor clears protected player targets
  acquired through another route, such as anger broadcasts.
- Creepers gate automatic ignition and clear normal/charged automatic fuses when
  calmed. **Intentional flint-and-steel ignition remains vanilla.**
- Slime/magma-cube contact damage and ravager roar damage/knockback filter out
  protected players, separately from ordinary targeting.
- Enderman staring and Warden nuisance tracking use the same mode rules.
  Warden sonic-boom transitions still need gameplay tests; darkness is retained.
  Creaking overrides preserve heart/death events and detect melee hits even when
  health does not decrease; heart-bound projectile hits need testing.
- Walkers/swimmers use native avoidance after a hit, retaining existing avoidance
  of cats/wolves. Flying mobs receive bounded impulse steering away from the
  attacker; native flight can affect the result. Shulkers try a clear supported
  position farther away, and stay put if none is found.
- Guardian spikes and elder-guardian mining fatigue are native side effects and
  are **not suppressed by this preview**. Guardians are not yet fully equivalent
  to the plugin's PASSIVE behavior.
- Already-fired projectiles and lingering effects are not erased by mode changes.
- Wither/Ender Dragon support and alternate progression recipes remain unfinished.
- Another pack overriding the same vanilla mob definition can conflict. Entity
  definitions do not merge automatically.

## Build and validation

With Node.js and npm installed:

```powershell
cd G:\Projects\GentleMobs\bedrock
npm.cmd ci
npm.cmd run build
npm.cmd run deploy
```

`build` type-checks, runs 49 automated checks, bundles scripts, generates the
allowlisted definitions and creates the `.mcpack`. Tests check mode logic,
script events/timing with a mocked API, special filters and preservation of
unrelated vanilla data for every included definition, such as loot, equipment,
spawning and transformations. **They do not run Minecraft's AI.** See
[TESTING.md](TESTING.md) for observed gameplay results and the acceptance pass.

The baseline is pinned to Mojang's stable `bedrock-samples v1.26.40.05`; see
[vendor/NOTICE.md](vendor/NOTICE.md). Future Minecraft updates require baseline
review. The minimum version is not a guarantee of future compatibility.
