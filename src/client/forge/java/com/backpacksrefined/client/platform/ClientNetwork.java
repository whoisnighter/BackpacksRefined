package com.backpacksrefined.client.platform;

import com.backpacksrefined.network.BackpackMessage;
import com.backpacksrefined.network.BackpackNetwork.Channel;
import com.backpacksrefined.platform.Network;

/**
 * The client half of {@code platform/Network} on Forge: sending to the server. The server's messages
 * are received through the handlers {@code Network} registers. Each loader has its own copy, with the
 * same public signatures.
 */
public final class ClientNetwork {
    private ClientNetwork() { }

    /** Nothing to do: the simple channel registers both directions at once. */
    public static void register() { }

    public static <T extends BackpackMessage> void send(Channel<T> channel, T message) {
        Network.CHANNEL.sendToServer(message);
    }
}
