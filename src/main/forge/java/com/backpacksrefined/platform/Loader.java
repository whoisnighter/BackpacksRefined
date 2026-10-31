package com.backpacksrefined.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.BiFunction;

/**
 * Every call into the mod loader, for MinecraftForge 47 (Minecraft 1.20.1). Each loader has its own
 * copy of this class, with the same name and signatures; see {@code platform/AGENTS.md}.
 */
public final class Loader {
    private Loader() { }

    public static String name() {
        return "Forge";
    }

    /** True under every development run task, false in the jar players install. */
    public static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    /** The directory config files are read from and written to. */
    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** A block entity type for {@code block}, from vanilla's builder. */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
            Block block) {
        return BlockEntityType.Builder.of(factory::apply, block).build(null);
    }
}
