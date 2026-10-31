package com.backpacksrefined.client.platform;

import com.backpacksrefined.client.panel.VanillaWindow;
import com.backpacksrefined.menu.BackpackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * A placed backpack's screen: one window around the slots {@link BackpackMenu} placed, the backpack's
 * name across the top and the player's inventory labelled above it, as vanilla labels it. The window is
 * drawn with plain fills ({@link VanillaWindow}), as the worn backpack's panel is.
 *
 * <p>Where versions differ: from 26.1 the size is given to the constructor, which keeps it final, and
 * the background is drawn in {@code extractBackground}, the tooltip by the screen itself; before, the
 * size is set after, the background is {@code renderBg}, and {@code render} adds the tooltip, after the
 * darkened world behind the screen, which 1.20.1 draws itself.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, BackpackMenu.WIDTH, BackpackMenu.height(menu.tier()));
        //?} else {
        /*super(menu, inventory, title);
        imageWidth = BackpackMenu.WIDTH;
        imageHeight = BackpackMenu.height(menu.tier());
        *///?}
        inventoryLabelX = BackpackMenu.INVENTORY_X;
        inventoryLabelY = BackpackMenu.inventoryLabelY(menu.tier());
    }

    private void drawWindow(GuiGraphicsExtractor graphics) {
        VanillaWindow.window(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) VanillaWindow.slot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
    }

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawWindow(graphics);
    }
    //?} else if >=1.20.2 {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        drawWindow(graphics);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
    *///?} else {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        drawWindow(graphics);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
    *///?}
}
