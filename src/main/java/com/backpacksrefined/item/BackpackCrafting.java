package com.backpacksrefined.item;

import com.backpacksrefined.platform.Stacks;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A backpack crafted from another backpack (an iron one from a leather one, say) keeps the one it was
 * made from: its id, and with it its contents, its name and the rest of its data, as a smithing upgrade
 * keeps them. Called by {@code mixin/ShapedRecipeMixin} with what a shaped recipe made.
 */
public final class BackpackCrafting {
    private BackpackCrafting() { }

    /** {@code result} with the data of the one backpack among {@code inputs}, or as it is if there is none or more. */
    public static ItemStack carryOver(List<ItemStack> inputs, ItemStack result) {
        if (!(result.getItem() instanceof BackpackItem)) return result;
        ItemStack from = null;
        for (ItemStack input : inputs) {
            if (!(input.getItem() instanceof BackpackItem)) continue;
            if (from != null) return result;
            from = input;
        }
        return from == null ? result : Stacks.transmute(from, result);
    }
}
