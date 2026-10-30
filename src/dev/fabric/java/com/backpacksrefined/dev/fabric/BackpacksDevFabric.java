package com.backpacksrefined.dev.fabric;

import com.backpacksrefined.dev.BackpacksDev;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The dev tools mod's main entrypoint on Fabric: the agent on a dedicated server. Fabric runs this
 * entrypoint on a client as well, where the agent belongs to {@link BackpacksDevFabricClient} instead.
 */
public final class BackpacksDevFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) BackpacksDev.initializeServer();
    }
}
