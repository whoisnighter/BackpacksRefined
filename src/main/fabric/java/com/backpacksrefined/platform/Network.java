package com.backpacksrefined.platform;

import com.backpacksrefined.network.BackpackMessage;
import com.backpacksrefined.network.BackpackNetwork;
import com.backpacksrefined.network.BackpackNetwork.Channel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * The mod's messages as Fabric API payloads, one payload type per channel. Each loader has its own copy
 * of this class, with the same public signatures; the client half is {@code client/platform/ClientNetwork}.
 * Fabric API named its play payload registries after Minecraft's names in 26.1.
 */
public final class Network {
    /** A message carried as a payload of its channel's type. */
    public record Packet(Type<Packet> type, BackpackMessage message) implements CustomPacketPayload { }

    private static final Map<Channel<?>, CustomPacketPayload.Type<Packet>> TYPES = new HashMap<>();

    private Network() { }

    /** Registers every channel, and the server's receivers. Called once at startup, on both sides. */
    public static void register() {
        for (Channel<?> channel : BackpackNetwork.CHANNELS) {
            CustomPacketPayload.Type<Packet> type = new CustomPacketPayload.Type<>(channel.id());
            TYPES.put(channel, type);
            StreamCodec<RegistryFriendlyByteBuf, Packet> codec = StreamCodec.of(
                    (buf, packet) -> packet.message().write(buf),
                    buf -> new Packet(type, channel.reader().apply(buf)));
            if (channel.toServer()) {
                //? if >=26.1 {
                PayloadTypeRegistry.serverboundPlay().register(type, codec);
                //?} else {
                /*PayloadTypeRegistry.playC2S().register(type, codec);
                *///?}
                ServerPlayNetworking.registerGlobalReceiver(type,
                        (packet, context) -> BackpackNetwork.handleServer(channel, context.player(), packet.message()));
            } else {
                //? if >=26.1 {
                PayloadTypeRegistry.clientboundPlay().register(type, codec);
                //?} else {
                /*PayloadTypeRegistry.playS2C().register(type, codec);
                *///?}
            }
        }
    }

    public static CustomPacketPayload.Type<Packet> type(Channel<?> channel) {
        return TYPES.get(channel);
    }

    public static <T extends BackpackMessage> void send(ServerPlayer player, Channel<T> channel, T message) {
        ServerPlayNetworking.send(player, new Packet(type(channel), message));
    }
}
