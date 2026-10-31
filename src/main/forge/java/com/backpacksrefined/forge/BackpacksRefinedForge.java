package com.backpacksrefined.forge;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.client.forge.BackpacksRefinedForgeClient;
import com.backpacksrefined.item.BackpacksItems;
import net.minecraft.core.registries.Registries;
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
        modBus.addListener((RegisterEvent event) ->
                event.register(Registries.ITEM, helper -> BackpacksItems.register(helper::register)));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BackpacksItems.backpack());
        });
        if (FMLEnvironment.dist == Dist.CLIENT) BackpacksRefinedForgeClient.initialize();
    }
}
