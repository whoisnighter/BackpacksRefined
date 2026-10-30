# Backpacks Refined: how the project is laid out

One source tree builds a jar per **node**, a Minecraft version on one loader, with
[Stonecutter](https://stonecutter.kikugie.dev/).

## Nodes

Every node is named `<minecraft>-<loader>` (`26.3-fabric`, `1.21.1-neoforge`, `1.20.1-forge`). They are
declared in `settings.gradle.kts`; the loader in the name picks the buildscript:

| File | Builds | With |
|------|--------|------|
| `build.fabric.gradle.kts` | Fabric nodes | Loom through loom-back-compat |
| `build.neoforge.gradle.kts` | NeoForge nodes | ModDevGradle |
| `build.forge.gradle.kts` | Forge 47 (1.20.1) | ModDevGradle Legacy |
| `gradle/moddev.gradle.kts` | what NeoForge and Forge share | |
| `gradle/shared.gradle.kts` | what every node shares: Java, checks, `buildAndCollect` | |
| `build-logic/` | node naming, the Java table, publishing, as plain Kotlin | |

To add a node: one `node(...)` line in `settings.gradle.kts` and one `[<loader>."<minecraft>"]` table
in `stonecutter.properties.toml`. CI finds it from that table.

## Versions and properties

`stonecutter.properties.toml` holds everything that differs per node. A node reads the top level, then
`["<minecraft>"]` (shared by its loaders), then `[<loader>."<minecraft>"]`. A key belongs in exactly one
table a node reads. Plugin versions are in `settings.gradle.kts` and `gradle/libs.versions.toml`.

Java follows Minecraft (`build-logic/.../Nodes.kt`): 25 from 26.1, 21 from 1.20.5, 17 before. Gradle
itself runs on 25 (`gradle/gradle-daemon-jvm.properties`).

## Sources

```
src/main/java/            loader independent code (no net.fabricmc / net.neoforged / net.minecraftforge)
src/main/resources/       shared resources, mixin config, lang
src/main/<loader>/        the loader's mod class, its copy of platform/Loader, its manifest
src/client/java/          loader independent client code
src/client/<loader>/      the loader's client entrypoint
src/dev/                  the agent client, a dev-only mod in no published jar (src/dev/java/AGENTS.md)
src/watchdog/             a -javaagent that stops an unattended NeoForge/Forge run FML failed to load
tools/agent/              what is sent to the agent from outside the game (tools/agent/AGENTS.md)
```

- Loom keeps `src/client` in its own source set on Fabric. NeoForge and Forge compile it into `main`, and
  only reach it from the client (`dist = Dist.CLIENT`, `FMLEnvironment.dist`).
- `platform/Loader` has one copy per loader with the same signatures; common code calls it. See
  `src/main/java/com/backpacksrefined/platform/AGENTS.md`.
- Sources are written in **26.3's** names. Renames between versions are Stonecutter `replacements` in
  `stonecutter.gradle.kts` (`Identifier` ↔ `ResourceLocation`, ...). Anything else that differs goes
  behind a `//?` conditional inside a `platform/` or `mixin/` package.
- Manifests (`fabric.mod.json`, `neoforge.mods.toml`, `mods.toml`) are templated from
  `stonecutter.properties.toml`; change the mod's name, description or links there.

## Checks

`./gradlew :<node>:check` runs, besides compiling:

- `checkLoaderSeam`: no loader API in `src/main/java` or `src/client/java`.
- `checkVersionSeam`: no `//?` outside `platform/` and `mixin/`.
- `checkLang`: every lang file has exactly the keys `en_us.json` has.
- `checkAgentCore`: the agent's `dev/agent/core` imports no Minecraft, loader or mod class.
- `devClasses`: the agent client compiles on the node, though nothing ships it.

## Running the game for a check

The agent client drives a real client and answers in numbers: `-Pagent=<file>` runs a file of
requests and closes the game, `-Pquickplay=<world>` opens a world, `-Pdriven` keeps a client from
taking the mouse. See [src/dev/java/com/backpacksrefined/dev/agent/AGENTS.md](src/dev/java/com/backpacksrefined/dev/agent/AGENTS.md)
and [tools/agent/AGENTS.md](tools/agent/AGENTS.md).

Before committing, the active node must be `26.3-fabric` (`./gradlew "Reset active project"`), so the
committed sources are 26.3's.
