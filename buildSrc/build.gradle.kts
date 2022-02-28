repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

plugins {
    `kotlin-dsl`
    `maven-publish`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}