package com.backpacksrefined.fabric;

import com.backpacksrefined.BackpacksRefined;
import net.fabricmc.api.ModInitializer;

/** The Fabric {@code main} entrypoint. Everything it starts is loader independent. */
public final class BackpacksRefinedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BackpacksRefined.initialize();
    }
}
