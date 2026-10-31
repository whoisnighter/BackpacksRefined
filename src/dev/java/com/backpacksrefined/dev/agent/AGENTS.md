# The agent client

A way for an agent to drive a real Minecraft client and read real numbers out of it, in place of
judging a screenshot. It exists for the checks a test without a client cannot reach: what a client
is told, what a screen shows, what the player wears and carries, how a menu reacts to a click. What
would be read off a screenshot is read as a value the client already holds: the item in the chest
slot, the open menu's slots, the tooltip's lines, a framebuffer sample at a named point. A screenshot
stays useful as evidence for a person; it is never the assertion.

It is on every node and every run task. The two extra clients, `runManualA` and `runManualB`, are on
the NeoForge and Forge nodes.

`core/` is the queue, the reply envelope and the dispatch loop, in plain Java and Gson; `backpacks/` is
everything that names Minecraft, a loader or the mod. [src/dev/java/AGENTS.md](../../../../AGENTS.md)
is the source set this belongs to, and holds the rules and the harness.

## The queue

Two text files in a directory, one line of JSON each way. No port, no firewall prompt, and the whole
exchange is still on disk to read afterwards.

    run/<node>/agent/<name>/
        ready.json    written once the game will answer; holds startedAt, pid and the command list
        in.jsonl      the agent appends one request a line
        out.jsonl     the game appends one reply a line
        previous-*    the run before this one, kept rather than overwritten
        screenshots/  what client.capture wrote, and a folder per client.record recording

`<name>` is `server` or `client` by default and `A` or `B` on the two extra clients, so a server and
a client of the same node never share a file. One queue per process: a client with an integrated
server is one process and has one queue, polled on the client tick, and `server.*` reaches its
integrated server from there.

A request is `{"id": "...", "command": "...", "args": {...}}`; `id` is echoed back and may be left
out, in which case the position in the queue stands in for it. A reply is

```json
{"id":"info","sequence":3,"command":"server.info","ok":true,"result":{...},"tookMs":1.1}
```

with `error` instead of `result` when `ok` is false. **Requests are answered in order, one at a
time.** A command that takes ticks to finish (`wait`, `client.hold`, `client.capture`, `client.slot`,
`client.respawn`) holds the next request up until it has answered, so a file of requests is a
sequence rather than a batch of things that all happen in one tick.

`drive.py` also accepts an `expect` object keyed by dotted reply paths, plus relational `checks`.
Expectations use exact JSON equality; a mismatch is printed as `assertionErrors` and exits 1. An
expected `null` passes when the path is absent as well as when it is null, because Gson leaves a null
property out of the reply: that is how "no screen is open" is written. A check has `left`, `op`, and
either another reply path in `right` or a literal in `value`. The operations are `eq`, `ne`, `lt`,
`le`, `gt`, `ge`, `contains`, `not_contains`, and `within` (with `tolerance`); `contains` on a list of
messages is membership, not a substring, so a whole message goes in `value`.

A dedicated server's queue is polled on the server tick, and a dedicated server with nobody online
stops ticking after `pause-when-empty-seconds`, which would kill the queue about a minute after
startup. `ServerAwake` holds that pause off for as long as the queue is open. A client's integrated
server is left to pause on the player's own rules.

**Wait for this run's `ready.json`, not for the file.** The previous run leaves one behind, and a
game takes most of a minute to come up; a request written into `in.jsonl` before the game opens the
queue is rotated into `previous-in.jsonl` and never answered. `startedAt` is there to tell the two
apart.

## Driving it

Appending a line and reading `out.jsonl` is the whole protocol, so an agent with only file tools
needs nothing else. [tools/agent/drive.py](../../../../../../../tools/agent/drive.py) does the matching up:

```bash
python tools/agent/drive.py run/1.21.1-neoforge/agent/server tools/agent/smoke/server-probe.jsonl
```

```bash
echo '{"command": "client.state"}' | python tools/agent/drive.py run/manual-1.21.1-neoforge-A/agent/A -
```

