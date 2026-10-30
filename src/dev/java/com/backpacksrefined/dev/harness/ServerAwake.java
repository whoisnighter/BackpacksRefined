package com.backpacksrefined.dev.harness;

import com.backpacksrefined.BackpacksRefined;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;

/**
 * Keeps a dedicated server with nobody online from pausing, for as long as a dev tool needs it to
 * keep ticking.
 *
 * <p>A dedicated server stops ticking once it has been empty for {@code pause-when-empty-seconds} (60
 * by default), and a paused server fires no tick event. The agent's queue is polled on that tick, so it
 * would stop being answered a minute after startup, which is before a client can be brought up.
 *
 * <p>There is no public way to hold the pause off; the field name is the Mojang name every supported
 * version runs with in a development environment, and a version that renames it logs once and loses
 * nothing but the keep-awake. Commands are still handled while the server is paused, so calling this
 * also wakes one that has already paused.
 *
 * <p>Only a dedicated server needs it. A client's integrated server pauses on the player's rules, and
 * the agent has no business overriding them; vanilla leaves {@code emptyTicks} alone there in any
 * case, so the guard costs nothing and says what is meant.
 */
public final class ServerAwake {
    private static final Field EMPTY_TICKS = emptyTicksField();
    private static boolean warned;

    private ServerAwake() { }

    /** Resets the empty-server counter. Called every tick a tool needs the server to keep running. */
    public static void keep(MinecraftServer server) {
        if (EMPTY_TICKS == null || server == null || !server.isDedicatedServer()) return;
        try {
            EMPTY_TICKS.setInt(server, 0);
        } catch (IllegalAccessException | RuntimeException e) {
            if (!warned) {
                warned = true;
                BackpacksRefined.LOGGER.warn("[BackpacksDev] could not keep the empty server from pausing; "
                        + "it stops ticking once it has been empty for pause-when-empty-seconds, and a "
                        + "the agent's queue stops with it", e);
            }
        }
    }

    private static Field emptyTicksField() {
        try {
            Field field = MinecraftServer.class.getDeclaredField("emptyTicks");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            BackpacksRefined.LOGGER.warn("[BackpacksDev] MinecraftServer.emptyTicks not found; an empty server "
                    + "may pause and stop answering", e);
            return null;
        }
    }
}
