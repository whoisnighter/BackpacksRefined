import com.backpacksrefined.buildlogic.Loader
import com.backpacksrefined.buildlogic.agentSystemProperties
import com.backpacksrefined.buildlogic.configureModPublishing
import com.backpacksrefined.buildlogic.devJvmArguments
import com.backpacksrefined.buildlogic.quickplayArguments
import com.backpacksrefined.buildlogic.quickplaySystemProperties
import com.backpacksrefined.buildlogic.requiredJava
import com.backpacksrefined.buildlogic.stringList

/*
 * The Fabric nodes. Loom splits the sources into `main` and `client`, so a class under src/client can
 * never be loaded on a dedicated server. What every loader shares is in gradle/shared.gradle.kts.
 */

plugins {
    id("dev.kikugie.loom-back-compat")
    id("backpacksrefined.build-logic")
    `maven-publish`
}

val modId = property("mod.id") as String
val loader = Loader.FABRIC
val loaderVersion = property("deps.fabric_loader") as String
val java = requiredJava { sc.current.parsed >= it }

version = "${property("mod.version")}+${sc.current.version}-${loader.id}"
base.archivesName = "BackpacksRefined"

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

// The dev tools: the agent client, its own small mod that no published jar contains. See
// src/dev/java/AGENTS.md.
val dev: SourceSet = sourceSets.create("dev")

loom {
    splitEnvironmentSourceSets()

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
        register("backpacksrefined_dev") {
            sourceSet(dev)
        }
    }

    runs {
        // The server runs with the dev tools on its classpath; the client gets them through devClient.
        named("server") {
            sourceSet = dev.name
        }
    }

    val gradleProperty = { name: String -> providers.gradleProperty(name).orNull }
    runConfigs.matching { it.name != "gameTest" }.all {
        // One run directory per node, outside versions/, so `clean` never deletes a dev world.
        runDirectory = rootProject.file("run/${project.name}")
        jvmArguments.addAll(devJvmArguments)
        systemProperties.putAll(agentSystemProperties(gradleProperty, rootProject.projectDir))
        if (name == "client") {
            programArguments.addAll("--username", "Dev")
            programArguments.addAll(quickplayArguments(gradleProperty))
            systemProperties.putAll(quickplaySystemProperties(gradleProperty))
        }
    }
}

// The loader's own sources: entrypoints and the Fabric copy of platform/Loader.
sourceSets.main {
    java.srcDir("src/main/${loader.id}/java")
    resources.srcDir("src/main/${loader.id}/resources")
}
sourceSets.named("client") {
    java.srcDir("src/client/${loader.id}/java")
    resources.srcDir("src/client/${loader.id}/resources")
}

dev.java.srcDir("src/dev/${loader.id}/java")
dev.resources.srcDir("src/dev/${loader.id}/resources")

tasks.named("processResources") { dependsOn("stonecutterGenerate") }

// The dev mod sees the mod, client half included: the agent answers about the client.
dev.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output +
    sourceSets["client"].compileClasspath + sourceSets["client"].output
dev.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output +
    sourceSets["client"].output
// What runClient runs: the dev mod beside the mod's client. Its own source set rather than `dev`, so
// the server run keeps no client classes on its classpath.
val devClient: SourceSet = sourceSets.create("devClient") {
    runtimeClasspath = dev.output + dev.runtimeClasspath + sourceSets["client"].runtimeClasspath
}
loom {
    runs {
        named("client") {
            sourceSet = devClient.name
        }
    }
}

// The game tests (src/gametest), on the nodes whose table sets `test.gametest`: their own small mod,
// in no published jar, that `runGameTest` loads into a headless server. It runs every test, writes
// build/gametest/report.xml and fails when one does. `check` compiles them.
if (findProperty("test.gametest")?.toString() == "true") {
    val gametest: SourceSet = sourceSets.create("gametest") {
        java.srcDir("src/gametest/${loader.id}/java")
        resources.srcDir("src/gametest/${loader.id}/resources")
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
    }
    loom {
        mods {
            register("backpacksrefined_gametest") {
                sourceSet(gametest)
            }
        }
        runs {
            register("gameTest") {
                server()
                displayName = "Game Test"
                sourceSet = gametest.name
                // A fresh world every run: the tests build their own structures and leave nothing worth keeping.
                runDirectory = layout.buildDirectory.dir("gametest")
                jvmArguments.addAll(devJvmArguments)
                systemProperties.put("fabric-api.gametest", "true")
                systemProperties.put("fabric-api.gametest.report-file",
                    layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath)
                generateRunConfig = false
            }
        }
    }
    tasks.named("runGameTest") {
        doFirst { layout.buildDirectory.dir("gametest/world").get().asFile.deleteRecursively() }
    }
    tasks.named("check") { dependsOn("gametestClasses") }
}

// `mod*` configurations exist on obfuscated versions only; on 26.x the plain one is the same thing.
fun modDependency(configuration: String, notation: Any) {
    val prefixed = "mod${configuration.replaceFirstChar(Char::uppercase)}"
    dependencies.add(if (configurations.findByName(prefixed) != null) prefixed else configuration, notation)
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    // Optional at runtime: the config screen is built with Cloth Config and ModMenu opens it. Compiled
    // against and in the dev runs, never a dependency of the jar.
    findProperty("deps.modmenu")?.let {
        modDependency("compileOnly", "maven.modrinth:modmenu:$it")
        modDependency("localRuntime", "maven.modrinth:modmenu:$it")
    }
    findProperty("deps.cloth_config")?.let {
        modDependency("compileOnly", "maven.modrinth:cloth-config:$it")
        modDependency("localRuntime", "maven.modrinth:cloth-config:$it")
    }
}

// What fabric.mod.json is expanded with, from stonecutter.properties.toml.
val manifestProperties = mapOf(
    "id" to modId,
    "name" to property("mod.name"),
    "description" to property("mod.description"),
    "authors" to property("mod.authors"),
    "license" to property("mod.license"),
    "credits" to property("mod.credits"),
    "homepage" to property("mod.homepage"),
    "sources" to property("mod.sources"),
    "issues" to property("mod.issues"),
    "version" to version,
    "minecraft" to property("mod.mc_compat"),
    "loader" to loaderVersion,
    "java" to java,
)

tasks.processResources {
    inputs.properties(manifestProperties)
    filesMatching("fabric.mod.json") { expand(manifestProperties) }
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$java") }
}

listOf("processClientResources", "processDevResources").forEach { task ->
    tasks.named<ProcessResources>(task) {
        inputs.property("java", java)
        filesMatching("*.mixins.json") { expand("java" to "JAVA_$java") }
    }
}

// Loom runs the client from the client source set's output, which does not exist until something is
// in src/client/resources.
tasks.named("runClient") {
    doFirst { layout.buildDirectory.dir("resources/client").get().asFile.mkdirs() }
}

tasks.withType<Jar>().matching { it.name.endsWith("sourcesJar") }.configureEach {
    dependsOn("stonecutterGenerate")
}

extra["node.requiredJava"] = java
extra["node.atLeast"] = { minecraft: String -> sc.current.parsed >= minecraft }
apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.named<Copy>("buildAndCollect") {
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
}

configureModPublishing(
    jar = loomx.modJar,
    sourcesJar = loomx.modSourcesJar,
    loader = loader,
    minecraftVersions = stringList(property("mod.mc_releases")),
    requiredMods = listOf("fabric-api"),
)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = property("mod.group") as String
            artifactId = modId
            from(components["java"])
        }
    }
}
