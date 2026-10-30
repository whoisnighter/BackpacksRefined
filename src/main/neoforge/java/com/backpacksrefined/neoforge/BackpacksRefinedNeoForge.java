package com.backpacksrefined.neoforge;

import com.backpacksrefined.BackpacksRefined;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** The NeoForge mod class. Everything it starts is loader independent. */
@Mod(BackpacksRefined.MOD_ID)
public final class BackpacksRefinedNeoForge {
    public BackpacksRefinedNeoForge(IEventBus modBus) {
        BackpacksRefined.initialize();
    }
}
