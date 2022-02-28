import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

fun Project.configureDoc(): TaskProvider<Jar> {
    val javadocJar by tasks.registering(Jar::class) { archiveClassifier.set("javadoc") }

    val dokkaOutputDir = "$buildDir/dokka"

    return javadocJar
}