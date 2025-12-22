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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import javax.inject.Inject

open class KmbedProjectExtension @Inject constructor( // @formatter:off
    objects: ObjectFactory,
    defaultNamespace: String
) { // @formatter:on
    val taskNamePrefix: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val compression: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val compressionThreshold: Property<Long> =
        objects.property(Long::class.java).convention(KmbedResourceConfig.DEFAULT_COMPRESSION_THRESHOLD)
    val export: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val generateIndex: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val namespace: Property<String> = objects.property(String::class.java).convention(defaultNamespace)
    val maxRecursionDepth: Property<Int> = objects.property(Int::class.java).convention(100)
    val commonSourceSetName: Property<String> = objects.property(String::class.java).convention("commonMain")
    val commonTestSourceSetName: Property<String> = objects.property(String::class.java).convention("commonTest")
    val excludes: SetProperty<String> = objects.setProperty(String::class.java)

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

    internal fun makeTaskName(name: String): Provider<String> {
        return taskNamePrefix.map { taskNamePrefix ->
            if (taskNamePrefix) "kmbed${name.capitalized()}"
            else name
        }
    }

    internal fun addDefaultResourceSets(project: Project) {
        project.pluginManager.withPlugin(KMP_PLUGIN_ID) {
            for (target in project.kmpExtension.targets) {
                if (target.platformType == KotlinPlatformType.common) continue
                for (compilation in target.compilations) {
                    resourceSets.create("${compilation.target.name}${compilation.name.capitalized()}") { set ->
                        set.compilationName.set(compilation.compilationName)
                        set.namespace.set(namespace)
                        set.compression.set(compression)
                        set.compressionThreshold.set(compressionThreshold)
                        set.export.set(export)
                        set.generateIndex.set(generateIndex)
                        set.excludes.addAll(excludes)
                    }
                }
            }
        }
    }
}