It waits for each answer, prints them in the order asked, and exits 1 if any was refused. Pass
`--ready <seconds>` when the game is still starting: it then waits for a `ready.json` newer than the
moment the command began. Requests can be read from a file or from standard input, and a line
starting with `//` is a comment.

Unattended, without an agent at all:

```bash
./gradlew ":26.3-fabric:runClient" -Pagent=tools/agent/smoke/boot.jsonl
```

`-Pagent=<file>` answers that file once the game is up and then stops the game. The path is relative
to the repository root. It applies to every run task of the node, clients included. Nothing in the
game reads the `expect` and `checks` lines beside the requests, so a run like this is checked
afterwards against what it recorded:

```bash
python tools/agent/drive.py run/26.3-fabric/agent/client tools/agent/smoke/boot.jsonl --verify --max-age 10
```

A run that crashed before it opened the queue leaves the previous run's files in place, and `--verify`
would pass on them. `--max-age <minutes>` refuses a `ready.json` older than that; use it whenever the
question is whether the run started at all.

A client script that needs a world gets one with `-Pquickplay=<world>`, which opens that singleplayer
world of `run/<node>/saves` straight from launch; server commands then reach its integrated server.
The world is a throwaway one made by `tools/agent/new_world.py` (see
[tools/agent/AGENTS.md](../../../../../../../tools/agent/AGENTS.md#a-world-for-a-script)).

### One game per queue, and an unattended run always ends

- **A queue belongs to one game.** The game that opens `run/<node>/agent/<name>/` holds an operating
  system lock on `queue.lock` in it for as long as it lives. A second game of the same node started
  meanwhile leaves the files alone, logs who owns them (the pid from their `ready.json`), and with
  `-Pagent` logs `DONE refused` and closes. The usual way to get there is killing a `runClient` Gradle
  task, which does not always close the game window. Two games of one node at once need
  `-Dbackpacksrefined.agent=<name>` on one of them.
- **`stop` jumps a running script.** Written to `in.jsonl` while a `-Pagent` script runs, it is answered
  on the poll that reads it rather than after the script's last line. Otherwise it keeps its place:
  `drive.py` writes a whole file at once, and one ending in `stop` means after the rest.
- **A stalled run stops.** With `-Pagent`, when requests are waiting and none has been answered for 300
  seconds (`-Dbackpacksrefined.agent.stallSeconds`), the rest is refused as stalled, the log says
  `DONE stalled` with the request it was stuck on, and the game closes. The longest request a script
  can make is a one minute `wait`.
- **A frozen game is halted.** A game thread that stops ticking cannot run that check, so a watchdog
  thread halts the process after 300 seconds without a tick (`-Dbackpacksrefined.agent.frozenSeconds`),
  logging where the game thread was stuck and writing a `watchdog` line to `out.jsonl`.
- **The window title says what the script is doing** on a driven client, once a second: the request
  it is answering, the ticks it still waits, and how far through the script it is. A long `wait` looks
  exactly like a hung game to anyone glancing at the window; the title is how to tell. It never
  reaches a capture.

## Driving a client while the machine is in use

A client that is being driven is not being played, and a few things a played client does get in the
way of using the desktop it opened on, or of the run going anywhere at all. `-Pdriven` turns them
around:

```bash
./gradlew ":26.3-neoforge:runManualA" -Pdriven
```

- **It never takes the mouse pointer.** Minecraft grabs the cursor as soon as a window with a world
  open is focused. A driven client refuses the grab outright, which costs nothing: every key the agent
  holds goes through the game's own key state and every command through the player's connection.
- **It opens maximised**, rather than at the small size the run tasks ask for. Maximised and not full
  screen, on purpose: exclusive full screen takes over the display the person is working on.
- **It passes vanilla's experimental settings prompt**, which stands between `-Pquickplay` and the
  world whenever a mod on the classpath turns a feature flag on. A driven client presses "I know what
  I'm doing!" for itself and writes a line to the log. `ClientWindow.passWorldPrompt` finds the button
  by its message rather than by naming the screen.
- **On NeoForge it passes the "warnings while loading mods" screen** when no warning on it is this
  mod's, and logs each warning it passes over. It stays on the screen for an error, for a warning
  about this mod, and for a warning that names no mod. `LoadingWarnings` in `src/dev/neoforge`.
- **An unattended NeoForge or Forge run that fails to load stops**, non-zero, rather than sitting on
  the error screen with the Gradle task open: `LoadingErrorScreenMixin` on NeoForge, and the
  `src/watchdog` `-javaagent` when FML fails before any mixin applies. A Fabric client exits on a
  loading crash by itself.

`-Pagent=<file>` implies `-Pdriven`, because an unattended run has nobody at the keyboard. It is off
by default otherwise, because with no grab there is no mouse look and no click reaches the world, so
a person cannot play the client. `client.info` answers `driven` and `mouseGrabbed`.

It is client side only. `-Pdriven` on `runServer` does nothing.

## The commands

`ready.json` lists what the process it belongs to answers; a server answers the first two groups and
a client answers all three.

| Command | Arguments | Answers |
|---|---|---|
| `probe` | | side, loader, Minecraft version, run directory, queue, whether a server is running, the command list |
| `wait` | `ticks` | after that many game ticks have run. The way to let the game catch up |
| `stop` | | after stopping this process: halting a server, closing a client's window. Answered ahead of a running `-Pagent` script |
| `server.info` | | dedicated, tick count, players, difficulty, levels |
| `server.players` | | every online player's position, health, food, flags and the item in their chest slot |
| `server.player` | `player`, which may be left out when one player is online | the same, for one player |
| `server.command` | `command`, `as` | what the command returned and what it said, collected rather than logged |
| `server.sprint` | `ticks` | after `/tick sprint <ticks>` has run every tick: `ran`, the wall-clock `seconds` it took. For what only time does |
| `client.info` | | window and GUI size, GUI scale, fps, screen, server, player, key names, `toggleCrouch`, `toggleSprint`, `driven`, `mouseGrabbed` |
| `client.state` | | the client's own player: position, health, food, sprinting, sneaking, dimension, the item in the chest slot, the open screen |
| `client.hud.toggle` | | the new hidden state after toggling the same vanilla state as F1 |
| `client.capture` | `name` | a PNG beside the queue, once it is on disk |
| `client.pixels` | `points`, `space`, `capture`, `name` | the framebuffer colour at each point, in GUI pixels by default |
| `client.command` | `command` | after sending it through the player's own connection |
| `client.chat` | `message` | after sending it |
| `client.hold` | `keys`, `ticks` | the movement state before and after holding those keys for that long |
| `client.key` | `key`, `down` | after setting one key's state and leaving it there |
| `client.screen` | `open` (`none`, `inventory`) | which screen is open now. An open menu is closed as Escape closes it, telling the server |
| `client.tooltip` | `item`, `count`, `slot`, `advanced` | the tooltip lines that item produced, as text, with their colours. `slot` reads the stack the player holds (`mainhand`, `offhand` or an inventory index) |
| `client.click` | `x`, `y`, `button`, `from` (`centre`, `corner`, `top`, `bottom`) | after pressing and releasing a mouse button on the open screen: which child was under the point and whether the press was taken |
| `client.scroll` | `x`, `y`, `from` as `client.click`, `amount` (wheel notches, positive down the page) | after turning the wheel over the open screen: whether the screen took it |
| `client.mouse` | `x`, `y`, `from` as `client.click`, `ticks`, `drag`, `hide` | after showing the virtual pointer and gliding it there over `ticks`. See [Recording](#recording-a-screen) |
| `client.record` | `action` (`start`, `stop`), `name`, `every` (ticks a frame, 2), `downscale` (2) | `start` at once; `stop` once every frame is on disk, with the frame count and how many `failed` |
| `client.slots` | | the open menu's slots that hold something, with their class and player inventory index, and what the cursor carries |
| `client.slot` | `slot` or `inventory`, `button`, `action` (`pickup`, `quick_move`, …) | the same, a few ticks after clicking that slot through the game mode |
| `client.language` | `code` | the language now selected, after loading its translations again |
| `client.textWidth` | `keys`, `texts` | how wide the game's font draws each, in GUI pixels, keys translated in the current language |
| `client.respawn` | | after pressing the death screen's button through the connection |
| `client.disconnect` | | after leaving to the title screen |
| `client.connect` | `address` | after starting a connection; `wait` for it to finish |
| `client.testWorld` | | after starting to make and open a fresh `Test World`, as the main menu's **Test world** button does; `wait` for it to load. For a run with no world yet, such as CI (`tools/agent/ci/test-world.jsonl`) |

`/backpacks agent probe` in game says the same thing `probe` does, for the moment before an agent is
wired up at all. It needs permission level 4.

Two things about input. It goes through the game's own key state, never the operating system, so a
key the agent holds stays held for as many ticks as it asks. But vanilla's Sneak and Sprint
accessibility settings turn those two keys into toggles; `client.info` answers both settings, so read
the state back rather than assuming. The dev client also turns `pauseOnLostFocus` off when it opens
the agent: an unattended window is necessarily unfocused, and otherwise vanilla keeps opening the
pause screen.

Clicks are for controls nothing else reaches; a screen the game can open, and a slot, have commands
of their own. `client.click` measures from the centre of the screen by default because a container
screen is centred, so a control on one keeps its offset from the centre at any window size.
`from: "top"` and `from: "bottom"` keep x measured from the centre and measure y from that edge, for a
screen made of a header, a list and a footer. `button` is 0 left, 1 right and 2 middle everywhere:
26.3 moved the client from GLFW to SDL, which numbers the buttons from one, and
`AgentClientVanilla.click` turns the number back so a file written on one node presses the same
button on another.

`client.slot` takes a player inventory index as `inventory`, the `container.N` of `/item replace`,
and finds that slot in whatever menu is open. That is how a script moves items into and out of a
placed backpack's menu.

## Recording a screen

`client.record` and `client.mouse` turn a script into a GIF: a showcase of a screen, with a cursor that
moves, hovers, drags and clicks. [tools/agent/make_gif.py](../../../../../../../tools/agent/make_gif.py)
makes the GIF from the frames.

- **The virtual pointer.** `client.mouse` shows a pointer the game believes is the mouse (`Pointer`,
  written into `MouseHandler` through `MouseHandlerAccessor`), so a screen draws its hover states and
  tooltips under it. Once it is shown, `client.click` and `client.scroll` without a point land where
  it is. While it is shown the real mouse is ignored (`MouseMoveMixin`). `{"hide": true}` gives the
  real mouse back.
- **Frames.** `client.record` writes `screenshots/<name>/frame-00001.png` onward, `every` ticks apart,
  and `frames.jsonl` beside them: the pointer's place in frame pixels, and whether a button is down.
  The framebuffer never holds a cursor, so `make_gif.py` draws one there. A frame is taken at the
  start of a pass of the game loop (`FrameStartMixin`), so the cursor and the screen under it always
  agree.
- **Lockstep.** Reading a maximised window back costs over a hundred milliseconds a frame, so rather
  than drop frames the queue waits while a frame is due: the GIF plays at the speed the script was
  written for however long the recording took.
- **The clock.** While a recording runs, `RecordingClock` holds the game's clock still and moves it on
  one tick's worth a pass until the next frame is due, so what reads the clock rather than counting
  ticks plays at its in-game speed too.
- **Size.** Frames are kept at every `downscale`th pixel, half the window each way by default.

A recording is evidence for a person, like `client.capture`; nothing is asserted on its pixels.

## Rules

- **Every probe answers a number or a string, never a picture.** `client.capture` exists so a person
  can look at the frame afterwards. The moment an assertion depends on a screenshot, the item is not
  automated.
- **No change to the mod for the agent's sake.** Where a probe cannot reach something, read it from
  this source set instead.
- **`dev/agent/core` knows nothing of Minecraft, of a loader or of the mod.** `checkAgentCore` fails
  the build when a class there imports one.
- **A probe for a backpack feature goes in `backpacks/`**, beside the vanilla ones, once the feature
  exists: what a placed backpack holds, its colour, where it landed on death.
