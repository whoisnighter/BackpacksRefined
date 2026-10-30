pluginManagement {
    // The build code every node script shares: the node naming, the Java table, the publishing setup.
    // See build-logic/settings.gradle.kts. A script puts it on its classpath with
    // `id("backpacksrefined.build-logic")`.
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    }
}

plugins {
    // One source tree, one jar per Minecraft version and loader. See AGENTS.md.
    id("dev.kikugie.stonecutter") version "0.9.8"
    // 26.1 dropped obfuscation, which changed how Loom consumes mod dependencies. This applies the
    // Loom variant each version needs and keeps `modImplementation` meaning the same thing on both.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    // NeoForge's build plugin, applied only by build.neoforge.gradle.kts: applying it to a Fabric node
    // would put a second Minecraft provider on a project Loom already owns.
    id("net.neoforged.moddev") version "2.0.148" apply false
    // The same plugin family for MinecraftForge 47, applied only by build.forge.gradle.kts. It remaps
    // Minecraft and mod dependencies to Mojang's names for development and the jar back to SRG names.
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
    // Provisions the JDK a node needs when it is not installed locally: 26.x wants Java 25, 1.21.x
    // Java 21, 1.20.1 Java 17. Gradle itself runs on Java 25 (gradle/gradle-daemon-jvm.properties).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Every node is `<minecraft>-<loader>`, and its loader picks its buildscript, so adding a node is
        // one line here and one table in stonecutter.properties.toml.
        fun node(minecraft: String, vararg loaders: String) = loaders.forEach { loader ->
            version("$minecraft-$loader", minecraft).buildscript = "build.$loader.gradle.kts"
        }

        node("26.3", "fabric", "neoforge")
        node("26.2", "fabric")
        node("1.21.11", "fabric")
        node("1.21.1", "neoforge")
        // MinecraftForge 47, not NeoForge: nearly every 1.20.1 modpack runs Forge.
        node("1.20.1", "forge")

        // The node the committed sources are written for. Switching the active node rewrites the
        // sources; "Reset active project" brings them back to this one before a commit.
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "BackpacksRefined"
