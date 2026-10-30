package com.backpacksrefined.dev.neoforge;

import com.backpacksrefined.dev.BackpacksDevClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** The dev tools mod on NeoForge, client side: the agent's queue, polled on the client tick. */
@Mod(value = BackpacksDevNeoForge.MOD_ID, dist = Dist.CLIENT)
public final class BackpacksDevNeoForgeClient {
    public BackpacksDevNeoForgeClient() {
        BackpacksDevClient.initialize();
        LoadingWarnings.register();
    }
}
