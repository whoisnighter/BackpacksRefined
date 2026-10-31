package com.backpacksrefined.block;

import com.backpacksrefined.platform.Loader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.BiConsumer;

/**
 * The mod's block entity types. Each loader's mod class calls {@link #register} after the blocks, which
 * the types are built from.
 */
public final class BackpacksBlockEntities {
    public static final Identifier BACKPACK_ID = BackpacksBlocks.BACKPACK_ID;

    private static BlockEntityType<BackpackBlockEntity> backpack;

    private BackpacksBlockEntities() { }

    /** Builds every block entity type and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, BlockEntityType<?>> registry) {
        backpack = Loader.blockEntityType(BackpackBlockEntity::new, BackpacksBlocks.backpack());
        registry.accept(BACKPACK_ID, backpack);
    }

    public static BlockEntityType<BackpackBlockEntity> backpack() {
        if (backpack == null) throw new IllegalStateException("Block entities are not registered yet");
        return backpack;
    }
}
