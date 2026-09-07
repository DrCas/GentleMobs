# GentleMobs

GentleMobs supports PASSIVE, NEUTRAL and VANILLA hostile-mob behavior, per-mob overrides, and optional alternate progression recipes.

## Builds in this repository

| Platform | Build entry point | Sources | Target |
| --- | --- | --- | --- |
| Fabric (existing) | Root `build.gradle.kts` | `src/fabricMain` | Existing Minecraft 26.2 build |
| Paper (existing sources) | `paper/build.gradle.kts` | `src/main` | Existing Minecraft 26.2 build |
| NeoForge (new) | `neoforge/build.gradle` | `neoforge/src/main` | Minecraft 1.21.1 / NeoForge 21.1.249 |
| Bedrock (preview) | `bedrock/package.json` | `bedrock/src`, `bedrock/vendor` | Bedrock 26.40+; 38 vanilla entity types |

The Paper and Fabric builds target Minecraft **26.2**. The `paper/` entry point compiles the existing Paper sources in place with their original dependency and Java version. The NeoForge build targets Minecraft **1.21.1** independently.

Independent Gradle projects keep Fabric's Java 25/Loom setup separate from NeoForge's Java 21/ModDevGradle setup. Open or link `neoforge/build.gradle` as another Gradle project in IntelliJ IDEA. Link `paper/build.gradle.kts` for the Paper build. There is no need to move the original source tree or change branches.

The NeoForge build reuses the original `src/main/java/io/github/drcas/gentlemobs/GentleMode.java` directly through a narrowly filtered source set. Its registry-ID policy and validated configuration are independent of Minecraft AI APIs; event, navigation and boss integration live separately. Sharing the existing Paper listeners would require adapting Bukkit/NMS types and would introduce unnecessary risk, so those listeners remain unchanged.

## Build

NeoForge (Java 21 recommended; its Gradle wrapper can provision the Java 21 compiler):

```powershell
cd neoforge
.\gradlew.bat build
```

Installable mod: `neoforge/build/libs/GentleMobs-NeoForge-1.21.1-1.0.0.jar`.
The `-sources.jar` is for development, not installation.

Paper, preserving the repository's existing Java 25 / Minecraft 26.2 target:

```powershell
cd paper
.\gradlew.bat build
```

Paper artifact: `paper/build/libs/GentleMobs-0.2.0.jar`.

See [NeoForge installation, configuration and behavior notes](neoforge/README.md).

The [Bedrock mob preview](bedrock/README.md) is a separate TypeScript/JSON
behavior pack. It is an early development preview, not a complete port or a
Marketplace release. See its README for building, installation and known limits.

## Verification

```powershell
cd neoforge
.\gradlew.bat test
.\gradlew.bat -PgameTests runGameTestServer
.\gradlew.bat build
```

JUnit checks vanilla-only scope (including exclusion of `cobblemon:pokemon`), explicit modded opt-in, mode semantics, config persistence, and invalid reload handling. The opt-in GameTest launches a real Minecraft/NeoForge server with a survival player and a synthetic registered modded Monster. It checks targeting, actual damage/fleeing, neutral expiration, vanilla overrides, boss and brain hooks, namespaced command parsing, recipe matching, and recipe disable/re-enable reloads. Test entity registration and the test arena are excluded from release jars.

These tests do not install Cobblemon or an All the Mons server pack. Full-modpack interaction testing remains separate from the verified default entity filtering and standalone NeoForge behavior.
