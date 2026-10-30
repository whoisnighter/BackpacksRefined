/*
 * The build code every node script shares, as plain Kotlin: node naming, the Java table, publishing.
 * It never depends on Loom, ModDevGradle or Stonecutter and never knows which node is active; the node
 * scripts pass that in and make every `sourceSets`, `loom` and `neoForge` call themselves.
 */
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "build-logic"
