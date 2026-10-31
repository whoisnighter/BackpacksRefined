package com.backpacksrefined.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
//?}

/**
 * Item stacks where versions differ: 1.20.5 made a stack's extra data components, compared with
 * {@code isSameItemSameComponents} and written through a stream codec that needs the registries; before,
 * a tag, compared with {@code isSameItemSameTags} and written by the buffer itself. A stack turned into
 * another item keeps its components through {@code transmuteCopy}, and its tag before.
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

    /** {@code result}'s item and count with {@code from}'s data, as a smithing upgrade makes it. */
    public static ItemStack transmute(ItemStack from, ItemStack result) {
        //? if >=1.20.5 {
        return from.transmuteCopy(result.getItem(), result.getCount());
        //?} else {
        /*ItemStack copy = result.copy();
        if (from.getTag() != null) copy.setTag(from.getTag().copy());
        return copy;
        *///?}
    }
}
