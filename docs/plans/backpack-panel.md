# Plan: backpack panel beside every container screen

The worn backpack's slots appear as a panel beside the player inventory (`E`) and every other
container screen (chest, furnace, anvil, crafting table, ...), so items move in and out without
opening a separate backpack screen.

Goals, in order: **no duplication exploits**, **lowest server, network and client cost**, **compatibility
with other mods**, the same behaviour on every node.

## Status

Done, on every node (compiled and `check`ed on all; played on 26.3-fabric only,
`tools/agent/gameplay/storage.jsonl`):

- **Storage.** `storage/BackpackStorage` and `BackpackContents`, saved by `platform/BackpackSavedData`.
  The id is in the item's custom data (`platform/BackpackIds`), not a component of the mod's own: no
  registry entry on three loaders, and the creative screen keeps it.
- **Four tiers**, each its own item and block (`item/BackpackTier`): backpack 8, iron 16, diamond 24,
  netherite 32 (fire and lava resistant). Only in the creative tabs and `/give` for now: no recipe or
  upgrade yet. The new tiers' textures are copies of the leather ones, to be replaced. All equip with
  the leather sound for now.
- **A placed backpack opens with a right click**, as vanilla's chest with a row per nine slots (see
  [Placed backpacks](#placed-backpacks)).
- **The panel** beside every container screen but the creative one, with left and right clicks, shift
  click into the inventory and Alt and a click to store a menu slot. Payloads, validation and click
  rules as below.

Where it differs from what follows, the code is right and this plan was updated:

- The panel is hung on screens by one mixin into `AbstractContainerScreen`
  (`client/mixin/AbstractContainerScreenMixin`), not by each loader's screen events: one code path per
  version instead of three loaders' events per version. It still never touches a menu.
- The panel sits left of the screen's image, or right when there is no room on the left, with its
  bottom lined up with the image's (where every container screen draws the player's inventory). It
  always shows the largest tier's 32 slots, four columns of eight rows, exactly the inventory screen's
  height, and never scrolls; the slots past the worn tier's are darkened and do nothing. Its window is
  drawn pixel for pixel as vanilla's container textures draw theirs, corners included. Nine columns do not fit beside the inventory at the usual GUI scales.
  The wheel over it is swallowed, so the screen behind does not scroll.
- Changed slots are sent after each click, not gathered for the end of the tick, and there is no
  revision: each click answers at once with what changed, and a snapshot on opening covers the rest.

Not done yet: a way to get the upper tiers (recipes or smithing upgrades), their own textures and equip sound, admin commands, Trinkets and Curios, JEI, EMI and REI
exclusion areas, the config (blocklist, side, toggle key), the dupe scripts under [Testing](#testing),
and a creative player breaking a full placed backpack (it drops nothing, as for any block; the items
stay in storage under its id).

## Placed backpacks

A placed backpack keeps the id, not the items (`block/BackpackBlockEntity`): placing takes the id from
the item (`setPlacedBy`), breaking gives it back to the dropped item (`getDrops`), so the items never
move and cannot be duplicated by placing and breaking. The block entity is a `Container` over the
stored items, for its menu and for hoppers; from 1.21.5 it overrides `preRemoveSideEffects`, which
would otherwise drop a container's items when its block goes.

Its menu is vanilla's `ChestMenu` with a row for each nine slots (`menu/BackpackMenu`; one row for the
plain backpack's 8, four for netherite's 32), so the client opens vanilla's chest screen and every mod that knows it (sorting, recipe viewers)
knows this one. Only on the server are its slots swapped for ones that refuse what may not go inside
another item; the slots past the backpack's size that fill out the last row hold nothing and take
nothing.

## Scope

- **Only a worn backpack opens in the panel.** Worn means the chest slot, or a back slot from
  Trinkets (Fabric) or Curios (NeoForge, Forge). A backpack carried in the inventory does not open.
