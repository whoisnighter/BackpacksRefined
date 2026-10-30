package com.backpacksrefined.dev.neoforge;

import com.backpacksrefined.dev.BackpacksDev;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * The dev tools mod on NeoForge, dedicated server side: the agent's queue, polled on the server tick.
 * One class per side, the same shape the mod itself uses.
 */
@Mod(value = BackpacksDevNeoForge.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class BackpacksDevNeoForge {
    /** NeoForge mod ids cannot contain a hyphen. */
    public static final String MOD_ID = "backpacksrefined_dev";

    public BackpacksDevNeoForge() {
        BackpacksDev.initializeServer();
    }
}
