package com.backpacksrefined.buildlogic

import java.io.File

/**
 * The system properties a development run gets from the agent's Gradle flags, the same on every
 * loader. See src/dev/java/com/backpacksrefined/dev/agent/AGENTS.md.
 *
 * - `-Pagent=<file>` runs that file of requests once the game is up, then stops the game. The path is
 *   relative to the repository root.
 * - `-Pdriven` makes a client a driven one: it never takes the mouse pointer and opens maximised.
 *   `-Pagent` implies it.
 *
 * @param gradleProperty the node's `providers.gradleProperty(name).orNull`
 * @param root the repository root
 */
fun agentSystemProperties(gradleProperty: (String) -> String?, root: File): Map<String, String> = buildMap {
    gradleProperty("agent")?.let { script ->
        put("backpacksrefined.agent.script", root.resolve(script).absolutePath)
        put("backpacksrefined.agent.script.exit", "true")
    }
    if (gradleProperty("driven") != null) put("backpacksrefined.agent.driven", "true")
}

/**
 * What `-Pquickplay=<world>` adds to a client: the program arguments that open that singleplayer world
 * of `run/<node>/saves` straight from launch, and the property the agent reports it under. Empty
 * without the flag.
 */
fun quickplayArguments(gradleProperty: (String) -> String?): List<String> =
    gradleProperty("quickplay")?.let { listOf("--quickPlaySingleplayer", it) }.orEmpty()

fun quickplaySystemProperties(gradleProperty: (String) -> String?): Map<String, String> =
    gradleProperty("quickplay")?.let { mapOf("backpacksrefined.agent.quickplay" to it) }.orEmpty()
