package com.backpacksrefined.dev.agent.backpacks;

import com.backpacksrefined.dev.agent.core.AgentException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Screenshots, and the pixels read back out of them.
 *
 * <p>A capture is not finished when the call returns: from 1.21.11 the framebuffer is read back from
 * the GPU asynchronously and the PNG is written on a worker thread. So a capture is a {@link Capture}
 * the caller waits on across ticks, rather than a value.
 *
 * <p>Samples are taken from the written file with {@code javax.imageio} instead of from the native
 * image, which keeps every Minecraft version's very different readback out of this and leaves the same
 * file on disk as evidence for whoever reads the report later.
 */
final class Frames {
    /** How the last capture is found again by {@code client.pixels} when it is not asked to take one. */
    private static Path lastFile;
    private static BufferedImage cached;
    private static Path cachedFile;
    private static int counter;

    private Frames() { }

    /** A screenshot being taken: its file, and vanilla's own message once it is on disk. */
    record Capture(Path file, AtomicReference<String> message, AtomicReference<String> failure) {
        boolean finished() {
            return message.get() != null || failure.get() != null;
        }
    }

    /**
     * Starts a capture into {@code <queue>/screenshots/<name>.png}. Vanilla builds that path itself from
     * the directory it is given, so the directory handed over is the queue's own and the file lands in
     * the subdirectory vanilla names.
     */
    static Capture capture(Minecraft minecraft, Path queue, String name) {
        String file = name == null || name.isBlank() ? String.format("frame-%04d", ++counter) : name.strip();
        if (!file.endsWith(".png")) file = file + ".png";
        Path target = queue.resolve(Screenshot.SCREENSHOT_DIR).resolve(file);
        AtomicReference<String> message = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();
        try {
            Files.createDirectories(target.getParent());
            AgentClientVanilla.screenshot(minecraft, queue.toFile(), file,
                    component -> message.set(component.getString()));
        } catch (IOException | RuntimeException e) {
            failure.set(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        lastFile = target;
        return new Capture(target, message, failure);
    }

    /** The file the last capture wrote, or null when nothing has been captured yet. */
    static Path lastFile() {
        return lastFile;
    }

    /**
     * The image a capture wrote. One image is kept decoded, because a probe normally samples several
     * points of the same frame and decoding a 1100x700 PNG for each of them is the slow part.
     */
    static BufferedImage image(Path file) {
        if (file == null) {
            throw new AgentException("no frame has been captured yet; run client.capture first, "
                    + "or ask client.pixels to take one with \"capture\": true");
        }
        if (file.equals(cachedFile) && cached != null) return cached;
        if (!Files.isRegularFile(file)) {
            throw new AgentException("the frame " + file + " is not there; the capture may still be running");
        }
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) throw new AgentException("the frame " + file + " is not a readable image");
            cached = image;
            cachedFile = file;
            return image;
        } catch (IOException e) {
            throw new AgentException("could not read the frame " + file + ": " + e.getMessage(), e);
        }
    }

    /** Forgets the decoded frame, so the next read picks up a file written since. */
    static void invalidate() {
        cached = null;
        cachedFile = null;
    }

    /** {@code #AARRGGBB}, which is how a colour is written everywhere else in this mod. */
    static String hex(int argb) {
        return String.format("#%08X", argb);
    }
}
