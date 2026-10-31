package com.backpacksrefined.block;

import com.backpacksrefined.item.BackpackTier;
import com.backpacksrefined.platform.IdBlockEntity;
import com.backpacksrefined.storage.BackpackContents;
import com.backpacksrefined.storage.BackpackStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * A placed backpack. It keeps the backpack's id, not its items: they stay in {@link BackpackStorage},
 * so placing a backpack and picking it up again moves nothing. It is a container over those items, for
 * its own menu and for hoppers; on the client it is empty.
 */
public class BackpackBlockEntity extends IdBlockEntity implements Container {
    private static final BackpackContents NONE = new BackpackContents(BackpackTier.MAX_SIZE);

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(BackpacksBlockEntities.backpack(), pos, state);
    }

    /** The items, on the server; a backpack placed without an id (by a command) is given one here. */
    private BackpackContents contents() {
        if (!(level instanceof ServerLevel server)) return NONE;
        UUID id = id();
        if (id == null) {
            id = UUID.randomUUID();
            setId(id);
        }
        return BackpackStorage.of(server.getServer()).contents(id, getContainerSize());
    }

    @Override
    public int getContainerSize() {
        return getBlockState().getBlock() instanceof BackpackBlock block ? block.tier().size() : BackpackTier.LEATHER.size();
    }

    @Override
    public boolean isEmpty() {
        return contents().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return contents().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return contents().removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        BackpackContents contents = contents();
        ItemStack removed = contents.removeItemNoUpdate(slot);
        contents.setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        contents().setItem(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return contents().canPlaceItem(slot, stack);
    }

    /** Menus change a stack in place and then call this, so the storage is marked to be saved too. */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel && id() != null) contents().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        contents().clearContent();
    }
}
