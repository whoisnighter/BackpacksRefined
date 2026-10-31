package com.backpacksrefined.client.panel;

import com.backpacksrefined.client.platform.ClientNetwork;
import com.backpacksrefined.client.platform.PanelGraphics;
import com.backpacksrefined.item.BackpackItem;
import com.backpacksrefined.network.BackpackNetwork;
import com.backpacksrefined.network.BackpackNetwork.PanelClick;
import com.backpacksrefined.network.BackpackNetwork.PanelContents;
import com.backpacksrefined.network.BackpackNetwork.PanelOpen;
import com.backpacksrefined.network.BackpackNetwork.PanelSlot;
import com.backpacksrefined.network.BackpackNetwork.PanelStore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The worn backpack's slots, drawn beside any container screen while a backpack is worn: the player's
 * inventory, a chest, a furnace. It is drawn and clicked on the client only; the open menu is never
 * changed, so other mods see the menus they expect. Every click goes to the server, which answers with
 * the slots that changed; this shows what the server last sent and nothing else.
 *
 * <p>It sits left of the screen, or right when something already moved the screen right (the recipe
 * book, or another mod's side panel). Nine rows, and a column for each nine slots.
 */
public final class BackpackPanel {
    private static final int SLOT = 18;
    private static final int ROWS = 9;
    private static final int PADDING = 7;
    private static final int TITLE = 11;
    private static final int GAP = 2;

    private static final int BACKGROUND = 0xFFC6C6C6;
    private static final int OUTLINE = 0xFF000000;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int HIGHLIGHT = 0x80FFFFFF;
    private static final int TITLE_COLOR = 0xFF404040;

    private static AbstractContainerScreen<?> screen;
    private static int containerId = -1;
    /** Whether the server has been asked for the contents (because a backpack is worn). */
    private static boolean requested;
    private static List<ItemStack> stacks = List.of();
    /** Whether the last press landed on the panel, so its release and drags are the panel's too. */
    private static boolean pressed;

    private static int left;
    private static int top;
    private static int width;
    private static int height;

    private BackpackPanel() { }

    /** Starts listening for the server's answers. Called once when the client starts. */
    public static void register() {
        BackpackNetwork.onClient(BackpackNetwork.PANEL_CONTENTS, BackpackPanel::onContents);
        BackpackNetwork.onClient(BackpackNetwork.PANEL_SLOT, BackpackPanel::onSlot);
    }

    /** A container screen was set up; again after a resize. */
    public static void attach(AbstractContainerScreen<?> opened) {
        if (opened instanceof CreativeModeInventoryScreen) return;
        int id = opened.getMenu().containerId;
        if (opened == screen && id == containerId) return;
        detach(screen);
        screen = opened;
        containerId = id;
        requested = false;
        stacks = List.of();
        sync();
    }

    public static void detach(AbstractContainerScreen<?> closed) {
        if (closed == null || closed != screen) return;
        if (requested) ClientNetwork.send(BackpackNetwork.PANEL_OPEN, new PanelOpen(containerId, false));
        screen = null;
        containerId = -1;
        requested = false;
        stacks = List.of();
        pressed = false;
    }

    /** Asks for the contents once a backpack is worn, and lets them go once it is not. */
    private static void sync() {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean worn = player != null && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem;
        if (worn && !requested) {
            requested = true;
            ClientNetwork.send(BackpackNetwork.PANEL_OPEN, new PanelOpen(containerId, true));
        } else if (!worn && requested) {
            requested = false;
            stacks = List.of();
            ClientNetwork.send(BackpackNetwork.PANEL_OPEN, new PanelOpen(containerId, false));
        }
    }

    private static void onContents(PanelContents message) {
        if (message.containerId() == containerId) stacks = new ArrayList<>(message.stacks());
    }

    private static void onSlot(PanelSlot message) {
        if (message.containerId() == containerId && message.slot() >= 0 && message.slot() < stacks.size()) {
            stacks.set(message.slot(), message.stack());
        }
    }

    private static boolean visible(AbstractContainerScreen<?> on) {
        return on == screen && requested && !stacks.isEmpty();
    }

    /** Places the panel beside the screen's image, given where the screen is this frame. */
    private static void layout(AbstractContainerScreen<?> on, int leftPos, int topPos, int imageWidth) {
        int columns = (stacks.size() + ROWS - 1) / ROWS;
        width = PADDING * 2 + columns * SLOT;
        height = TITLE + PADDING * 2 + Math.min(stacks.size(), ROWS) * SLOT;
        boolean movedRight = leftPos > (on.width - imageWidth) / 2 + 1;
        left = leftPos - width - GAP;
        if (movedRight || left < 0) left = leftPos + imageWidth + GAP;
        top = topPos;
    }

    private static int slotAt(double mouseX, double mouseY) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            int x = slotX(slot);
            int y = slotY(slot);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) return slot;
        }
        return -1;
    }

    private static int slotX(int slot) {
        return left + PADDING + (slot / ROWS) * SLOT;
    }

    private static int slotY(int slot) {
        return top + TITLE + PADDING + (slot % ROWS) * SLOT;
    }

    private static boolean inside(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /** Draws the panel, under the carried stack. */
    public static void render(AbstractContainerScreen<?> on, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            int leftPos, int topPos, int imageWidth) {
        if (on != screen) return;
        sync();
        if (!visible(on)) return;
        layout(on, leftPos, topPos, imageWidth);
        Minecraft minecraft = Minecraft.getInstance();
        background(graphics);
        PanelGraphics.text(graphics, minecraft.font, Component.translatable("container.backpacksrefined.backpack"),
                left + PADDING + 1, top + PADDING - 1, TITLE_COLOR);
        int hovered = slotAt(mouseX, mouseY);
        for (int slot = 0; slot < stacks.size(); slot++) {
            int x = slotX(slot);
            int y = slotY(slot);
            slot(graphics, x, y);
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) PanelGraphics.item(graphics, minecraft.font, stack, x + 1, y + 1);
            if (slot == hovered) PanelGraphics.fill(graphics, x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, HIGHLIGHT);
        }
        if (hovered >= 0 && !stacks.get(hovered).isEmpty() && on.getMenu().getCarried().isEmpty()) {
            PanelGraphics.tooltip(graphics, minecraft.font, stacks.get(hovered), mouseX, mouseY);
        }
    }

    /** Vanilla's window: a black outline with its corners cut, a light top and left, a dark bottom and right. */
    private static void background(GuiGraphicsExtractor graphics) {
        int right = left + width;
        int bottom = top + height;
        PanelGraphics.fill(graphics, left + 1, top, right - 1, bottom, OUTLINE);
        PanelGraphics.fill(graphics, left, top + 1, right, bottom - 1, OUTLINE);
        PanelGraphics.fill(graphics, left + 1, top + 1, right - 1, bottom - 1, BACKGROUND);
        PanelGraphics.fill(graphics, left + 1, top + 1, right - 3, top + 3, LIGHT);
        PanelGraphics.fill(graphics, left + 1, top + 1, left + 3, bottom - 3, LIGHT);
        PanelGraphics.fill(graphics, left + 3, bottom - 3, right - 1, bottom - 1, SHADOW);
        PanelGraphics.fill(graphics, right - 3, top + 3, right - 1, bottom - 1, SHADOW);
    }

    /** Vanilla's slot: dark top and left, light bottom and right, around the grey square. */
    private static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        PanelGraphics.fill(graphics, x, y, x + SLOT - 1, y + SLOT - 1, SLOT_DARK);
        PanelGraphics.fill(graphics, x + 1, y + 1, x + SLOT, y + SLOT, LIGHT);
        PanelGraphics.fill(graphics, x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
    }

    /**
     * A press on the screen. True when the panel takes it: a press on the panel, which never reaches the
     * screen (it would count as a click outside and drop the carried stack), or Alt and a press on a
     * filled menu slot, which stores that slot in the backpack. {@code button} is the game's own number,
     * which 26.3 changed when it moved from GLFW to SDL.
     */
    public static boolean mouseClicked(AbstractContainerScreen<?> on, double mouseX, double mouseY, int button,
            boolean shift, boolean alt, Slot hoveredSlot) {
        pressed = false;
        if (!visible(on)) return false;
        if (inside(mouseX, mouseY)) {
            pressed = true;
            int slot = slotAt(mouseX, mouseY);
            boolean left = button == InputConstants.MOUSE_BUTTON_LEFT;
            if (slot >= 0 && (left || button == InputConstants.MOUSE_BUTTON_RIGHT)) {
                ClientNetwork.send(BackpackNetwork.PANEL_CLICK, new PanelClick(containerId, slot, left ? 0 : 1, shift));
            }
            return true;
        }
        if (alt && button == InputConstants.MOUSE_BUTTON_LEFT && hoveredSlot != null && hoveredSlot.hasItem()) {
            ClientNetwork.send(BackpackNetwork.PANEL_STORE, new PanelStore(containerId, hoveredSlot.index));
            pressed = true;
            return true;
        }
        return false;
    }

    /** A release or a drag: the panel's when its press was, or when it is over the panel. */
    public static boolean mouseOther(AbstractContainerScreen<?> on, double mouseX, double mouseY, boolean release) {
        boolean taken = visible(on) && (pressed || inside(mouseX, mouseY));
        if (release) pressed = false;
        return taken;
    }
}
