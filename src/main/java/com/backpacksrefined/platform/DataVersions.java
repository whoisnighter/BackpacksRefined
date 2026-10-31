package com.backpacksrefined.platform;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/**
 * The game's data version, and its data fixer brought to bear on an item stack saved by an older one.
 * The data version became a record component in 1.21.6; before, it was read through getters. Nodes
 * between 1.21.6 and 1.21.11 have no branch of their own here.
 */
public final class DataVersions {
    private DataVersions() { }

    /** The data version of the running game, as it writes into every file it saves. */
    public static int current() {
        //? if >=1.21.11 {
        return SharedConstants.getCurrentVersion().dataVersion().version();
        //?} else {
        /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        *///?}
    }

    /** {@code item}, saved by the game at data version {@code from}, as the running game reads it. */
    public static <T> Dynamic<T> fixItem(Dynamic<T> item, int from) {
        return DataFixers.getDataFixer().update(References.ITEM_STACK, item, from, current());
    }
}
