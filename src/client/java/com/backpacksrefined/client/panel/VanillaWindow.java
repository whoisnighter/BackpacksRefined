package com.backpacksrefined.client.panel;

import com.backpacksrefined.client.platform.PanelGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Vanilla's container window and slot, drawn with plain fills rather than a texture, so no blit API
 * differs between versions and the window can be any size. The worn backpack's panel and the placed
 * backpack's screen both draw with it.
 */
public final class VanillaWindow {
    /** A slot is this big, its sunken edges included. */
    public static final int SLOT = 18;

    private static final int BACKGROUND = 0xFFC6C6C6;
    private static final int OUTLINE = 0xFF000000;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    private VanillaWindow() { }

    /**
     * The window, pixel for pixel as vanilla's container textures draw it: a black outline whose corners
     * step in two pixels, a light band along the top and left, a dark one along the bottom and right.
     */
    public static void window(GuiGraphicsExtractor graphics, int left, int top, int w, int h) {
        pixels(graphics, left, top, 1, 2, w - 2, h - 4, BACKGROUND);
        pixels(graphics, left, top, 2, 1, w - 4, 1, BACKGROUND);
        pixels(graphics, left, top, 2, h - 2, w - 4, 1, BACKGROUND);
        pixels(graphics, left, top, 2, 1, w - 5, 1, LIGHT);
        pixels(graphics, left, top, 1, 2, w - 4, 1, LIGHT);
        pixels(graphics, left, top, 1, 3, 2, h - 6, LIGHT);
        pixels(graphics, left, top, 3, 3, 1, 1, LIGHT);
        pixels(graphics, left, top, w - 3, 3, 2, h - 6, SHADOW);
        pixels(graphics, left, top, w - 4, h - 4, 1, 1, SHADOW);
        pixels(graphics, left, top, 3, h - 3, w - 4, 1, SHADOW);
        pixels(graphics, left, top, 3, h - 2, w - 5, 1, SHADOW);
        pixels(graphics, left, top, 2, 0, w - 5, 1, OUTLINE);
        pixels(graphics, left, top, 1, 1, 1, 1, OUTLINE);
        pixels(graphics, left, top, w - 3, 1, 1, 1, OUTLINE);
        pixels(graphics, left, top, 0, 2, 1, h - 5, OUTLINE);
        pixels(graphics, left, top, w - 2, 2, 1, 1, OUTLINE);
        pixels(graphics, left, top, w - 1, 3, 1, h - 5, OUTLINE);
        pixels(graphics, left, top, 1, h - 3, 1, 1, OUTLINE);
        pixels(graphics, left, top, 2, h - 2, 1, 1, OUTLINE);
        pixels(graphics, left, top, w - 2, h - 2, 1, 1, OUTLINE);
        pixels(graphics, left, top, 3, h - 1, w - 5, 1, OUTLINE);
    }

    /** A rectangle of the window, from its top left corner. */
    private static void pixels(GuiGraphicsExtractor graphics, int left, int top, int x, int y, int w, int h, int color) {
        PanelGraphics.fill(graphics, left + x, top + y, left + x + w, top + y + h, color);
    }

    /** Vanilla's sunken slot, from its top left corner: dark top and left, light bottom and right, mid grey where they meet. */
    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        PanelGraphics.fill(graphics, x, y, x + SLOT - 1, y + SLOT - 1, SLOT_DARK);
        PanelGraphics.fill(graphics, x + 1, y + 1, x + SLOT, y + SLOT, LIGHT);
        PanelGraphics.fill(graphics, x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
        PanelGraphics.fill(graphics, x + SLOT - 1, y, x + SLOT, y + 1, SLOT_FILL);
        PanelGraphics.fill(graphics, x, y + SLOT - 1, x + 1, y + SLOT, SLOT_FILL);
    }
}
