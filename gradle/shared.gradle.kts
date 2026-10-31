/*
 * The tasks every node shares, whatever mod loader it builds for: the Java toolchain, the seam checks,
 * what never belongs in a jar, and the task that collects the jars.
 *
 * Applied by build.fabric.gradle.kts, build.neoforge.gradle.kts and build.forge.gradle.kts, each of
 * which sets `node.requiredJava` first: the Java version follows from the node's Minecraft version,
 * and only the node's own script can read that. A script applied with `apply(from)` cannot see
 * build-logic's classes, so it is handed what it needs as plain values.
 */

val requiredJava: Int = project.extensions.extraProperties.get("node.requiredJava") as Int

// Each node compiles and runs with the JDK its Minecraft version ships for, as players run it: 17 for
// 1.20.1, 21 for 1.21.x, 25 for 26.x. foojay downloads one that is not installed.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(requiredJava)
}

configure<JavaPluginExtension> {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(requiredJava)
    targetCompatibility = JavaVersion.toVersion(requiredJava)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(requiredJava))
    }
}

// Folder notes for contributors stay out of the jars.
tasks.withType<ProcessResources>().configureEach {
    exclude("**/AGENTS.md", "**/*.bak")
}
tasks.withType<Jar>().matching { it.name.endsWith("sourcesJar") }.configureEach {
    exclude("**/AGENTS.md", "**/*.bak")
}

// The licence goes into every jar under a name of its own, so it does not collide with another mod's.
// The credits and the notice of every mod this one takes from (licenses/) go in too: MIT asks for the
// notice in every copy.
val modId = property("mod.id") as String
tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { rename { "LICENSE_$modId" } }
    from(rootProject.file("CREDITS.md")) { rename { "CREDITS_$modId.md" } }
    from(rootProject.file("licenses")) { into("licenses/$modId") }
}

// The loader seam: code under src/main/java and src/client/java compiles on every loader, so it never
// imports a loader's API. It would compile on the active node and break on the others; this catches it
// on any node.
tasks.register("checkLoaderSeam") {
    group = "verification"
    description = "Fails when loader independent sources import a mod loader's API"
    val roots = listOf("src/main/java", "src/client/java").map(rootProject::file).filter(File::isDirectory)
    val forbidden = Regex("""\b(net\.fabricmc|net\.neoforged|net\.minecraftforge)\.""")
    inputs.files(roots.map { fileTree(it) { include("**/*.java") } })
    doLast {
        val offenders = roots.flatMap { root ->
            root.walk().filter { it.extension == "java" }.flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> forbidden.containsMatchIn(line) }
                    .map { (index, line) ->
                        "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                    }
            }
        }
        check(offenders.isEmpty()) {
            "Loader API in loader independent code. Route it through platform/Loader instead:\n" +
                offenders.joinToString("\n")
        }
    }
}

