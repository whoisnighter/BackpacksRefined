package com.backpacksrefined.client.platform;

import com.backpacksrefined.network.BackpackMessage;
import com.backpacksrefined.network.BackpackNetwork;
import com.backpacksrefined.network.BackpackNetwork.Channel;
import com.backpacksrefined.platform.Network;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * The client half of {@code platform/Network} on Fabric: receivers for the server's messages, and
 * sending to the server. Each loader has its own copy, with the same public signatures.
 */
public final class ClientNetwork {
    private ClientNetwork() { }

    /** Registers a receiver for every channel the server sends on. Called once, after {@code Network.register}. */
    public static void register() {
        for (Channel<?> channel : BackpackNetwork.CHANNELS) {
            if (channel.toServer()) continue;
            ClientPlayNetworking.registerGlobalReceiver(Network.type(channel),
                    (packet, context) -> BackpackNetwork.handleClient(channel, packet.message()));
        }
    }

    public static <T extends BackpackMessage> void send(Channel<T> channel, T message) {
        ClientPlayNetworking.send(new Network.Packet(Network.type(channel), message));
    }
}
