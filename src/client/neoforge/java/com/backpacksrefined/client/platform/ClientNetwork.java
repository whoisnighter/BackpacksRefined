package com.backpacksrefined.client.platform;

import com.backpacksrefined.network.BackpackMessage;
import com.backpacksrefined.network.BackpackNetwork.Channel;
import com.backpacksrefined.platform.Network;
//? if >=1.21.9 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}

/**
 * The client half of {@code platform/Network} on NeoForge: sending to the server. The server's messages
 * are received through the handlers {@code Network} registers. NeoForge moved sending to the server
 * into a client class in 1.21.9. Each loader has its own copy, with the same public signatures.
 */
public final class ClientNetwork {
    private ClientNetwork() { }

    /** Nothing to do: NeoForge registers both directions in its payload event. */
    public static void register() { }

    public static <T extends BackpackMessage> void send(Channel<T> channel, T message) {
        //? if >=1.21.9 {
        ClientPacketDistributor.sendToServer(new Network.Packet(Network.type(channel), message));
        //?} else {
        /*PacketDistributor.sendToServer(new Network.Packet(Network.type(channel), message));
        *///?}
    }
}
