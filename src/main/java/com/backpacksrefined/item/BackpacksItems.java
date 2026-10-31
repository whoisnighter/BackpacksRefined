package com.backpacksrefined.item;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.block.BackpacksBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.BiConsumer;

/**
 * The mod's items. Each loader's mod class calls {@link #register} at the point its loader takes new
 * items, after the blocks: at startup on Fabric and in the registry event on NeoForge and Forge.
 */
public final class BackpacksItems {
    public static final Identifier BACKPACK_ID = BackpacksRefined.id("backpack");

    private static Item backpack;

    private BackpacksItems() { }

    /** Builds every item and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, Item> registry) {
        backpack = new BackpackItem(BACKPACK_ID, BackpacksBlocks.backpack(), new Item.Properties().stacksTo(1));
        registry.accept(BACKPACK_ID, backpack);
    }

    public static Item backpack() {
        if (backpack == null) throw new IllegalStateException("Items are not registered yet");
        return backpack;
    }
}
