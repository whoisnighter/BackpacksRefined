package com.backpacksrefined.dev.platform;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * The client half of {@link DevLoader}, for Forge. Loaded only from the dev tools' client half, so a
 * dedicated server never reaches a class that names a client-only event.
 */
public final class DevClientLoader {
    private DevClientLoader() { }

    /** Runs at the end of every client tick, whether or not a world is loaded. */
    public static void onClientTickEnd(Runnable handler) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) handler.run();
        });
    }

    /**
     * Runs as the client shuts down. Forge, like NeoForge, fires no event for it, so nothing is
     * registered; see the NeoForge copy for why a shutdown hook is not used instead.
     */
    public static void onClientStopping(Runnable handler) {
    }
}
