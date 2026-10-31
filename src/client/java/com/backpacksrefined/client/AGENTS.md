# com.backpacksrefined.client

The loader independent client code. On Fabric it is its own source set; on NeoForge and Forge it is
compiled into `main` and only reached from the client (see the root `AGENTS.md`).

| Class | What it is |
|---|---|
| `BackpacksRefinedClient` | The common client entrypoint each loader's client entrypoint calls |
| `BackpackRendering` | The worn backpack's model layer, texture and shape (`createWornLayer`), the same on every version |
| `platform/BackpackWornModel` | A humanoid model with every part hidden but the backpack hung on its body |
| `platform/BackpackWornLayer` | The render layer that draws it, and `create(renderer, models)`, which makes one for a renderer with a humanoid model |

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

## Registering, per loader

Each loader registers the model layer and adds the render layer to every living entity renderer, and
`BackpackWornLayer.create` keeps the ones whose model is humanoid: players, mannequins, armor stands,
zombies, skeletons, piglins and the rest.

| Loader | Where | How |
|---|---|---|
| Fabric | `src/client/fabric/.../platform/FabricClientRegistration` | `ModelLayerRegistry` and `LivingEntityRenderLayerRegistrationCallback` from 26.1; `EntityModelLayerRegistry` and `LivingEntityFeatureRendererRegistrationCallback` before |
| NeoForge | `src/client/neoforge/.../platform/NeoForgeClientRegistration` | `RegisterLayerDefinitions`; `AddLayers` over its entity types and player skins |
| Forge | `src/client/forge/.../BackpacksRefinedForgeClient` | the same events; Forge 47 has no list of entity types, so it asks for every one in the registry |

The placed block is a plain block model, not drawn from here. Its texture has cut-out pixels: Fabric
before 26.1 needs `BlockRenderLayerMap` (in `FabricClientRegistration`), NeoForge and Forge read
`render_type` in the model file, and from 26.1 the game sees it in the texture.

## Checking it

Summon an armor stand, a mannequin (26.x) and a husk wearing one, then capture them from behind:

```
summon minecraft:armor_stand ~ ~ ~2 {equipment:{chest:{id:"backpacksrefined:backpack"}}}
```

The agent client has no third-person view, so a player's own backpack is checked through a mannequin,
which is drawn by the player renderer.
