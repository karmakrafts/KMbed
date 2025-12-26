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

import com.squareup.kotlinpoet.FileSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

abstract class KmbedGenerateSourcesTask : DefaultTask() {
    @get:InputFiles
    abstract val inputDirectories: ConfigurableFileCollection

    @get:Input
    abstract val platformType: Property<KotlinPlatformType>

    @get:Input
    abstract val namespace: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun invoke() {
        FileSpec.builder(namespace.get(), "__kmbed_resources.kt").apply {
            when (val platformType = platformType.get()) {
                KotlinPlatformType.common -> generateForCommon()
                else -> generateForPlatform(platformType)
            }
        }.build().writeTo(outputDirectory.get().asFile)
    }

    private fun FileSpec.Builder.generateForCommon() {

    }

    private fun FileSpec.Builder.generateForPlatform(platform: KotlinPlatformType) {

    }
}