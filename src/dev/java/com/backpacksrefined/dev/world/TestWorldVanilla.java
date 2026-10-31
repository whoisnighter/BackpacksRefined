package com.backpacksrefined.dev.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;

/**
 * The vanilla calls {@link TestWorld} makes whose shape moved between Minecraft versions: what a new
 * world's settings hold, what the dimension function is handed, and what the game rules are called.
 */
final class TestWorldVanilla {
    private TestWorldVanilla() { }

    /** A fixed seed, so a test world looks the same every time; the floor hides it anyway. */
    private static final WorldOptions OPTIONS = new WorldOptions("test".hashCode(), false, false);

    /**
     * What runs once the player is in the new world. 1.21.11 renamed the game rules to snake case, and
     * splits mob spawning into mobs, monsters, patrols, traders and phantoms.
     */
    static final List<String> COMMANDS = List.of(
            //? if >1.21.1 {
            "gamerule advance_time false",
            "gamerule advance_weather false",
            "gamerule spawn_mobs false",
            "gamerule spawn_monsters false",
            "gamerule spawn_patrols false",
            "gamerule spawn_wandering_traders false",
            "gamerule spawn_phantoms false",
            "gamerule respawn_radius 0",
            //?} else {
            /*"gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "gamerule doPatrolSpawning false",
            "gamerule doTraderSpawning false",
            "gamerule doInsomnia false",
            "gamerule spawnRadius 0",
            *///?}
            "time set noon",
            "weather clear");

    /** Makes the world and opens it, as the Create New World screen does once it is filled in. */
    static void create(Minecraft minecraft, String name, Screen parent) {
        //? if >1.20.1 {
        minecraft.createWorldOpenFlows().createFreshLevel(name, settings(name), OPTIONS, TestWorldVanilla::dimensions, parent);
        //?} else {
        /*minecraft.createWorldOpenFlows().createFreshLevel(name, settings(name), OPTIONS, TestWorldVanilla::dimensions);
        *///?}
    }

    /** Creative with cheats. 26.1 moved the game rules out of the settings and the difficulty into a record. */
    private static LevelSettings settings(String name) {
        //? if >1.21.11 {
        return new LevelSettings(name, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true,
                WorldDataConfiguration.DEFAULT);
        //?} elif >1.21.1 {
        /*return new LevelSettings(name, GameType.CREATIVE, false, net.minecraft.world.Difficulty.NORMAL, true,
                new net.minecraft.world.level.gamerules.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),
                WorldDataConfiguration.DEFAULT);
        *///?} else {
        /*return new LevelSettings(name, GameType.CREATIVE, false, net.minecraft.world.Difficulty.NORMAL, true,
                new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
        *///?}
    }

    /** The default dimensions with the overworld's generator swapped for the sky floor. 1.21.2 hands a lookup, not a registry access. */
    //? if >1.21.1 {
    private static WorldDimensions dimensions(net.minecraft.core.HolderLookup.Provider registries) {
    //?} else {
    /*private static WorldDimensions dimensions(net.minecraft.core.RegistryAccess registries) {
    *///?}
        return WorldPresets.createNormalWorldDimensions(registries)
                .replaceOverworldGenerator(registries, new FlatLevelSource(TestWorld.skyFloor(registries)));
    }
}
