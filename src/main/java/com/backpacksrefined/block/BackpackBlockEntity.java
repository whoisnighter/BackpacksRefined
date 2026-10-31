package com.backpacksrefined.block;

import com.backpacksrefined.item.BackpackTier;
import com.backpacksrefined.platform.OpenedBlockEntity;
import com.backpacksrefined.storage.BackpackContents;
import com.backpacksrefined.storage.BackpackStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * A placed backpack. It keeps the backpack's id, not its items: they stay in {@link BackpackStorage},
 * so placing a backpack and picking it up again moves nothing. It is a container over those items, for
 * its own menu and for hoppers; on the client it is empty.
 *
 * <p>The client is told, by block events, when the lid opens or closes and when the backpack was just
 * set down, and keeps the game time of each for the renderer to animate from. Nothing of it is saved.
 */
public class BackpackBlockEntity extends OpenedBlockEntity {
    private static final BackpackContents NONE = new BackpackContents(BackpackTier.MAX_SIZE);
    /** The block event saying how many have the backpack open; the lid is open while any do. */
    static final int LID_EVENT = 1;
    /** The block event saying the backpack was just set down. */
    static final int PLACED_EVENT = 2;
    /** How long, in ticks, the lid takes to settle after it opens or closes. */
    private static final float LID_TICKS = 20.0F;

    private boolean lidOpen;
    private long lidMovedAt;
    /** Where the lid was when it last started to move, so a lid closed while still opening turns back from there. */
    private float lidFrom;
    private long placedAt = Long.MIN_VALUE;

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

    @Override
    protected void openersChanged(int openers) {
        if (level != null) level.blockEvent(worldPosition, getBlockState().getBlock(), LID_EVENT, openers);
    }

    /** A block event, on both sides; the server sends it on to the clients when this returns true. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (level == null) return super.triggerEvent(id, param);
        if (id == LID_EVENT) {
            boolean open = param > 0;
            if (open != lidOpen) {
                lidFrom = lidOpenness(0.0F);
                lidOpen = open;
                lidMovedAt = level.getGameTime();
            }
            return true;
        }
        if (id == PLACED_EVENT) {
            placed();
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /**
     * The backpack was just set down. The player who set it down hears of it first, from their own
     * placement, and the event that follows from the server does not start the drop again.
     */
    void placed() {
        if (level == null) return;
        long now = level.getGameTime();
        if (placedAt == Long.MIN_VALUE || now - placedAt > LID_TICKS) placedAt = now;
    }

    /** Ticks since the backpack was set down, or a negative number when it was not set down lately. */
    public float ticksSincePlaced(float partialTick) {
        if (level == null || placedAt == Long.MIN_VALUE) return -1.0F;
        float ticks = level.getGameTime() - placedAt + partialTick;
        return ticks > LID_TICKS ? -1.0F : ticks;
    }

    /**
     * How far open the lid is: 0 closed, 1 open. It springs open past 1 and settles back; it drops shut
     * and bounces once off the backpack.
     */
    public float lidOpenness(float partialTick) {
        float target = lidOpen ? 1.0F : 0.0F;
        if (level == null) return target;
        float ticks = level.getGameTime() - lidMovedAt + partialTick;
        if (ticks >= LID_TICKS) return target;
        float wave = Mth.cos(ticks * 0.5F);
        if (!lidOpen) wave = Math.abs(wave);
        return target + (lidFrom - target) * (float) Math.pow(2.0, -ticks / 2.0F) * wave;
    }
}
