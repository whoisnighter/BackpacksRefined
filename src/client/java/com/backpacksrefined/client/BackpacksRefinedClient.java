package com.backpacksrefined.client;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.client.panel.BackpackPanel;
import com.backpacksrefined.client.platform.ClientNetwork;

/** The loader independent client entrypoint. Each loader's client entrypoint calls it once. */
public final class BackpacksRefinedClient {
    private BackpacksRefinedClient() { }

    public static void initialize() {
        BackpacksRefined.LOGGER.info("{} client starting", BackpacksRefined.MOD_NAME);
        ClientNetwork.register();
        BackpackPanel.register();
    }
}
