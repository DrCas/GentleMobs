# Bedrock acceptance tests

## 0.2.0 expanded preview

Build/type-check and **49 automated checks pass**. This version expands to 38
entity types and revises the zombie generator to share the same filters/guards.
The 0.1.0 user-tested results below are historical evidence; new mobs and the
revised zombie still need in-game regression testing.

Windows Bedrock **26.45** smoke check on 2026-09-06: after reactivating the
updated behavior pack, the script replied with global PASSIVE and no overrides.
A creeper summoned two blocks in front of the Survival player did not explode
or damage the player and subsequently wandered away. The user subsequently
confirmed that hitting the creeper in PASSIVE makes it run away. Initial calm
and basic hit/flee checks pass; VANILLA and charged-fuse tests remain pending.
Exact flee duration and repeated hits have not been separately tested.
During the requested NEUTRAL test, the user reported that the creeper explodes
when followed and clarified that NEUTRAL was active. Following the requested
hit, retreat, 35-second wait and reapproach test, the user reported that it
returned to passive behavior. Return to calm in NEUTRAL passes as a user-reported
gameplay check; the exact 600-tick boundary was not measured. Calm behavior
before the first hit and pursuit were not separately confirmed.
The first reload did not respond to script commands; removing and reactivating
the pack in world settings resolved it. A world backup was made through the
game's "Copy and continue" option before reactivation.

Use Normal difficulty and Survival mode. Clear leftover overrides, then set
global PASSIVE. Use night/cover for undead. Test one mob at a time:

1. Zombie regression: PASSIVE hit/flee, NEUTRAL retaliation/30-second calm,
   VANILLA aggression and switching an existing attacker to PASSIVE.
2. Creeper: proximity must not start a fuse in PASSIVE/calm NEUTRAL; a hit in
   PASSIVE should cause retreat. VANILLA should ignite normally. Switch to
   PASSIVE during a fuse, including charged creepers. Flint-and-steel ignition
   intentionally remains vanilla.
3. Skeleton/stray/bogged/parched: shooting in all three modes; repeat with melee
   equipment and different difficulties to exercise component-group transitions.
4. Spiders/Enderman: daylight/night targeting, staring, and retaliation in sunlight.
5. Slime/magma cube: walk directly into different sizes to check contact damage.
6. Nether/raiders: piglin gold/chest/group anger, baby hoglins, zoglins, ravager
   roar, evoker summons, vindicator named Johnny and witch potions.
7. Flying/aquatic/shulker: actual retreat in suitable terrain. Guardian spike
   damage and mining fatigue remain known limitations.
8. Warden: vibration, sniffing, melee, sonic boom, provocation and timeout.
   Creaking: explicitly override it, then test player-spawned/heart-bound variants,
   looking away, melee/projectile hits and heart destruction.
9. Persistence, overrides, transformations, split slimes, chunk reloads, two
   players and separate add-on compatibility tests.

Wither and Ender Dragon remain vanilla and are not supported by this build.

## 0.1.0 zombie prototype results

Status: build/type-check and 8 automated tests pass. Initial Windows Bedrock
**26.45** smoke check on 2026-09-06: pack activated in a new flat test world,
status command returned PASSIVE, and a summoned adult zombie ignored the nearby
Survival player. The user subsequently confirmed that the zombie flees after
being hit in PASSIVE mode: the basic fleeing check passes. The user also reported
that NEUTRAL worked in their in-game test. The exact neutral timeout was not
separately reported or timed. The user confirmed that VANILLA attacks the
Survival player without being hit first. Basic adult-zombie behavior has now
passed in all three modes. The user then confirmed that switching the actively
attacking zombie from VANILLA to PASSIVE stopped its attacks and reported that
it ran away during that test. Clearing an existing attack on mode change passes.
The user also confirmed that after Save & Quit and reopening the world, status
still reported PASSIVE and the zombie continued to ignore them. Global mode
persistence across a world reload passes. During the PASSIVE zombie override /
global VANILLA check, the user confirmed the zombie remained passive and a
separately summoned creeper behaved normally. Zombie override precedence passes;
the creeper observation is an initial scope check, not evidence of compatibility
with all other entities or add-ons. The user then confirmed that clearing the
zombie override restored aggression with global VANILLA selected: override
clearing passes. Override persistence, exact durations, repeat hits, projectile
hits, chunk reloads, variants and multiplayer still need the acceptance pass below.

Use a new world, Normal difficulty, cheats enabled, and only the GentleMobs
prototype behavior pack. Enable the game's content log in Creator settings to
see loading/script errors. Keep a copy of any world you later test with other packs.

## Set up

In Creative, make a flat, covered arena at least 24 blocks across so zombies have
room to flee and cannot burn. Keep villagers/golems and other players away from
the initial test. Switch to Survival for targeting checks. Give yourself food and
armor first. Summon a fresh zombie for each separate test.

```mcfunction
/difficulty normal
/gamerule doDaylightCycle false
/time set midnight
/scriptevent gentlemobs:override minecraft:zombie CLEAR
/scriptevent gentlemobs:mode PASSIVE
/summon minecraft:zombie ~ ~ ~5
/gamemode survival
```

## Checks

1. **PASSIVE:** Stand nearby for at least 10 seconds without attacking. The zombie
   must not pursue or damage you. Punch it once. It should flee without retaliating,
   then settle after roughly 3 seconds. Repeat with a bow and a baby zombie.
2. **NEUTRAL:** Set NEUTRAL. A fresh zombie must initially ignore you. Hit it once;
   it should pursue and be able to damage you. Keep it alive and reachable without
   hitting it again. After roughly 30 seconds it must stop targeting/damaging you.
3. **VANILLA:** Set VANILLA. A fresh zombie must pursue and damage a Survival player
   without being hit first. While it pursues, switch to PASSIVE; it must stop.
4. **Override:** Set global VANILLA, then zombie override PASSIVE. Zombies must remain
   passive. Clear the override; they must return to vanilla aggression.
5. **Persistence/load:** Set NEUTRAL globally and PASSIVE for zombies. Save, leave and
   reopen the world. `gentlemobs:status` must retain both settings. Recheck behavior.
   Also unload/reload a fleeing or engaged zombie's chunk: it should return calm.
6. **Vanilla preservation:** Check baby/adult spawns, equipment, XP/drops, daylight
   burning and underwater conversion. Drowned are deliberately unsupported and
   become dangerous after conversion. Check the content log for schema errors.
7. **Scope:** A skeleton must remain hostile in PASSIVE. Test with a separately
   installed custom entity add-on later; GentleMobs should not touch its entities.
8. **Multiplayer:** Later, repeat modes with two Survival players. Document which
   player an engaged zombie chooses and whether fleeing works with both nearby.

Record game version, pack version, passed/failed checks and relevant content-log
errors here before calling the prototype gameplay-verified. Test villagers/golems,
zombie horse riders, restricted terrain and pack-load conflicts separately before
expanding to further mob types.
