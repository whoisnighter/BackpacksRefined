package com.backpacksrefined.dev.agent.backpacks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.dev.agent.core.AgentDispatcher;
import com.backpacksrefined.dev.harness.DevEnvironment;

/** The three commands every process answers, whichever side it is. */
final class CommonProbes {
    private CommonProbes() { }

    static void register(AgentDispatcher dispatcher, String side) {
        /*
         * The heartbeat. An agent sends this first to learn what it is talking to, and it is the one
         * command whose answer never depends on a world, a player or a server being there.
         */
        dispatcher.register("probe", (request, reply) -> {
            JsonObject result = DevEnvironment.describe();
            result.addProperty("side", side);
            result.addProperty("queue", dispatcher.queue().directory().toAbsolutePath().toString());
            result.addProperty("serverRunning", BackpacksAgent.server() != null);
            result.addProperty("pending", dispatcher.pending());
            JsonArray commands = new JsonArray();
            dispatcher.commands().forEach(commands::add);
            result.add("commands", commands);
            reply.ok(result);
        });

        /*
         * Answers N ticks from now. This is how a script says "let the game catch up" without the agent
         * having to guess at wall-clock sleeps: the answer arrives when the ticks have actually run, so
         * a paused or slow game delays it instead of being read too early.
         */
        dispatcher.register("wait", (request, reply) -> {
            int ticks = request.integer("ticks", 1, 20 * 60);
            dispatcher.defer(ticks, reply, () -> {
                JsonObject result = new JsonObject();
                result.addProperty("ticks", ticks);
                reply.ok(result);
            });
        });

        /*
         * Stops this process. A script ends with it; `-Dbackpacksrefined.agent.script.exit=true` does
         * the same thing without the line. The answer is written before the game goes down, so
         * `out.jsonl` is complete afterwards.
         */
        dispatcher.register("stop", (request, reply) -> {
            reply.ok();
            BackpacksRefined.LOGGER.info("[BackpacksAgent] stopping, as the queue asked");
            BackpacksAgent.stop();
        });
    }
}
