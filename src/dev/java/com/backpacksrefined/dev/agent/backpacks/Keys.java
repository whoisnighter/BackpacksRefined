package com.backpacksrefined.dev.agent.backpacks;

import com.backpacksrefined.dev.agent.core.AgentException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The keys the agent may hold down, by short name.
 *
 * <p>Input goes through the game's own key state, never through the operating system. That is what
 * removes the footnote the manual-testing skill carries: a modifier sent as a real key event is
 * released again before the server ever sees the player crouch, whereas a key the game believes is
 * down stays down for as many ticks as it is asked to.
 *
 * <p>Only the keys every supported version has are listed. Vanilla has added and removed others around
 * them, and a name that resolves on one version and not on another would make a script version
 * specific for no gain.
 *
 * <p>Two of them are not simply held. Vanilla's own Sneak and Sprint accessibility settings turn those
 * keys into toggles, and a toggled key that the agent presses for thirty ticks crouches the player and
 * leaves them crouching, rather than crouching them for thirty ticks. The dev clients in this
 * repository have {@code toggleCrouch:true} in their options, so that is what a script gets unless it
 * changes it. {@code client.info} answers both settings for exactly this reason: read them before
 * holding sneak or sprint, and read the state back afterwards rather than assuming.
 */
final class Keys {
    private Keys() { }

    static Map<String, KeyMapping> all(Minecraft minecraft) {
        Map<String, KeyMapping> keys = new LinkedHashMap<>();
        keys.put("forward", minecraft.options.keyUp);
        keys.put("back", minecraft.options.keyDown);
        keys.put("left", minecraft.options.keyLeft);
        keys.put("right", minecraft.options.keyRight);
        keys.put("jump", minecraft.options.keyJump);
        keys.put("sneak", minecraft.options.keyShift);
        keys.put("sprint", minecraft.options.keySprint);
        keys.put("use", minecraft.options.keyUse);
        keys.put("attack", minecraft.options.keyAttack);
        keys.put("drop", minecraft.options.keyDrop);
        keys.put("inventory", minecraft.options.keyInventory);
        keys.put("swapHands", minecraft.options.keySwapOffhand);
        keys.put("pickItem", minecraft.options.keyPickItem);
        keys.put("playerList", minecraft.options.keyPlayerList);
        return keys;
    }

    static List<String> names(Minecraft minecraft) {
        return List.copyOf(all(minecraft).keySet());
    }

    static KeyMapping of(Minecraft minecraft, String name) {
        KeyMapping key = all(minecraft).get(name);
        if (key == null) {
            throw new AgentException("no key called '" + name + "'; the ones the agent holds are "
                    + String.join(", ", names(minecraft)));
        }
        return key;
    }
}
