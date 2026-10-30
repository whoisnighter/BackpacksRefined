import net.neoforged.moddevgradle.dsl.ModDevExtension

/*
 * What the NeoForge and Forge nodes share: both build with ModDevGradle, which has no client/server
 * source split. So the client sources compile into `main` here, and the NeoForge and Forge mod
 * classes keep client code off a dedicated server by only reaching it on the client (`dist =
 * Dist.CLIENT`, `FMLEnvironment.dist`). Loom splits them on Fabric instead; see build.fabric.gradle.kts.
 *
 * Applied by build.neoforge.gradle.kts and build.forge.gradle.kts after they set:
 * - `node.loader`, the loader's id, which names its source directories;
 * - `node.manifest`, the manifest path processResources expands;
 * - `node.clientSources`, where the client sources are for this node (see below);
 * - `node.manifestProperties`, what the manifest is expanded with;
 * - `node.devJvmArguments`, from build-logic;
 * - `node.agentSystemProperties`, `node.quickplayArguments` and `node.quickplaySystemProperties`, what
 *   `-Pagent`, `-Pdriven` and `-Pquickplay` hand a run (build-logic's Agent.kt);
 * - `node.devRunProgramArguments`, what every run that loads the dev mod also needs (Forge names its
 *   mixin config there).
 *
 * The dev tools (the agent client) are a mod of their own in the `dev` source set, loaded by every run
 * and in no published jar. See src/dev/java/AGENTS.md.
 */

@Suppress("UNCHECKED_CAST")
fun <T> handed(name: String): T = project.extensions.extraProperties.get(name) as T

val loader: String = handed("node.loader")
val clientSources: File = handed("node.clientSources")
val modId = property("mod.id") as String
val devModId = "backpacksrefined_dev"
val agentScript: Boolean = providers.gradleProperty("agent").isPresent

val sourceSets = the<SourceSetContainer>()
val main: SourceSet = sourceSets["main"]

// An empty `client` source set, only so Stonecutter generates src/client for this node
// (`stonecutterGenerateClient`): on a node that is not active, the client sources compiled below are
// the generated copy, not src/client itself.
sourceSets.create("client")

main.apply {
    java.srcDir("src/main/$loader/java")
    resources.srcDir("src/main/$loader/resources")
    java.srcDir(files(clientSources.resolve("java"), clientSources.resolve("$loader/java"))
        .builtBy("stonecutterGenerateClient"))
    resources.srcDir(files(clientSources.resolve("resources"), clientSources.resolve("$loader/resources"))
        .builtBy("stonecutterGenerateClient"))
}

val dev: SourceSet = sourceSets.create("dev") {
    java.srcDir("src/dev/$loader/java")
    resources.srcDir("src/dev/$loader/resources")
    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

// A -javaagent that stops an unattended run FML fails to load before any mod mixin applies (a missing
// dependency), which neither the agent nor a mixin can see. See src/watchdog.
val watchdog: SourceSet = sourceSets.create("watchdog") {
    java.srcDir("src/watchdog/java")
}
val watchdogJar = tasks.register<Jar>("watchdogJar") {
    archiveBaseName.set("backpacksrefined-watchdog")
    destinationDirectory.set(layout.buildDirectory.dir("watchdog"))
    from(watchdog.output)
    manifest.attributes("Premain-Class" to "com.backpacksrefined.dev.watchdog.LoadingFailureWatchdog")
}
if (agentScript) tasks.matching { it.name.startsWith("run") }.configureEach { dependsOn(watchdogJar) }

// What the runs put on their classpath: the dev mod beside the mod.
val clientRun: SourceSet = sourceSets.create("clientRun") {
    runtimeClasspath += dev.output + main.output + main.runtimeClasspath
}
val serverRun: SourceSet = sourceSets.create("serverRun") {
    runtimeClasspath += dev.output + main.output + main.runtimeClasspath
}

extensions.getByType<ModDevExtension>().apply {
    mods {
        create(modId) { sourceSet(main) }
        create(devModId) { sourceSet(dev) }
    }
    runs {
        val node = project.name
        configureEach {
            // One run directory per node, outside versions/, so `clean` never deletes a dev world.
            gameDirectory.set(rootProject.file(when (name) {
                "manualA", "manualB" -> "run/manual-$node-${name.last()}"
                else -> "run/$node"
            }))
            jvmArguments.addAll(handed<List<String>>("node.devJvmArguments"))
            handed<Map<String, String>>("node.agentSystemProperties").forEach(::systemProperty)
            if (agentScript) {
                jvmArguments.add(watchdogJar.flatMap { jar -> jar.archiveFile }.map { "-javaagent:${it.asFile.absolutePath}" })
            }
        }
        create("client") {
            client()
            sourceSet = clientRun
            programArguments.addAll("--username", "Dev")
            programArguments.addAll(handed<List<String>>("node.quickplayArguments"))
            handed<Map<String, String>>("node.quickplaySystemProperties").forEach(::systemProperty)
        }
        create("server") {
            server()
            sourceSet = serverRun
        }
        // Two more clients, each with a player name and a queue of its own, for what one client cannot
        // check: what each player sees of another's backpack. Start runServer first; they join it.
        listOf("A", "B").forEach { tester ->
            create("manual$tester") {
                client()
                sourceSet = clientRun
                programArguments.addAll(
                    "--username", "Tester$tester",
                    "--width", "1100", "--height", "700",
                    "--quickPlayMultiplayer", "localhost:25565",
                )
                systemProperty("backpacksrefined.agent", tester)
            }
        }
        val mainMod = mods.named(modId)
        val devMod = mods.named(devModId)
        val devRunArguments: List<String> = handed("node.devRunProgramArguments")
        configureEach {
            loadedMods.set(mainMod.zip(devMod) { main, agent -> setOf(main, agent) })
            programArguments.addAll(devRunArguments)
        }
    }
}

tasks.named<ProcessResources>("processDevResources") {
    val java = handed<Int>("node.requiredJava")
    inputs.property("java", java)
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$java") }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("stonecutterGenerate")
    val props: Map<String, Any> = handed("node.manifestProperties")
    inputs.properties(props)
    filesMatching(handed<String>("node.manifest")) { expand(props) }
}

// ModDevGradle decompiles and recompiles Minecraft per node. Several nodes doing it at once under
// `--parallel` run out of memory, so one at a time.
abstract class CreateMinecraftArtifactsMutex : BuildService<BuildServiceParameters.None>
val createMinecraftArtifactsMutex = gradle.sharedServices
    .registerIfAbsent("createMinecraftArtifactsMutex", CreateMinecraftArtifactsMutex::class) { maxParallelUsages = 1 }
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
    usesService(createMinecraftArtifactsMutex)
}
