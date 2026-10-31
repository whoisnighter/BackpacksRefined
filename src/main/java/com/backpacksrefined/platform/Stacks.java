package com.backpacksrefined.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
//?}

/**
 * Item stacks where versions differ: 1.20.5 made a stack's extra data components, compared with
 * {@code isSameItemSameComponents} and written through a stream codec that needs the registries; before,
 * a tag, compared with {@code isSameItemSameTags} and written by the buffer itself.
 */
public final class Stacks {
    private Stacks() { }

    /** Whether the two stacks are the same item with the same data, so they stack together. */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        //? if >=1.20.5 {
        return ItemStack.isSameItemSameComponents(a, b);
        //?} else {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?}
    }

    /** Writes a stack, empty or not, into a play-phase buffer. */
    public static void write(FriendlyByteBuf buf, ItemStack stack) {
        //? if >=1.20.5 {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
        //?} else {
        /*buf.writeItem(stack);
        *///?}
    }

    public static ItemStack read(FriendlyByteBuf buf) {
        //? if >=1.20.5 {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        //?} else {
        /*return buf.readItem();
        *///?}
    }
}
