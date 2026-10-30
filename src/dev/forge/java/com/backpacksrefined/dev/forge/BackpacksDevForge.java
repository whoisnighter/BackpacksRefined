package com.backpacksrefined.dev.forge;

import com.backpacksrefined.dev.BackpacksDev;
import com.backpacksrefined.dev.BackpacksDevClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * The dev tools mod on Forge: the agent's queue, polled on the server tick on a dedicated server and on
 * the client tick on a client. Forge 47 has one mod class per mod rather than one per side, so this
 * asks which side it is on.
 */
@Mod(BackpacksDevForge.MOD_ID)
public final class BackpacksDevForge {
    /** Forge mod ids cannot contain a hyphen. */
    public static final String MOD_ID = "backpacksrefined_dev";

    public BackpacksDevForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) BackpacksDevClient.initialize();
        else BackpacksDev.initializeServer();
    }
}
