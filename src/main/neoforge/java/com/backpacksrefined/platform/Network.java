package com.backpacksrefined.platform;

import com.backpacksrefined.network.BackpackMessage;
import com.backpacksrefined.network.BackpackNetwork;
import com.backpacksrefined.network.BackpackNetwork.Channel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

/**
 * The mod's messages as NeoForge payloads, one payload type per channel. Each loader has its own copy
 * of this class, with the same public signatures; the client half is {@code client/platform/ClientNetwork}.
 * Both directions are registered here, in the payload event; a client handler only runs on a client.
 */
public final class Network {
    /** A message carried as a payload of its channel's type. */
    public record Packet(Type<Packet> type, BackpackMessage message) implements CustomPacketPayload { }

    private static final Map<Channel<?>, CustomPacketPayload.Type<Packet>> TYPES = new HashMap<>();

    private Network() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (Channel<?> channel : BackpackNetwork.CHANNELS) {
            CustomPacketPayload.Type<Packet> type = new CustomPacketPayload.Type<>(channel.id());
            TYPES.put(channel, type);
            StreamCodec<RegistryFriendlyByteBuf, Packet> codec = StreamCodec.of(
                    (buf, packet) -> packet.message().write(buf),
                    buf -> new Packet(type, channel.reader().apply(buf)));
            if (channel.toServer()) {
                registrar.playToServer(type, codec, (packet, context) ->
                        BackpackNetwork.handleServer(channel, (ServerPlayer) context.player(), packet.message()));
            } else {
                registrar.playToClient(type, codec, (packet, context) ->
                        BackpackNetwork.handleClient(channel, packet.message()));
            }
        }
    }

    public static CustomPacketPayload.Type<Packet> type(Channel<?> channel) {
        return TYPES.get(channel);
    }

    public static <T extends BackpackMessage> void send(ServerPlayer player, Channel<T> channel, T message) {
        PacketDistributor.sendToPlayer(player, new Packet(type(channel), message));
    }
}
