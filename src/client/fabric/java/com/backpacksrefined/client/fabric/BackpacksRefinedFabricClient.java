package com.backpacksrefined.client.fabric;

import com.backpacksrefined.client.BackpacksRefinedClient;
import net.fabricmc.api.ClientModInitializer;

/** The Fabric {@code client} entrypoint. Everything it starts is loader independent. */
public final class BackpacksRefinedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BackpacksRefinedClient.initialize();
    }
}
