# com.backpacksrefined.client

The loader independent client code. On Fabric it is its own source set; on NeoForge and Forge it is
compiled into `main` and only reached from the client (see the root `AGENTS.md`).

| Class | What it is |
|---|---|
| `BackpacksRefinedClient` | The common client entrypoint each loader's client entrypoint calls |
| `BackpackRendering` | The model layers, textures and shapes of the worn backpack (`createWornLayer`) and the placed one (`createBlockLayer`), the same on every version |
| `BackpackBlockPose` | How a placed backpack is posed in a frame: the lid, the drop when set down, the bobbing on water |
| `platform/BackpackBlockRenderer` | The placed backpack's block entity renderer |
| `config/BackpacksConfig` | The client settings, `config/backpacksrefined.json` |
| `config/BackpacksConfigScreen` | The Cloth Config screen over them; only reached when Cloth Config is installed |
| `platform/BackpackWornModel` | A humanoid model with every part hidden but the backpack hung on its body |
| `platform/BackpackWornLayer` | The render layer that draws it, and `create(renderer, models)`, which makes one for a renderer with a humanoid model |
| `panel/BackpackPanel` | The worn backpack's panel: layout, drawing, clicks, and what the server last sent |
| `panel/VanillaWindow` | Vanilla's container window and slot drawn with plain fills, for the panel and the placed backpack's screen |
| `platform/PanelGraphics` | What the panel draws with, where the GUI's method names differ |
| `platform/BackpackScreen` | A placed backpack's screen, over `menu/BackpackMenu`: its size and background method differ between versions |
| `platform/ClientNetwork` | Not here: one copy per loader under `src/client/<loader>`, sending to the server |
| `mixin/AbstractContainerScreenMixin` | Hangs the panel on every container screen. Listed in `backpacksrefined.client.mixins.json` |
| `mixin/CapeLayerMixin` | Skips a player's cape while they wear a backpack. Listed in `backpacksrefined.client.mixins.json` |

## Drawing the worn backpack

The backpack is a child of the humanoid model's `body` part, offset as the old mod had it, so it is
posed by the same code that poses a body: it leans when sneaking and turns when swinging. The model's
other parts are hidden and the body's own cubes are skipped (`ModelPart.skipDraw`), so only the backpack
draws. Its texture is `textures/entity/backpack.png`, 64x64, the old mod's `textures/model/backpack.png`.

How the layer draws changed in 1.21.9, which is why the model and layer are in `platform/`:

- **From 1.21.9** (1.21.11, 26.x): `submit` hands the model and the render state to the
  `SubmitNodeCollector`, and the game calls `setupAnim(state)` when it draws, later in the frame. Never
  pose a shared model in `submit`: every entity using the layer shares it. 26.3 has a shorter
  `submitModel` overload than 1.21.11 and 26.2.
- **Before** (1.21.1, 1.20.1): `render` copies the wearer's pose (`copyPropertiesTo`) and draws the
  body part at once.

The layer reads the chest slot from the render state (`HumanoidRenderState.chestEquipment`). From 1.21.4
the game fills that only for an item whose `equippable` names an equipment asset, so the backpack names
one with no file behind it (`platform/WearableBlockItem` in `src/main/java`). Take the asset away and
the layer silently draws nothing.

## The cape

`CapeLayerMixin` cancels the vanilla cape layer at its head when the chest slot holds a backpack: from
1.21.9 in `submit`, reading `AvatarRenderState.chestEquipment`; before, in `render`, reading the
player's chest slot. A dev account has no cape, so the agent client cannot show it; a boot only proves
the mixin found its target (`injectors.defaultRequire` is 1, so a missing target fails the load).

## The panel

