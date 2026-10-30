package com.backpacksrefined.dev.agent.backpacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.backpacksrefined.dev.agent.core.AgentException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A screen recording: a frame every few ticks into {@code screenshots/<name>/}, with the virtual
 * {@link Pointer} beside each one in {@code frames.jsonl}. {@code tools/agent/make_gif.py} turns the
 * folder into a GIF and draws the cursor.
 *
 * <p>Time is the script's, not the wall clock's. Reading a maximised window back costs vanilla a
 * hundred milliseconds or more, pixel by pixel on the render thread, which holds the game to a few
 * frames a second while it records. So a recording runs in lockstep: once {@code every} ticks have
 * passed, the queue waits ({@link #clientTick}) until the frame has been taken. A {@code wait} or a
 * pointer glide in the script then spans exactly the frames its ticks call for, and {@code
 * make_gif.py} shows each frame for {@code every} × 50 ms, so the result plays at the speed the
 * script was written for however long it took to record. What reads the clock rather than counting
 * ticks is kept in step by {@link RecordingClock}, which holds the game's clock still between frames.
 *
 * <p>A frame is taken at the start of a pass of the game loop ({@link #frameStart}), not on a tick. The
 * main target then holds the frame the last pass drew, and the pointer has not moved since, so the
 * position written beside it is the one the frame was drawn with. Taken on the tick instead, a game
 * running under 20 frames a second drew the cursor a frame or two ahead of the screen under it.
 *
 * <p>Frames are not {@code client.capture}'s screenshots, which encode a full size PNG per frame on a
 * pool of their own and whose downscale refuses a window whose height is odd. A recording reads the pixels back
 * ({@link AgentClientVanilla#readFrame}), keeps every {@code downscale}th of them, which loses nothing
 * of a GUI drawn at a scale {@code downscale} divides, and writes the PNGs on one thread of its own.
 *
 * <p>Evidence for a person, like {@code client.capture}: nothing is asserted on it.
 */
public final class Recorder {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    /** One writer, so a recording never has more than one core encoding PNGs behind the game's back. */
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "BackpacksAgent recorder");
        thread.setDaemon(true);
        return thread;
    });

    private static String name;
    private static Path directory;
    private static int every;
    private static int downscale;
    private static int frame;
    private static long ticks;
    private static long lastFrameTick;
    /** Whether the game's clock follows the recording, rather than running at real time. */
    private static boolean clockFrozen;
    private static final long TICK_NANOS = 50_000_000L;
    private static final List<String> lines = new ArrayList<>();
    /** Frames asked for and not yet on disk, and frames that failed; both across the thread boundary. */
    private static final AtomicInteger pending = new AtomicInteger();
    private static final AtomicInteger failed = new AtomicInteger();
    private static volatile String firstFailure;

    private Recorder() { }

    static boolean recording() {
        return name != null;
    }

    static void start(Path queue, String recordingName, int ticksPerFrame, int smaller) {
        if (recording()) throw new AgentException("client.record: '" + name + "' is still recording; stop it first");
        if (pending.get() > 0) throw new AgentException("client.record: the last recording is still being written");
        String clean = recordingName.strip();
        if (clean.isEmpty() || clean.contains("/") || clean.contains("\\") || clean.contains("..")) {
            throw new AgentException("client.record: 'name' has to be a plain folder name, got '" + recordingName + "'");
        }
        Path folder = queue.resolve(Screenshot.SCREENSHOT_DIR).resolve(clean);
        try {
            // A second take of the same name replaces the first, rather than mixing its frames in.
            if (Files.isDirectory(folder)) {
                try (var old = Files.list(folder)) {
                    for (Path file : old.toList()) Files.deleteIfExists(file);
                }
            }
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new AgentException("client.record: could not prepare " + folder + ": " + e.getMessage(), e);
        }
        name = clean;
        clockFrozen = RecordingClock.start();
        directory = folder;
        every = ticksPerFrame;
        downscale = smaller;
        frame = 0;
        ticks = 0;
        lastFrameTick = 0;
        lines.clear();
        failed.set(0);
        firstFailure = null;
    }

    /**
     * Called every client tick, before the queue, and answers whether the queue may run this tick.
     * While a frame is due and not yet taken it may not: the script waits for the recording rather
     * than running ahead of it. Otherwise it moves the virtual pointer's click on, writes its position
     * again, and counts the tick for the recording.
     */
    public static boolean clientTick(Minecraft minecraft) {
        if (recording() && (frame == 0 || ticks - lastFrameTick >= every)) return false;
        Pointer.tick(minecraft);
        if (recording()) ticks++;
        return true;
    }

    /**
     * Called at the start of every pass of the game loop, before its ticks and its render, by {@code
     * FrameStartMixin}. Takes a frame when {@code every} ticks have run since the last one.
     */
    public static void frameStart(Minecraft minecraft) {
        if (!recording()) return;
        if (frame == 0 || ticks - lastFrameTick >= every) capture(minecraft);
        // The frozen clock moves one tick's worth a pass until the next frame is due, so the game runs
        // one tick and draws once between two looks, never a burst of ticks behind one frame.
        if (ticks - lastFrameTick < every) RecordingClock.advance(TICK_NANOS);
    }

    private static void capture(Minecraft minecraft) {
        lastFrameTick = ticks;
        frame++;
        String file = String.format("frame-%05d.png", frame);
        Path target = directory.resolve(file);
        int step = downscale;
        pending.incrementAndGet();
        try {
            AgentClientVanilla.readFrame(minecraft, (width, height, argb) -> {
                BufferedImage image = shrink(width, height, argb, step);
                WRITER.execute(() -> write(image, target));
            });
        } catch (RuntimeException e) {
            fail(file, e);
        }
        double scale = AgentClientVanilla.guiScale(minecraft) / downscale;
        JsonObject line = new JsonObject();
        line.addProperty("frame", frame);
        line.addProperty("file", file);
        line.addProperty("tick", ticks);
        line.addProperty("pointer", Pointer.shown());
        if (Pointer.shown()) {
            line.addProperty("x", Math.round(Pointer.x() * scale));
            line.addProperty("y", Math.round(Pointer.y() * scale));
            line.addProperty("pressed", Pointer.pressed());
        }
        lines.add(GSON.toJson(line));
    }

    /** Stops taking frames. The frames already read may still be on their way to disk; see {@link #finished}. */
    static JsonObject stop(Minecraft minecraft) {
        if (!recording()) throw new AgentException("client.record: nothing is recording");
        JsonObject header = new JsonObject();
        header.addProperty("name", name);
        header.addProperty("every", every);
        header.addProperty("msPerFrame", every * 50);
        header.addProperty("frames", frame);
        header.addProperty("ticks", ticks);
        header.addProperty("downscale", downscale);
        header.addProperty("clockFrozen", clockFrozen);
        header.addProperty("guiScale", AgentClientVanilla.guiScale(minecraft) / downscale);
        header.addProperty("frameWidth", minecraft.getWindow().getWidth() / downscale);
        header.addProperty("frameHeight", minecraft.getWindow().getHeight() / downscale);
        List<String> out = new ArrayList<>();
        out.add(GSON.toJson(header));
        out.addAll(lines);
        try {
            Files.write(directory.resolve("frames.jsonl"), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AgentException("client.record: could not write frames.jsonl: " + e.getMessage(), e);
        }
        header.addProperty("directory", directory.toAbsolutePath().toString());
        name = null;
        RecordingClock.stop();
        return header;
    }

    /** Whether every frame of the last recording is on disk, or failed. */
    static boolean finished() {
        return pending.get() == 0;
    }

    /** How many frames of the last recording failed. */
    static int failed() {
        return failed.get();
    }

    /** Why the first of them failed, or null. */
    static String firstFailure() {
        return firstFailure;
    }

    /** Keeps every {@code step}th pixel each way: a GUI drawn at a scale {@code step} divides loses nothing. */
    private static BufferedImage shrink(int width, int height, int[] argb, int step) {
        int w = width / step;
        int h = height / step;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            int source = y * step * width;
            for (int x = 0; x < w; x++) row[x] = argb[source + x * step];
            image.setRGB(0, y, w, 1, row, 0, w);
        }
        return image;
    }

    private static void write(BufferedImage image, Path target) {
        try {
            ImageIO.write(image, "png", target.toFile());
        } catch (IOException | RuntimeException e) {
            fail(target.getFileName().toString(), e);
            return;
        }
        pending.decrementAndGet();
    }

    private static void fail(String file, Exception e) {
        failed.incrementAndGet();
        if (firstFailure == null) firstFailure = file + ": " + e.getClass().getSimpleName() + ": " + e.getMessage();
        pending.decrementAndGet();
    }
}
