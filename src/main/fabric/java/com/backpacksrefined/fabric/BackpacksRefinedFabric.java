package com.backpacksrefined.fabric;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.item.BackpacksItems;
import com.backpacksrefined.platform.CreativeTabs;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The Fabric {@code main} entrypoint. Everything it starts is loader independent. */
public final class BackpacksRefinedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BackpacksRefined.initialize();
        BackpacksItems.register((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        CreativeTabs.addToTools(BackpacksItems::backpack);
    }
}
