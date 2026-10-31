# src/main/resources

Shared by every node. Each loader adds its manifest from `src/main/<loader>/resources`. AGENTS.md files
are kept out of the jars.

```
assets/backpacksrefined/
  blockstates/backpack.json       turned by `facing`, the floating model when `floating`
  models/block/backpack.json      the placed backpack (see below)
  models/block/backpack_floating.json  the same, 3 pixels lower: half sunk into the water under it
  models/item/backpack.json       the item: the block model in 3D, with display transforms
  models/item/backpack_flat.json  the old mod's flat sprite, only for the creative tab's icon
  items/backpack.json             the item model definition 1.21.4 and later read; older versions ignore it
  textures/item/backpack.png      16x16 sprite
  textures/block/backpack.png     64x64, the old mod's textures/entity/backpack.png
  textures/entity/backpack.png    64x64, the worn model; the old mod's textures/model/backpack.png
  ...copper_, iron_, gold_, diamond_, netherite_backpack  the same files again for each upper tier, the
                                  models a child of the leather ones with their own textures (leather with
                                  metal trim, from tools/scripts/tiers/draw_tiers.py)
  sounds.json                     what each of the mod's sound events plays
  lang/                           en_us.json holds every key; checkLang holds the others to it
data/backpacksrefined/
  recipe/backpack.json            and copper_, iron_, gold_, diamond_ (shaped, around a backpack), netherite_ (smithing)
  loot_table/blocks/backpack.json
```

## Data files are written in 26.3's format

Like the sources, a data file is written for 26.3, and `processResources` rewrites it for older nodes
(the end of `gradle/shared.gradle.kts`):

| Before | Rewrite |
|---|---|
| 1.21 | `recipe/` and `loot_table/` become `recipes/` and `loot_tables/` |
| 1.21.2 | a recipe ingredient `"L": "minecraft:leather"` becomes `{"item": ...}`, `"#tag"` becomes `{"tag": ...}`; so do a smithing recipe's `template`, `base` and `addition` |
| 1.20.5 | a recipe result's `"id"` becomes `"item"` |

The ingredient rewrite works line by line and only on one-character keys, the keys a shaped recipe's
`key` has, and on a smithing recipe's three, so keep one ingredient a line. A format change it does not cover gets a rule there, not a
copy of the file per version. Check the result in `versions/<node>/build/resources/main/data`.

## The flat icon

The creative tab's icon is the backpack with custom model data set (`platform/FlatIcon`), and the
item models send that to `backpack_flat`: from 1.21.4 `items/backpack.json` selects on the string
`flat`; before, `models/item/backpack.json` has an override on `custom_model_data` 1, which 1.21.4 and
later ignore. Every other backpack is drawn in 3D.

## The block model

`models/block/backpack.json` is the old mod's block entity model (`BackpackModel.createBlockLayer`)
turned into block model elements: the base, the lid, and the lid's underside, a flat face seen from
below. Its UVs are the entity model's box UVs divided by 4 (64 pixels to 16 units). An entity model's
-y is up and its -x is east here, so its `DOWN` face is the block's `up` and its `WEST` face the block's
`east`. Facing north, the front, with the clasp, faces south: towards the player who placed it.

`render_type: cutout` is for NeoForge and Forge; the lid's sides have transparent pixels.

## Things that look missing but are not

- **No `equipment/backpack.json`.** The backpack's `equippable` names the equipment asset
  `backpacksrefined:backpack` and no file exists for it, on purpose: the armor layer then draws nothing,
  and the mod's own layer draws the backpack. See `src/client/java/com/backpacksrefined/client/AGENTS.md`.
- **No item lang key.** The item keeps the block's name, `block.backpacksrefined.backpack`, on every
  version.
