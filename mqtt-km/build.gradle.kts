import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover") version "0.5.0"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    kotlin.targets.withType(KotlinNativeTarget::class.java) {
        binaries.all {
            binaryOptions["memoryModel"] = "experimental"
        }
    }

    mingwX64()
    linuxX64()
    linuxArm64()

    configureMavenPublish()

    sourceSets {

        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.RequiresOptIn")
        }

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Deps.kotlinSerialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Deps.kotlinCoroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Deps.kotlinDateTime}") // allow arm64 support
                implementation("ru.pocketbyte.kydra:kydra-log:${Deps.kydraVersion}")
                implementation(project(":socket"))
                //implementation("io.github.petretiandrea:socket:1.0-SNAPSHOT")
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
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:${Deps.junit}")
            }
        }

        val mingwX64Main by getting {
            dependsOn(posixMain)
        }
        val mingwX64Test by getting {
            dependsOn(commonTest)
        }

        val linuxX64Main by getting {
            dependsOn(posixMain)
        }
        val linuxX64Test by getting {
            dependsOn(commonTest)
        }

        val linuxArm64Main by getting {
            dependsOn(posixMain)
        }
        val linuxArm64Test by getting
    }
}

tasks.withType<Test> {
    extensions.configure(kotlinx.kover.api.KoverTaskExtension::class) {
        isDisabled = false
        binaryReportFile.set(file("$buildDir/custom/result.bin"))
    }
}