// The version seam: a Stonecutter conditional (`//?`) only inside a platform/ or mixin/ package, so
// the rest of the mod reads as one version and every difference has one home.
tasks.register("checkVersionSeam") {
    group = "verification"
    description = "Fails when mod sources outside platform/ and mixin/ contain a version conditional"
    val roots = listOf("main", "client").flatMap { set ->
        val dir = rootProject.file("src/$set")
        listOf(dir.resolve("java")) + dir.listFiles().orEmpty().filter(File::isDirectory).map { it.resolve("java") }
    }.filter(File::isDirectory)
    val allowed = setOf("platform", "mixin")
    inputs.files(roots.map { fileTree(it) { include("**/*.java") } })
    doLast {
        val offenders = roots.flatMap { root ->
            root.walk()
                .filter { it.extension == "java" }
                .filterNot { file -> file.relativeTo(root).invariantSeparatorsPath.split('/').any(allowed::contains) }
                .flatMap { file ->
                    file.readLines().withIndex()
                        .filter { (_, line) -> line.contains("//?") }
                        .map { (index, line) ->
                            "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                        }
                }
        }
        check(offenders.isEmpty()) {
            "Version conditional outside platform/ and mixin/. Put the difference behind a class in a " +
                "platform package instead:\n" + offenders.joinToString("\n")
        }
    }
}

// Every lang file has exactly the keys en_us.json has, so no language shows a raw key.
tasks.register("checkLang") {
    group = "verification"
    description = "Fails when a lang file does not have exactly the keys en_us.json has"
    val langDirs = rootProject.file("src").walk()
        .onEnter { it.name != "java" }
        .filter { it.isDirectory && it.name == "lang" && it.parentFile.parentFile.name == "assets" }
        .toList()
    inputs.files(langDirs.map { fileTree(it) { include("*.json") } })
    doLast {
        @Suppress("UNCHECKED_CAST")
        fun keys(file: File): Set<String> = (groovy.json.JsonSlurper().parse(file) as Map<String, Any?>).keys
        val problems = langDirs.flatMap { dir ->
            val english = dir.resolve("en_us.json")
            if (!english.isFile) return@flatMap emptyList<String>()
            val expected = keys(english)
            dir.listFiles().orEmpty().filter { it.extension == "json" && it.name != "en_us.json" }.sortedBy { it.name }
                .flatMap { file ->
                    val actual = keys(file)
                    val path = file.relativeTo(rootProject.projectDir).invariantSeparatorsPath
                    (expected - actual).map { "$path: missing $it" } + (actual - expected).map { "$path: not in en_us: $it" }
                }
        }
        check(problems.isEmpty()) {
            "Every lang file must have exactly the keys en_us.json has:\n" + problems.joinToString("\n")
        }
    }
}

// The agent's core package is plain Java and Gson: the queue, the replies, the dispatch loop. What
// names Minecraft, a loader or the mod lives in dev/agent/backpacks.
tasks.register("checkAgentCore") {
    group = "verification"
    description = "Fails when the agent's core package imports Minecraft, a mod loader or the mod"
    val root = rootProject.file("src/dev/java/com/backpacksrefined/dev/agent/core")
    val forbidden = Regex("""^import\s+(net\.minecraft|net\.fabricmc|net\.neoforged|net\.minecraftforge|"""
        + """com\.mojang|com\.backpacksrefined(?!\.dev\.agent\.core))""")
    inputs.files(fileTree(root) { include("**/*.java") })
    doLast {
        val offenders = root.walk().filter { it.extension == "java" }.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> forbidden.containsMatchIn(line.trim()) }
                .map { (index, line) ->
                    "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                }
        }.toList()
        check(offenders.isEmpty()) {
            "The agent's core package is meant to know nothing of Minecraft, of a loader or of this " +
                "mod. Put what needs them in dev/agent/backpacks instead:\n" + offenders.joinToString("\n")
        }
    }
}

// `devClasses` compiles the agent client, which no published jar contains, so nothing else would
// notice it breaking on one version.
tasks.named("check") { dependsOn("checkLoaderSeam", "checkVersionSeam", "checkLang", "checkAgentCore", "devClasses") }

// Each node script adds its own jar and sources jar: which task makes the jar players install differs
// per loader (Loom's remapped jar, Forge's reobfuscated one).
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds the mod jar and copies it to build/libs/"
    into(rootProject.layout.buildDirectory.dir("libs"))
}

// Data files are written in 26.3's format, as the sources are, and rewritten here for the nodes that read
// an older one. Recipes and loot tables lived in plural folders before 1.21. A recipe's ingredients were
// objects before 1.21.2 ({"item": ...}, {"tag": ...}) where they are now an id or #tag, and its result
// named the item `item` before 1.20.5. Ingredients are found as single character keys, the only ones a
// shaped recipe's `key` has, so a recipe keeps one ingredient a line.
@Suppress("UNCHECKED_CAST")
val atLeast = project.extensions.extraProperties.get("node.atLeast") as (String) -> Boolean
tasks.named<ProcessResources>("processResources") {
    val ingredient = Regex("""^(\s*"."\s*:\s*)"(#?)([^"]+)"(,?)\s*$""")
    if (!atLeast("1.21.2")) filesMatching("data/*/recipe/*.json") {
        filter { line ->
            ingredient.matchEntire(line)?.let { match ->
                val (key, tag, id, comma) = match.destructured
                "$key{\"${if (tag.isEmpty()) "item" else "tag"}\": \"$id\"}$comma"
            } ?: line
        }
    }
    if (!atLeast("1.20.5")) filesMatching("data/*/recipe/*.json") {
        filter { line -> line.replace("\"id\":", "\"item\":") }
    }
    if (!atLeast("1.21")) {
        filesMatching("data/*/recipe/**") { path = path.replace("/recipe/", "/recipes/") }
        filesMatching("data/*/loot_table/**") { path = path.replace("/loot_table/", "/loot_tables/") }
    }
}
