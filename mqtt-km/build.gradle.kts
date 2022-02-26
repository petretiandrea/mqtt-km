import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover") version "0.5.0"

    id("maven-publish")
    id("signing")
}

group = "io.github.petretiandrea"
version = "1.0-SNAPSHOT"

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

publishing {
    repositories {
        maven {
            name = "Sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = project.findProperty("sonatype.publish.user")?.toString() ?: ""
                password = project.findProperty("sonatype.publish.password")?.toString() ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

publishing {

}