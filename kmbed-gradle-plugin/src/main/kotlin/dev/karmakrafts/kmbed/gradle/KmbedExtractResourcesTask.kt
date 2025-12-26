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
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.util.jar.JarFile

/**
 * A special task only used for web targets in order to extract embedded
 * resources from the KLIB file(s) correctly to make them accessible to fetch
 * from the generated resources root.
 */
abstract class KmbedExtractResourcesTask : DefaultTask() {
    @get:InputFiles
    abstract val artifactFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun invoke() {
        val files = artifactFiles.asSequence().filter { file -> file.extension == "klib" }
        val outputDir = outputDirectory.get()
        loop@ for (file in files) JarFile(file).use { jarFile ->
            val indexEntry = jarFile.getJarEntry("__kmbed_resources.json") ?: continue@loop
            val index = jarFile.getInputStream(indexEntry).use { stream ->
                json.decodeFromStream<KmbedResourceIndex>(stream)
            }
            check(index.version >= KmbedResourceIndex.VERSION) {
                "Resource index version in ${file.absolutePath} is incompatible with current kMbed version"
            }
            logger.info("Extracting resources from dependency ${file.absolutePath}")
            for (relativePath in index.resources) {
                val resourceEntry = jarFile.getJarEntry(relativePath)
                val targetPath = outputDir.dir(relativePath).asFile
                targetPath.ensureParentDirsCreated()
                jarFile.getInputStream(resourceEntry).use { inputStream ->
                    targetPath.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                logger.info("Extracted resource to ${targetPath.absolutePath}")
            }
        }
    }
}