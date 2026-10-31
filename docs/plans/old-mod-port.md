# Plan: bringing over Backpacks for Dummies

What the mod has so far, what is left to bring over from
[Backpacks for Dummies](https://github.com/Spydnel/Backpacks) (the old mod this one is based on), and
the decisions already made about later work. Update it when a piece lands or a decision changes.
Holding items and tiers have their own plan: [backpack-panel.md](backpack-panel.md).

## Done

- **The backpack item.** One stack holds one backpack, and it can't go in a bundle. It burns, unlike the
  old mod's (see [Fire](#fire)). It uses the old mod's item sprite and is listed in the Tools and
  Utilities creative tab.
- **Worn in the chest slot.** Using it in the air puts it on, or swaps it with what is worn there; the
  armor slot takes it by click or shift click. Equip sound: leather armor (see [Sounds](#sounds)).
- **Drawn on the wearer.** The old mod's worn model, on players, armor stands, mannequins and every mob
  drawn with a humanoid model. It bends and turns with the body.
- **Placed as a block.** Using it on a block sets it down, facing the player who placed it. Its shape is
  smaller than a full block; breaking it drops the backpack. Nothing happens when it is used.
- **Recipe.** Eight leather around a chest.

Tested in game on 26.3-fabric only (`tools/agent/gameplay/wear.jsonl`, and by hand: placing, breaking,
the recipe, the worn model on an armor stand, a mannequin and a husk). Every node compiles and passes
`check`. The 1.21.1 and 1.20.1 nodes take the older code paths (`Equipable`, the old render layer, the
rewritten data files), so they are the ones to run next.

## Left out on purpose for now

- **Dyeing.** The old mod tinted an overlay texture with `dyed_color`, on the item, the worn model and
  the block. Its overlay textures are in the old repository (`textures/*/backpack_overlay.png`).
- **Vanity designs.** The old mod's `chest` and `full_toned` variants, through the Vanity mod.

## Still to bring over

- **Holding items.** Not the old mod's way (a shulker box menu, contents on the item and the block
  entity): [backpack-panel.md](backpack-panel.md) replaces it. Still open there: what a placed backpack
  does with its contents, and whether it can be opened at all.
- **A full backpack can't be taken off.** The old `ArmorSlotMixin` refused to pick a backpack with items
  in it out of the armor slot.
- **Sneak to pick up and set down.** Sneak and use an empty hand on a placed backpack to wear it; sneak
  and use an empty hand on the top of a block, wearing one, to set it down (`BackpackPickupEvents`).
- **A dropped full backpack places itself.** The old `ItemEntityMixin` turned a dropped backpack with
  items into a placed block where it landed, so it never despawned.
- **The block's animations.** The lid opening, a bounce when placed, bobbing while floating. These need
  the block drawn by a block entity renderer again; it is a static block model now.
- **Floating and waterlogging.** The old block floated on water and could be waterlogged.
- **The lid opening on the wearer** while someone looks inside (`LivingEntityMixin`, `OPEN_COUNT`).
- **Hiding the cape** while a backpack is worn (`CapeLayerMixin`). The cape draws over it now.
- **The rope recipe** (`backpack_from_rope.json`), when Farmer's Delight is installed.
- **Other languages.** The old mod had German, Spanish (Mexico), Russian and Swedish names.

## Decisions

### Sounds

- **Equip:** vanilla leather armor (`item.armor.equip_leather`) on every node, for every backpack.
- **Tiers:** the upgraded tiers in [backpack-panel.md](backpack-panel.md#tiers) (iron and up) equip with
  plain wolf armor (`item.armor.equip_wolf`, no pitch change). 1.20.1 has no wolf armor sound, so it
  needs a fallback there.
- **Place:** the bundle insert sound at pitch 0.1, as the old mod had it.

### Fire

Only the netherite tier survives fire and lava, like netherite gear
([backpack-panel.md](backpack-panel.md#tiers)). Every other backpack burns, the plain one included,
though the old mod's survived fire.
