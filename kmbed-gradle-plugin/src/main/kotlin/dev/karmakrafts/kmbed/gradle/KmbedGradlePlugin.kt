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

                val cleanTasks = ArrayList<TaskProvider<KmbedCleanGeneratedTask>>()
                val generateIndexTasks = ArrayList<TaskProvider<KmbedGenerateResourceIndexTask>>()
                val extractTasks = ArrayList<TaskProvider<KmbedExtractResourcesTask>>()

                // Configure all resource sets
                for (resourceSet in extension.resourceSets) {
                    val compilation = resourceSet.getCompilation(project)
                    // Add the generated kMbed sources and resources to the associated source set's compilation
                    compilation.defaultSourceSet.apply {
                        kotlin.srcDir(resourceSet.generatedSourceDirectory)
                        resources.srcDir(resourceSet.generatedResourceDirectory)
                    }
                    // Register tasks required for this resource set and its associated compilation
                    val cleanTask = registerCleanTask(project, extension, resourceSet)
                    cleanTasks += cleanTask
                    generateIndexTasks += registerGenerateResourceIndexTask(project, extension, resourceSet)
                    if (resourceSet.extractDependencyResources.get()) {
                        val extractTask = registerExtractResourcesTask(project, extension, resourceSet) {
                            dependsOn(cleanTask)
                        }
                        extractTasks += extractTask
                        val processResourcesName =
                            if ("main" in compilation.name.lowercase()) "${resourceSet.targetName.get()}ProcessResources"
                            else "${resourceSet.fullTargetName}ProcessResources"
                        project.tasks.named(processResourcesName) { task -> task.dependsOn(extractTask) }
                        project.tasks.named(compilation.compileKotlinTaskName) { task -> task.dependsOn(extractTask) }
                        project.tasks.named("prepareKotlinIdeaImport") { task -> task.dependsOn(extractTask) }
                    }
                }

                // Register grouped parent tasks to run clean, extract etc.
                project.tasks.register("kmbedCleanGenerated") { task ->
                    task.dependsOn(*cleanTasks.toTypedArray())
                    task.group = TASK_GROUP
                    task.description = "Clean all generated files for all resource sets"
                }
                project.tasks.register("kmbedGenerateResourceIndices") { task ->
                    task.dependsOn(*generateIndexTasks.toTypedArray())
                    task.group = TASK_GROUP
                    task.description = "Generate resource indices for all resource sets"
                }
                project.tasks.register("kmbedExtractResources") { task ->
                    task.dependsOn(*extractTasks.toTypedArray())
                    task.group = TASK_GROUP
                    task.description = "Extract all dependency resources for all resource sets"
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
        return project.tasks.register( // @formatter:off
            "kmbedGenerateResourceIndex${name.capitalized()}",
            KmbedGenerateResourceIndexTask::class.java
        ) { task -> // @formatter:off
            task.group = TASK_GROUP
            task.description = "Generate a resource index JSON for all exported resources in the resulting artifact"
            // Only add task input if we want to export the current resource set, index should still be generated
            if(resourceSet.export.get()) task.inputDirectories.from(*resourceDirectories)
            task.maxRecursionDepth.set(extension.maxRecursionDepth)
            task.excludes.set(resourceSet.excludes.get() + resourceSet.compileExportExcludes())
            task.outputDirectory.set(resourceSet.generatedResourceDirectory) // Index gets generated into generated resource root
        }
    }

    private inline fun registerExtractResourcesTask(
        project: Project,
        extension: KmbedProjectExtension,
        resourceSet: KmbedResourceSet,
        crossinline initializer: KmbedExtractResourcesTask.() -> Unit
    ): TaskProvider<KmbedExtractResourcesTask> {
        val name = resourceSet.name
        val compilation = resourceSet.getCompilation(project)
        return project.tasks.register( // @formatter:off
            "kmbedExtractResources${name.capitalized()}",
            KmbedExtractResourcesTask::class.java
        ) { task -> // @formatter:on
            task.group = TASK_GROUP
            task.description = "Extract all dependency resources for the $name resource set"
            task.artifactFiles.from(project.configurations.named(compilation.compileDependencyConfigurationName))
            task.outputDirectory.set(resourceSet.generatedResourceDirectory) // Extracted resources are also copied to the generated resource root
            task.initializer()
        }
    }

    private fun registerCleanTask(
        project: Project, extension: KmbedProjectExtension, resourceSet: KmbedResourceSet
    ): TaskProvider<KmbedCleanGeneratedTask> {
        val name = resourceSet.name
        return project.tasks.register(
            "kmbedCleanGenerated${name.capitalized()}", KmbedCleanGeneratedTask::class.java
        ) { task ->
            task.group = TASK_GROUP
            task.description = "Clean all generated files for the $name resource set"
            task.sourceDirectory.set(resourceSet.generatedSourceDirectory)
            task.resourceDirectory.set(resourceSet.generatedResourceDirectory)
        }
    }
}