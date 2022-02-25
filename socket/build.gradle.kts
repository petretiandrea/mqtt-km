plugins {
    kotlin("multiplatform")
}

group = "io.github.petretiandrea"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
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