import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.outputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
}

kotlin {
    sourceSets {
        main {
            resources.srcDir("build/generated")
        }
    }
}

tasks {
    val createVersionFile by registering {
        doFirst {
            val path = (layout.buildDirectory.asFile.get().toPath() / "generated" / "version")
            path.deleteIfExists()
            path.parent.createDirectories()
            path.outputStream(StandardOpenOption.CREATE).bufferedWriter().use {
                it.write("${rootProject.version}")
            }
        }
        outputs.upToDateWhen { false } // Always re-generate this file
    }
    processResources { dependsOn(createVersionFile) }
    compileKotlin { dependsOn(processResources) }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    System.getenv("CI_PROJECT_URL")?.let {
        website.set(it)
        vcsUrl.set(it)
    }
    plugins {
        create("KMbed Gradle Plugin") {
            id = "$group.${rootProject.name}-gradle-plugin"
            implementationClass = "$group.gradle.KmbedGradlePlugin"
            displayName = "KMbed Gradle Plugin"
            description = "Gradle plugin for applying the KMbed Kotlin compiler plugin"
            tags.addAll("kotlin", "native", "interop", "codegen")
        }
    }
}

publishing {
    System.getenv("CI_API_V4_URL")?.let { apiUrl ->
        repositories {
            maven {
                url = uri("$apiUrl/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create("header", HttpHeaderAuthentication::class)
                }
            }
        }
    }
    publications.configureEach {
        if (this is MavenPublication) {
            pom {
                name = project.name
                description = "Embedded resource tooling for Kotlin/Native."
                url = System.getenv("CI_PROJECT_URL")
                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "kitsunealex"
                        name = "KitsuneAlex"
                        url = "https://git.karmakrafts.dev/KitsuneAlex"
                    }
                }
                scm {
                    url = this@pom.url
                }
            }
        }
    }
}