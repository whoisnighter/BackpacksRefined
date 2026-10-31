package com.backpacksrefined.client.platform;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What the panel draws with, where the GUI's names differ: 26.1 renamed the draw target and its item
 * and text methods (the type name is a Stonecutter replacement), and 1.21.6 made a tooltip something
 * set for the end of the frame rather than drawn at once.
 */
public final class PanelGraphics {
    private PanelGraphics() { }

    public static void fill(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, color, false);
        //?} else {
        /*graphics.drawString(font, text, x, y, color, false);
        *///?}
    }

    public static void item(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
        //?} else {
        /*graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        *///?}
    }

    public static void tooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(font, stack, x, y);
        //?} else {
        /*graphics.renderTooltip(font, stack, x, y);
        *///?}
    }
}
