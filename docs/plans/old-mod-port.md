# Plan: bringing over Backpacks for Dummies

What the mod has so far, what is left to bring over from
[Backpacks for Dummies](https://github.com/Spydnel/Backpacks) (the old mod this one is based on), and
the decisions already made about later work. Update it when a piece lands or a decision changes.
Holding items and tiers have their own plan: [backpack-panel.md](backpack-panel.md).

## Done

- **The backpack item.** One stack holds one backpack, and it can't go in a bundle. It burns, unlike the
  old mod's (see [Fire](#fire)). It is drawn in 3D everywhere, the placed block's model, where the old
  mod drew a flat sprite. It is listed in the Tools and Utilities creative tab and in the mod's own
  tab, whose icon is the old flat sprite.
- **Worn in the chest slot.** Using it in the air puts it on, or swaps it with what is worn there; the
  armor slot takes it by click or shift click. Equip sound: leather armor (see [Sounds](#sounds)).
- **Drawn on the wearer.** The old mod's worn model, on players, armor stands, mannequins and every mob
  drawn with a humanoid model. It bends and turns with the body. A player's cape is hidden while they
  wear one (`CapeLayerMixin`), as in the old mod.
- **Placed as a block.** Using it on a block sets it down, facing the player who placed it. Its shape is
  smaller than a full block; breaking it drops the backpack, its items still in it.
- **Floating and waterlogging.** A placed backpack can stand in water. Over a water source, and not in
  water itself, it floats: it sits 3 pixels lower, half in the water, and its shape is 8 pixels tall.
  It changes as the water under it comes and goes.
- **The block's animations.** The placed backpack is drawn by a block entity renderer again, from the
  old mod's block model. Its lid springs open while anyone has it open and drops shut with a bounce
  (block event 1, the number of openers). The drop when set down (block event 2) and the bobbing on
  water are the old mod's, and are off unless turned on in the config (see [Config](#config)).
- **Recipe.** Four leather around a chest (the old mod had eight). The upper tiers are crafted from it, see [backpack-panel.md](backpack-panel.md).
- **Holding items.** 8 to 32 slots by tier (leather, copper, iron, gold, diamond, netherite), not the old mod's way: worn, in a panel beside the inventory and every
  container screen; placed, a right click opens them as a chest. See
  [backpack-panel.md](backpack-panel.md), which has what is left (tiers, accessory slots, ...).

Tested in game on 26.3-fabric only (`tools/agent/gameplay/wear.jsonl`, `tools/agent/gameplay/water.jsonl`,
and by hand: placing, breaking, the recipe, the worn model on an armor stand, a mannequin and a husk).
The cape mixin applies on 26.3-fabric, 1.21.1-neoforge and 1.20.1-forge (the game boots with it), but no
dev account has a cape, so hiding one has not been seen. Every node compiles and passes
`check`. The 1.21.1 and 1.20.1 nodes take the older code paths (`Equipable`, the old render layer, the
rewritten data files), so they are the ones to run next.

## Left out on purpose for now

- **Dyeing.** The old mod tinted an overlay texture with `dyed_color`, on the item, the worn model and
  the block. Its overlay textures are in the old repository (`textures/*/backpack_overlay.png`).
- **Vanity designs.** The old mod's `chest` and `full_toned` variants, through the Vanity mod.
- **The rope recipe** (the old `backpack_from_rope.json`, with Farmer's Delight installed). Dropped.
- **Sneak to pick up and set down** (the old `BackpackPickupEvents`: sneak and use an empty hand on a
  placed backpack to wear it, or on the top of a block while wearing one to set it down). Dropped.
- **A full backpack can't be taken off** (the old `ArmorSlotMixin` refused to pick a backpack with items
  out of the armor slot). Dropped: with no sneak to set one down it would leave a full backpack stuck
  on the back, and swapping a chestplate on got round it anyway.
- **The lid opening on the wearer** while someone looks inside the worn backpack (the old
  `LivingEntityMixin`, `OPEN_COUNT`). Dropped: only the placed backpack's lid opens.
- **The old mod's other languages** (German, Spanish (Mexico), Russian, Swedish). Dropped.

## Still to bring over

- **A dropped full backpack places itself.** The old `ItemEntityMixin` turned a dropped backpack with
  items into a placed block where it landed, so it never despawned.

## Decisions

### Config

- Client settings in `config/backpacksrefined.json`, read when the client starts: `bounceWhenPlaced` and
  `bobOnWater`, both off by default.
- The screen is built with Cloth Config, so it is the same on every loader and version. Cloth Config is
  optional: without it there is no screen, and the file still applies. ModMenu opens it on Fabric, the
  mod list's Config button on NeoForge and Forge.

### Sounds

- **Equip:** vanilla leather armor (`item.armor.equip_leather`) on every node, for every backpack.
- **Tiers:** iron and up (see [backpack-panel.md](backpack-panel.md#tiers)) equip with plain wolf armor
  (`item.armor.equip_wolf`, no pitch change), through the mod's `item.backpack.equip_armored` event;
  leather and copper keep leather's. 1.20.1 has no wolf armor sound, so `processResources` points the
  event at leather's there (the end of `gradle/shared.gradle.kts`). Opening a placed backpack plays its
  tier's equip sound too.
- **Place:** the bundle insert sound at pitch 0.1, as the old mod had it.

### Fire

Only the netherite tier survives fire and lava, like netherite gear
([backpack-panel.md](backpack-panel.md#tiers)). Every other backpack burns, the plain one included,
though the old mod's survived fire.
