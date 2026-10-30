import com.backpacksrefined.buildlogic.loaderOf
import com.backpacksrefined.buildlogic.minecraftOf

plugins {
    id("dev.kikugie.stonecutter")
    id("backpacksrefined.build-logic")
}

// The node the source tree is currently checked out for, kept in its own file so switching it with
// `./gradlew "Set active project to <name>"` leaves this script alone. Every node is built regardless.
stonecutter active file(".sc_active_version")

stonecutter parameters {
    // Tagging both halves of the node name lets stonecutter.properties.toml keep what the loaders share
    // in `["26.3"]` and the rest in `[fabric."26.3"]` or `[neoforge."26.3"]`; see the top of that file.
    properties {
        tags(minecraftOf(current.project), loaderOf(current.project).id)
    }

    // `//? if fabric {`, `//? if neoforge {`, `//? if forge {`. Only inside platform/ and mixin/
    // packages; checkVersionSeam enforces it.
    constants.match(loaderOf(current.project).id, "fabric", "neoforge", "forge")

    // Bakes the target Minecraft version into the jar, so the startup log line is never stale.
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    // Every replacement also runs backwards, on each node whose condition is false and on "Reset
    // active project". What a rule writes must therefore be the same on every node it applies to, and
    // must not occur in the sources for any other reason. Sources are written in 26.3's names.
    replacements {
        // 1.21.11 renamed ResourceLocation to Identifier and ResourceKey#location to #identifier, and
        // moved Util into net.minecraft.util, changing nothing else about any of them.
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
            replace(".identifier()", ".location()")
            replace("net.minecraft.util.Util", "net.minecraft.Util")
        }
        // 1.21 made ResourceLocation's constructors private behind factory methods of the same meaning.
        string(current.parsed >= "1.21" && current.parsed < "1.21.11") {
            replace("Identifier.fromNamespaceAndPath(", "ResourceLocation.fromNamespaceAndPath(")
            replace("Identifier.withDefaultNamespace(", "ResourceLocation.withDefaultNamespace(")
            replace("Identifier.parse(", "ResourceLocation.parse(")
        }
        // Before 1.21 all three are constructors, spelled apart so each finds its way back.
        string(current.parsed < "1.21") {
            replace("Identifier.fromNamespaceAndPath(", "new ResourceLocation(")
            replace("Identifier.withDefaultNamespace(", "new ResourceLocation(\"minecraft\", ")
            replace("Identifier.parse(", "new ResourceLocation((String) ")
        }
        // 26.1 renamed the HUD draw target, keeping the drawing methods.
        string(current.parsed < "26.1") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace(".fakeItem(", ".renderFakeItem(")
        }
    }
}

// Conveniences over every node. `chiseledBuild`-style aggregates, without the IDE having to know
// which node is active.
tasks.register("buildAll") {
    group = "build"
    description = "Builds every node and copies the jars to build/libs/"
    dependsOn(subprojects.map { "${it.path}:buildAndCollect" })
}

tasks.register("publishAll") {
    group = "publishing"
    description = "Publishes every node to Modrinth and CurseForge (a dry run without tokens)"
    dependsOn(subprojects.map { "${it.path}:publishMods" })
}

val activeNode = providers.fileContents(layout.projectDirectory.file(".sc_active_version")).asText.map(String::trim)
listOf("Client", "Server").forEach { side ->
    tasks.register("runActive$side") {
        group = "stonecutter"
        description = "Runs the ${side.lowercase()} of the active node"
        dependsOn(activeNode.map { ":$it:run$side" })
    }
}
