package com.backpacksrefined.dev;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.dev.agent.backpacks.AgentCommand;
import com.backpacksrefined.dev.agent.backpacks.BackpacksAgent;
import com.backpacksrefined.dev.harness.DevEnvironment;
import com.backpacksrefined.dev.platform.DevLoader;
import net.minecraft.server.MinecraftServer;

/**
 * What the dev tools mod registers on a server, whichever loader it is: the agent's queue, its
 * {@code /backpacks agent} command and the tick that polls it.
 *
 * <p>Nothing is registered unless {@link DevEnvironment#dev()} is true: the loader's own
 * development-environment check, true under every run task and false in the jar players install, and
 * this source set is not in that jar either way.
 *
 * <p>This class is safe to load on a dedicated server: it names no client class. The client half is
 * {@link BackpacksDevClient}, which the client entrypoint calls instead.
 */
public final class BackpacksDev {
    private BackpacksDev() { }

    /**
     * Installs the agent on a dedicated server. Its queue is polled on the server tick and opened once
     * the server is up, which is also when a launch's script runs.
     */
    public static void initializeServer() {
        if (!enabled()) return;
        BackpacksAgent.install(false, BackpacksDev::halt);
        registerCommand();
        DevLoader.onServerTickEnd(BackpacksAgent::serverTick);
        DevLoader.onServerStarted(server -> BackpacksAgent.start());
        DevLoader.onServerStopping(server -> BackpacksAgent.flush());
    }

    /** Whether the dev tools should register anything at all, with the reason logged when they should not. */
    public static boolean enabled() {
        if (DevEnvironment.dev()) return true;
        BackpacksRefined.LOGGER.warn("backpacksrefined_dev is loaded outside a development run; "
                + "the agent stays disabled");
        return false;
    }

    /** Adds {@code /backpacks agent} to the server's command tree, on a client's integrated server too. */
    public static void registerCommand() {
        DevLoader.onRegisterCommands(AgentCommand::register);
    }

    private static void halt() {
        MinecraftServer server = BackpacksAgent.server();
        if (server != null) server.halt(false);
    }
}
