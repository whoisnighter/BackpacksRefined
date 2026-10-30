package com.backpacksrefined.buildlogic

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import java.util.Properties

/**
 * A value from the environment, or from the git-ignored `.env` file at the root of the repository when
 * the environment does not have it. Tokens live there; see .env.template.
 */
fun Project.env(name: String): String? =
    providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)
        ?: rootProject.file(".env").takeIf { it.isFile }?.let { file ->
            Properties().apply { file.inputStream().use(::load) }.getProperty(name)?.takeIf(String::isNotBlank)
        }

/**
 * `publishMods` for one node: uploads [jar] to Modrinth and CurseForge for [minecraftVersions].
 *
 * A site is left out when its project id in stonecutter.properties.toml is empty, and the whole task is
 * a dry run when a site it uploads to has no token, so `publishMods` is always safe to run locally.
 * [requiredMods] are slugs both sites know, such as `fabric-api`.
 */
fun Project.configureModPublishing(
    jar: TaskProvider<out Jar>,
    sourcesJar: TaskProvider<out Jar>,
    loader: Loader,
    minecraftVersions: List<String>,
    requiredMods: List<String> = emptyList(),
) {
    pluginManager.apply("me.modmuss50.mod-publish-plugin")

    val modName = property("mod.name").toString()
    val modrinthId = findProperty("publish.modrinth_id")?.toString().orEmpty()
    val curseforgeId = findProperty("publish.curseforge_id")?.toString().orEmpty()
    val modrinthToken = env("MODRINTH_TOKEN")
    val curseforgeToken = env("CURSEFORGE_TOKEN")
    val changelogFile = rootProject.file("CHANGELOG.md")

    extensions.configure<ModPublishExtension> {
        file.set(jar.flatMap { it.archiveFile })
        additionalFiles.from(sourcesJar.flatMap { it.archiveFile })
        type.set(ReleaseType.of(property("publish.type").toString()))
        version.set(project.version.toString())
        displayName.set("$modName ${project.version}")
        changelog.set(if (changelogFile.isFile) changelogFile.readText() else "")
        modLoaders.add(loader.id)
        dryRun.set((modrinthId.isNotEmpty() && modrinthToken == null) ||
            (curseforgeId.isNotEmpty() && curseforgeToken == null))

        if (modrinthId.isNotEmpty()) modrinth {
            projectId.set(modrinthId)
            accessToken.set(modrinthToken)
            this.minecraftVersions.addAll(minecraftVersions)
            if (requiredMods.isNotEmpty()) requires(*requiredMods.toTypedArray())
        }
        if (curseforgeId.isNotEmpty()) curseforge {
            projectId.set(curseforgeId)
            accessToken.set(curseforgeToken)
            this.minecraftVersions.addAll(minecraftVersions)
            if (requiredMods.isNotEmpty()) requires(*requiredMods.toTypedArray())
        }
    }
}
