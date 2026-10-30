package com.backpacksrefined.dev.agent.backpacks;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * The server-side vanilla calls the agent makes that differ between Minecraft versions, the
 * counterpart of {@link AgentClientVanilla}. Every Stonecutter conditional the agent needs on the
 * server lives here, so the probes read as one version.
 */
public final class AgentVanilla {
    private AgentVanilla() { }

    /** Whether a command source may run game master commands: permission level 2. 1.21.11 reworked permissions. */
    static boolean isGameMaster(CommandSourceStack source) {
        //? if >=1.21.11 {
        return net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(source.permissions());
        //?} else
        //return source.hasPermission(2);
    }

    /** Whether a command source may run owner-only commands: permission level 4. */
    static boolean isOwner(CommandSourceStack source) {
        //? if >=1.21.11 {
        return net.minecraft.commands.Commands.LEVEL_OWNERS.check(source.permissions());
        //?} else
        //return source.hasPermission(4);
    }

    /** A stack's item id, {@code minecraft:air} for an empty one. */
    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
