package com.backpacksrefined.forge;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.block.BackpacksBlockEntities;
import com.backpacksrefined.block.BackpacksBlocks;
import com.backpacksrefined.client.forge.BackpacksRefinedForgeClient;
import com.backpacksrefined.item.BackpacksCreativeTabs;
import com.backpacksrefined.item.BackpacksItems;
import com.backpacksrefined.platform.Network;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

/**
 * The Forge mod class. Forge 47 has no client-only mod class, so the client half is started from here,
 * on a client only, which is what keeps the client sources off a dedicated server.
 */
@Mod(BackpacksRefined.MOD_ID)
public final class BackpacksRefinedForge {
    public BackpacksRefinedForge() {
        BackpacksRefined.initialize();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterEvent event) -> {
            event.register(Registries.SOUND_EVENT, helper -> BackpacksSounds.register(helper::register));
            event.register(Registries.BLOCK, helper -> BackpacksBlocks.register(helper::register));
            event.register(Registries.BLOCK_ENTITY_TYPE, helper -> BackpacksBlockEntities.register(helper::register));
            event.register(Registries.ITEM, helper -> BackpacksItems.register(helper::register));
            event.register(Registries.CREATIVE_MODE_TAB,
                    helper -> BackpacksCreativeTabs.register(helper::register, CreativeModeTab::builder));
        });
        Network.register();
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) BackpacksItems.all().forEach(event::accept);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) BackpacksRefinedForgeClient.initialize(modBus);
    }
}
