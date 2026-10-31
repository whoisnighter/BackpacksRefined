package com.backpacksrefined.neoforge;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.item.BackpacksItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge mod class. Everything it starts is loader independent. */
@Mod(BackpacksRefined.MOD_ID)
public final class BackpacksRefinedNeoForge {
    public BackpacksRefinedNeoForge(IEventBus modBus) {
        BackpacksRefined.initialize();
        modBus.addListener((RegisterEvent event) ->
                event.register(Registries.ITEM, helper -> BackpacksItems.register(helper::register)));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BackpacksItems.backpack());
        });
    }
}
