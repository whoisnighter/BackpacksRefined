package com.backpacksrefined.storage;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.platform.DataVersions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The layout the storage is saved in, and how an older one is brought up to date. Two numbers sit at
 * the top of the saved data, as vanilla's {@code DataVersion} does for its files:
 *
 * <ul>
 *   <li>{@code schema}, the mod's own layout. A change of layout after a release adds one to
 *       {@link #CURRENT} and one step to {@link #STEPS}, which takes data from the version before to
 *       this one; loading runs every step from the saved version up, so a step is never edited once
 *       released.</li>
 *   <li>{@code data_version}, the game's when it was saved. The items inside are brought up to the
 *       running game's with vanilla's own data fixer, so a world opened by a newer Minecraft keeps them.</li>
 * </ul>
 *
 * <p><b>Only a layout a released version wrote gets a number.</b> Until a layout ships, change it in
 * place and leave {@link #CURRENT} alone: no released world holds it, so there is nothing to migrate.
 * Whether the current layout has shipped is in {@code CHANGELOG.md}: it has once a released version's
 * entry is there and its layout is the one below. Worlds made by development builds before that may
 * then be unreadable; make them again.
 *
 * <p>Layouts so far:
 * <ol>
 *   <li>{@code {schema, data_version, backpacks: [{id, size, items: [{slot, item}]}]}}. Not released yet.</li>
 * </ol>
 *
 * <p>Data that cannot be read, saved by a newer schema than this one or damaged, is never read in part
 * nor replaced: the codec hands it to {@code unreadable}, which keeps it as it is to be saved back
 * unchanged, so a world opened by an older mod and saved again loses nothing.
 */
public final class StorageSchema {
    /** The first schema, which needs no step. */
    private static final int FIRST = 1;
    public static final int CURRENT = 1;

    static final String SCHEMA = "schema";
    static final String DATA_VERSION = "data_version";
    static final String BACKPACKS = "backpacks";
    static final String ITEMS = "items";
    static final String ITEM = "item";

    /** One layout change: data saved by the version before, in the layout of the next. */
    private interface Step {
        <T> Dynamic<T> apply(Dynamic<T> data);
    }

    /** {@code STEPS.get(n - FIRST)} takes schema {@code n} to {@code n + 1}. None yet: one layout so far. */
    private static final List<Step> STEPS = List.of();

    static {
        if (STEPS.size() != CURRENT - FIRST) throw new IllegalStateException("One migration step per schema version");
    }

    private StorageSchema() { }

    /**
     * {@code current}, reading data of any older schema too: it is brought up to date first. Data that
     * still cannot be read becomes {@code unreadable(data)}, and a value for which {@code unread} gives
     * data is saved as exactly that data.
     */
    static <A> Codec<A> versioned(Codec<A> current, Function<Dynamic<?>, A> unreadable,
            Function<A, Optional<Dynamic<?>>> unread) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> saved = new Dynamic<>(ops, input);
                DataResult<Pair<A, T>> read = migrate(saved).flatMap(data -> current.decode(ops, data.getValue()));
                if (read.result().isPresent()) return read;
                BackpacksRefined.LOGGER.error("Could not read the saved backpacks, so they are kept as they are and "
                        + "every backpack stays locked until a version of the mod that reads them opens the world: {}",
                        read.error().map(error -> error.message()).orElse("unknown"));
                return DataResult.success(Pair.of(unreadable.apply(saved), ops.empty()));
            }

            @Override
            public <T> DataResult<T> encode(A value, DynamicOps<T> ops, T prefix) {
                Optional<Dynamic<?>> data = unread.apply(value);
                if (data.isPresent()) return DataResult.success(data.get().convert(ops).getValue());
                return current.encode(value, ops, prefix);
            }

            @Override
            public String toString() {
                return "StorageSchema[" + current + "]";
            }
        };
    }

    /** {@code data} in the current layout, its items as the running game reads them. */
    static <T> DataResult<Dynamic<T>> migrate(Dynamic<T> data) {
        int schema = data.get(SCHEMA).asInt(0);
        if (schema < FIRST || schema > CURRENT) {
            return DataResult.error(() -> "Backpacks saved with storage schema " + schema
                    + ", which this version of the mod does not read (" + FIRST + " to " + CURRENT + ")");
        }
        for (int version = schema; version < CURRENT; version++) data = STEPS.get(version - FIRST).apply(data);
        return DataResult.success(fixItems(data));
    }

    /** Runs vanilla's fixer over every item saved by an older game. Data with no game version is left. */
    private static <T> Dynamic<T> fixItems(Dynamic<T> data) {
        int from = data.get(DATA_VERSION).asInt(-1);
        if (from < 0 || from >= DataVersions.current()) return data;
        return eachOf(data, BACKPACKS, backpack -> eachOf(backpack, ITEMS, slot ->
                slot.update(ITEM, item -> DataVersions.fixItem(item, from))));
    }

    /** {@code data} with each element of its list {@code key} changed by {@code change}. */
    private static <T> Dynamic<T> eachOf(Dynamic<T> data, String key, UnaryOperator<Dynamic<T>> change) {
        if (data.get(key).result().isEmpty()) return data;
        return data.set(key, data.createList(data.get(key).asList(change).stream()));
    }
}
