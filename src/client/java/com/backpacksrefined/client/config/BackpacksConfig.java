package com.backpacksrefined.client.config;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.platform.Loader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The client's settings, kept in {@code config/backpacksrefined.json}. Read once when the client starts
 * and written when the config screen saves. A missing or broken file leaves the defaults, and is
 * written back with them.
 */
public final class BackpacksConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Values values = new Values();

    private BackpacksConfig() { }

    /** What the file holds. A field the file lacks keeps the value it is given here. */
    public static final class Values {
        /** A placed backpack drops into place with a wobble. */
        public boolean bounceWhenPlaced = false;
        /** A backpack floating on water rises and falls. */
        public boolean bobOnWater = false;
    }

    public static boolean bounceWhenPlaced() {
        return values.bounceWhenPlaced;
    }

    public static boolean bobOnWater() {
        return values.bobOnWater;
    }

    /** The settings as they are now, to change and then {@link #save}. */
    public static Values values() {
        return values;
    }

    private static Path file() {
        return Loader.configDir().resolve(BackpacksRefined.MOD_ID + ".json");
    }

    public static void load() {
        Path file = file();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Values read = GSON.fromJson(reader, Values.class);
                if (read != null) values = read;
            } catch (IOException | JsonParseException e) {
                BackpacksRefined.LOGGER.warn("Could not read {}, using the defaults", file, e);
            }
        }
        save();
    }

    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(values, writer);
            }
        } catch (IOException e) {
            BackpacksRefined.LOGGER.warn("Could not write {}", file, e);
        }
    }
}
