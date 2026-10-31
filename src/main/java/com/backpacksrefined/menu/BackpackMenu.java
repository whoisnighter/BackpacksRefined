package com.backpacksrefined.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A placed backpack's menu: vanilla's chest menu with a row for each nine slots, so the client opens
 * vanilla's chest screen and every mod that knows that screen (sorting, recipe viewers) knows this one.
 * The only change is on the server: the backpack's slots take only what the backpack does (nothing that
 * may not go inside another item, as a bundle, and nothing while its contents could not be read), and the slots past the backpack's size, which fill out the last row, hold nothing.
 */
public class BackpackMenu extends ChestMenu {
    private static final MenuType<?>[] TYPES = {MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6};

    private final Container backpack;

    public BackpackMenu(int containerId, Inventory inventory, Container backpack) {
        this(containerId, inventory, backpack, rows(backpack));
    }

    private BackpackMenu(int containerId, Inventory inventory, Container backpack, int rows) {
        super(TYPES[rows - 1], containerId, inventory, new Padded(backpack, rows * 9), rows);
        this.backpack = backpack;
        // The chest menu tells the padded container, which does not pass it on; the placed backpack
        // counts who has it open, for its lid. A player is what both versions of startOpen take.
        backpack.startOpen(inventory.player);
        for (int i = 0; i < rows * 9; i++) {
            Slot chest = slots.get(i);
            Slot slot = i < backpack.getContainerSize()
                    ? new BackpackSlot(backpack, chest.getContainerSlot(), chest.x, chest.y)
                    : new EmptySlot(getContainer(), chest.getContainerSlot(), chest.x, chest.y);
            slot.index = i;
            slots.set(i, slot);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        backpack.stopOpen(player);
    }

    private static int rows(Container backpack) {
        return Math.min(TYPES.length, (backpack.getContainerSize() + 8) / 9);
    }

    private static final class BackpackSlot extends Slot {
        BackpackSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    /** A slot past the backpack's size: nothing goes in, and there is nothing to take. */
    private static final class EmptySlot extends Slot {
        EmptySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** The backpack, filled out to whole rows of nine with slots that are always empty. */
    private record Padded(Container backpack, int size) implements Container {
        private boolean inside(int slot) {
            return slot < backpack.getContainerSize();
        }

        @Override
        public int getContainerSize() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return backpack.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return inside(slot) ? backpack.getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return inside(slot) ? backpack.removeItem(slot, amount) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return inside(slot) ? backpack.removeItemNoUpdate(slot) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (inside(slot)) backpack.setItem(slot, stack);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return inside(slot) && backpack.canPlaceItem(slot, stack);
        }

        @Override
        public void setChanged() {
            backpack.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return backpack.stillValid(player);
        }

        @Override
        public void clearContent() {
            backpack.clearContent();
        }
    }
}
