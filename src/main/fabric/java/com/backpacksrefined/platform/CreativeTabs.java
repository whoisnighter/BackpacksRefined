package com.backpacksrefined.platform;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
*///?}

import java.util.function.Supplier;

/** Fabric API's creative tab events, which 26.1 renamed to Minecraft's own names. */
public final class CreativeTabs {
    private CreativeTabs() { }

    public static void addToTools(Supplier<? extends ItemLike> item) {
        //? if >=26.1 {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(item.get()));
        //?} else {
        /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(item.get()));
        *///?}
    }
}
