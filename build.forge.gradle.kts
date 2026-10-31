import com.backpacksrefined.buildlogic.Loader
import com.backpacksrefined.buildlogic.agentSystemProperties
import com.backpacksrefined.buildlogic.configureModPublishing
import com.backpacksrefined.buildlogic.devJvmArguments
import com.backpacksrefined.buildlogic.quickplayArguments
import com.backpacksrefined.buildlogic.quickplaySystemProperties
import com.backpacksrefined.buildlogic.forgeMixinConfigLine
import com.backpacksrefined.buildlogic.requiredJava
import com.backpacksrefined.buildlogic.stringList

/*
 * The MinecraftForge 47 node (1.20.1), built with ModDevGradle Legacy. It differs from NeoForge in what
 * Forge 47 lacks: Mixin is not set up by the loader, so the annotation processor writes a refmap, the
 * jar manifest names the mixin configs, and MixinExtras is nested in the jar. The jar players install
 * is `reobfJar`, remapped back to SRG names.
 */

plugins {
    id("net.neoforged.moddev.legacyforge")
    id("backpacksrefined.build-logic")
    `maven-publish`
}

val modId = property("mod.id") as String
val loader = Loader.FORGE
val forgeVersion = property("deps.forge") as String
val java = requiredJava { sc.current.parsed >= it }

version = "${property("mod.version")}+${sc.current.version}-${loader.id}"
base.archivesName = "BackpacksRefined"

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

legacyForge {
    version = "${sc.current.version}-$forgeVersion"
}

// `modLocalRuntime`: a remapped mod in the dev runs that the published metadata never names. Only
// runtimeClasspath extends it, which no published variant is built from.
val localRuntime: Configuration = configurations.maybeCreate("localRuntime")
configurations.named("runtimeClasspath") { extendsFrom(localRuntime) }
obfuscation {
    createRemappingConfiguration(localRuntime)
}

extra["node.loader"] = loader.id
extra["node.manifest"] = "META-INF/mods.toml"
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
extra["node.devRunProgramArguments"] = listOf("--mixin.config", "backpacksrefined.dev.mixins.json")
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
    "forge" to forgeVersion,
)
apply(from = rootProject.file("gradle/moddev.gradle.kts"))

// The Mixin annotation processor writes the refmap, and the mappings reobfJar reads, only once there
// is a mixin to map. Until then the configs name no refmap and reobfJar is given none.
val hasMixins = listOf("src/main/java", "src/main/forge/java", "src/client/java", "src/client/forge/java")
    .map(rootProject::file).filter(File::isDirectory)
    .any { dir -> dir.walk().any { it.extension == "java" && "@Mixin(" in it.readText() } }
val refmap = "$modId.refmap.json".takeIf { hasMixins }

mixin {
    refmap?.let { add(sourceSets.main.get(), it) }
    config("$modId.mixins.json")
    config("$modId.client.mixins.json")
}

dependencies {
    annotationProcessor("${libs.mixin.processor.get()}:processor")
    val mixinExtras = property("deps.mixinextras") as String
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtras")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:$mixinExtras")!!)
    // Optional at runtime: the config screen is built with Cloth Config, remapped from SRG names like
    // any Forge mod. Compiled against and in the dev runs, never a dependency of the jar.
    val clothConfig = "maven.modrinth:cloth-config:${property("deps.cloth_config")}"
    "modCompileOnly"(clothConfig)
    "modLocalRuntime"(clothConfig)
}

tasks.processResources {
    inputs.property("refmap", refmap.orEmpty())
    filesMatching("*.mixins.json") {
        expand("java" to "JAVA_$java")
        filter { line -> forgeMixinConfigLine(line, refmap) }
    }
}

// Forge 47 reads the dev mod's mixin config from the command line (node.devRunProgramArguments), and its
// Mixin wants a minVersion there too. The dev mixins are never reobfuscated, so they take no refmap.
tasks.named<ProcessResources>("processDevResources") {
    filesMatching("*.mixins.json") { filter { line -> forgeMixinConfigLine(line, null) } }
}

// Forge 47 deprecated much of what it still requires; the warnings say nothing actionable.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-removal")
}

apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.jar {
    manifest.attributes("MixinConfigs" to mixin.configs.map { it.joinToString(",") })
}

val reobfJar = tasks.named<Jar>("reobfJar")
tasks.named<Copy>("buildAndCollect") {
    from(reobfJar.flatMap { it.archiveFile }, tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
}

configureModPublishing(
    jar = reobfJar,
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
