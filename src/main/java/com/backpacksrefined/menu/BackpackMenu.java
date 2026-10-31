package com.backpacksrefined.menu;

import com.backpacksrefined.item.BackpackTier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A placed backpack's menu: its slots in rows of eight, centred over the player's inventory, as
 * vanilla's chest lays out its own. A row that is not full (copper's and gold's last) is centred too.
 * Each tier has its own menu type ({@link BackpacksMenus}), so the client knows the size from the type
 * alone.
 *
 * <p>The screen is one window, vanilla's chest's width: the backpack's name across the top, its slots
 * below it, and the player's inventory under them. Where the slots sit is worked out here, the same on
 * both sides, and the screen draws around them.
 *
 * <p>The backpack's slots take only what the backpack does: nothing that may not go inside another item,
 * as a bundle, and nothing while its contents could not be read.
 */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 8;
    public static final int SLOT = 18;
    /** Where the backpack's first row of items is drawn: past the title and the slot's own edge. */
    public static final int GRID_Y = 18;
    /** Where the player's inventory's first item is drawn: past the window's border and the slot's edge. */
    public static final int INVENTORY_X = 8;
    /** A border of seven, the inventory's nine columns, and a border, as vanilla's chest. */
    public static final int WIDTH = 7 + 9 * SLOT + 7;
    /** The player's inventory from its label down, as vanilla's container screens place it. */
    private static final int INVENTORY_HEIGHT = 94;

    private final Container backpack;
    private final BackpackTier tier;

    /** On the server, over a placed backpack. */
    public BackpackMenu(int containerId, Inventory inventory, Container backpack, BackpackTier tier) {
        super(BackpacksMenus.menu(tier), containerId);
        checkContainerSize(backpack, tier.size());
        this.backpack = backpack;
        this.tier = tier;
        // The placed backpack counts who has it open, for its lid. A player is what both versions of
        // startOpen take.
        backpack.startOpen(inventory.player);
        for (int i = 0; i < tier.size(); i++) {
            int row = i / COLUMNS;
            int inRow = Math.min(COLUMNS, tier.size() - row * COLUMNS);
            int x = (WIDTH - inRow * SLOT) / 2 + 1 + i % COLUMNS * SLOT;
            addSlot(new BackpackSlot(backpack, i, x, GRID_Y + row * SLOT));
        }
        int inventoryY = inventoryLabelY(tier) + 11;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, INVENTORY_X + column * SLOT, inventoryY + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * SLOT, inventoryY + 3 * SLOT + 4));
        }
    }

    /**
     * On the client: an empty container the server fills. It turns away what the backpack does, so a
     * shift-click is not shown going in only to come back.
     */
    static BackpackMenu client(int containerId, Inventory inventory, BackpackTier tier) {
        SimpleContainer backpack = new SimpleContainer(tier.size()) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return stack.getItem().canFitInsideContainerItems();
            }
        };
        return new BackpackMenu(containerId, inventory, backpack, tier);
    }

    public BackpackTier tier() {
        return tier;
    }

    /** The window's height: the title, the backpack's rows, a gap of three and the player's inventory. */
    public static int height(BackpackTier tier) {
        int rows = (tier.size() + COLUMNS - 1) / COLUMNS;
        return GRID_Y - 1 + rows * SLOT + 3 + INVENTORY_HEIGHT;
    }

    /** Where the label over the player's inventory goes. */
    public static int inventoryLabelY(BackpackTier tier) {
        return height(tier) - INVENTORY_HEIGHT;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        int size = tier.size();
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, size, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return backpack.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        backpack.stopOpen(player);
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
}
