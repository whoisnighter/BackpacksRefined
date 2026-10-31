package com.backpacksrefined.dev;

import com.backpacksrefined.dev.agent.backpacks.ClientWindow;
import com.backpacksrefined.dev.agent.backpacks.Recorder;
import com.backpacksrefined.dev.agent.backpacks.BackpacksAgent;
import com.backpacksrefined.dev.platform.DevClientLoader;
import com.backpacksrefined.dev.world.TestWorld;
import net.minecraft.client.Minecraft;

/**
 * What the dev tools mod registers on a client. The client half of {@link BackpacksDev}, kept apart from
 * it because a dedicated server must never load a class that names {@link Minecraft}.
 *
 * <p>The queue is polled on the client tick rather than the server tick, even when the client has an
 * integrated server. One process has one queue, and it belongs to the side that can answer about the
 * HUD and the framebuffer; {@code server.*} commands reach the integrated server from there.
 */
public final class BackpacksDevClient {
    private static boolean started;
    private static long ticks;

    private BackpacksDevClient() { }

    public static void initialize() {
        if (!BackpacksDev.enabled()) return;
        BackpacksAgent.install(true, () -> Minecraft.getInstance().stop());
        BackpacksDev.registerCommand();
        // Opened on the first tick rather than at initialization: the window, the framebuffer and the
        // key mappings all exist by then, and a script may ask about any of them in its first line.
        DevClientLoader.onClientTickEnd(() -> {
            if (!started) {
                started = true;
                // An unattended client has no focused window. Leaving vanilla's focus pause on would
                // keep opening PauseScreen and make client.hold report stationary keys rather than
                // the player's movement state the probe is meant to exercise.
                Minecraft.getInstance().options.pauseOnLostFocus = false;
                // And, when this client is driven rather than played, it opens maximised and stops
                // taking the mouse pointer, so the desktop around it stays usable. See ClientWindow.
                ClientWindow.open(Minecraft.getInstance());
                BackpacksAgent.start();
            }
            // Before the queue is polled: a script's first line waits for a world, and on a driven
            // client the prompt vanilla puts in front of one is nobody's to press.
            ClientWindow.passWorldPrompt(Minecraft.getInstance());
            // Before the queue too, which waits while a recording's frame is due. See Recorder.
            if (Recorder.clientTick(Minecraft.getInstance())) BackpacksAgent.tick();
            ClientWindow.showStatus(Minecraft.getInstance(), BackpacksAgent.status(), ++ticks);
            TestWorld.tick(Minecraft.getInstance());
        });
        // The "Test world" button on the world list, for opening a fresh test world by hand.
        DevClientLoader.afterScreenInit(TestWorld::addButton);
        DevClientLoader.onClientStopping(BackpacksAgent::flush);
    }
}
