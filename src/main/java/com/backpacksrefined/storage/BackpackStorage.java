package com.backpacksrefined.storage;

import com.backpacksrefined.platform.BackpackSavedData;
import com.backpacksrefined.platform.DataVersions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every backpack's contents, by the id on the backpack. It is the only copy: a backpack item or a
 * placed backpack holds just the id, so moving one never moves its items, and nothing can be in two
 * places. It is saved with the world, on the server; see {@code docs/plans/backpack-panel.md}.
 *
 * <p>An entry is never removed, even when its backpack burns: the items stay here.
 *
 * <p>It is saved in the layout {@link StorageSchema} describes, which also reads every older one. Saved
 * data that cannot be read is kept as it was and written back unchanged; meanwhile every backpack is
 * locked, empty and taking nothing, so nothing new is put where it would not be saved.
 */
public final class BackpackStorage {
    /** One backpack, as it is saved: its id beside the fields of its contents. */
    private record Entry(UUID id, BackpackContents contents) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                BackpackContents.MAP_CODEC.forGetter(Entry::contents)
        ).apply(instance, Entry::new));
    }

    /**
     * The current layout. Both numbers are written on every save and read past on load, where
     * {@link StorageSchema} has already used them.
     */
    private static final Codec<BackpackStorage> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf(StorageSchema.SCHEMA, 0).forGetter(storage -> StorageSchema.CURRENT),
            Codec.INT.optionalFieldOf(StorageSchema.DATA_VERSION, 0).forGetter(storage -> DataVersions.current()),
            Entry.CODEC.listOf().fieldOf(StorageSchema.BACKPACKS).forGetter(BackpackStorage::entries)
    ).apply(instance, (schema, dataVersion, entries) -> new BackpackStorage(entries)));

    public static final Codec<BackpackStorage> CODEC = StorageSchema.versioned(CURRENT_CODEC,
            BackpackStorage::new, storage -> Optional.ofNullable(storage.unread));

    private final Map<UUID, BackpackContents> backpacks = new HashMap<>();
    private Runnable onChange = () -> { };
    /** The saved data, when it could not be read: kept to be saved back as it is. */
    private final Dynamic<?> unread;

    public BackpackStorage() {
        unread = null;
    }

    private BackpackStorage(Dynamic<?> unread) {
        this.unread = unread;
    }

    private BackpackStorage(List<Entry> entries) {
        unread = null;
        for (Entry entry : entries) backpacks.put(entry.id(), entry.contents());
    }

    /** The storage of the world {@code server} runs. */
    public static BackpackStorage of(MinecraftServer server) {
        return BackpackSavedData.storage(server);
    }

    /** Called by whatever saves this storage, to be told when it has changed. */
    public void onChange(Runnable onChange) {
        this.onChange = onChange;
        backpacks.values().forEach(contents -> contents.onChange(onChange));
    }

    /**
     * The contents of backpack {@code id}, with at least {@code size} slots: made empty the first time,
     * and grown when the backpack has grown since.
     */
    public BackpackContents contents(UUID id, int size) {
        if (unread != null) return BackpackContents.locked(size);
        BackpackContents contents = backpacks.get(id);
        if (contents == null) {
            contents = new BackpackContents(size);
        } else if (contents.getContainerSize() < size) {
            contents = contents.grow(size);
        } else {
            return contents;
        }
        contents.onChange(onChange);
        backpacks.put(id, contents);
        onChange.run();
        return contents;
    }

    private List<Entry> entries() {
        return backpacks.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList();
    }
}
