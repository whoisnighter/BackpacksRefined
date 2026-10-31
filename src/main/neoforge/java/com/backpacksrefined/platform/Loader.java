package com.backpacksrefined.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.BiFunction;
//? if >=1.21.2 {
import java.util.Set;
//?}

/**
 * Every call into the mod loader, for NeoForge. Each loader has its own copy of this class, with the
 * same name and signatures; see {@code platform/AGENTS.md}.
 */
public final class Loader {
    private Loader() { }

    public static String name() {
        return "NeoForge";
    }

    /** True under every development run task, false in the jar players install. */
    public static boolean isDevelopmentEnvironment() {
        // FML turned its environment fields into methods in NeoForge 21.9.
        //? if >=1.21.9 {
        return !FMLEnvironment.isProduction();
        //?} else {
        /*return !FMLEnvironment.production;
        *///?}
    }

    /** The directory config files are read from and written to. */
    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** A block entity type for {@code block}: NeoForge opens the constructor that replaced the builder in 1.21.2. */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
            Block... blocks) {
        //? if >=1.21.2 {
        return new BlockEntityType<>(factory::apply, Set.of(blocks));
        //?} else {
        /*return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
        *///?}
    }
}
