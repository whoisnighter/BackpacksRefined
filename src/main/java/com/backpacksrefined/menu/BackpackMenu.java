package com.backpacksrefined.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A placed backpack's menu: vanilla's three-row chest menu, so the client opens vanilla's chest screen
 * and every mod that knows that screen (sorting, recipe viewers) knows this one. The only change is on
 * the server: the backpack's slots refuse what may not go inside another item, as a bundle does.
 */
public class BackpackMenu extends ChestMenu {
    public BackpackMenu(int containerId, Inventory inventory, Container backpack) {
        super(MenuType.GENERIC_9x3, containerId, inventory, backpack, 3);
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            Slot chest = slots.get(i);
            Slot slot = new BackpackSlot(backpack, chest.getContainerSlot(), chest.x, chest.y);
            slot.index = i;
            slots.set(i, slot);
        }
    }

    private static final class BackpackSlot extends Slot {
        BackpackSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem().canFitInsideContainerItems();
        }
    }
}