`BackpackPanel` draws the worn backpack's slots beside any container screen but the creative one, in
vanilla's look drawn with plain fills (no texture, so no blit API to differ between versions): left
of the screen's image, bottoms lined up, always the largest tier's 32 slots in four columns of eight
(the inventory screen's height), with those past the worn tier's darkened and locked. The window's
outline and corners copy vanilla's container textures pixel for pixel. The
mixin calls it from the screen's `init`, `removed`, drawing and mouse methods:

- **Drawing** comes after the screen's contents and before the carried stack: at the end of
  `extractContents` (26.x) or `renderContents` (1.21.6 to 1.21.11), where the carried stack is drawn
  after, in a later layer; before 1.21.6 at the end of `render`, where the carried stack sits further
  forward. The tooltip is set for the end of the frame from 1.21.6, drawn at once before.
- **The mouse**: a press, release or drag over the panel is taken before the screen sees it, so it never
  counts as a click outside and drops the carried stack. So is the wheel over it, so the screen behind does not scroll:
  the screen has a `mouseScrolled` of its own from 1.21.2, and before that the mixin adds one over the
  inherited default (with a horizontal amount from 1.20.2). Alt and a press on a filled menu slot stores
  it. The button numbers are the game's own (`InputConstants`): 26.3 moved to SDL, where left is 1.
- **Sync**: while the screen is open, the panel asks the server for the contents once a backpack is
  worn and drops them once it is not, checked every frame against the chest slot.

## The placed backpack's screen

`BackpackScreen` draws one window around the slots `BackpackMenu` placed (the backpack's in rows of
four on the left, the player's inventory on the right), with `VanillaWindow`, so it looks like the
panel. Vanilla draws the slots' items, the labels and the tooltip. From 26.1 the size goes to the
constructor and the window is drawn in `extractBackground`; before, `renderBg` draws it and `render`
adds the tooltip (and on 1.20.1 the darkened world first).

## Registering, per loader

Each loader registers the model layer and adds the render layer to every living entity renderer, and
`BackpackWornLayer.create` keeps the ones whose model is humanoid: players, mannequins, armor stands,
zombies, skeletons, piglins and the rest.

| Loader | Where | How |
|---|---|---|
| Fabric | `src/client/fabric/.../platform/FabricClientRegistration` | `ModelLayerRegistry` and `LivingEntityRenderLayerRegistrationCallback` from 26.1; `EntityModelLayerRegistry` and `LivingEntityFeatureRendererRegistrationCallback` before |
| NeoForge | `src/client/neoforge/.../platform/NeoForgeClientRegistration` | `RegisterLayerDefinitions`; `AddLayers` over its entity types and player skins |
| Forge | `src/client/forge/.../BackpacksRefinedForgeClient` | the same events; Forge 47 has no list of entity types, so it asks for every one in the registry |

The same places register the placed backpack's model layer and its block entity renderer (Fabric
straight into `BlockEntityRenderers`, NeoForge and Forge through `RegisterRenderers`), and the config
screen: ModMenu's `modmenu` entrypoint (`BackpacksRefinedModMenu`) on Fabric, `IConfigScreenFactory`
on NeoForge, `ConfigScreenFactory` on Forge, each only when Cloth Config is loaded. They also give each
tier's menu type its screen: `MenuScreens.register` on Fabric (Fabric API opens it to mods) and on Forge
(in client setup), `RegisterMenuScreensEvent` on NeoForge.

## Drawing the placed backpack

The block's render shape is invisible; `BackpackBlockRenderer` draws it from the old mod's block model on
`textures/block/<tier>.png`, so the lid can turn. The block models stay for the item and the breaking
particles. The item is drawn with the block model, whose texture has cut-out pixels: Fabric before
26.1 needs `BlockRenderLayerMap` (in `FabricClientRegistration`), NeoForge and Forge read `render_type`
in the model file, and from 26.1 the game sees it in the texture.

The block entity hears of the lid and of being set down through block events and keeps their game
times; `BackpackBlockPose` works the pose out from them each frame. From 1.21.9 the pose goes into the
render state and the model is posed in `setupAnim`, when it is drawn, as for the worn backpack. The
no-cull cutout render type is `entityCutout` from 26.1, `entityCutoutNoCull` before, and 26.3 submits
the breaking cracks with `submitCrumblingOverlay`.

## Checking it

Summon an armor stand, a mannequin (26.x) and a husk wearing one, then capture them from behind:

```
summon minecraft:armor_stand ~ ~ ~2 {equipment:{chest:{id:"backpacksrefined:backpack"}}}
```

The agent client has no third-person view, so a player's own backpack is checked through a mannequin,
which is drawn by the player renderer.
