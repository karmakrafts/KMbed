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
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.div

open class KmbedGradlePlugin @Inject constructor(
    private val providers: ProviderFactory
) : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("kmbed", KmbedProjectExtension::class.java)
        super.apply(target)
    }

    @OptIn(ExperimentalPathApi::class)
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val compilation = kotlinCompilation as KotlinNativeCompilation
        val project = compilation.project
        // Register all required generation tasks for this compilation
        val resourceSet = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it.exists() }
        val compName = "${compilation.target.name}${compilation.name.capitalized()}"
        val outputDir = project.layout.buildDirectory.asFile.get().toPath() / "kmbedSources" / compName
        outputDir.createDirectories()
        val taskName = "generate${compName.capitalized()}KmbedSources"
        val generateTask = project.tasks.register(taskName, KmbedGenerateSourcesTask::class.java) { task ->
            task.group = "kmbed"
            task.resourceDirectories.setFrom(*resourceSet.toTypedArray())
            task.sourceDirectory.set(outputDir.toFile())
        }.get()
        // Add dependency to compile task so sources get automatically regenerated on every build
        project.tasks.getByName(compilation.compileKotlinTaskName) { task ->
            task.dependsOn(generateTask)
            task.mustRunAfter(generateTask)
        }
        // Inject generated sources into default source set of current compilation
        compilation.defaultSourceSet.kotlin.srcDir(outputDir.toFile())
        return providers.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = BuildInfo.PLUGIN_ID
    override fun getPluginArtifact(): SubpluginArtifact = BuildInfo.PLUGIN_ARTIFACT

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.platformType == KotlinPlatformType.native
    }
}
