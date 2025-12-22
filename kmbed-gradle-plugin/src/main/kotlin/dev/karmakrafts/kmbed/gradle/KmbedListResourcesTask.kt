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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.streams.asSequence

/**
 * A task which gathers all resources in the specified directory,
 * applying the given exclude patterns to filter out unwanted files.
 */
abstract class KmbedListResourcesTask @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask() {
    @get:InputFiles
    abstract val directories: ConfigurableFileCollection

    @get:Input
    abstract val excludes: SetProperty<String>

    @get:Input
    abstract var maxRecursionDepth: Property<Int>

    @get:OutputFiles
    val resources: FileCollection
        field: ConfigurableFileCollection = objectFactory.fileCollection()

    private fun gatherResources(file: File, excludeFilter: (Path) -> Boolean): List<File> { // @formatter:off
        return Files.walk(file.toPath(), maxRecursionDepth.get())
            .asSequence()
            .filterNot(excludeFilter)
            .map { path -> path.toFile() }
            .toList()
    } // @formatter:on

    @TaskAction
    fun invoke() {
        // @formatter:off
        val excludeFilter: (Path) -> Boolean = excludes.get()
            .map { pattern -> FileSystems.getDefault().getPathMatcher("glob:$pattern") }
            .map<_, Function1<Path, Boolean>> { matcher -> matcher::matches }
            .reduce { acc, fn -> { path -> acc(path) || fn(path) } }
        // @formatter:on
        resources.from(*directories.flatMap { file -> gatherResources(file, excludeFilter) }.toTypedArray())
    }
}