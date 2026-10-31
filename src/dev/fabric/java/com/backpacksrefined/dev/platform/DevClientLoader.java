package com.backpacksrefined.dev.platform;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The client half of {@link DevLoader}, for Fabric. Loaded only from the dev tools' client entrypoint,
 * so a dedicated server never reaches a class that names a client-only event.
 */
public final class DevClientLoader {
    private DevClientLoader() { }

    /** Runs at the end of every client tick, whether or not a world is loaded. */
    public static void onClientTickEnd(Runnable handler) {
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> handler.run());
    }

    /** Runs as the client shuts down. */
    public static void onClientStopping(Runnable handler) {
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> handler.run());
    }

    /**
     * Runs after any screen is set up, with a way to add a button to it. Fabric API for 26.1 renamed
     * {@code getButtons} to {@code getWidgets}, after the Mojang names.
     */
    public static void afterScreenInit(BiConsumer<Screen, Consumer<Button>> handler) {
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) ->
                //? if >1.21.11 {
                handler.accept(screen, Screens.getWidgets(screen)::add));
                //?} else {
                /*handler.accept(screen, Screens.getButtons(screen)::add));
                *///?}
    }
}
