package com.backpacksrefined.dev.agent.backpacks;

import com.backpacksrefined.BackpacksRefined;
import net.minecraft.util.TimeSource;
import net.minecraft.util.Util;

import java.lang.reflect.Field;

/**
 * The game's clock while a recording runs: {@code Util.getNanos}, and {@code Util.getMillis} through
 * it, stand still between frames and step on by exactly the time one frame shows for in the GIF.
 *
 * <p>A recording takes a hundred milliseconds or more of real time a frame, and plays each back in
 * fifty. Whatever reads the clock rather than counting ticks, an animation,
 * a tooltip's fade, a text field's caret, then ran three times too fast in the result. On this
 * clock the game sees fifty milliseconds pass a frame, and its ticks follow from it as they do from
 * the real one, so everything plays at the speed it has in game.
 *
 * <p>{@code Util.timeSource} is the field vanilla keeps for exactly this, swapping the clock in tests;
 * it is not final on any supported version, so it is set by reflection rather than by a mixin on a
 * class loaded before any mod. When the recording stops, the real clock comes back less the time the
 * recording spent frozen, so the clock never jumps: a jump forward would have the game run a burst of
 * ticks to catch up. The frame limiter and the agent's own stall checks read {@code System.nanoTime},
 * which is left alone.
 */
final class RecordingClock {
    private static Field field;
    private static TimeSource.NanoTimeSource real;
    private static volatile boolean frozen;
    private static volatile long now;
    /** How far the game's clock is behind the real one, from the recordings so far. */
    private static volatile long behind;

    private RecordingClock() { }

    /** Freezes the game's clock where it is. Answers false, and leaves the real clock, when it cannot. */
    static boolean start() {
        if (!install()) return false;
        now = Util.getNanos();
        frozen = true;
        return true;
    }

    /** Moves the frozen clock on. */
    static void advance(long nanos) {
        if (frozen) now += nanos;
    }

    /** Hands the clock back to real time, carrying on from where the frozen one stopped. */
    static void stop() {
        if (!frozen) return;
        behind = real.getAsLong() - now;
        frozen = false;
    }

    private static boolean install() {
        if (real != null) return true;
        try {
            field = Util.class.getDeclaredField("timeSource");
            field.setAccessible(true);
            real = (TimeSource.NanoTimeSource) field.get(null);
            TimeSource.NanoTimeSource source = () -> frozen ? now : real.getAsLong() - behind;
            field.set(null, source);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            BackpacksRefined.LOGGER.warn("[BackpacksAgent] could not take over the game's clock, so what runs on it will "
                    + "play too fast in the recording: {}", e.toString());
            real = null;
            return false;
        }
    }
}
