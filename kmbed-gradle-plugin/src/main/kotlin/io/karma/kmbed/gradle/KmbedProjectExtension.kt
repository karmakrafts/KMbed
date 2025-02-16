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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import javax.inject.Inject

open class KmbedProjectExtension @Inject constructor( // @formatter:off
    objects: ObjectFactory,
    defaultGroup: String
) { // @formatter:on
    /**
     * May be used to override the default resource compression for all resources in this project.
     */
    var compression: Boolean = true

    /**
     * May be used to adjust the default deflate compression threshold.
     * Any resource data that's larger than the specified value in bytes will be automatically compressed
     * by the resource compiler.
     */
    var compressionThreshold: Int = 256

    /**
     * Allows adjusting the default per-module namespace used for generated resources.
     */
    var resourceNamespace: String = defaultGroup

    val kmbedSourceSets: NamedDomainObjectContainer<KmbedSourceSet> =
        objects.domainObjectContainer(KmbedSourceSet::class.java)

    /**
     * Configure single KMbed source sets or add custom ones using the NamedDomainObjectContainer DSL.
     *
     * @param action The closure to configure and create KMbed source sets.
     */
    inline fun kmbedSourceSets(action: NamedDomainObjectContainer<KmbedSourceSet>.() -> Unit) {
        kmbedSourceSets.action()
    }
}

/**
 * Retrieves the KMbed project extension for this project.
 * Throws an exception if the extension is not available.
 */
val Project.kmbedExtension: KmbedProjectExtension
    get() = requireNotNull(extensions.findByType(KmbedProjectExtension::class.java)) {
        "Could not find KMbed project extension in $name"
    }

/**
 * Adds KMbed source sets for all configured Kotlin targets.
 *
 * @param project The project to add all KMbed source sets for.
 */
fun NamedDomainObjectContainer<KmbedSourceSet>.defaultSourceSets(project: Project) {
    project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val extension = project.kmbedExtension
        for (target in project.kotlinMultiplatformExtension.targets) {
            if (target.platformType == KotlinPlatformType.common) continue
            for (compilation in target.compilations) {
                create("${compilation.target.name}${compilation.name.capitalized()}") {
                    it.compilation = compilation
                    it.compression = extension.compression
                    it.compressionThreshold = extension.compressionThreshold
                    it.resourceNamespace = extension.resourceNamespace
                }
            }
        }
    }
}