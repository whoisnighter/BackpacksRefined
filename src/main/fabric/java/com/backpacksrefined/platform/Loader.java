package com.backpacksrefined.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import java.util.Set;
//?} else {
/*import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
*///?}

import java.nio.file.Path;
import java.util.function.BiFunction;

/**
 * Every call into the mod loader, for Fabric. Each loader has its own copy of this class, with the
 * same name and signatures; see {@code platform/AGENTS.md}.
 */
public final class Loader {
    private Loader() { }

    public static String name() {
        return "Fabric";
    }

    /** True under every development run task, false in the jar players install. */
    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /** The directory config files are read from and written to. */
    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /** A block entity type for {@code block}. Its constructor is public from 26.1; before, Fabric API builds it. */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
            Block block) {
        //? if >=26.1 {
        return new BlockEntityType<>(factory::apply, Set.of(block));
        //?} else {
        /*return FabricBlockEntityTypeBuilder.create(factory::apply, block).build();
        *///?}
    }
}
