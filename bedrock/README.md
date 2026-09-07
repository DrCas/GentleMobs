# GentleMobs for Bedrock — zombie prototype

Version **0.1.0 prototype**, targeting **Bedrock 26.40 or newer** with stable
`@minecraft/server` 2.0.0. This is the first development milestone, not the
complete Paper/NeoForge port or a Marketplace release.

Only **`minecraft:zombie`** is included. This means ordinary zombies, babies,
and zombie riders. Husks, drowned, zombie villagers, other hostile mobs, bosses,
and entities from other add-ons are not yet handled.

## Intended behavior

| Mode | Zombie behavior toward players |
| --- | --- |
| PASSIVE (default) | Ignores players. After a player hit, avoids nearby players for 60 ticks (3 seconds at 20 TPS), at 1.3× movement speed. |
| NEUTRAL | Ignores players until hit by one. Player targeting is then allowed for 600 ticks (30 seconds at 20 TPS); another player hit refreshes the timeout. |
| VANILLA | Normal targeting and combat. |

Modes and the zombie override are saved with the world. Changing configuration
clears temporary targets/combat state on loaded zombies. Reloading the world or
an entity also clears temporary combat state. Non-player interactions retain
vanilla behavior, including zombies attacking villagers and golems.

## Install and try it on Windows

1. Double-click `dist/GentleMobs-Bedrock-0.1.0-prototype.mcpack` to import it.
   Developers who used `npm run deploy` already have it under development packs
   and should use that copy instead of importing a second copy with the same UUID.
2. Create a **new test world** and activate **GentleMobs - Zombie Prototype**
   in its Behavior Packs. No resource pack or experimental toggle is required.
3. Use **Easy, Normal or Hard**, with cheats enabled for the prototype commands.
   Peaceful still follows vanilla spawning/despawning rules.
4. Enter the world, then use the commands below. Test combat in Survival;
   Creative players are normally ignored by hostile mobs anyway.

```mcfunction
/scriptevent gentlemobs:status
/scriptevent gentlemobs:mode PASSIVE
/scriptevent gentlemobs:mode NEUTRAL
/scriptevent gentlemobs:mode VANILLA
/scriptevent gentlemobs:override minecraft:zombie NEUTRAL
/scriptevent gentlemobs:override minecraft:zombie CLEAR
```

These use Bedrock's operator/cheat-gated `/scriptevent` command. Mode names are
case-insensitive. Overrides take precedence over the global setting. Unsupported
entity IDs and invalid modes are rejected. A player issuing a command gets a
private response; commands from the server/command blocks report to the log.

For a dedicated Bedrock server, copy the built `dist/GentleMobs_BP` directory
into the server's `behavior_packs`, and add the following object to the desired
world's `world_behavior_packs.json` array while the server is stopped. Preserve
any existing entries. Match the server version to the pack's minimum version.

```json
{
  "pack_id": "5c385238-6e64-43d2-bcde-105360b7d06b",
  "version": [0, 1, 0]
}
```

Dedicated server and console/Realm play have not been tested with this prototype.

## Development

This directory is an independent TypeScript/JSON project in the existing
GentleMobs repository. Paper, Fabric and NeoForge sources/builds are unchanged.
Java's mode names, default timing and override concepts carry over; Minecraft AI
integration must use Bedrock entity definitions and Script API instead of Java.

With Node.js and npm installed:

```powershell
cd G:\Projects\GentleMobs\bedrock
npm.cmd ci
npm.cmd run build
npm.cmd run deploy
```

`build` type-checks the scripts, runs automated tests, bundles JavaScript, generates
the modified entity definition, and creates the `.mcpack` in `dist/`.
`deploy` copies the built pack to the current Windows Bedrock development folder.
You can pass a different `com.mojang` directory using `npm run deploy -- <path>`.
Exit and reopen the test world after deploying changes.

The original Mojang zombie definition is in `vendor/zombie.jsonc`, with provenance
and licensing in [vendor/NOTICE.md](vendor/NOTICE.md). It comes from the stable
`v1.26.40.05` sample release and structurally matches the installed Windows
1.26.4501.0 game's latest zombie definition. The generator adds GentleMobs
properties/events and wraps player-target filters. Original spawn events,
equipment, loot, daylight damage, movement, horse behavior and drowned
transformations remain in the baseline. Future Bedrock updates require a baseline
review; the minimum version alone does not guarantee future compatibility.

## Verification and known differences

Automated checks cover configuration/persistence, unsupported entity exclusion,
target filter logic, preservation of unrelated vanilla data, projectile ownership,
and script event/timer behavior in a mocked Script API. They **do not execute
Minecraft's AI**. An initial Bedrock 26.45 smoke check confirmed pack activation,
the status command and an adult zombie ignoring a Survival player in PASSIVE.
The user also confirmed in-game fleeing after a hit in PASSIVE mode.
NEUTRAL was subsequently reported working by the user; its exact timeout has
not been separately timed.
The user also confirmed unprovoked attacks in VANILLA, completing the basic
adult-zombie behavior checks for all three modes.
Switching an actively attacking zombie from VANILLA to PASSIVE also passed:
the user confirmed that attacks stopped and observed it running away.
Global mode persistence also passed: after leaving and reopening the world,
the user confirmed PASSIVE was retained and the zombie still ignored them.
The remaining in-game acceptance checks are pending; see TESTING.md.

The current differences and limits are:

- Only zombies are included; no alternate Nether Star/Dragon's Breath recipes yet.
- Fleeing uses Bedrock's navigation/avoid-mob goal and avoids nearby players,
  rather than tracking only the attacker. Paths and obstacles determine movement.
- Engaged neutral zombies may target nearby eligible players, not just the attacker.
- A zombie transformed into a drowned resumes vanilla drowned behavior because
  drowned support has not been added yet.
- This pack overrides the vanilla zombie behavior definition. Another pack
  overriding that same definition can conflict; vanilla overrides do not merge.
- It does not keep hostile mobs alive in Peaceful difficulty or replace the
  difficulty system. Other unsupported mobs remain dangerous in this prototype.
- No Marketplace acceptance, complete parity, or broad add-on compatibility is
  claimed. Those are separate milestones after gameplay verification and expansion.

See [TESTING.md](TESTING.md) for the first in-game acceptance pass.
