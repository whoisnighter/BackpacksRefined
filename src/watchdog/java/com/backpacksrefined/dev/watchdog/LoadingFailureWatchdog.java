package com.backpacksrefined.dev.watchdog;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

/**
 * A {@code -javaagent} that ends an unattended NeoForge run once FML reports a loading failure.
 *
 * <p>{@code LoadingErrorScreenMixin} stops such a run from inside the game, but only when FML got as
 * far as applying mod mixins. A missing dependency fails earlier, in FML's mod sorting: no mod loads,
 * no mixin of this mod is applied, and the loading error screen waits on a button nobody is there to
 * press, holding the Gradle task open. What FML does write in every such case is
 * {@code crash-reports/crash-<time>-fml.txt}, and only for errors, never for warnings alone. This
 * agent, loaded by the JVM before FML and depending on nothing but the JDK, watches for a new one and
 * exits non-zero.
 *
 * <p>The loader script adds it only to {@code -Pagent} runs. Its argument is the crash report
 * directory; without one it is {@code crash-reports} in the working directory, the game directory.
 */
public final class LoadingFailureWatchdog {
    private static final long POLL_MILLIS = 1000;
    /** Time for FML to finish logging the issues before the process goes. */
    private static final long SETTLE_MILLIS = 2000;

    private LoadingFailureWatchdog() { }

    public static void premain(String args) {
        Path reports = Path.of(args == null || args.isBlank() ? "crash-reports" : args);
        FileTime since = FileTime.from(Instant.now());
        Thread thread = new Thread(() -> watch(reports, since), "BackpacksAgent loading watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    private static void watch(Path reports, FileTime since) {
        try {
            while (true) {
                Path report = newFmlReport(reports, since);
                if (report != null) {
                    Thread.sleep(SETTLE_MILLIS);
                    System.err.println("[BackpacksAgent] FML reported a loading failure in " + report
                            + "; stopping the unattended run");
                    // On Windows the JVM usually dies natively on the way out (0xC0000409), with the
                    // render thread still inside GLFW; halt instead of exit does the same. Either way
                    // the process ends non-zero and the Gradle task fails, which is the point.
                    System.exit(1);
                }
                Thread.sleep(POLL_MILLIS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Path newFmlReport(Path reports, FileTime since) {
        if (!Files.isDirectory(reports)) return null;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(reports, "crash-*-fml.txt")) {
            for (Path file : files) {
                if (Files.getLastModifiedTime(file).compareTo(since) > 0) return file;
            }
        } catch (IOException e) {
            // A report half written or a directory being created: the next poll sees it.
        }
        return null;
    }
}
