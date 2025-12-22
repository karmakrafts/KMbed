/*
 * Copyright 2025 Karma Krafts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.defaultDokkaConfig
import dev.karmakrafts.conventions.setProjectInfo
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.outputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.serialization)
    `java-gradle-plugin`
    `maven-publish`
}

configureJava(libs.versions.java)
defaultDokkaConfig()

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
    sourceSets {
        main {
            resources.srcDir("build/generated")
        }
    }
}

tasks {
    val buildPath = layout.buildDirectory.asFile.get().toPath()
    val createVersionFile by registering {
        inputs.property("buildPath", buildPath)
        doFirst {
            val path = (buildPath / "generated" / "kmbed.version")
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

gradlePlugin {
    System.getenv("CI_PROJECT_URL")?.let {
        website = it
        vcsUrl = it
    }
    plugins {
        create("plugin") {
            id = "$group.${rootProject.name}-gradle-plugin"
            implementationClass = "$group.gradle.KmbedGradlePlugin"
            displayName = "kMbed Gradle Plugin"
            description = "Gradle plugin for applying the kMbed Kotlin compiler plugin"
            tags.addAll("kotlin", "native", "interop", "codegen")
        }
    }
}

publishing {
    setProjectInfo(
        name = "kMbed Gradle Plugin",
        description = "Gradle Plugin for bootstrapping the kMbed resource compiler.",
        url = "https://git.karmakrafts.dev/kk/kmbed"
    )
}