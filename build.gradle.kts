import org.gradle.api.tasks.testing.logging.*;

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://h1.danbrough.org/maven/")
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.repsy.io/mvn/chrynan/public")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Deps.kotlin}")
    }
}

allprojects {

    group = "io.github.petretiandrea"
    version = BuildVersion.version

    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://h1.danbrough.org/maven/")
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.repsy.io/mvn/chrynan/public")
    }

    tasks.withType<Test> {
        dependsOn("cleanAllTests")
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
            showExceptions = true
            showStackTraces = true
            showStandardStreams = true
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR
            )
        }
    }
}

tasks.register("cleanBuild").configure {
    delete(rootProject.buildDir)
}