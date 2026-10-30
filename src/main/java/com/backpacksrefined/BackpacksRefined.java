package com.backpacksrefined;

import com.backpacksrefined.platform.Loader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The loader independent entrypoint. Each loader's mod class calls {@link #initialize()} once. */
public final class BackpacksRefined {
    public static final String MOD_ID = "backpacksrefined";
    public static final String MOD_NAME = "Backpacks Refined";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    /** The Minecraft version this jar was built for, baked in by Stonecutter's `minecraft` swap. */
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";

    private BackpacksRefined() { }

    public static void initialize() {
        LOGGER.info("{} starting on {} for Minecraft {}", MOD_NAME, Loader.name(), MINECRAFT);
    }

    /** An id in this mod's namespace. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
