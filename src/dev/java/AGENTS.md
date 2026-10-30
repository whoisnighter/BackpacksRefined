# src/dev — development-only tooling

Tools for working on the mod that must never ship. This is its own source set and its own small mod,
`backpacksrefined_dev` on every loader (declared in `src/dev/resources/fabric.mod.json`,
`src/dev/neoforge/resources/META-INF/neoforge.mods.toml` and `src/dev/forge/resources/META-INF/mods.toml`),
so none of it can reach a published jar. Every run task loads it: `runClient` and `runServer`
everywhere, and `runManualA` and `runManualB` on NeoForge and Forge.

One tool lives here: the agent client, for driving a real client and reading numbers out of it. It
documents itself in [agent/AGENTS.md](com/backpacksrefined/dev/agent/AGENTS.md); `tools/agent/` holds
what is sent to it from outside the game.

| In the source set | What it is |
|---|---|
| `agent/core` | The queue, the reply envelope and the dispatch loop, in plain Java and Gson |
| `agent/backpacks` | Everything that names Minecraft, a loader or the mod: the probes, the window, the virtual pointer, the recorder |
| `harness/` | What any dev tool would need: `ServerAwake`, `DevEnvironment`, `Autorun` |
| `mixin/` | How the agent refuses the mouse grab, moves its virtual pointer and times a recording's frames |
| `BackpacksDev`, `BackpacksDevClient` | The two halves of the entrypoint, apart so a dedicated server never loads a class naming `Minecraft` |
| `fabric/`, `neoforge/`, `forge/` | Each loader's entrypoints and its copy of `DevLoader`/`DevClientLoader`, the seam for the loader calls the mod's own `Loader` does not cover |

`BackpacksDev` registers nothing unless the DEV gate is open: the loader's own
development-environment check, true under every run task and false in the jar players install.
`-Dbackpacksrefined.dev=true|false` overrides it either way.

`src/watchdog` is a `-javaagent` the NeoForge and Forge runs get under `-Pagent`. It stops an
unattended run that FML fails to load before any mod mixin applies (a missing dependency), which
nothing inside the game can see.

## The shared harness

| In `harness/` | What it is for |
|---|---|
| `ServerAwake` | A dedicated server with nobody online stops ticking after `pause-when-empty-seconds`, and the agent's queue is polled on that tick. This holds the pause off while the queue is open |
| `DevEnvironment` | Which loader, which Minecraft, which build of the mod, whether the DEV gate is open, where the run directory is. `ready.json` and `probe` both answer from it |
| `Autorun` | `-Pagent` reaches the game as a pair of system properties: one naming the work and one, with `.exit` on the end, saying the game stops once it is done. An unattended run ends with a `DONE` line, because the Gradle task exits 0 whatever happened |

## Rules for everything here

- Nothing in `main` or `client` may reference this source set.
- The harness and `agent/` name no mod loader. Everything loader-specific goes through `DevLoader` or
  `DevClientLoader`. `check` compiles this source set on every node (`devClasses`), so a Fabric import
  here fails the NeoForge and Forge nodes.
- `agent/core` knows nothing of Minecraft, of a loader or of the mod; `checkAgentCore` enforces it.
- Everything here compiles on every supported version. A vanilla call that differs between versions
  goes behind `AgentVanilla` (server) or `AgentClientVanilla` (client), never a `//?` at the call site.
- No change to the mod for a tool's sake. Where a tool cannot reach something, read it from this
  source set instead, rather than widening an API surface players never get.
- CI only compiles this source set; it runs no tool.
