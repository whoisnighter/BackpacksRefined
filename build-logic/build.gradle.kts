plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("buildLogic") {
            id = "backpacksrefined.build-logic"
            implementationClass = "com.backpacksrefined.buildlogic.BuildLogicPlugin"
        }
    }
}

dependencies {
    // On the node scripts' classpath through this plugin, so Publishing.kt can apply and configure it.
    implementation(libs.mod.publish.plugin)
}
