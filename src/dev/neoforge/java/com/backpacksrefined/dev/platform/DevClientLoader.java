package com.backpacksrefined.dev.platform;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The client half of {@link DevLoader}, for NeoForge. Loaded only from the dev tools' client
 * entrypoint, so a dedicated server never reaches a class that names a client-only event.
 */
public final class DevClientLoader {
    private DevClientLoader() { }

    /** Runs at the end of every client tick, whether or not a world is loaded. */
    public static void onClientTickEnd(Runnable handler) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> handler.run());
    }

    /**
     * Runs as the client shuts down — except that NeoForge fires no event for it, and the JVM's
     * shutdown hook is the wrong place: it runs beside the game thread, and what the queue does at
     * this point is answer deferred requests by reading client state. So nothing is registered here,
     * and a request still waiting when the window closes is simply never answered, which is what the
     * agent sees anyway when a client is killed.
     */
    public static void onClientStopping(Runnable handler) {
    }

    /** Runs after any screen is set up, with a way to add a button to it. */
    public static void afterScreenInit(BiConsumer<Screen, Consumer<Button>> handler) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) ->
                handler.accept(event.getScreen(), event::addListener));
    }
}
