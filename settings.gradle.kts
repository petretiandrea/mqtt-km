enableFeaturePreview("GRADLE_METADATA")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    plugins {
        id("org.jetbrains.dokka") version "1.4.0-rc"
    }
}

include(":mqtt-km")
project(":mqtt-km").projectDir = File(settingsDir, "./mqtt-km/")

include(":socket")

include(":example")