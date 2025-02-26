/*
 * Copyright 2025 Karma Krafts & associates
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

package io.karma.kmbed.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.extensions.stdlib.capitalized
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.div

open class KmbedGradlePlugin @Inject constructor(
    private val providers: ProviderFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent(
            "KMbed Build Service", KmbedBuildService::class.java
        )

        val logger = project.logger
        logger.lifecycle(
            """
                
            8  dP 8b   d8 8             8
            8wdP  8YbmdP8 88b. .d88b .d88  Resource Compiler
            88Yb  8  "  8 8  8 8.dP' 8  8  Version ${BuildInfo.version}
            8  Yb 8     8 88P' `Y88P `Y88
        """.trimIndent()
        )

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val groupName = project.group.toString()
            val extension = project.extensions.create("kmbed", KmbedProjectExtension::class.java, groupName)
            project.extensions.add("kmbedSourceSets", extension.kmbedSourceSets)
            project.afterEvaluate {
                // Add all defined source sets
                extension.kmbedSourceSets.forEach { sourceSet ->
                    sourceSet.registerGenerationTask(project, serviceProvider)
                }
                registerCommonGenerationTask(project, extension.commonSourceSetName)
                registerCommonGenerationTask(project, extension.commonTestSourceSetName)
            }
        }
    }

    private fun registerCommonGenerationTask(project: Project, name: String) {
        val generateCommonMainKmbedSources = project.tasks.register(
            "generate${name.capitalized()}KmbedSources", KmbedGenerateCommonSourcesTask::class.java
        ).get().apply {
            group = "kmbed"
            val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / name
            sourceDirectory.set(outputDir.toFile())
        }
        project.kotlinMultiplatformExtension.sourceSets.findByName(name)?.kotlin?.srcDir(
            generateCommonMainKmbedSources.sourceDirectory.asFile
        )
    }

    private fun KmbedSourceSet.registerGenerationTask(project: Project, serviceProvider: Provider<KmbedBuildService>) {
        // Register all required generation tasks for this compilation
        val resourceSet = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it.exists() }
        val compName = "${compilation.target.name}${compilation.name.capitalized()}"
        val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / compName
        outputDir.createDirectories()
        val taskName = "generate${compName.capitalized()}KmbedSources"

        val generateTask = project.tasks.register(
            taskName, KmbedGenerateSourcesTask::class.java, serviceProvider
        ).get().apply {
            group = "kmbed"
            platformType = compilation.platformType
            resourceDirectories.setFrom(*resourceSet.toTypedArray())
            sourceDirectory.set(outputDir.toFile())
        }

        // Add dependency from compile task so sources get automatically regenerated on every build
        fun Task.dependsOnGeneration() {
            dependsOn(generateTask)
            mustRunAfter(generateTask)
            // We depend on either source set, defaulting to main instead of test
            val commonName = if ("test" in name.lowercase()) "generateCommonTestKmbedSources"
            else "generateCommonMainKmbedSources"
            dependsOn(commonName)
            mustRunAfter(commonName)
        }

        project.tasks.findByName("${compilation.target.name}SourcesJar")?.apply {
            dependsOnGeneration()
        }
        project.tasks.findByName(compilation.compileKotlinTaskName)?.apply {
            dependsOnGeneration()
        }
        project.tasks.findByName("commonize")?.apply {
            dependsOnGeneration()
        }
        // Inject generated sources into default source set of current compilation
        compilation.defaultSourceSet.kotlin.srcDir(outputDir.toFile())
    }
}
