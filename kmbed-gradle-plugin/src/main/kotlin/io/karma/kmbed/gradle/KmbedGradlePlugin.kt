package io.karma.kmbed.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.div

open class KmbedGradlePlugin @Inject constructor(
    private val providers: ProviderFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("kmbed", KmbedProjectExtension::class.java)
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlinExtension = requireNotNull(project.kotlinExtension as? KotlinMultiplatformExtension) {
                "KMbed requires Kotlin Multiplatform"
            }
            project.afterEvaluate {
                kotlinExtension.targets.forEach { target ->
                    target.compilations.filter { it.platformType == KotlinPlatformType.native }.forEach { compilation ->
                        // Register all required generation tasks for this compilation
                        val resourceSet = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }
                            .filter { it.exists() }
                        val compName = "${compilation.target.name}${compilation.name.capitalized()}"
                        val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / compName
                        outputDir.createDirectories()
                        val taskName = "generate${compName.capitalized()}KmbedSources"
                        val generateTask = project.tasks.register(
                            taskName, KmbedGenerateSourcesTask::class.java
                        ) { task ->
                            task.group = "kmbed"
                            task.resourceDirectories.setFrom(*resourceSet.toTypedArray())
                            task.sourceDirectory.set(outputDir.toFile())
                        }.get()
                        // Add dependency from compile task so sources get automatically regenerated on every build
                        project.tasks.getByName(compilation.compileKotlinTaskName) { task ->
                            task.dependsOn(generateTask)
                            task.mustRunAfter(generateTask)
                            // We depend on either source set, defaulting to main instead of test
                            // TODO: find a more robust solution to this
                            val commonName = if ("test" in task.name.lowercase()) "generateNativeTestKmbedSources"
                            else "generateNativeMainKmbedSources"
                            task.dependsOn(commonName)
                            task.mustRunAfter(commonName)
                        }
                        // Inject generated sources into default source set of current compilation
                        compilation.defaultSourceSet.kotlin.srcDir(outputDir.toFile())
                    }
                }
                // TODO: make this configurable through project extension eventually
                val generateNativeMainKmbedSources = project.tasks.register(
                    "generateNativeMainKmbedSources", KmbedGenerateCommonSourcesTask::class.java
                ) { task ->
                    task.group = "kmbed"
                    val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / "nativeMain"
                    task.sourceDirectory.set(outputDir.toFile())
                }.get()
                kotlinExtension.sourceSets.getByName("nativeMain").kotlin.srcDir(
                    generateNativeMainKmbedSources.sourceDirectory.asFile
                )

                val generateNativeTestKmbedSources = project.tasks.register(
                    "generateNativeTestKmbedSources", KmbedGenerateCommonSourcesTask::class.java
                ) { task ->
                    task.group = "kmbed"
                    val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / "nativeTest"
                    task.sourceDirectory.set(outputDir.toFile())
                }.get()
                kotlinExtension.sourceSets.getByName("nativeTest").kotlin.srcDir(
                    generateNativeTestKmbedSources.sourceDirectory.asFile
                )
            }
        }
    }
}
