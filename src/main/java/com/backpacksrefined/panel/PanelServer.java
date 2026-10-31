package com.backpacksrefined.panel;

import com.backpacksrefined.item.BackpackItem;
import com.backpacksrefined.network.BackpackNetwork;
import com.backpacksrefined.network.BackpackNetwork.PanelClick;
import com.backpacksrefined.network.BackpackNetwork.PanelContents;
import com.backpacksrefined.network.BackpackNetwork.PanelOpen;
import com.backpacksrefined.network.BackpackNetwork.PanelSlot;
import com.backpacksrefined.network.BackpackNetwork.PanelStore;
import com.backpacksrefined.platform.BackpackIds;
import com.backpacksrefined.platform.Network;
import com.backpacksrefined.platform.Stacks;
import com.backpacksrefined.storage.BackpackContents;
import com.backpacksrefined.storage.BackpackStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of the worn backpack's panel. A player is subscribed while a screen showing the panel
 * is open; every click is checked against what the server knows (the open menu, the backpack worn) and
 * applied to the stored contents directly, in the server thread. Nothing is kept on the client but
 * what the server last sent, so there is no second copy of the items to fall out of step.
 */
public final class PanelServer {
    /** A player's open panel: which backpack, in which menu. */
    private record Session(UUID backpack, int containerId) { }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private PanelServer() { }

    /** The backpack {@code player} wears, or an empty stack. */
    public static ItemStack worn(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof BackpackItem ? chest : ItemStack.EMPTY;
    }

    public static void open(ServerPlayer player, PanelOpen message) {
        if (!message.open()) {
            Session session = SESSIONS.get(player.getUUID());
            if (session != null && session.containerId() == message.containerId()) SESSIONS.remove(player.getUUID());
            return;
        }
        if (player.containerMenu.containerId != message.containerId()) return;
        ItemStack worn = worn(player);
        if (worn.isEmpty() || !player.isAlive() || player.isSpectator()) {
            hide(player, message.containerId());
            return;
        }
        UUID id = BackpackIds.getOrCreate(worn);
        SESSIONS.put(player.getUUID(), new Session(id, message.containerId()));
        BackpackContents contents = contents(player, worn, id);
        List<ItemStack> stacks = new ArrayList<>(contents.getContainerSize());
        for (int slot = 0; slot < contents.getContainerSize(); slot++) stacks.add(contents.getItem(slot).copy());
        Network.send(player, BackpackNetwork.PANEL_CONTENTS, new PanelContents(message.containerId(), stacks));
    }

