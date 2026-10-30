package com.backpacksrefined.client;

import com.backpacksrefined.BackpacksRefined;

/** The loader independent client entrypoint. Each loader's client entrypoint calls it once. */
public final class BackpacksRefinedClient {
    private BackpacksRefinedClient() { }

    public static void initialize() {
        BackpacksRefined.LOGGER.info("{} client starting", BackpacksRefined.MOD_NAME);
    }
}
