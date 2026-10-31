package com.backpacksrefined.item;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * A backpack's tier: each is its own item and block, a row of four slots more than the one before, but
 * netherite, two rows more than diamond.
 * The panel always shows the largest tier's slots, and those past the worn backpack's are locked.
 */
public enum BackpackTier {
    LEATHER("backpack", 8, false),
    COPPER("copper_backpack", 12, false),
    IRON("iron_backpack", 16, false),
    GOLD("gold_backpack", 20, false),
    DIAMOND("diamond_backpack", 24, false),
    NETHERITE("netherite_backpack", 32, true);

    /** The largest tier's slots: how many the panel shows. */
    public static final int MAX_SIZE = 32;

    private final String name;
    private final int size;
    private final boolean fireResistant;

    BackpackTier(String name, int size, boolean fireResistant) {
        this.name = name;
        this.size = size;
        this.fireResistant = fireResistant;
    }

    /** The item's and the block's path, and the name of its textures. */
    public String path() {
        return name;
    }

    public Identifier id() {
        return BackpacksRefined.id(name);
    }

    public int size() {
        return size;
    }

    /** What it sounds like put on, or opened where it is placed: leather up to copper, wolf armor from iron. */
    public SoundEvent equipSound() {
        return ordinal() >= IRON.ordinal() ? BackpacksSounds.EQUIP_ARMORED : BackpacksSounds.EQUIP;
    }

    /** Whether it survives fire and lava, as netherite gear does. */
    public boolean fireResistant() {
        return fireResistant;
    }
}
