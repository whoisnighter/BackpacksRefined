# com.backpacksrefined

The loader independent half of the mod. Nothing here imports a loader (`checkLoaderSeam`), and nothing
outside `platform/` and `mixin/` has a version conditional (`checkVersionSeam`).

| Package | What is in it |
|---|---|
| `BackpacksRefined` | The common entrypoint each loader's mod class calls, the mod id, `id(path)` |
| `sound/` | `BackpacksSounds`: the sound events. What they play is in `assets/backpacksrefined/sounds.json` |
| `block/` | `BackpacksBlocks` (the registry) and `BackpackBlock`, the placed backpack |
| `item/` | `BackpacksItems` (the registry), `BackpackItem`, and `BackpacksCreativeTabs`, the mod's own tab |
| `platform/` | Where versions differ, and the loader seam (`platform/AGENTS.md`) |
| `mixin/` | Mixins, listed in `backpacksrefined.mixins.json`. None yet |

## Registering

The game takes new blocks and items at a different moment on each loader, so the common code never
registers anything itself. Each registry class has `register(BiConsumer<Identifier, T>)`, which builds
its objects and hands each one over, and each loader's mod class calls them at its own moment:

- Fabric (`BackpacksRefinedFabric`): at startup, with `Registry.register(BuiltInRegistries.X, ...)`.
- NeoForge and Forge (`BackpacksRefinedNeoForge`, `BackpacksRefinedForge`): in `RegisterEvent`, with the
  event's helper.

The item is built from the block, so blocks come before items: the loader events fire blocks first,
and Fabric calls them in that order. Sounds have no place in the order: the block and the item hold the
`SoundEvent` objects themselves rather than looking them up. The objects are kept in static fields
and read through getters (`BackpacksItems.backpack()`), which throw when called before registration.

The creative tab is registered the same way, after the items. Each loader starts a tab with its own
builder, so its mod class passes one in: `CreativeTabs::builder` on Fabric, `CreativeModeTab::builder`
on NeoForge and Forge.

Adding a block or item means one line in its registry class and nothing in the loaders, unless it is a
new kind of registry: then each of the three mod classes registers it too.

## The backpack

- `BackpackItem` extends `platform/WearableBlockItem`: a block item that is worn in the chest slot.
  Used on a block it places `BackpackBlock`; used in the air it swaps with the chest slot.
  `tools/agent/gameplay/wear.jsonl` checks the wearing and the swap in a real client.
- `BackpackBlock` has a horizontal `facing`, `waterlogged` (through `platform/WaterloggableBlock`) and
  `floating`: true over a water source while not in water, checked when placed and whenever a
  neighbor changes. `tools/agent/gameplay/water.jsonl` checks both. It has no block entity yet, so it
  holds nothing; the plan for that is in
  [docs/plans/backpack-panel.md](../../../../../docs/plans/backpack-panel.md).
- The block's display name is `block.backpacksrefined.backpack` on every version: from 1.21.2 the item
  asks to keep the block's name (`useBlockDescriptionPrefix`), as it did by default before.
