package com.backpacksrefined.platform;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.network.BackpackMessage;
import com.backpacksrefined.network.BackpackNetwork;
import com.backpacksrefined.network.BackpackNetwork.Channel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The mod's messages on a Forge 47 simple channel, one message class per channel. Each loader has its
 * own copy of this class, with the same public signatures; the client half is
 * {@code client/platform/ClientNetwork}.
 */
public final class Network {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(BackpacksRefined.id("main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Network() { }

    /** Registers every channel, both directions; a client handler only runs on a client. */
    public static void register() {
        int index = 0;
        for (Channel<?> channel : BackpackNetwork.CHANNELS) register(channel, index++);
    }

    private static <T extends BackpackMessage> void register(Channel<T> channel, int index) {
        CHANNEL.messageBuilder(channel.type(), index,
                        channel.toServer() ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BackpackMessage::write)
                .decoder(buf -> channel.reader().apply(buf))
                .consumerMainThread((message, context) -> {
                    if (channel.toServer()) BackpackNetwork.handleServer(channel, context.get().getSender(), message);
                    else BackpackNetwork.handleClient(channel, message);
                })
                .add();
    }

    public static <T extends BackpackMessage> void send(ServerPlayer player, Channel<T> channel, T message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
