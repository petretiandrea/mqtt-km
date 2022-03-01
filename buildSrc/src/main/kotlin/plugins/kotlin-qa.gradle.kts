package plugins

import gradle.kotlin.dsl.accessors._e22d22ce689a20181b312a7a3bfeafc1.detekt
import gradle.kotlin.dsl.accessors._e22d22ce689a20181b312a7a3bfeafc1.detektPlugins
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories

plugins {
    id("io.gitlab.arturbosch.detekt")
    //id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Deps.detekt}")
}

tasks.register<Detekt>("detektAll") {
    description = "Runs Detekt on the whole project at once."
    parallel = true
    setSource(projectDir)
    include("**/*.kt", "**/*.kts")
    exclude("**/resources/**", "**/build/**", "**/*.gradle.kts")
    config.setFrom(project.file("detekt/detekt.yml"))
}

//val baselineFile = project.file("detekt/baseline.xml")
//
//tasks.register<Detekt>("detektAll") {
//    ...
//    baseline.set(baselineFile)
//}
//
//
//tasks.register<DetektCreateBaselineTask>("detektAllBaseline") {
//    description = "Create Detekt baseline on the whole project."
//    ignoreFailures.set(true)
//    setSource(projectDir)
//    config.setFrom(project.file("detekt/detekt.yml"))
//    include("**/*.kt", "**/*.kts")
//    exclude("**/resources/**", "**/build/**")
//    baseline.set(baselineFile)
//}