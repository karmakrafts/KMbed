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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import javax.inject.Inject

open class KmbedProjectExtension @Inject constructor( // @formatter:off
    objects: ObjectFactory,
    defaultNamespace: String
) { // @formatter:on
    companion object {
        private val webPlatformTypes: Set<KotlinPlatformType> = setOf(KotlinPlatformType.js, KotlinPlatformType.wasm)
    }

    // Global resource set overrides
    val compression: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val compressionThreshold: Property<Long> =
        objects.property(Long::class.java).convention(KmbedResourceConfig.DEFAULT_COMPRESSION_THRESHOLD)
    val export: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val namespace: Property<String> = objects.property(String::class.java).convention(defaultNamespace)
    val maxRecursionDepth: Property<Int> = objects.property(Int::class.java).convention(100)
    val excludes: SetProperty<String> = objects.setProperty(String::class.java)

    // Global options
    val generatedDirectory: DirectoryProperty = objects.directoryProperty() // This is initially set from the plugin

    val resourceSets: NamedDomainObjectContainer<KmbedResourceSet> =
        objects.domainObjectContainer(KmbedResourceSet::class.java)

    @KmbedDsl
    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    @KmbedDsl
    inline fun resourceSets(block: NamedDomainObjectContainer<KmbedResourceSet>.() -> Unit) {
        resourceSets.block()
    }

    internal fun addDefaultResourceSets(project: Project) {
        val srcDir = generatedDirectory.dir("src")
        val resourcesDir = generatedDirectory.dir("resources")
        for (target in project.kmpExtension.targets) {
            val platformType = target.platformType
            if (platformType == KotlinPlatformType.common) continue
            for (compilation in target.compilations) {
                resourceSets.create("${target.name}${compilation.name.capitalized()}") { set ->
                    set.targetName.set(target.targetName)
                    set.compilationName.set(compilation.compilationName)
                    set.namespace.set(namespace)
                    set.compression.set(compression)
                    set.compressionThreshold.set(compressionThreshold)
                    set.export.set(export)
                    set.excludes.set(excludes)
                    set.generatedSourceDirectory.set(srcDir.map { dir -> dir.dir(set.name) })
                    set.generatedResourceDirectory.set(resourcesDir.map { dir -> dir.dir(set.name) })
                    // Resources are only extracted for web targets by default
                    set.extractDependencyResources.set(platformType in webPlatformTypes)
                }
            }
        }
    }
}