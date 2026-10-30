package com.backpacksrefined.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

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
}
