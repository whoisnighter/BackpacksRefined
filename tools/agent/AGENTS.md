# tools/agent — agent client scripts

Request files for the agent queue: one JSON request per line, `//` lines are comments, and `expect` /
`checks` beside a request are what `drive.py --verify` asserts afterwards. The protocol, the queue
directories and every command are in
[src/dev/java/com/backpacksrefined/dev/agent/AGENTS.md](../../src/dev/java/com/backpacksrefined/dev/agent/AGENTS.md).

```bash
./gradlew ":26.3-fabric:runClient" -Pagent=tools/agent/smoke/boot.jsonl
```

```bash
python tools/agent/drive.py run/26.3-fabric/agent/client tools/agent/smoke/boot.jsonl --verify --max-age 10
```

## Waiting for a `-Pagent` run

A `-Pagent` run ends by itself: the game closes once the last request is answered, and a run stuck for
300 seconds closes too (`DONE stalled`). So **the Gradle task finishing is the signal**. Run it in the
background, wait for that one process, and read the result afterwards:

```bash
./gradlew ":26.3-fabric:runClient" -Pagent=tools/agent/smoke/boot.jsonl -q > build/agent-run.log 2>&1; echo "exit=$?"; grep -a "BackpacksAgent\] DONE" run/26.3-fabric/logs/latest.log | tail -1
```

`DONE script answered` is a finished run; `DONE stalled` or `DONE refused` names what went wrong. Do
not start a second loop that polls `out.jsonl` while the game runs: the previous run's `out.jsonl`
already holds the same ids until the queue rotates, so it has no reliable end.

Every path is relative to the repository root. Each script's header says which nodes it runs on, the
world it needs and how to read the result; read it before running one.

## Layout

| Path | What is in it |
|---|---|
| `drive.py` | Sends a file or standard input to a queue, waits for the answers, and `--verify`s a finished run |
| `make_gif.py` | Turns a `client.record` folder into a GIF or WebP, drawing the virtual pointer as a cursor. Options in its docstring |
| `new_world.py`, `nbt.py` | Makes the throwaway world a script runs in. See [A world for a script](#a-world-for-a-script) |
| `clean.py` | Deletes the worlds and captures a run left behind and the next run makes again. See [Cleaning up](#cleaning-up) |
| `smoke/` | Checks with no gameplay: `boot.jsonl` (a client comes up and its queue answers) and `server-probe.jsonl` (a dedicated server's queue answers with nobody online) |
| `gameplay/` | The mod's own features: `wear.jsonl` (a right click wears a backpack and swaps it with a chestplate, both ways) |

Scripts for the mod's own features go in a folder named for what they check (`gameplay/`, `ui/`).

## A world for a script

A client script runs in a throwaway world, made fresh for each run:

```bash
python tools/agent/new_world.py 26.3-fabric BackpackAgent
```

By default the world is a **sky floor**: a superflat overworld with one layer of stone at y 199 and
nothing below it, one biome (plains, or `--biome <id>`), no structures. The player arrives standing on
it at y 200, at noon, in clear weather, with no mobs spawning. Nothing is under it to fall into, and
chunks generate in a fraction of the time real terrain takes.

Pass **`--terrain`** only when the script needs real ground: screenshots, natural biomes, heights,
water to float a backpack on. That world keeps the generator, seed and spawn point of another world of
the node, so the node needs one world made by hand first (start `runClient` once and create one).

`--datapack <folder>` adds a data pack; the tool's docstring has the rest. `nbt.py` is the NBT reader
and writer it uses, with no dependency.

Use the world name the script's header gives, every time. A new name for each attempt leaves one
more world per attempt that nothing opens again.

### A node with no world yet

`new_world.py` refuses a node whose `saves/` is empty. A dedicated server would make one, but only
once someone has accepted `eula.txt` by hand, which is the user's call. Otherwise start a driven client
(`runClient -Pdriven`, in the background) and click through it with the agent: the accessibility
screen's Continue, Singleplayer (which opens world creation when there are no worlds), Allow Commands,
Create New World. `client.capture` after each click shows where the next button is; `client.click`
takes GUI pixels, the capture's pixels divided by the GUI scale.

### Commands from a script

`server.command` runs as the server, at the world spawn, not at the player. A command that needs the
player's position runs through them: `execute as @a at @s run playsound <sound> player @s`. A command
that hands the player an item is seen by the client a few ticks later; `wait` before reading it back.

## Cleaning up

A world is throwaway when a run makes it again; delete those, keep the rest.

```bash
python tools/agent/clean.py 26.3-fabric --world BackpackAgent
```

once a script has run and its result has been read. The world goes; the queue's `out.jsonl` stays as
the record.

```bash
python tools/agent/clean.py --dry-run
```

for a sweep of every node, or name the nodes. `--screenshots` adds the queues' `screenshots/` folders.

| Delete | Why it is safe |
|---|---|
| A world in `saves/` holding `.agent-world` | `new_world.py` made it and makes it again from nothing on the next run |
| `agent/<queue>/screenshots/`, `client.record` frames | Once the keepers are copied out or a GIF is made, the next capture writes them again |

| Keep | Why |
|---|---|
| Any world without `.agent-world` | `new_world.py` copies its seed and spawn point, so deleting the last one of a node stops every `--terrain` script there |
| The queue's `in.jsonl`, `out.jsonl`, `previous-*` | Small, and the only record of what the last run did |
| A world a person is testing in | Ask before deleting a world you did not make in this session |

## Adding a script

- Put it in the folder its subject belongs to.
- Start with a header: what it proves, the nodes it needs, the world (`-Pquickplay=<world>` and the
  `new_world.py` line that makes it), and what to read afterwards when the file cannot say it with
  `expect` and `checks`.
- Reference it from the `AGENTS.md` of the area it checks, by its full `tools/agent/<folder>/<name>.jsonl`
  path, so a move shows up in a search.
- A script is for what needs a real client: rendering, screens, the real right click, other mods, and
  the client's view of synced values.
