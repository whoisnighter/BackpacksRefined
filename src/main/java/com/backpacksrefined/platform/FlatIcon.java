package com.backpacksrefined.platform;

import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
//?}
//? if >=1.21.4 {
import java.util.List;
//?}

/**
 * Marks a stack to be drawn with its flat sprite instead of its 3D model, for the creative tab icon.
 * The item models pick the sprite by custom model data: from 1.21.4 the string {@value #MARK} in
 * {@code items/backpack.json}, before the number 1 in an override of {@code models/item/backpack.json}.
 * The number was a component from 1.20.5 and a tag before.
 */
public final class FlatIcon {
    public static final String MARK = "flat";

    private FlatIcon() { }

    public static ItemStack of(ItemStack stack) {
        //? if >=1.21.4 {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(MARK), List.of()));
        //?} else if >=1.20.5 {
        /*stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        *///?} else {
        /*stack.getOrCreateTag().putInt("CustomModelData", 1);
        *///?}
        return stack;
    }
}