    public static void click(ServerPlayer player, PanelClick message) {
        BackpackContents contents = validate(player, message.containerId());
        if (contents == null || message.slot() < 0 || message.slot() >= contents.getContainerSize()) return;
        List<ItemStack> before = snapshot(contents);
        AbstractContainerMenu menu = player.containerMenu;
        if (message.quickMove()) {
            ItemStack stack = contents.getItem(message.slot()).copy();
            if (!stack.isEmpty()) {
                player.getInventory().add(stack);
                contents.setItem(message.slot(), stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        } else if (message.button() == 0 || message.button() == 1) {
            pickup(menu, contents, message.slot(), message.button() == 0);
        }
        finish(player, message.containerId(), contents, before);
    }

    public static void store(ServerPlayer player, PanelStore message) {
        BackpackContents contents = validate(player, message.containerId());
        AbstractContainerMenu menu = player.containerMenu;
        if (contents == null || message.menuSlot() < 0 || message.menuSlot() >= menu.slots.size()) return;
        Slot slot = menu.slots.get(message.menuSlot());
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !slot.mayPickup(player) || !contents.canPlaceItem(0, stack)) return;
        int room = room(contents, stack);
        if (room <= 0) return;
        List<ItemStack> before = snapshot(contents);
        // Through the slot, so a result slot (crafting, anvil, smithing) charges what vanilla charges.
        // A result slot takes all or nothing, so with too little room nothing is taken.
        ItemStack taken = slot.safeTake(stack.getCount(), room, player);
        insert(contents, taken);
        // The room was counted first, so nothing is left; if something were, it goes back where it came from.
        if (!taken.isEmpty()) taken = slot.safeInsert(taken);
        if (!taken.isEmpty()) player.getInventory().add(taken);
        finish(player, message.containerId(), contents, before);
    }

    /** Vanilla's pickup click on one slot: take, place or swap with left, half or one with right. */
    private static void pickup(AbstractContainerMenu menu, BackpackContents contents, int index, boolean left) {
        ItemStack carried = menu.getCarried();
        ItemStack inSlot = contents.getItem(index);
        if (inSlot.isEmpty()) {
            if (carried.isEmpty() || !contents.canPlaceItem(index, carried)) return;
            int amount = Math.min(left ? carried.getCount() : 1, max(contents, carried));
            contents.setItem(index, carried.split(amount));
        } else if (carried.isEmpty()) {
            int amount = left ? inSlot.getCount() : (inSlot.getCount() + 1) / 2;
            menu.setCarried(inSlot.split(amount));
            if (inSlot.isEmpty()) contents.setItem(index, ItemStack.EMPTY);
        } else if (Stacks.sameItem(carried, inSlot)) {
            int amount = Math.min(left ? carried.getCount() : 1, max(contents, inSlot) - inSlot.getCount());
            if (amount > 0) {
                inSlot.grow(amount);
                carried.shrink(amount);
            }
        } else if (contents.canPlaceItem(index, carried) && carried.getCount() <= max(contents, carried)) {
            contents.setItem(index, carried);
            menu.setCarried(inSlot);
            return;
        }
        contents.setChanged();
    }

    /** How much of {@code stack} the backpack can take: what tops up equal stacks, and empty slots. */
    private static int room(BackpackContents contents, ItemStack stack) {
        int room = 0;
        for (int i = 0; i < contents.getContainerSize() && room < stack.getCount(); i++) {
            ItemStack inSlot = contents.getItem(i);
            if (inSlot.isEmpty()) room += max(contents, stack);
            else if (Stacks.sameItem(inSlot, stack)) room += Math.max(0, max(contents, inSlot) - inSlot.getCount());
        }
        return Math.min(room, stack.getCount());
    }

    /** Puts {@code stack} in: first onto equal stacks, then into empty slots. What does not fit stays. */
    private static void insert(BackpackContents contents, ItemStack stack) {
        for (int i = 0; i < contents.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack inSlot = contents.getItem(i);
            if (!inSlot.isEmpty() && Stacks.sameItem(inSlot, stack)) {
                int amount = Math.min(stack.getCount(), max(contents, inSlot) - inSlot.getCount());
                if (amount > 0) {
                    inSlot.grow(amount);
                    stack.shrink(amount);
                }
            }
        }
        for (int i = 0; i < contents.getContainerSize() && !stack.isEmpty(); i++) {
            if (contents.getItem(i).isEmpty()) contents.setItem(i, stack.split(max(contents, stack)));
        }
        contents.setChanged();
    }

    private static int max(BackpackContents contents, ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), contents.getMaxStackSize());
    }

    /**
     * The worn backpack's contents if {@code player} may use the panel of menu {@code containerId}: the
     * menu is still open, and the backpack worn is the one the panel opened on. Otherwise null, and the
     * panel is told to hide.
     */
    private static BackpackContents validate(ServerPlayer player, int containerId) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.containerId() != containerId) return null;
        if (player.containerMenu.containerId != containerId || !player.isAlive() || player.isSpectator()) return null;
        ItemStack worn = worn(player);
        if (worn.isEmpty() || !session.backpack().equals(BackpackIds.get(worn))) {
            SESSIONS.remove(player.getUUID());
            hide(player, containerId);
            return null;
        }
        return contents(player, worn, session.backpack());
    }

    private static BackpackContents contents(ServerPlayer player, ItemStack worn, UUID id) {
        return BackpackStorage.of(((ServerLevel) player.level()).getServer()).contents(id, ((BackpackItem) worn.getItem()).size());
    }

    private static List<ItemStack> snapshot(BackpackContents contents) {
        List<ItemStack> stacks = new ArrayList<>(contents.getContainerSize());
        for (int slot = 0; slot < contents.getContainerSize(); slot++) stacks.add(contents.getItem(slot).copy());
        return stacks;
    }

    /** Sends the menu's changes, the carried stack among them, and the backpack slots that changed. */
    private static void finish(ServerPlayer player, int containerId, BackpackContents contents, List<ItemStack> before) {
        player.containerMenu.broadcastChanges();
        for (int slot = 0; slot < contents.getContainerSize(); slot++) {
            ItemStack now = contents.getItem(slot);
            ItemStack was = before.get(slot);
            if (now.getCount() != was.getCount() || !Stacks.sameItem(now, was)) {
                Network.send(player, BackpackNetwork.PANEL_SLOT, new PanelSlot(containerId, slot, now.copy()));
            }
        }
    }

    private static void hide(ServerPlayer player, int containerId) {
        Network.send(player, BackpackNetwork.PANEL_CONTENTS, new PanelContents(containerId, List.of()));
    }
}
