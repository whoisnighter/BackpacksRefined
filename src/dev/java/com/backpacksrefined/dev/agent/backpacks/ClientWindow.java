package com.backpacksrefined.dev.agent.backpacks;

import com.backpacksrefined.BackpacksRefined;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * What a client that is being driven rather than played does with its window, the mouse pointer and
 * the screens between it and a world.
 *
 * <p>A driven client is one an agent sends requests to while the person who launched it is doing
 * something else on the same desktop. Three things about a played client get in the way of that, and
 * all are turned around by {@code -Pdriven}:
 *
 * <ul>
 *   <li><b>The pointer.</b> Minecraft takes the cursor as soon as the window is focused with a world
 *       open, and holds it inside the frame until a screen opens. Clicking the window to glance at
 *       the bar then costs the pointer everywhere else on the desktop. A driven client needs none of
 *       it: every key the agent holds goes through the game's own key state and every command goes
 *       through the player's connection, so nothing here is ever steered by a physical mouse.
 *       {@code MouseGrabMixin} refuses the grab outright rather than releasing it a tick later, so
 *       the cursor is never hidden and never warped to the middle of the window.</li>
 *   <li><b>The window size.</b> The run tasks open a small window, which is right for two clients
 *       side by side and wrong for reading a HUD. A driven one is maximised instead. Maximised, not
 *       full screen: exclusive full screen takes the whole display and is exactly what a person
 *       working beside it does not want.</li>
 *   <li><b>Vanilla's experimental settings prompt.</b> "Worlds using Experimental Settings are not
 *       supported" stands between {@code -Pquickplay} and the world whenever a mod on the classpath
 *       turns a feature flag on, which the Supplementaries and Moonlight Lib clients do. A driven
 *       client presses "I know what I'm doing!" for itself; a played one still gets to read it. This
 *       is the loader independent sibling of NeoForge's {@code LoadingWarnings}.</li>
 * </ul>
 *
 * <p>It is off by default because it is the opposite of what a manual pass needs: without the grab
 * there is no mouse look and no click reaching the world, so a person, or computer use standing in
 * for one, cannot play the client at all. An unattended run turns it on by itself, since
 * {@code -Pagent=<file>} means nobody is at the keyboard.
 */
public final class ClientWindow {
    /**
     * Read once, from a system property rather than from anything the game holds, because
     * {@code MouseGrabMixin} asks before the mod has initialized.
     */
    private static final boolean DRIVEN =
            Boolean.parseBoolean(System.getProperty("backpacksrefined.agent.driven", "false"))
                    || System.getProperty("backpacksrefined.agent.script") != null;

    /** "I know what I'm doing!", the button that opens a world vanilla calls experimental. */
    private static final Component SKIP_BACKUP = Component.translatable("selectWorld.backupJoinSkipButton");

    private ClientWindow() { }

    /** Whether this client is being driven by an agent rather than played by a person. */
    public static boolean driven() { return DRIVEN; }

    /**
     * Maximises the window and gives the pointer back, once, on the first client tick. The release is
     * not redundant with the mixin: a client started with {@code --quickPlayMultiplayer} can be in a
     * world with the cursor already taken before this runs, and the mixin only refuses the next grab.
     */
    public static void open(Minecraft minecraft) {
        if (!DRIVEN) return;
        // 26.3 moved the game off GLFW onto SDL, so the window handle is an SDL one from there on.
        //? if >=26.3 {
        org.lwjgl.sdl.SDLVideo.SDL_MaximizeWindow(AgentClientVanilla.windowHandle(minecraft));
        //?} else {
        /*org.lwjgl.glfw.GLFW.glfwMaximizeWindow(AgentClientVanilla.windowHandle(minecraft));
        *///?}
        if (minecraft.mouseHandler.isMouseGrabbed()) minecraft.mouseHandler.releaseMouse();
    }

    /**
     * Puts what the agent is doing in the window title, once a second, on a driven client: the request
     * it is answering, the ticks it still waits and how far through the script it is. A script that
     * waits minutes for a keg to ferment otherwise looks exactly like a hung game, a player standing
     * still with a bucket, to whoever glances at the window. The title rather than the screen, so
     * nothing of it reaches a capture. Vanilla sets its own title now and then, on joining a world for
     * one; the next second puts this one back.
     */
    public static void showStatus(Minecraft minecraft, String status, long tick) {
        if (!DRIVEN || status == null || tick % 20 != 0) return;
        minecraft.getWindow().setTitle("BackpacksRefined agent: " + status);
    }

    /**
     * Presses "I know what I'm doing!" on vanilla's experimental settings prompt, on a driven client
     * only. Called from the client tick while no world is loaded, which is the only time the prompt can
     * be up and keeps this off the path a client in a world takes.
     *
     * <p>The button is found by its message rather than by naming the screen, so a version that moves
     * or renames that screen leaves the prompt standing rather than failing to compile. It is pressed
     * by clicking its middle through {@link AgentClientVanilla#click}, which is where the one version
     * difference in a click already lives; {@code Button.onPress} took an argument from 26.3.
     */
    public static void passWorldPrompt(Minecraft minecraft) {
        if (!DRIVEN || minecraft.level != null) return;
        Screen screen = AgentClientVanilla.screen(minecraft);
        if (screen == null) return;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && SKIP_BACKUP.equals(button.getMessage())) {
                BackpacksRefined.LOGGER.info("[BackpacksAgent] passing \"{}\" on {}",
                        button.getMessage().getString(), screen.getClass().getSimpleName());
                AgentClientVanilla.click(screen, button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, 0);
                return;
            }
        }
    }
}
