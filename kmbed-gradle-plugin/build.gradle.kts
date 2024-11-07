import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
}

val generatedSourceDir: Path = projectDir.toPath() / "build" / "generated" / "sources"
val buildInfoFile: Path = projectDir.toPath() / "src" / "main" /
        rootProject.group.toString().replace('.', '/') / rootProject.name / "gradle" / "BuildInfo.kt"

sourceSets {
    val main by getting {
        kotlin {
            exclude(buildInfoFile.absolutePathString())
            srcDir(generatedSourceDir)
        }
    }
}

tasks {
    create<Copy>("processBuildInfo") {
        from(buildInfoFile) {
            filter<ReplaceTokens>(
                "tokens" to mapOf(
                    "GROUP" to group,
                    "VERSION" to version,
                    "PLUGIN_NAME" to "${rootProject.name}-compiler-plugin"
                )
            )
        }
        into(generatedSourceDir)
    }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    System.getenv("CI_PROJECT_URL")?.let {
        website.set(it)
        vcsUrl.set(it)
    }
    plugins {
        create("KMbed Gradle Plugin") {
            id = "$group.${rootProject.name}.gradle-plugin"
            implementationClass = "$group.${rootProject.name}.gradle.MbedGradlePlugin"
            displayName = "KMbed Gradle Plugin"
            description = "Gradle plugin for applying the KMbed Kotlin compiler plugin"
            tags.addAll("kotlin", "native", "interop", "codegen")
        }
    }
}
