package com.backpacksrefined.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Does nothing when applied. `id("backpacksrefined.build-logic")` in a script's `plugins { }` is how
 * that script gets [loaderOf], [requiredJava] and [configureModPublishing] on its classpath.
 */
class BuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) { }
}
