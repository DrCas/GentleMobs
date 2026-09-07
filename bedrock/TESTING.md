# Zombie prototype acceptance pass

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
Exact durations, repeat hits, projectile hits, persistence, variants and
multiplayer still need the acceptance pass below.

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
