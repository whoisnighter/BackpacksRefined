package com.backpacksrefined.item;

import com.backpacksrefined.platform.WearableBlockItem;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** The backpack: worn in the chest slot, swapped on with a right click, placed down on a block. */
public class BackpackItem extends WearableBlockItem {
    public BackpackItem(Identifier id, Block block, Properties properties) {
        super(id, block, BackpacksSounds.EQUIP, properties);
    }

    /** A backpack holds items of its own, so it never goes inside a bundle. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
