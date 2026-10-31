package com.backpacksrefined.platform;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.storage.BackpackStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21.5 {
import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.SavedDataType;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
*///?}
//? if >=1.20.5 && <1.21.5 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
*///?}

/**
 * Saves {@link BackpackStorage} with the world, as one file. How saved data is declared changed twice:
 * from 1.21.5 it is a type with a codec, and from 26.1 the server holds it rather than the overworld,
 * under an id; from 1.20.5 to 1.21.4 a factory with a loader taking the registries; before, a loader
 * alone. Before 1.21.5 the storage's codec is written into the file's tag by hand.
 * Nodes between 1.21.5 and 1.21.11, and before 1.20.5 but 1.20.1, have no branch of their own here.
 */
public final class BackpackSavedData extends SavedData {
    //? if >=26.1 {
    private static final SavedDataType<BackpackSavedData> TYPE = new SavedDataType<>(BackpacksRefined.id("backpacks"),
            BackpackSavedData::new, BackpackStorage.CODEC.xmap(BackpackSavedData::new, data -> data.storage), null);
    //?} else if >=1.21.5 {
    /*private static final SavedDataType<BackpackSavedData> TYPE = new SavedDataType<>(BackpacksRefined.MOD_ID + "_backpacks",
            BackpackSavedData::new, BackpackStorage.CODEC.xmap(BackpackSavedData::new, data -> data.storage), null);
    *///?} else {
    /*private static final String NAME = BackpacksRefined.MOD_ID + "_backpacks";
    private static final String KEY = "backpacks";
    *///?}

    private final BackpackStorage storage;

    private BackpackSavedData() {
        this(new BackpackStorage());
    }

    private BackpackSavedData(BackpackStorage storage) {
        this.storage = storage;
        storage.onChange(this::setDirty);
    }

    public static BackpackStorage storage(MinecraftServer server) {
        //? if >=26.1 {
        return server.getDataStorage().computeIfAbsent(TYPE).storage;
        //?} else if >=1.21.5 {
        /*return server.overworld().getDataStorage().computeIfAbsent(TYPE).storage;
        *///?} else if >=1.20.5 {
        /*return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BackpackSavedData::new, BackpackSavedData::load, null), NAME).storage;
        *///?} else {
        /*return server.overworld().getDataStorage().computeIfAbsent(BackpackSavedData::load, BackpackSavedData::new, NAME).storage;
        *///?}
    }

    //? if >=1.20.5 && <1.21.5 {
    /*private static BackpackSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new BackpackSavedData(BackpackStorage.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag.get(KEY))
                .resultOrPartial(BackpacksRefined.LOGGER::error).orElseGet(BackpackStorage::new));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        BackpackStorage.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), storage)
                .resultOrPartial(BackpacksRefined.LOGGER::error).ifPresent(encoded -> tag.put(KEY, encoded));
        return tag;
    }
    *///?}
    //? if <1.20.5 {
    /*private static BackpackSavedData load(CompoundTag tag) {
        Tag saved = tag.get(KEY);
        return new BackpackSavedData(saved == null ? new BackpackStorage() : BackpackStorage.CODEC.parse(NbtOps.INSTANCE, saved)
                .resultOrPartial(BackpacksRefined.LOGGER::error).orElseGet(BackpackStorage::new));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        BackpackStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage)
                .resultOrPartial(BackpacksRefined.LOGGER::error).ifPresent(encoded -> tag.put(KEY, encoded));
        return tag;
    }
    *///?}
}
