package com.backpacksrefined.item;

import com.backpacksrefined.platform.ChestWearableItem;
import net.minecraft.resources.Identifier;

/** The backpack: worn in the chest slot, swapped on with a right click. */
public class BackpackItem extends ChestWearableItem {
    public BackpackItem(Identifier id, Properties properties) {
        super(id, properties);
    }

    /** A backpack holds items of its own, so it never goes inside a bundle. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
