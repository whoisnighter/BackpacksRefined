package com.backpacksrefined.network;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.panel.PanelServer;
import com.backpacksrefined.platform.Stacks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The messages of the worn backpack's panel, and where each one is handled. The client only ever says
 * what the player did ("left click on backpack slot 7"); the server decides, and answers with the
 * slots that changed. See {@code docs/plans/backpack-panel.md}.
 */
public final class BackpackNetwork {
    /** One kind of message: its id, how it is read, and which way it goes. */
    public record Channel<T extends BackpackMessage>(Identifier id, Class<T> type, Function<FriendlyByteBuf, T> reader,
            boolean toServer) { }

    /** The client opened (or closed) a screen that shows the panel. */
    public record PanelOpen(int containerId, boolean open) implements BackpackMessage {
        static PanelOpen read(FriendlyByteBuf buf) {
            return new PanelOpen(buf.readVarInt(), buf.readBoolean());
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(containerId);
            buf.writeBoolean(open);
        }
    }

    /** A click on a panel slot: button 0 or 1, shift held or not. */
    public record PanelClick(int containerId, int slot, int button, boolean quickMove) implements BackpackMessage {
        static PanelClick read(FriendlyByteBuf buf) {
            return new PanelClick(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readBoolean());
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(containerId);
            buf.writeVarInt(slot);
            buf.writeByte(button);
            buf.writeBoolean(quickMove);
        }
    }

    /** Move what is in one slot of the open menu into the backpack (Alt and a click on that slot). */
    public record PanelStore(int containerId, int menuSlot) implements BackpackMessage {
        static PanelStore read(FriendlyByteBuf buf) {
            return new PanelStore(buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(containerId);
            buf.writeVarInt(menuSlot);
        }
    }

    /** Every slot of the worn backpack; none when the panel is to hide. */
    public record PanelContents(int containerId, List<ItemStack> stacks) implements BackpackMessage {
        static PanelContents read(FriendlyByteBuf buf) {
            int containerId = buf.readVarInt();
            int size = buf.readVarInt();
            List<ItemStack> stacks = new ArrayList<>(size);
            for (int i = 0; i < size; i++) stacks.add(Stacks.read(buf));
            return new PanelContents(containerId, stacks);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(containerId);
            buf.writeVarInt(stacks.size());
            for (ItemStack stack : stacks) Stacks.write(buf, stack);
        }
    }

    /** One changed slot of the worn backpack. */
    public record PanelSlot(int containerId, int slot, ItemStack stack) implements BackpackMessage {
        static PanelSlot read(FriendlyByteBuf buf) {
            return new PanelSlot(buf.readVarInt(), buf.readVarInt(), Stacks.read(buf));
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(containerId);
            buf.writeVarInt(slot);
            Stacks.write(buf, stack);
        }
    }

    public static final Channel<PanelOpen> PANEL_OPEN = new Channel<>(BackpacksRefined.id("panel_open"), PanelOpen.class, PanelOpen::read, true);
    public static final Channel<PanelClick> PANEL_CLICK = new Channel<>(BackpacksRefined.id("panel_click"), PanelClick.class, PanelClick::read, true);
    public static final Channel<PanelStore> PANEL_STORE = new Channel<>(BackpacksRefined.id("panel_store"), PanelStore.class, PanelStore::read, true);
    public static final Channel<PanelContents> PANEL_CONTENTS = new Channel<>(BackpacksRefined.id("panel_contents"), PanelContents.class, PanelContents::read, false);
    public static final Channel<PanelSlot> PANEL_SLOT = new Channel<>(BackpacksRefined.id("panel_slot"), PanelSlot.class, PanelSlot::read, false);

    public static final List<Channel<?>> CHANNELS = List.of(PANEL_OPEN, PANEL_CLICK, PANEL_STORE, PANEL_CONTENTS, PANEL_SLOT);

    private static final Map<Channel<?>, Consumer<?>> CLIENT_HANDLERS = new HashMap<>();

    private BackpackNetwork() { }

    /** Handles a message from {@code player}, on the server thread. */
    public static void handleServer(Channel<?> channel, ServerPlayer player, BackpackMessage message) {
        if (player == null) return;
        if (channel == PANEL_OPEN) PanelServer.open(player, (PanelOpen) message);
        else if (channel == PANEL_CLICK) PanelServer.click(player, (PanelClick) message);
        else if (channel == PANEL_STORE) PanelServer.store(player, (PanelStore) message);
    }

    /** The client's handler for {@code channel}, set by the client when it starts. */
    public static <T extends BackpackMessage> void onClient(Channel<T> channel, Consumer<T> handler) {
        CLIENT_HANDLERS.put(channel, handler);
    }

    /** Handles a message from the server, on the client thread. */
    @SuppressWarnings("unchecked")
    public static void handleClient(Channel<?> channel, BackpackMessage message) {
        Consumer<BackpackMessage> handler = (Consumer<BackpackMessage>) CLIENT_HANDLERS.get(channel);
        if (handler != null) handler.accept(message);
    }
}
