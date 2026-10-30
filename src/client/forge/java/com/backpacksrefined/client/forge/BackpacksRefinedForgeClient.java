package com.backpacksrefined.client.forge;

import com.backpacksrefined.client.BackpacksRefinedClient;

/** The Forge client half, started by the mod class on a client only. */
public final class BackpacksRefinedForgeClient {
    private BackpacksRefinedForgeClient() { }

    public static void initialize() {
        BackpacksRefinedClient.initialize();
    }
}
