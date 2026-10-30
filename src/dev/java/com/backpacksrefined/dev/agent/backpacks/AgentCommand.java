package com.backpacksrefined.dev.agent.backpacks;

import com.mojang.brigadier.CommandDispatcher;
import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.dev.agent.core.AgentDispatcher;
import com.backpacksrefined.dev.platform.DevLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /backpacks agent probe|queue}, the one part of the agent a person types.
 *
 * <p>It answers the same thing the queue's {@code probe} does, and exists for the moment before an
 * agent is wired up at all: it says whether this process has a queue, where it is, and what it can
 * answer. A run where {@code /backpacks agent probe} works is a run whose {@code in.jsonl} will be read.
 */
public final class AgentCommand {
    private AgentCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Merges into the mod's own /backpacks node if it ever has one: whichever registration runs
        // first decides the root requirement, so it is stated here too.
        dispatcher.register(Commands.literal("backpacks")
                .requires(AgentVanilla::isGameMaster)
                .then(Commands.literal("agent")
                        .requires(AgentVanilla::isOwner)
                        .executes(context -> probe(context.getSource()))
                        .then(Commands.literal("probe").executes(context -> probe(context.getSource())))
                        .then(Commands.literal("queue").executes(context -> queue(context.getSource())))));
    }

    private static int probe(CommandSourceStack source) {
        AgentDispatcher agent = BackpacksAgent.dispatcher();
        if (agent == null) {
            source.sendFailure(Component.literal("No agent queue in this process. "
                    + "It is only installed under a development run, where the DEV gate is open."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("BackpacksRefined agent: side " + BackpacksAgent.side()
                + ", " + DevLoader.LOADER + ", Minecraft " + BackpacksRefined.MINECRAFT
                + ", " + agent.commands().size() + " commands, " + agent.pending() + " waiting"), false);
        source.sendSuccess(() -> Component.literal("Queue: "
                + agent.queue().directory().toAbsolutePath()), false);
        return agent.commands().size();
    }

    private static int queue(CommandSourceStack source) {
        AgentDispatcher agent = BackpacksAgent.dispatcher();
        if (agent == null) {
            source.sendFailure(Component.literal("No agent queue in this process"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(agent.queue().directory().toAbsolutePath().toString()), false);
        return 1;
    }
}
