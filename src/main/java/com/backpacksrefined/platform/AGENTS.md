# platform/

The seam between the mod and everything that differs per node.

- `Loader` is not in this directory: every loader has its own copy, with the same name and the same
  public signatures, in `src/main/<loader>/java/com/backpacksrefined/platform/Loader.java`. The build
  compiles exactly one. Common code calls `Loader.whatever()` without knowing which.
- A difference between Minecraft versions goes behind a class in a `platform` package, written with
  Stonecutter conditionals (`//? if >=1.21.11 {`). `checkVersionSeam` fails a `//?` anywhere else
  outside `mixin/`. Any package named `platform` counts, so the client and the loader source sets have
  their own (below).
- When a new method is added to one `Loader`, add it to all three. `Loader.blockEntityType` is one:
  1.21.2 to 1.21.11 keep the block entity type's constructor private, and Fabric API builds it there.
- `Network` is like `Loader`: one copy per loader (`src/main/<loader>/.../platform/Network.java`),
  carrying `network/BackpackNetwork`'s messages as Fabric API payloads, NeoForge payloads, or a Forge
  47 simple channel. Its client half is `client/platform/ClientNetwork`, one per loader under
  `src/client/<loader>`.
- Write each conditional in its 26.3 form, the branch for older versions commented out, since the
  committed sources are 26.3's. A branch for versions with no node (1.21.2 to 1.21.8, say) can be left
  out; say so in the class's comment.

## What is here

| Class | Where versions differ |
|---|---|
| `RegistryKeys` | From 1.21.2 a block is given its id in its properties (`setId`), before it is registered |
| `BackpackSavedData` | Saved data: a type with a codec from 1.21.5 (held by the server rather than the overworld from 26.1), a factory with the registries from 1.20.5, a loader alone before |
| `BackpackIds` | The id on a backpack: in custom data from 1.20.5, in the tag before; read through a codec from 1.21.5 |
| `IdBlockEntity` | How a block entity saves (value input and output from 1.21.6, a tag and the registries from 1.20.5, a tag before), and from 1.21.5 not dropping a container's items when removed |
| `OpenableBlock` | Using a block: `useWithoutItem` from 1.20.5, `use` with the hand before; one success for both sides from 1.21.2; `isClientSide()` a method from 1.21.9 |
| `DataVersions` | The game's data version, a record component from 1.21.6 and getters before; and vanilla's data fixer run over a saved item stack |
| `Stacks` | Comparing stacks (components or tags) and writing them to a buffer (a stream codec with the registries from 1.20.5) |
| `WaterloggableBlock` | 1.21.2 changed `updateShape`'s arguments and passed the tick scheduler apart |
| `FlatIcon` | Custom model data: a string list from 1.21.4, a number component from 1.20.5, a tag before |
| `WearableBlockItem` | From 1.21.2 an item is worn through the `equippable` component, and also given its id and told to keep the block's name. Before, it implemented `Equipable`, swapped in `use`, and returned its equip sound, as a holder from 1.20.5 |

`WearableBlockItem` gives the `equippable` component an equipment asset, the item's own id, which has
no file. Nothing draws for it, but without one a humanoid's render state leaves the chest slot empty
and the worn backpack is never drawn; see `src/client/java/com/backpacksrefined/client/AGENTS.md`.

## The other platform packages

| Class | Where versions differ |
|---|---|
| `src/main/fabric/.../platform/CreativeTabs` | Fabric API renamed its creative tab events and builder in 26.1 (`CreativeModeTabEvents`, `FabricCreativeModeTab`; before `ItemGroupEvents`, `FabricItemGroup`) |
| `src/client/java/.../client/platform/BackpackScreen` | A container screen's size is given to its constructor from 26.1 (final) and set after before; its background is `extractBackground` from 26.1, `renderBg` before, with `render` adding the tooltip |
| `src/client/java/.../client/platform/BackpackWornModel`, `BackpackWornLayer` | How a render layer draws: by submitting with a render state from 1.21.9, by drawing at once before |
| `src/client/fabric/.../client/platform/FabricClientRegistration` | Fabric API renamed its model layer and render layer registries in 26.1, and 26.1 dropped the block cutout map |
| `src/client/neoforge/.../client/platform/NeoForgeClientRegistration` | NeoForge renamed the player renderer lookup in `AddLayers` in 1.21.9 |
