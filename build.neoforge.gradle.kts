import com.backpacksrefined.buildlogic.Loader
import com.backpacksrefined.buildlogic.agentSystemProperties
import com.backpacksrefined.buildlogic.configureModPublishing
import com.backpacksrefined.buildlogic.devJvmArguments
import com.backpacksrefined.buildlogic.quickplayArguments
import com.backpacksrefined.buildlogic.quickplaySystemProperties
import com.backpacksrefined.buildlogic.requiredJava
import com.backpacksrefined.buildlogic.stringList

/*
 * The NeoForge nodes. What they share with the Forge node is in gradle/moddev.gradle.kts, what every
 * loader shares in gradle/shared.gradle.kts.
 */

plugins {
    id("net.neoforged.moddev")
    id("backpacksrefined.build-logic")
    `maven-publish`
}

val modId = property("mod.id") as String
val loader = Loader.NEOFORGE
val neoForgeVersion = property("deps.neoforge") as String
val java = requiredJava { sc.current.parsed >= it }

version = "${property("mod.version")}+${sc.current.version}-${loader.id}"
base.archivesName = "BackpacksRefined"

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

neoForge {
    version = neoForgeVersion
}

// A mod in the dev runs that the published metadata never names. Only runtimeClasspath extends it,
// which no published variant is built from.
val localRuntime: Configuration = configurations.maybeCreate("localRuntime")
configurations.named("runtimeClasspath") { extendsFrom(localRuntime) }

dependencies {
    // Optional at runtime: the config screen is built with Cloth Config. Compiled against and in the
    // dev runs, never a dependency of the jar.
    val clothConfig = "maven.modrinth:cloth-config:${property("deps.cloth_config")}"
    compileOnly(clothConfig)
    localRuntime(clothConfig)
}

extra["node.loader"] = loader.id
extra["node.manifest"] = "META-INF/neoforge.mods.toml"
extra["node.clientSources"] =
    if (sc.current.isActive) rootProject.file("src/client")
    else layout.buildDirectory.dir("generated/stonecutter/client").get().asFile
extra["node.devJvmArguments"] = devJvmArguments
extra["node.requiredJava"] = java
extra["node.atLeast"] = { minecraft: String -> sc.current.parsed >= minecraft }
val gradleProperty = { name: String -> providers.gradleProperty(name).orNull }
extra["node.agentSystemProperties"] = agentSystemProperties(gradleProperty, rootProject.projectDir)
extra["node.quickplayArguments"] = quickplayArguments(gradleProperty)
extra["node.quickplaySystemProperties"] = quickplaySystemProperties(gradleProperty)
extra["node.devRunProgramArguments"] = emptyList<String>()
extra["node.manifestProperties"] = mapOf(
    "id" to modId,
    "name" to property("mod.name"),
    "description" to property("mod.description"),
    "authors" to property("mod.authors"),
    "license" to property("mod.license"),
    "credits" to property("mod.credits"),
    "homepage" to property("mod.homepage"),
    "issues" to property("mod.issues"),
    "version" to version,
    "minecraft" to property("mod.mc_compat"),
    "neoforge" to neoForgeVersion,
)
apply(from = rootProject.file("gradle/moddev.gradle.kts"))

tasks.processResources {
    inputs.property("java", java)
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$java") }
}

apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.named<Copy>("buildAndCollect") {
    from(tasks.jar.flatMap { it.archiveFile }, tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
}

configureModPublishing(
    jar = tasks.jar,
    sourcesJar = tasks.named<Jar>("sourcesJar"),
    loader = loader,
    minecraftVersions = stringList(property("mod.mc_releases")),
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
