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

package dev.karmakrafts.kmbed.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized

@Suppress("UNUSED") // This is constructed/invoked by Gradle dynamically
open class KmbedGradlePlugin : Plugin<Project> {
    companion object {
        private const val TASK_GROUP: String = "kmbed"
    }

    private fun Logger.printHeader() = info(
        """
                
            8  dP 8b   d8 8             8
            8wdP  8YbmdP8 88b. .d88b .d88  Resource Compiler
            88Yb  8  "  8 8  8 8.dP' 8  8  Version ${BuildInfo.VERSION}
            8  Yb 8     8 88P' `Y88P `Y88
            
        """.trimIndent()
    )

    override fun apply(project: Project) {
        val logger = project.logger
        logger.printHeader()
        val pluginManager = project.pluginManager
        // Always ensure that KGP is present for multiplatform
        check(pluginManager.hasPlugin(KMP_PLUGIN_ID)) {
            "Kotlin Multiplatform plugin must be present in order to use kMbed"
        }
        pluginManager.withPlugin(KMP_PLUGIN_ID) {
            val defaultNamespace = project.group.toString()
            val extension = project.extensions.create("kmbed", KmbedProjectExtension::class.java, defaultNamespace)
            extension.generatedDirectory.set(project.layout.buildDirectory.dir("kmbed"))
            project.afterEvaluate {
                extension.addDefaultResourceSets(project)
                for (resourceSet in extension.resourceSets) {
                    registerTasksForResourceSet(project, extension, resourceSet)
                }
            }
        }
    }

    private fun registerListResourcesTask(
        project: Project, extension: KmbedProjectExtension, resourceSet: KmbedResourceSet
    ): TaskProvider<KmbedListResourcesTask> {
        val name = resourceSet.name
        val compilationName = resourceSet.compilationName.get()
        val targetName = resourceSet.targetName.get()
        // @formatter:off
        val compilation = project.kmpExtension.targets
            .first { target -> target.targetName == targetName }
            .compilations
            .first { compilation -> compilation.compilationName == compilationName }
        val resourceDirectories = compilation.allKotlinSourceSets
            .flatMap { sourceSet -> sourceSet.resources.srcDirs }
            .toTypedArray()
        // @formatter:on

        // Register task to find all resources for the compilation associated with the given resource set
        return project.tasks.register(
            extension.makeTaskName("listResources${name.capitalized()}").get(), KmbedListResourcesTask::class.java
        ) { task ->
            task.group = TASK_GROUP
            task.description = "Index all resources for the $name resource set"
            task.maxRecursionDepth.set(extension.maxRecursionDepth)
            task.inputDirectories.from(*resourceDirectories)
            task.excludes.addAll(resourceSet.excludes)
        }
    }

    private fun registerGenerateResourceIndexTask(
        project: Project,
        extension: KmbedProjectExtension,
        resourceSet: KmbedResourceSet,
        listTask: TaskProvider<KmbedListResourcesTask>
    ): TaskProvider<KmbedGenerateResourceIndexTask> {
        val name = resourceSet.name
        return project.tasks.register(
            extension.makeTaskName("generateResourceIndex${name.capitalized()}").get(),
            KmbedGenerateResourceIndexTask::class.java
        ) { task ->
            task.dependsOn(listTask)
            task.group = TASK_GROUP
            task.description = "Generate a resource index JSON for all exported resources in the resulting artifact"
            task.resources.from(listTask.map { listTask -> listTask.outputResources })
            task.outputDirectory.set(resourceSet.generatedResourceDirectory) // Index gets generated into generated resource root
            // TODO: implement proper resource root
        }
    }

    private fun registerExtractResourcesTask(
        project: Project, extension: KmbedProjectExtension, resourceSet: KmbedResourceSet
    ): TaskProvider<KmbedExtractResourcesTask> {
        val name = resourceSet.name
        // Register task to extract all resources from incoming dependencies to make them accessible on applicable targets
        return project.tasks.register(
            extension.makeTaskName("extractResources${name.capitalized()}").get(), KmbedExtractResourcesTask::class.java
        ) { task ->
            task.group = TASK_GROUP
            task.description = "Extract all dependency resources for the $name resource set"
            task.outputDirectory.set(resourceSet.generatedResourceDirectory) // Extracted resources are also copied to the generated resource root
        }
    }

    private fun registerTasksForResourceSet(
        project: Project, extension: KmbedProjectExtension, resourceSet: KmbedResourceSet
    ) {
        val listTask = registerListResourcesTask(project, extension, resourceSet)
        val generateIndexTask = registerGenerateResourceIndexTask(project, extension, resourceSet, listTask)
        if (!resourceSet.extractDependencyResources.get()) return // Early return if we don't need resource extraction
        registerExtractResourcesTask(project, extension, resourceSet)
    }
}