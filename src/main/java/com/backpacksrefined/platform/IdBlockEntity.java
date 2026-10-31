package com.backpacksrefined.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.6 {
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
*///?}
//? if >=1.20.5 && <1.21.6 {
/*import net.minecraft.core.HolderLookup;
*///?}

import java.util.UUID;

/**
 * A block entity that keeps one id and nothing else. How a block entity saves changed twice: from 1.21.6
 * through value input and output, from 1.20.5 with a tag and the registries, before with a tag alone.
 * Its type is built by {@code Loader.blockEntityType}, since 1.21.2 to 1.21.11 keep the constructor
 * private and leave building it to the loader.
 * Nodes between 1.20.5 and 1.21.6 but 1.21.1 have no branch of their own here.
 */
public abstract class IdBlockEntity extends BlockEntity {
    private static final String KEY = "backpack";

    private UUID id;

    protected IdBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The id, or null when it has none yet. */
    public UUID id() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
        setChanged();
    }

    /**
     * From 1.21.5 a block entity that is a container drops its items when its block goes. Its items are
     * not its own: they stay in the storage, under the id the dropped item takes.
     */
    //? if >=1.21.5 {
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) { }
    //?}

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (id != null) output.store(KEY, UUIDUtil.CODEC, id);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        id = input.read(KEY, UUIDUtil.CODEC).orElse(null);
    }
    //?} else if >=1.20.5 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (id != null) tag.putUUID(KEY, id);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        id = tag.hasUUID(KEY) ? tag.getUUID(KEY) : null;
    }
    *///?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (id != null) tag.putUUID(KEY, id);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        id = tag.hasUUID(KEY) ? tag.getUUID(KEY) : null;
    }
    *///?}
}
