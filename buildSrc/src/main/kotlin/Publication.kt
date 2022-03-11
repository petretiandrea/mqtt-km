import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import java.util.*

fun Project.configureMavenPublish() {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    loadMavenCredentialsTo(this)

    // actually empty javadoc to fix maven
    val javadocTask = configureDoc()

    the<PublishingExtension>().apply {
        repositories {
            maven {
                name = "oss"
                val releaseRepoUrl =
                    uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releaseRepoUrl
                credentials {
                    username = project.extra["ossrhUsername"].toString()
                    password = project.extra["ossrhPassword"].toString()
                }
            }
        }

        publications {
            withType<MavenPublication> {
                if (name.contains("jvm")) {
                    artifact(javadocTask)
                }

                pom {
                    name.set(project.name)
                    description.set(project.description)
                    url.set("https://github.com/petretiandrea/mqtt-km/")

                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    scm {
                        connection.set("https://github.com/petretiandrea/mqtt-km.git")
                        url.set("https://github.com/petretiandrea/mqtt-km.git")
                    }

                    developers {
                        developer {
                            name.set("Andrea Petreti")
                            email.set("petretiandrea@gmail.com")
                        }
                    }
                }
            }
        }
    }

    val publishToMavenLocal = tasks.getByName("publishToMavenLocal")
    tasks.getByName("publish").dependsOn(publishToMavenLocal)

    val keyId = project.extra["signing.keyId"]?.toString()
    val signingKey = project.extra["signing.key"]?.toString()
    val signingPassword = project.extra["signing.password"]?.toString()

    if (keyId != null && signingKey != null && signingPassword != null) {
        the<SigningExtension>().apply {
            useInMemoryPgpKeys(keyId, signingKey, signingPassword)
            sign(the<PublishingExtension>().publications)
        }
    }
}

private fun loadMavenCredentialsTo(project: Project) {
    val secretPropsFile = project.rootProject.file("local.properties")
    if (secretPropsFile.exists()) {
        // Read local.properties file first if it exists
        val p = Properties()
        secretPropsFile.inputStream().use { input -> p.load(input) }
        p.forEach { (name, value) -> project.extra[name.toString()] = value }
    } else {
        // Use system environment variables
        project.extra["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
        project.extra["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
        project.extra["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
        project.extra["signing.password"] = System.getenv("SIGNING_PASSWORD")
        project.extra["signing.key"] = System.getenv("SIGNING_KEY")
    }
}