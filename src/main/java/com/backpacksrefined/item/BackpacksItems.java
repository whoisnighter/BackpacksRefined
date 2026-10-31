package com.backpacksrefined.item;

import com.backpacksrefined.block.BackpacksBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The mod's items: a backpack for each tier. Each loader's mod class calls {@link #register} at the point
 * its loader takes new items, after the blocks: at startup on Fabric and in the registry event on
 * NeoForge and Forge.
 */
public final class BackpacksItems {
    public static final Identifier BACKPACK_ID = BackpackTier.LEATHER.id();

    private static List<Item> backpacks;

    private BackpacksItems() { }

    /** Builds every item and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, Item> registry) {
        List<Item> built = new ArrayList<>();
        for (BackpackTier tier : BackpackTier.values()) {
            Item.Properties properties = new Item.Properties().stacksTo(1);
            if (tier.fireResistant()) properties.fireResistant();
            Item item = new BackpackItem(tier, BackpacksBlocks.backpack(tier), properties);
            registry.accept(tier.id(), item);
            built.add(item);
        }
        backpacks = List.copyOf(built);
    }

    /** The plain, leather backpack. */
    public static Item backpack() {
        return backpack(BackpackTier.LEATHER);
    }

    public static Item backpack(BackpackTier tier) {
        return all().get(tier.ordinal());
    }

    /** Every tier's backpack, smallest first. */
    public static List<Item> all() {
        if (backpacks == null) throw new IllegalStateException("Items are not registered yet");
        return backpacks;
    }
}
