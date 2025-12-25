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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

/**
 * Generates a __kmbed_resources.json file for a specified resource set.
 * This may be used by the runtime or by the plugin downstream to extract resources
 * when consuming a dependency with kMbed resources.
 */
abstract class KmbedGenerateResourceIndexTask : DefaultTask() {
    @get:InputFiles
    abstract val inputDirectories: ConfigurableFileCollection

    @get:Input
    abstract val excludes: SetProperty<String>

    @get:Input
    abstract val maxRecursionDepth: Property<Int>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun invoke() {
        // @formatter:off
        val relativePaths = gatherResources(inputDirectories, excludes, maxRecursionDepth)
            .map { (rootPath, filePath) -> filePath.relativeTo(rootPath).toString() }
        // @formatter:on
        val index = KmbedResourceIndex(KmbedResourceIndex.VERSION, relativePaths)
        val outputDir = outputDirectory.get().asFile.toPath()
        val outputFile = outputDir / "__kmbed_resources.json"
        outputFile.deleteIfExists()
        outputFile.outputStream().use { stream ->
            json.encodeToStream(index, stream)
        }
    }
}