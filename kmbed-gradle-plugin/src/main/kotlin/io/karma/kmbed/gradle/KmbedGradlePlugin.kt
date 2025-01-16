package io.karma.kmbed.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import javax.inject.Inject
import kotlin.io.path.div

class KmbedGradlePlugin @Inject constructor(
    private val providers: ProviderFactory
) : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("kmbed", KmbedProjectExtension::class.java)
        super.apply(target)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val compilation = kotlinCompilation as KotlinNativeCompilation
        val project = compilation.project
        val compName = compilation.compilationName.capitalized()
        // Register all required generation tasks for this compilation
        val generationTasks = ArrayList<KmbedGenerateHeadersTask>()
        for (sourceSet in compilation.kotlinSourceSets) {
            val resourceSet = sourceSet.resources
            val setName = resourceSet.name
            val taskName = "generate${compName}${setName}ResourceHeaders"
            generationTasks += project.tasks.register(taskName, KmbedGenerateHeadersTask::class.java) { task ->
                task.group = "ḱmbed"
                task.resourceDirectories.setFrom(*resourceSet.srcDirs.toTypedArray())
                task.headerDirectory.set(
                    (project.layout.buildDirectory.asFile.get()
                        .toPath() / "resourceHeaders" / compName / setName).toFile()
                )
            }.get() // Register immediately
        }
        // Inject a cinterop configuration for all generation task outputs
        //compilation.cinterops.create("kmbedResources") { interopConfig ->
        //    project.tasks.getByName(interopConfig.interopProcessingTaskName) { interopTask ->
        //        for (generationTask in generationTasks) {
        //            interopTask.dependsOn(generationTask)
        //            interopTask.mustRunAfter(generationTask)
        //        }
        //    }
        //    interopConfig.packageName = "io.karma.kmbed.generated"
        //    interopConfig.headers(*generationTasks.flatMap { it.listHeaderFiles() }.toTypedArray())
        //}
        return providers.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = BuildInfo.PLUGIN_ID
    override fun getPluginArtifact(): SubpluginArtifact = BuildInfo.PLUGIN_ARTIFACT

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.platformType == KotlinPlatformType.native
    }
}
