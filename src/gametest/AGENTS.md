# src/gametest — the game tests

What a backpack does on the server, checked by vanilla's game test framework on a headless server:
no window, no world to make, a few seconds of ticks. It is its own source set and its own small mod,
`backpacksrefined_gametest`, so none of it reaches a published jar.

```bash
./gradlew ":26.3-fabric:runGameTest"
```

runs every test in a fresh world under `versions/<node>/build/gametest/`, writes `report.xml` there
(JUnit's format) and fails the task when a test fails. CI runs it in `build.yml` on every node that
has the tests.

| Path | What is in it |
|---|---|
| `java/.../gametest/BackpackGameTests` | The tests themselves, one method each, loader independent (`checkLoaderSeam` reads this folder too) |
| `<loader>/java/` | The loader's registration: one annotated method a test, calling the method of the same name |
| `<loader>/resources/` | The test mod's manifest |

## Which nodes

A node has the tests when a table it reads sets `test.gametest = true` in `stonecutter.properties.toml`;
the node's buildscript then makes the source set and `runGameTest`, `check` compiles it, and CI's
discover step finds it there. Only `26.3-fabric` does for now. Bringing another node in means its
loader's registration under `<loader>/`, its buildscript's run (see `build.fabric.gradle.kts`), and
whatever `GameTestHelper` call differs on that version: the framework changed a lot in 1.21.5.

## What belongs here, and what in the agent client

A game test checks what the server holds: block states, block entities, the storage, menus, what a
message from the client does to them (`PanelServer` called directly, with vanilla's mock player). Its
answer is a pass or a fail, so it needs no one to read a capture.

What only a client shows stays with the agent client (`tools/agent/gameplay/`): rendering, the screens
and the panel's layout, the lid's animation, the models, the real right click and the network round
trip. When a feature has both halves, test the server half here and keep the script to what it alone
can see.

## Adding a test

1. A public method in `BackpackGameTests` taking a `GameTestHelper`, ending with `helper.succeed()`
   (or a `succeedWhen` for something that takes ticks). Build at y 1 and up of the 8×8×8 empty
   structure.
2. One method calling it in each loader's registration class, under the same name.
3. Break the code it checks once and run it, to see it fail.
