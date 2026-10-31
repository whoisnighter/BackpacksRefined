package com.backpacksrefined.dev.world;

import com.backpacksrefined.BackpacksRefined;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A "Test world" button on the main menu, for manual testing: one press makes a fresh throwaway world
 * and opens it, the same world {@code tools/agent/new_world.py} makes for a script.
 *
 * <p>The world is a sky floor: a superflat overworld of one layer of stone at y {@value #FLOOR_Y} in
 * plains, with no structures, features or lakes, so chunks generate at once and nothing is under the
 * player to fall into. It opens in creative with cheats on, and once the player is in it the clock and
 * the weather stop at a clear noon and nothing spawns, so nothing attacks mid test.
 *
 * <p>Every press makes the world anew. The one it replaces is deleted only when this button made it,
 * which a {@value #MARKER} file in it says, the same mark {@code new_world.py} leaves; a world of that
 * name made by hand is kept, and the new one takes the next free name.
 */
public final class TestWorld {
    private static final String NAME = "Test World";
    private static final String MARKER = ".agent-world";
    /** The floor's height. The overworld starts at {@value #MIN_Y} on every node. */
    private static final int FLOOR_Y = 199;
    private static final int MIN_Y = -64;

    /** The folder of the world just made, until the player stands in it and its rules are set. */
    private static String pending;

    private TestWorld() { }

    /**
     * Adds the button to the top right of the main menu, where nothing else is on any version, and of
     * the world list. Called after any screen is set up; ignores the others.
     */
    public static void addButton(Screen screen, Consumer<Button> add) {
        if (!(screen instanceof TitleScreen) && !(screen instanceof SelectWorldScreen)) return;
        int width = 80;
        add.accept(Button.builder(Component.literal("Test world"), button -> create(screen))
                .bounds(screen.width - width - 6, 6, width, 20)
                .tooltip(Tooltip.create(Component.literal(
                        "Make a fresh sky floor world, creative, clear noon, no mobs, and open it")))
                .build());
    }

    /** Sets the new world's rules once the player is in it. Runs every client tick. */
    public static void tick(Minecraft minecraft) {
        if (pending == null || minecraft.player == null) return;
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) return;
        Path folder = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        String opened = folder.getFileName().toString();
        if (!opened.equals(pending)) return;
        pending = null;
        server.execute(() -> {
            for (String command : TestWorldVanilla.COMMANDS) {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), command);
            }
        });
        try {
            Files.writeString(folder.resolve(MARKER), "Made by the Test world button.\n");
        } catch (IOException e) {
            BackpacksRefined.LOGGER.warn("Could not mark {} as a test world", folder, e);
        }
    }

    /**
     * Makes the world and opens it, as a press of the button does: the agent's {@code client.testWorld},
     * for a run with no world to start from (CI).
     */
    public static void create(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        Path saves = minecraft.getLevelSource().getBaseDir();
        String name = NAME;
        try {
            for (int number = 2; Files.exists(saves.resolve(name)); number++) {
                if (Files.exists(saves.resolve(name).resolve(MARKER))) {
                    delete(saves.resolve(name));
                    break;
                }
                name = NAME + " " + number;
            }
        } catch (IOException e) {
            BackpacksRefined.LOGGER.error("Could not delete the old {}", name, e);
            return;
        }
        pending = name;
        TestWorldVanilla.create(minecraft, name, parent);
    }

    /** The sky floor: air from the bottom of the world up to the floor, then one layer of stone. */
    static FlatLevelGeneratorSettings skyFloor(HolderLookup.Provider registries) {
        Holder<Biome> plains = registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), plains, List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(FLOOR_Y - MIN_Y, Blocks.AIR));
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.STONE));
        settings.updateLayers();
        return settings;
    }

    private static void delete(Path world) throws IOException {
        try (Stream<Path> files = Files.walk(world)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }
}
