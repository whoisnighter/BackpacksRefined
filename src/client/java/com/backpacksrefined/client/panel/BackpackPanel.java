package com.backpacksrefined.client.panel;

import com.backpacksrefined.client.platform.ClientNetwork;
import com.backpacksrefined.client.platform.PanelGraphics;
import com.backpacksrefined.item.BackpackItem;
import com.backpacksrefined.item.BackpackTier;
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
import net.minecraft.util.Mth;
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
 * <p>It sits left of the screen's image, or right when there is no room on the left. It always shows the
 * largest tier's 32 slots, four columns of eight rows, as tall as the player's inventory screen, and
 * darkens and locks those past the worn backpack's tier. Its bottom lines up with the image's, where
 * every container screen draws the player's inventory.
 */
public final class BackpackPanel {
    private static final int SLOT = VanillaWindow.SLOT;
    private static final int COLUMNS = 4;
    private static final int PADDING = 7;
    /** Room above the slots for the title. */
    private static final int HEADER = 15;
    private static final int GAP = 2;

    private static final int HIGHLIGHT = 0x80FFFFFF;
    private static final int LOCKED = 0x80000000;
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
    private static int gridTop;

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

    private static int rows() {
        return (BackpackTier.MAX_SIZE + COLUMNS - 1) / COLUMNS;
    }

    /** Places the panel beside the screen's image, given where the screen is this frame, bottoms lined up. */
    private static void layout(AbstractContainerScreen<?> on, int leftPos, int topPos, int imageWidth, int imageHeight) {
        width = PADDING + COLUMNS * SLOT + PADDING;
        height = HEADER + rows() * SLOT + PADDING;
        top = topPos + imageHeight - height;
        gridTop = top + HEADER;
        left = leftPos - width - GAP;
        if (left < 0 && leftPos + imageWidth + GAP + width <= on.width) left = leftPos + imageWidth + GAP;
    }

    /** The backpack slot under the mouse, or -1; a locked slot is none. */
    private static int slotAt(double mouseX, double mouseY) {
        int column = Mth.floor((mouseX - left - PADDING) / SLOT);
        int row = Mth.floor((mouseY - gridTop) / SLOT);
        if (column < 0 || column >= COLUMNS || row < 0) return -1;
        int slot = row * COLUMNS + column;
        return slot < stacks.size() ? slot : -1;
    }

    private static boolean inside(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /** Draws the panel, under the carried stack. */
    public static void render(AbstractContainerScreen<?> on, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            int leftPos, int topPos, int imageWidth, int imageHeight) {
        if (on != screen) return;
        sync();
        if (!visible(on)) return;
        layout(on, leftPos, topPos, imageWidth, imageHeight);
        Minecraft minecraft = Minecraft.getInstance();
        VanillaWindow.window(graphics, left, top, width, height);
        PanelGraphics.text(graphics, minecraft.font, Component.translatable("container.backpacksrefined.backpack"),
                left + PADDING + 1, top + 5, TITLE_COLOR);
        int hovered = slotAt(mouseX, mouseY);
        for (int slot = 0; slot < BackpackTier.MAX_SIZE; slot++) {
            int x = left + PADDING + slot % COLUMNS * SLOT;
            int y = gridTop + slot / COLUMNS * SLOT;
            VanillaWindow.slot(graphics, x, y);
            if (slot >= stacks.size()) {
                PanelGraphics.fill(graphics, x, y, x + SLOT, y + SLOT, LOCKED);
                continue;
            }
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) PanelGraphics.item(graphics, minecraft.font, stack, x + 1, y + 1);
            if (slot == hovered) PanelGraphics.fill(graphics, x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, HIGHLIGHT);
        }
        if (hovered >= 0 && !stacks.get(hovered).isEmpty() && on.getMenu().getCarried().isEmpty()) {
            PanelGraphics.tooltip(graphics, minecraft.font, stacks.get(hovered), mouseX, mouseY);
        }
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
        boolean left = button == InputConstants.MOUSE_BUTTON_LEFT;
        if (inside(mouseX, mouseY)) {
            pressed = true;
            int slot = slotAt(mouseX, mouseY);
            if (slot >= 0 && (left || button == InputConstants.MOUSE_BUTTON_RIGHT)) {
                ClientNetwork.send(BackpackNetwork.PANEL_CLICK, new PanelClick(containerId, slot, left ? 0 : 1, shift));
            }
            return true;
        }
        if (alt && left && hoveredSlot != null && hoveredSlot.hasItem()) {
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

    /** The wheel over the panel goes no further, so the screen behind does not scroll. */
    public static boolean mouseScrolled(AbstractContainerScreen<?> on, double mouseX, double mouseY, double amount) {
        return visible(on) && inside(mouseX, mouseY);
    }
}
