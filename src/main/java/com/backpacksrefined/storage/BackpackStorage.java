package com.backpacksrefined.storage;

import com.backpacksrefined.platform.BackpackSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every backpack's contents, by the id on the backpack. It is the only copy: a backpack item or a
 * placed backpack holds just the id, so moving one never moves its items, and nothing can be in two
 * places. It is saved with the world, on the server; see {@code docs/plans/backpack-panel.md}.
 *
 * <p>An entry is never removed, even when its backpack burns: the items stay here.
 */
public final class BackpackStorage {
    private record Entry(UUID id, BackpackContents contents) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                BackpackContents.CODEC.fieldOf("contents").forGetter(Entry::contents)
        ).apply(instance, Entry::new));
    }

    public static final Codec<BackpackStorage> CODEC = Entry.CODEC.listOf().xmap(BackpackStorage::new, BackpackStorage::entries);

    private final Map<UUID, BackpackContents> backpacks = new HashMap<>();
    private Runnable onChange = () -> { };

    public BackpackStorage() { }

    private BackpackStorage(List<Entry> entries) {
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
