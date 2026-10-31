package com.backpacksrefined.platform;

import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
//?}
//? if >=1.21.5 {
import net.minecraft.core.UUIDUtil;
//?}

import java.util.UUID;

/**
 * The id a backpack item carries, which names its contents in the storage. It is in the item's custom
 * data from 1.20.5 and in its tag before; from 1.21.5 a tag reads and writes it through a codec.
 * Nodes between 1.20.5 and 1.21.5 but 1.21.1 have no branch of their own here.
 */
public final class BackpackIds {
    private static final String KEY = "backpacksrefined:id";

    private BackpackIds() { }

    /** The id on {@code stack}, or null for a backpack that has never been opened. */
    public static UUID get(ItemStack stack) {
        //? if >=1.20.5 {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        //?} else {
        /*net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        *///?}
        //? if >=1.21.5 {
        return tag.read(KEY, UUIDUtil.CODEC).orElse(null);
        //?} else {
        /*return tag.hasUUID(KEY) ? tag.getUUID(KEY) : null;
        *///?}
    }

    public static void set(ItemStack stack, UUID id) {
        //? if >=1.21.5 {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.store(KEY, UUIDUtil.CODEC, id));
        //?} else if >=1.20.5 {
        /*CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(KEY, id));
        *///?} else {
        /*stack.getOrCreateTag().putUUID(KEY, id);
        *///?}
    }

    /** The id on {@code stack}, given one first if it has none. Server side only. */
    public static UUID getOrCreate(ItemStack stack) {
        UUID id = get(stack);
        if (id == null) {
            id = UUID.randomUUID();
            set(stack, id);
        }
        return id;
    }
}
