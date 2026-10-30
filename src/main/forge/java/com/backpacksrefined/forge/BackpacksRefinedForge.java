package com.backpacksrefined.forge;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.client.forge.BackpacksRefinedForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * The Forge mod class. Forge 47 has no client-only mod class, so the client half is started from here,
 * on a client only, which is what keeps the client sources off a dedicated server.
 */
@Mod(BackpacksRefined.MOD_ID)
public final class BackpacksRefinedForge {
    public BackpacksRefinedForge() {
        BackpacksRefined.initialize();
        if (FMLEnvironment.dist == Dist.CLIENT) BackpacksRefinedForgeClient.initialize();
    }
}
