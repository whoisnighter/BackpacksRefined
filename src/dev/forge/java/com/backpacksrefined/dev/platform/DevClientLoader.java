package com.backpacksrefined.dev.platform;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    /** Runs after any screen is set up, with a way to add a button to it. */
    public static void afterScreenInit(BiConsumer<Screen, Consumer<Button>> handler) {
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) ->
                handler.accept(event.getScreen(), event::addListener));
    }
}
