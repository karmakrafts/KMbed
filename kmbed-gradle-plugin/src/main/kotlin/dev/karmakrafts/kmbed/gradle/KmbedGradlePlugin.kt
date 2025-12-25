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
                
             __     _______ __             __  
            |  |--.|   |   |  |--.-----.--|  |  Resource Compiler
            |    < |       |  _  |  -__|  _  |  Version ${BuildInfo.VERSION}
            |__|__||__|_|__|_____|_____|_____| 
            
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
                    val compilation = resourceSet.getCompilation(project)
                    // Add the generated kMbed sources and resources to the associated source set's compilation
                    compilation.defaultSourceSet.apply {
                        kotlin.srcDir(resourceSet.generatedSourceDirectory)
                        resources.srcDir(resourceSet.generatedResourceDirectory)
                    }
                    registerTasksForResourceSet(project, extension, resourceSet)
                }
            }
        }
    }

    private fun registerGenerateResourceIndexTask(
        project: Project,
        extension: KmbedProjectExtension,
        resourceSet: KmbedResourceSet,
    ): TaskProvider<KmbedGenerateResourceIndexTask> {
        val name = resourceSet.name
        val compilation = resourceSet.getCompilation(project)
        // @formatter:off
        val resourceDirectories = compilation.allKotlinSourceSets
            .flatMap { sourceSet -> sourceSet.resources.srcDirs }
            .toTypedArray()
        // @formatter:on
        return project.tasks.register(
            extension.makeTaskName("generateResourceIndex${name.capitalized()}").get(),
            KmbedGenerateResourceIndexTask::class.java
        ) { task ->
            task.group = TASK_GROUP
            task.description = "Generate a resource index JSON for all exported resources in the resulting artifact"
            task.inputDirectories.from(*resourceDirectories)
            task.maxRecursionDepth.set(extension.maxRecursionDepth)
            task.excludes.set(resourceSet.excludes)
            task.outputDirectory.set(resourceSet.generatedResourceDirectory) // Index gets generated into generated resource root
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
        val generateIndexTask = registerGenerateResourceIndexTask(project, extension, resourceSet)
        if (!resourceSet.extractDependencyResources.get()) return // Early return if we don't need resource extraction
        registerExtractResourcesTask(project, extension, resourceSet)
    }
}