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
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${BuildVersion.kotlin}")
    }
}


allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://h1.danbrough.org/maven/")
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.repsy.io/mvn/chrynan/public")
    }
}

tasks.register("clean").configure {
    delete(rootProject.buildDir)
}