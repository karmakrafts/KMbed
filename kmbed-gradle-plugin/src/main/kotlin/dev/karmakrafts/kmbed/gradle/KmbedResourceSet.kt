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

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.Serializable
import javax.inject.Inject

open class KmbedResourceSet @Inject constructor( // @formatter:off
    private val name: String,
    @PublishedApi internal val objects: ObjectFactory
) : Named, Serializable { // @formatter:on
    val targetName: Property<String> = objects.property(String::class.java)
    val compilationName: Property<String> = objects.property(String::class.java)
    val generatedSourceDirectory: DirectoryProperty = objects.directoryProperty()
    val generatedResourceDirectory: DirectoryProperty = objects.directoryProperty()
    val namespace: Property<String> = objects.property(String::class.java)

    val extractDependencyResources: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val generateIndex: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val compression: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val compressionThreshold: Property<Long> =
        objects.property(Long::class.java).convention(KmbedResourceConfig.DEFAULT_COMPRESSION_THRESHOLD)
    val export: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val excludes: SetProperty<String> = objects.setProperty(String::class.java)
    val resources: MapProperty<String, KmbedResourceConfig> =
        objects.mapProperty(String::class.java, KmbedResourceConfig::class.java)

    override fun getName(): String = name

    @KmbedDsl
    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    @KmbedDsl
    fun resourcesFrom(other: KmbedResourceSet) {
        resources.putAll(other.resources)
    }

    @KmbedDsl
    inline fun resource(path: String, block: KmbedResourceConfig.() -> Unit) {
        resources.put(path, objects.newInstance(KmbedResourceConfig::class.java).apply(block))
    }
}