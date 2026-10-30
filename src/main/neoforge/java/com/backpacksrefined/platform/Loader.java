package com.backpacksrefined.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

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
}
