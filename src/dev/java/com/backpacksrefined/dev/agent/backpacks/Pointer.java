package com.backpacksrefined.dev.agent.backpacks;

import com.mojang.blaze3d.platform.Window;
import com.backpacksrefined.dev.mixin.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;

/**
 * The agent's virtual mouse pointer: a position in GUI pixels the game is told the mouse is at, so a
 * screen draws hover states and tooltips under it exactly as it would under a real mouse.
 *
 * <p>It exists for recordings. {@code client.click} alone presses a control without the mouse ever
 * being over it, which is the right thing for a check and looks like nothing happened in a video. Once
 * {@code client.mouse} has shown the pointer, clicks and scrolls with no point of their own land where
 * it is, and {@link Recorder} writes its position beside every frame so {@code tools/agent/make_gif.py}
 * can draw a cursor there: the framebuffer never holds one, since the system draws the real cursor.
 *
 * <p>While it is shown the real mouse is ignored ({@code MouseMoveMixin}), and the position is written
 * again every tick, because opening a screen may centre the game's idea of the mouse.
 */
public final class Pointer {
    /** Ticks a click stays visible in the recording, so a frame taken every few ticks still catches it. */
    private static final int CLICK_TICKS = 4;

    private static boolean shown;
    private static double x;
    private static double y;
    private static boolean held;
    private static int clickTicks;

    private Pointer() { }

    /** Whether the virtual pointer is standing in for the real mouse. Read by {@code MouseMoveMixin}. */
    public static boolean shown() {
        return shown;
    }

    static double x() {
        return x;
    }

    static double y() {
        return y;
    }

    /** Whether a button is down, for the cursor a recording draws: held by a drag, or a click just made. */
    static boolean pressed() {
        return held || clickTicks > 0;
    }

    /** Shows the pointer at a point in GUI pixels, and tells the game the mouse is there. */
    static void place(Minecraft minecraft, double guiX, double guiY) {
        shown = true;
        x = guiX;
        y = guiY;
        apply(minecraft);
    }

    /** Hides it and hands the position back to the real mouse, from its next movement. */
    static void hide() {
        shown = false;
        held = false;
        clickTicks = 0;
    }

    static void hold(boolean down) {
        held = down;
    }

    static void clicked() {
        clickTicks = CLICK_TICKS;
    }

    /** Called every client tick, before the queue: counts a click down and writes the position again. */
    static void tick(Minecraft minecraft) {
        if (!shown) return;
        if (clickTicks > 0) clickTicks--;
        apply(minecraft);
    }

    /** The mouse handler's fields are in window pixels, which are not GUI pixels and not always framebuffer ones. */
    private static void apply(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        MouseHandlerAccessor mouse = (MouseHandlerAccessor) minecraft.mouseHandler;
        mouse.backpacks$setXpos(x * window.getScreenWidth() / window.getGuiScaledWidth());
        mouse.backpacks$setYpos(y * window.getScreenHeight() / window.getGuiScaledHeight());
    }
}