- **Tiers from the start.** See [Tiers](#tiers).
- **Trinkets and Curios slots count as worn.** See [Worn slots](#worn-slots).

## Decisions

### 1. The panel is a client overlay, not slots added to menus

The panel is drawn and clicked on the client through screen events. The open `AbstractContainerMenu`
is never changed: no `Slot` is added to it and no `quickMoveStack` is redirected.

Why: adding real slots to every menu (the way yyz's backpack does it, 256 `Slot`s per menu) shifts
what other mods expect from a menu, breaks sorting mods (Inventory Profiles Next, Mouse Tweaks,
Inventory Tweaks), and makes `broadcastChanges` walk hundreds of extra slots every tick on every open menu.

### 2. Contents live on the server, the item holds only a UUID

A `SavedData` maps `UUID -> contents`. The backpack item carries only its UUID (a data component from
1.20.5, an NBT tag before).

Why:
- Editing the contents never changes the worn `ItemStack`. Vanilla therefore never resends the
  equipment, so players nearby get no packet and cannot read what is inside.
- The owner receives one changed slot (tens of bytes) instead of the whole backpack stack.
- No oversized-packet ("chunk ban") exploit: a backpack item cannot be stuffed with NBT.
- Fewer version-specific mixins. Only how the UUID sits on the item differs between versions.

Rejected alternative: keep the contents in the `CONTAINER` component and mixin equipment sync to strip
them. The owner still gets the full stack on every click, the oversized-packet exploit remains, and
it needs fragile mixins into `LivingEntity` equipment detection and `ClientboundSetEquipmentPacket` on
every version.

### 3. One source of truth, the server decides everything

- There is no mirror or copy of the contents anywhere: no extra list in `Inventory`, no cached copy on
  the player. Every operation reads and writes the `SavedData` entry directly, inside one server-thread task.
- The client sends only *intent* ("left click on backpack slot 7"), never an `ItemStack`.
- The client never predicts. It shows what the server last sent.

Why: yyz's backpack keeps the item's `CONTAINER` component and a 256-slot copy inside `Inventory`, and
reconciles them once per tick. By reading their code: shift-click the backpack into a chest
(`split()` bypasses `Inventory.setItem`, so the copy is not cleared), then, in the same tick, take
items from the stale copy. The items end up both on the cursor and in the backpack in the chest.
Not tested in game, but every dupe of that shape needs two copies, and this design has one.

## Data layer (`src/main/java`)

| Class | Role |
|-------|------|
| `storage/BackpackStorage` | `SavedData` on the overworld: `Map<UUID, BackpackContents>`, `setDirty()` on write |
| `storage/BackpackContents` | `NonNullList<ItemStack>` of a fixed size, a revision counter, a last-accessed time |
| `platform/BackpackIds` | read and write the UUID on a stack; component from 1.20.5, NBT tag before |
| `platform/` storage glue | `SavedData` registration differs: `SavedDataType` + codec (26.x, 1.21.5+), `SavedData.Factory` (1.20.2 to 1.21.4), `computeIfAbsent(load, create, name)` (1.20.1) |

- **UUIDs are assigned lazily**, server side, the first time a worn backpack is opened. A fresh item
  (crafted, from the creative tab, from `/give`) has none and is simply an empty backpack. Assigning it
  changes the stack once; after that the stack never changes again.
- **Size** comes from the item's tier, read on every access. Sizes only grow through upgrades. If a
  config change ever shrinks a tier, the overflow is dropped at the player's feet, never deleted.
- **Saving** rides on the vanilla world save (`setDirty()`), never a file write per click. Start with
  one file; split per backpack later only if servers report large files.
- **Schema versioning.** The file carries `schema` (the mod's layout, 1 so far) and `data_version`
  (the game's). Loading migrates an older layout step by step (`storage/StorageSchema`) and runs
  vanilla's item fixer when the game was older. Unreadable data (a newer schema, damage) is kept and
  saved back as it was, with every backpack locked, never replaced by an empty storage. The schema only
  goes up for a layout a released version wrote; before release the layout changes in place.
- **Orphans** (backpack burnt or in the void): never deleted automatically. Each entry records when
  it was last accessed; admin commands `/backpacksrefined list | restore <uuid> | prune <days>` handle them.
- **Copies sharing a UUID** (creative middle-click, `/give` with components): they share one inventory.
  Nothing is duplicated, so this is accepted. Survival cannot copy an item without an existing dupe.

## Tiers

Each tier is its own item and block (`item/BackpackTier`), two rows of four more than the one before,
so the largest fills the panel's four columns at the inventory screen's height. Decided; there is no
gold tier.

| Tier | Slots | Notes |
|------|-------|-------|
| Backpack | 8 | leather |
| Iron | 16 | |
| Diamond | 24 | |
| Netherite | 32 | fire and lava resistant, like netherite gear |

- `BackpackItem` and `BackpackBlock` take their tier in the constructor; size and fire resistance come
  from the tier. A placed backpack's block entity reads its size from its block.
- The server opens only the worn tier's slots: if the stored contents are larger (a copy of the id
  that was a larger tier), the slots past it are left as they are and out of reach.
- **Upgrades** happen at the smithing table, one tier at a time, with an upgrade template plus the
  material. The smithing transform copies the item's components (its NBT tag on 1.20.1), so the UUID
  and with it the contents carry over. The size grows on the next access.
- The panel always shows 32 slots; a smaller tier's missing slots are darkened and locked.

## Worn slots

`WornBackpack.find(player)` returns the one worn backpack, or none. It checks, in order:

1. the vanilla chest slot;
2. each registered provider: Trinkets (Fabric), Curios (NeoForge, Forge).

- Each provider is its own class, loaded only when `Loader.isModLoaded(...)` is true, compiled
  against a compile-only dependency. Nodes where the mod is not published simply have no provider.
- **One backpack worn at a time.** The accessory slot's `canEquip` refuses a backpack while another
  one is worn. Vanilla's armor swap cannot be refused cleanly, so if both slots ever hold one, the
  chest slot wins and the other is ignored.
- The UUID design matters here too: Trinkets and Curios send their slots to nearby players for
  rendering, and an item that never changes is never resent.
- Rendering the backpack on the player when it sits in an accessory slot is separate work; not part of
  this plan.

## Networking

Payloads are registered per loader (Fabric `PayloadTypeRegistry`, NeoForge `RegisterPayloadHandlersEvent`,
Forge 1.20.1 `SimpleChannel`) behind `Loader`. Payload records and handlers stay in common code.

### Client to server

| Payload | Fields | Server action |
|---------|--------|---------------|
| `PanelVisible` | `containerId`, `visible` | subscribe or unsubscribe the player; on subscribe send a snapshot |
| `BackpackClick` | `containerId`, `revision`, `slot`, `button`, `action` (`PICKUP`, `QUICK_MOVE`) | apply the click against `menu.getCarried()` |
| `StoreFromSlot` | `containerId`, `menuSlot` | move one menu slot into the backpack (Alt+click) |

### Server to client

| Payload | Fields |
|---------|--------|
| `BackpackSnapshot` | `revision`, `size`, all stacks |
| `BackpackSlotUpdate` | `revision`, `slot`, stack |

### Validation, on every client-to-server payload

1. `containerId == player.containerMenu.containerId`, the player is alive and not a spectator.
2. `WornBackpack.find(player)` returns a backpack whose UUID is the subscribed one; otherwise reject
   and send a fresh snapshot (or hide the panel when nothing is worn).
3. `slot` is within the backpack size; `menuSlot` is within `menu.slots`.
4. Inserted items pass `Item#canFitInsideContainerItems()` (`BackpackItem` already returns false) and
   the slot's own rules (`Slot#mayPickup`, `Slot#mayPlace`).
5. Taking from a menu slot goes through `Slot#safeTake`, so result slots (crafting output, anvil,
   smithing) run their `onTake` and cost exactly as vanilla charges them.
6. A stale `revision` is not an error: the server applies the click to its current state and the
   client is corrected by the resulting update.

After each action: `menu.broadcastChanges()` (it syncs the carried stack), mark the changed backpack
slots dirty, and bump the revision.

### Sync cost

- A per-player subscription exists only while a panel is visible.
- Changed slots are collected in a dirty set and flushed once at the end of the server tick, so a
  burst of clicks becomes one packet.
- There is no per-tick scan of players, inventories or backpacks.

## Click semantics

Mirror vanilla `AbstractContainerMenu#doClick` for `PICKUP` (left: take, place or swap all; right:
take half, place one) and `QUICK_MOVE` (shift-click: backpack to player inventory through
`Inventory#add`). The server code reads like vanilla's so it inherits vanilla's stacking and
max-stack rules.

Not in the first version: number-key swap, double-click collect, drag-spreading, throw (`Q`). Each one
added later goes through the same validation.

Default shift-click behaviour of the host screen is left alone. Moving items *into* the backpack uses
Alt+click (rebindable key) over any menu slot.

## Client panel (`src/client/java`)

| Class | Role |
|-------|------|
| `client/panel/BackpackPanel` | layout, rendering, hit-testing, tooltip, scroll offset |
| `client/panel/PanelState` | the last snapshot plus updates, the revision |
| `client/panel/PanelScreens` | which screens get a panel: any `AbstractContainerScreen` except creative and the config blocklist |
| `mixin/AbstractContainerScreenAccessor` | `leftPos`, `topPos`, `imageWidth`, `imageHeight` (Fabric has no getters; one accessor serves every loader) |

Per-loader event wiring lives in `src/client/<loader>/` and only forwards to `BackpackPanel`:

| Need | Fabric | NeoForge / Forge |
|------|--------|------------------|
| attach to a screen | `ScreenEvents.AFTER_INIT` | `ScreenEvent.Init.Post` |
| draw | `ScreenEvents.afterRender` | `ScreenEvent.Render.Post` |
| click, release, drag | `ScreenMouseEvents.allowMouseClick` / `allowMouseRelease` / `allowMouseDrag` | `ScreenEvent.MouseButtonPressed.Pre` / `Released.Pre` / `Dragged.Pre`, cancelled |
| scroll | `ScreenMouseEvents.allowMouseScroll` | `ScreenEvent.MouseScrolled.Pre` |
| close | `ScreenEvents.remove` | `ScreenEvent.Closing` |

Mouse event signatures changed in 1.21.9 (`MouseButtonEvent`). That difference goes behind a class in
a client `platform/` package.

Rules:
- **Every click, release, drag and scroll over the panel is cancelled before the screen sees it.**
  Otherwise `AbstractContainerScreen#hasClickedOutside` treats it as a click outside the GUI and
  drops the carried stack.
- Position is recomputed every frame from `leftPos` and `topPos`: the recipe book moves `leftPos`. The
  default side is left, offset past the status effect list; it is configurable.
- Draw order: panel, then its hovered slot highlight, then its tooltip, then the carried stack on top.
- Large backpacks scroll inside a panel no taller than the screen's image.

## Compatibility

- **JEI / EMI / REI:** report the panel rectangle as an exclusion area (JEI `IGuiContainerHandler#getGuiExtraAreas`
  or `IGlobalGuiHandler`, EMI `EmiRegistry#addExclusionArea`, REI `ExclusionZones`). Each plugin is
  its own class, loaded only when `Loader.isModLoaded(...)` is true, with compile-only dependencies.
- **Sorting mods:** unaffected, because menus are untouched. The panel can offer its own sort button later.
- **Config:** a blocklist of screen class names, a side (left or right), and a key to toggle the panel.
- **Creative inventory:** no panel in the first version.

## Edge cases

| Case | Result |
|------|--------|
| Backpack taken off (chest or accessory slot), thrown or moved into a chest while the panel is open | the next payload fails check 2; the server unsubscribes and the panel hides |
| Backpack upgraded at the smithing table | same UUID, larger size; the next snapshot shows the new slots |
| Backpack in the inventory, not worn | no panel; it is an ordinary item |
| Death | the backpack drops as an item with its UUID; the contents stay in storage (works with grave mods) |
| `keepInventory` | nothing to do |
| Disconnect mid-click | the server task runs fully or not at all; nothing is held on the client |
| Two screens or two clients, same player | impossible: the server checks `containerId` |
| Server crash before save | contents and inventories roll back together to the last world save, as vanilla does |

## Performance budget

| Where | Idle | While the panel is open |
|-------|------|-------------------------|
| Server tick | 0 | 0 (work happens only on a payload) |
| Server per click | - | O(1) slot operation and one dirty mark |
| Network | 0 | one `BackpackSlotUpdate` per changed slot per tick, owner only |
| Client frame | 0 | the panel background and up to one visible page of item icons |

## Testing

Use the agent client (`-Pagent`, see `src/dev/java/com/backpacksrefined/dev/agent/AGENTS.md`) on a sky
floor test world. Add agent requests for panel clicks if the existing vocabulary lacks them.

Dupe scenarios to script, each expecting the total item count to stay unchanged:
1. Take items from the panel in the same tick the backpack is shift-clicked into a chest.
2. Take the backpack off (armor slot) and click the panel before the hide arrives.
3. Send `BackpackClick` with an old `containerId`, an out-of-range slot or a stale revision.
4. Two clicks on one slot in one tick.
5. Die with the panel open; respawn; pick the backpack up.
6. Take a crafting, anvil or smithing result straight into the backpack with Alt+click.
7. Disconnect between a click and its update.

## Phases

1. **Storage and tiers** on `26.3-fabric`: UUID on the item, `BackpackStorage`, the tier items,
   smithing upgrades, `WornBackpack` with the chest slot, admin commands.
2. **Server logic and payloads**: validation, `PICKUP`, `QUICK_MOVE`, `StoreFromSlot`, dirty-slot flush.
3. **Panel on `InventoryScreen`**: rendering, input capture, tooltip, scrolling.
4. **Every container screen**: blocklist config, recipe book offset, side setting.
5. **Worn-slot providers**: Trinkets on Fabric, Curios on NeoForge.
6. **JEI / EMI / REI** exclusion areas.
7. **Other nodes**: 26.2 and 1.21.11 Fabric, 26.3 and 1.21.1 NeoForge, 1.20.1 Forge (NBT UUID,
   `SimpleChannel`, older `SavedData`, Curios on Forge).
8. **Later**: tooltip preview (requested from the server on hover), sorting, bulk moves, number-key
   swap, double-click collect, backpack rendering in accessory slots.

## Open questions

- Upgrade recipe costs: which template and how much material each tier takes.
- How the upper tiers are got: crafted, or upgraded at the smithing table (with or without a template).
