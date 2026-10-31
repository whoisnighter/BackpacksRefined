package com.backpacksrefined.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * A message between client and server. Each kind has a {@link BackpackNetwork.Channel}, which reads it;
 * each loader's {@code platform/Network} carries it as that loader's packet.
 */
public interface BackpackMessage {
    void write(FriendlyByteBuf buf);
}
