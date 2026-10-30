package com.backpacksrefined.dev.fabric;

import com.backpacksrefined.dev.BackpacksDevClient;
import net.fabricmc.api.ClientModInitializer;

/** The dev tools mod's client entrypoint on Fabric: the agent, polled on the client tick. */
public final class BackpacksDevFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BackpacksDevClient.initialize();
    }
}
