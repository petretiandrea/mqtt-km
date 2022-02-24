import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "1.6.10"
}

group = "io.github.petretiandrea"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://h1.danbrough.org/maven/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.repsy.io/mvn/chrynan/public")
}

kotlin {
    val compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
//        testRuns["test"].executionTask.configure {
//            useJUnitPlatform()
//        }
    }

    kotlin.targets.withType(KotlinNativeTarget::class.java) {
        binaries.all {
            binaryOptions["memoryModel"] = "experimental"
        }
    }

    mingwX64()
    linuxX64()
    linuxArm64()

    sourceSets {
        
        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
        }

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-danbroid")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2-danbroid") // allow arm64 support
                implementation("ru.pocketbyte.kydra:kydra-log:1.1.8")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val posixMain by creating {
            dependsOn(commonMain)
        }
        val posixTest by creating

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }

        val mingwX64Main by getting {
            dependsOn(posixMain)
        }
        val mingwX64Test by getting

        val linuxX64Main by getting {
            dependsOn(posixMain)
        }
        val linuxX64Test by getting

        val linuxArm64Main by getting {
            dependsOn(posixMain)
        }
        val linuxArm64Test by getting
    }
}
