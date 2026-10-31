package com.backpacksrefined.item;

import com.backpacksrefined.platform.WearableBlockItem;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.world.level.block.Block;

/**
 * A backpack of one tier: worn in the chest slot, swapped on with a right click, placed down on a block.
 * It holds {@link #size()} stacks, kept in the storage under the id it carries (see {@code BackpackStorage}).
 */
public class BackpackItem extends WearableBlockItem {
    private final BackpackTier tier;

    public BackpackItem(BackpackTier tier, Block block, Properties properties) {
        super(tier.id(), block, BackpacksSounds.EQUIP, properties);
        this.tier = tier;
    }

    public BackpackTier tier() {
        return tier;
    }

    public int size() {
        return tier.size();
    }

    /** A backpack holds items of its own, so it never goes inside a bundle. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
