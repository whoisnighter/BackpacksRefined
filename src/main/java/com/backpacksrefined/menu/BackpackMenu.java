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
 * A placed backpack's menu: its slots in rows of four, as the worn backpack's panel shows them, so every
 * tier fills its rows with no slot to spare, and the player's inventory to the right of them. Each tier
 * has its own menu type ({@link BackpacksMenus}), so the client knows the size from the type alone.
 *
 * <p>The screen is one window: the backpack's name across the top, its slots below it on the left, and
 * the player's inventory on the right, its bottom lined up with the window's. Where the slots sit is
 * worked out here, the same on both sides, and the screen draws around them.
 *
 * <p>The backpack's slots take only what the backpack does: nothing that may not go inside another item,
 * as a bundle, and nothing while its contents could not be read.
 */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 4;
    public static final int SLOT = 18;
    /** Where the backpack's first item is drawn: past the window's border and the slot's own edge. */
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    /** Where the player's inventory's first item is drawn, eight right of the backpack's slots. */
    public static final int INVENTORY_X = GRID_X + COLUMNS * SLOT + 8;
    /** A border of seven, the backpack's four columns, the gap, the inventory's nine, and a border. */
    public static final int WIDTH = INVENTORY_X - 1 + 9 * SLOT + 7;
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
            addSlot(new BackpackSlot(backpack, i, GRID_X + i % COLUMNS * SLOT, GRID_Y + i / COLUMNS * SLOT));
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

    /**
     * The window's height: the title, the backpack's rows and a border of eight, or, when that is less,
     * room for the player's inventory with its label level with the backpack's first row.
     */
    public static int height(BackpackTier tier) {
        int rows = (tier.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(GRID_Y - 1 + rows * SLOT + 8, GRID_Y - 1 + INVENTORY_HEIGHT);
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
