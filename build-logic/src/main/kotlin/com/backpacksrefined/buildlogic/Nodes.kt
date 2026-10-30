package com.backpacksrefined.buildlogic

/** The mod loaders a node builds for. Its [id] is the second half of the node's name. */
enum class Loader(val id: String, val displayName: String) {
    FABRIC("fabric", "Fabric"),
    NEOFORGE("neoforge", "NeoForge"),
    /** MinecraftForge 47, the `1.20.1-forge` node. */
    FORGE("forge", "Forge"),
}

/** The loader a node builds for, from its name: `26.3-neoforge` is NeoForge. */
fun loaderOf(node: String): Loader {
    val id = node.substringAfterLast('-')
    return Loader.entries.firstOrNull { it.id == id }
        ?: error("Node '$node' does not end in a loader (${Loader.entries.joinToString { it.id }})")
}

/** A node's name without its loader: `26.3` for `26.3-neoforge`. */
fun minecraftOf(node: String): String = node.substringBeforeLast('-')

/**
 * The Java release a Minecraft version runs on, and so the toolchain its node compiles and runs with:
 * 25 from 26.1, 21 from 1.20.5, 17 before. Only Gradle itself always runs on 25.
 *
 * @param atLeast whether the node's Minecraft version is at least the one given, which the node script
 *   answers with Stonecutter's `sc.current.parsed >= version`
 */
fun requiredJava(atLeast: (String) -> Boolean): Int = when {
    atLeast("26.1") -> 25
    atLeast("1.20.5") -> 21
    else -> 17
}

/** JVM arguments every development run gets, on every loader. */
val devJvmArguments = listOf(
    "-XX:+IgnoreUnrecognizedVMOptions",
    "-XX:+ShowCodeDetailsInExceptionMessages",
    "-Dfile.encoding=UTF-8",
)

/**
 * A Mixin config's `"package"` line with what Forge 47's Mixin needs in front of it: the refmap's
 * name, which Loom writes in on Fabric, and the `minVersion` it logs an error without. [refmap] is
 * null while the mod has no mixin, since the annotation processor then writes none.
 */
fun forgeMixinConfigLine(line: String, refmap: String?): String {
    if (!line.trimStart().startsWith("\"package\":")) return line
    val indent = line.substringBefore("\"package\":")
    val added = listOfNotNull(refmap?.let { "\"refmap\": \"$it\"," }, "\"minVersion\": \"0.8\",")
    return added.joinToString("") { "$indent$it\n" } + line
}

/**
 * A comma separated stonecutter.properties.toml value as a list. Stonecutter exports no TOML array to
 * the node as a project property, so lists are written as one string; this also takes a list.
 */
fun stringList(value: Any?): List<String> = when (value) {
    null -> emptyList()
    is Iterable<*> -> value.map { it.toString() }
    else -> value.toString().removeSurrounding("[", "]").split(',')
        .map { it.trim().trim('"') }.filter(String::isNotEmpty)
}
