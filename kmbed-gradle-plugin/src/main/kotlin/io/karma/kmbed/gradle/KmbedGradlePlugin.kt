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
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.div

open class KmbedGradlePlugin @Inject constructor(
    private val providers: ProviderFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val extension = project.extensions.create(
                "kmbed", KmbedProjectExtension::class.java, project.group.toString()
            )
            project.extensions.add("kmbedSourceSets", extension.kmbedSourceSets)
            project.afterEvaluate {
                // Add all defined source sets
                extension.kmbedSourceSets.forEach { sourceSet ->
                    val compilation = sourceSet.compilation
                    // Register all required generation tasks for this compilation
                    val resourceSet =
                        compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it.exists() }
                    val compName = "${compilation.target.name}${compilation.name.capitalized()}"
                    val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / compName
                    outputDir.createDirectories()
                    val taskName = "generate${compName.capitalized()}KmbedSources"
                    val generateTask = project.tasks.register(
                        taskName, KmbedGenerateSourcesTask::class.java
                    ) { task ->
                        task.group = "kmbed"
                        task.platformType = compilation.platformType
                        task.resourceDirectories.setFrom(*resourceSet.toTypedArray())
                        task.sourceDirectory.set(outputDir.toFile())
                    }.get()

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
                    project.tasks.getByName(compilation.compileKotlinTaskName) { task ->
                        task.dependsOnGeneration()
                    }
                    project.tasks.getByName("commonizeNativeDistribution") { task ->
                        task.dependsOnGeneration()
                    }
                    // Inject generated sources into default source set of current compilation
                    compilation.defaultSourceSet.kotlin.srcDir(outputDir.toFile())
                }
                val generateCommonMainKmbedSources = project.tasks.register(
                    "generateCommonMainKmbedSources", KmbedGenerateCommonSourcesTask::class.java
                ) { task ->
                    task.group = "kmbed"
                    val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / "commonMain"
                    task.sourceDirectory.set(outputDir.toFile())
                }.get()
                project.kotlinExtension.sourceSets.getByName("commonMain").kotlin.srcDir(
                    generateCommonMainKmbedSources.sourceDirectory.asFile
                )

                val generateCommonTestKmbedSources = project.tasks.register(
                    "generateCommonTestKmbedSources", KmbedGenerateCommonSourcesTask::class.java
                ) { task ->
                    task.group = "kmbed"
                    val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / "commonTest"
                    task.sourceDirectory.set(outputDir.toFile())
                }.get()
                project.kotlinExtension.sourceSets.getByName("commonTest").kotlin.srcDir(
                    generateCommonTestKmbedSources.sourceDirectory.asFile
                )
            }
        }
    }
}
