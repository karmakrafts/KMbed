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

import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.streams.asSequence

@PublishedApi
internal const val KMP_PLUGIN_ID: String = "org.jetbrains.kotlin.multiplatform"

internal val json: Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    prettyPrintIndent = "\t"
    allowComments = true
    allowTrailingComma = true
}

internal fun gatherResources(
    inputDirectories: List<File>, excludes: Set<String>, maxRecursionDepth: Int
): List<Pair<Path, Path>> {
    val fileSystem = FileSystems.getDefault()
    // @formatter:off
    val excludeFilter: (Path) -> Boolean = excludes
        .map<_, Function1<Path, Boolean>> { pattern -> fileSystem.getPathMatcher("glob:$pattern")::matches }
        .fold({ false }) { acc, fn -> { path -> acc(path) || fn(path) } }
    return inputDirectories.flatMap { dir ->
        val path = dir.toPath()
        if (!path.exists()) return@flatMap emptyList()
        Files.walk(path, maxRecursionDepth)
            .asSequence()
            .filterNot(Path::isDirectory)
            .filterNot(excludeFilter)
            .map { filePath -> path to filePath }
            .toList()
    }
}