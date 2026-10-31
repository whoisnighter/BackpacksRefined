package com.backpacksrefined.neoforge;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.block.BackpacksBlockEntities;
import com.backpacksrefined.block.BackpacksBlocks;
import com.backpacksrefined.item.BackpacksCreativeTabs;
import com.backpacksrefined.item.BackpacksItems;
import com.backpacksrefined.platform.Network;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
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
        modBus.addListener((RegisterEvent event) -> {
            event.register(Registries.SOUND_EVENT, helper -> BackpacksSounds.register(helper::register));
            event.register(Registries.BLOCK, helper -> BackpacksBlocks.register(helper::register));
            event.register(Registries.BLOCK_ENTITY_TYPE, helper -> BackpacksBlockEntities.register(helper::register));
            event.register(Registries.ITEM, helper -> BackpacksItems.register(helper::register));
            event.register(Registries.CREATIVE_MODE_TAB,
                    helper -> BackpacksCreativeTabs.register(helper::register, CreativeModeTab::builder));
        });
        modBus.addListener(Network::register);
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BackpacksItems.backpack());
        });
    }
}
