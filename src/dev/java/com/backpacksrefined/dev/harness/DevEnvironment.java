package com.backpacksrefined.dev.harness;

import com.google.gson.JsonObject;
import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.dev.platform.DevLoader;
import com.backpacksrefined.platform.Loader;

/**
 * What this process is, as every dev tool reports it: which loader, which Minecraft, which build of
 * the mod, whether the DEV gate is open and where the run directory is. The agent's
 * {@code ready.json} and its {@code probe} both answer from here.
 */
public final class DevEnvironment {
    /** {@code -Dbackpacksrefined.dev=true|false} overrides the loader's development check either way. */
    public static final String DEV_PROPERTY = "backpacksrefined.dev";

    private DevEnvironment() { }

    /** The Minecraft version this node builds against. */
    public static String minecraft() {
        return BackpacksRefined.MINECRAFT;
    }

    /** The mod loader this build compiled for: {@code fabric}, {@code neoforge} or {@code forge}. */
    public static String loader() {
        return DevLoader.LOADER;
    }

    /** The version of the mod these tools are loaded beside, as the loader reports it. */
    public static String modVersion() {
        return DevLoader.modVersion(BackpacksRefined.MOD_ID);
    }

    /**
     * Whether the DEV gate is open: the loader's own development-environment check, true under every
     * run task and false in the jar players install, unless {@link #DEV_PROPERTY} says otherwise.
     */
    public static boolean dev() {
        String forced = System.getProperty(DEV_PROPERTY);
        return forced == null ? Loader.isDevelopmentEnvironment() : Boolean.parseBoolean(forced);
    }

    /** The run directory, {@code run/<node>}: where queues and screenshots are written. */
    public static String runDirectory() {
        return DevLoader.gameDir().toAbsolutePath().toString();
    }

    /** All of the above, in one object. */
    public static JsonObject describe() {
        JsonObject json = new JsonObject();
        json.addProperty("loader", loader());
        json.addProperty("minecraft", minecraft());
        json.addProperty("modVersion", modVersion());
        json.addProperty("dev", dev());
        json.addProperty("runDirectory", runDirectory());
        return json;
    }
}
