package com.backpacksrefined.fabric;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.block.BackpacksBlockEntities;
import com.backpacksrefined.block.BackpacksBlocks;
import com.backpacksrefined.item.BackpacksCreativeTabs;
import com.backpacksrefined.item.BackpacksItems;
import com.backpacksrefined.platform.CreativeTabs;
import com.backpacksrefined.platform.Network;
import com.backpacksrefined.sound.BackpacksSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The Fabric {@code main} entrypoint. Everything it starts is loader independent. */
public final class BackpacksRefinedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BackpacksRefined.initialize();
        BackpacksSounds.register((id, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound));
        BackpacksBlocks.register((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        BackpacksBlockEntities.register((id, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type));
        BackpacksItems.register((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        BackpacksCreativeTabs.register((id, tab) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab), CreativeTabs::builder);
        CreativeTabs.addToTools(BackpacksItems::backpack);
        Network.register();
    }
}
