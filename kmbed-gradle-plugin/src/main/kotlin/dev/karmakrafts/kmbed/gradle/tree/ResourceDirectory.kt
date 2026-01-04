/*
 * Copyright 2026 Karma Krafts
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

package dev.karmakrafts.kmbed.gradle.tree

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import dev.karmakrafts.kmbed.gradle.getFriendlyName
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@ConsistentCopyVisibility
internal data class ResourceDirectory private constructor( // @formatter:off
    override val rootPath: Path,
    override val path: Path,
) : Resource { // @formatter:on
    companion object {
        const val DEFAULT_DEPTH: Int = 256

        private fun collect( // @formatter:off
            parent: ResourceDirectory?,
            path: Path,
            rootPath: Path,
            maxDepth: Int,
            depth: Int,
            excludeFilter: (Path) -> Boolean
        ): ResourceDirectory { // @formatter:on
            require(path.isDirectory()) { "Invalid path for ResourceDirectory: $path is not a directory" }
            require(depth < maxDepth) { "Reached maximum recursion depth of $maxDepth" }
            return ResourceDirectory(rootPath, path).apply directory@{
                this.parent = parent
                // @formatter:off
                children = path.listDirectoryEntries()
                    .filterNot(excludeFilter)
                    .associateWith { entryPath -> when {
                        entryPath.isDirectory() -> collect(this, entryPath, rootPath, maxDepth, depth + 1, excludeFilter)
                        else -> ResourceFile(rootPath, entryPath).apply {
                            this.parent = this@directory
                        }
                    } }
                    .toMutableMap()
                // @formatter:on
                groupFilesAsNeeded()
            }
        }

        fun collect(
            path: Path, rootPath: Path = path, maxDepth: Int = DEFAULT_DEPTH, excludes: List<String> = emptyList()
        ): ResourceDirectory {
            val excludeFilter = excludes.map<_, Function1<Path, Boolean>> { glob ->
                FileSystems.getDefault().getPathMatcher("glob:$glob")::matches
            }.fold<_, Function1<Path, Boolean>>({ false }) { acc, fn -> { p -> acc(p) || fn(p) } }
            return collect(null, path, rootPath, maxDepth, 0, excludeFilter)
        }
    }

    var children: Map<Path, Resource> = emptyMap()
    override var parent: ResourceDirectory? = null

    init {
        require(rootPath.isDirectory()) { "Invalid root directory for ResourceDirectory: $rootPath is not a directory" }
        require(path.isDirectory()) { "Invalid path for ResourceDirectory: $path is not a directory" }
    }

    fun generateRoot( // @formatter:off
        fileBuilder: FileSpec.Builder,
        typeBuilder: TypeSpec.Builder,
        isCommon: Boolean,
        isActual: (Path) -> Boolean
    ) { // @formatter:on
        for ((_, child) in children) child.generate(fileBuilder, typeBuilder, isCommon, isActual)
    }

    override fun generate( // @formatter:off
        fileBuilder: FileSpec.Builder,
        typeBuilder: TypeSpec.Builder,
        isCommon: Boolean,
        isActual: (Path) -> Boolean
    ) { // @formatter:on
        val modifiers = EnumSet.of(KModifier.PUBLIC)
        if (isActual(path)) modifiers += KModifier.ACTUAL
        val childTypeBuilder = TypeSpec.objectBuilder(getFriendlyName(path.name)).addModifiers(modifiers)
        for ((_, child) in children) child.generate(fileBuilder, childTypeBuilder, isCommon, isActual)
        typeBuilder.addType(childTypeBuilder.build())
    }

    override operator fun contains(path: Path): Boolean {
        if (path == this.path) return true
        for ((_, child) in children) {
            if (path !in child) continue
            return true
        }
        return false
    }

    private fun hasAlternatives(path: Path): Boolean = children.keys.filterNot(Path::isDirectory)
        .any { filePath -> filePath != path && filePath.nameWithoutExtension == path.nameWithoutExtension }

    /**
     * If this directory contains the files `test.txt` and `test.png`,
     * this function will replace both of the entries with a single
     * instance of [ResourceFileGroup] with its [ResourceFileGroup.extensions]
     * set to `["txt", "png"]`.
     * This is applied recursively to all directories which are part of this directory.
     */
    private fun groupFilesAsNeeded() {
        val newChildren = HashMap<Path, Resource>()
        val groupedNames = HashSet<String>()

        for ((path, child) in children) {
            val name = path.nameWithoutExtension
            when (child) {
                is ResourceDirectory -> {
                    child.groupFilesAsNeeded()
                    newChildren[path] = child
                }

                else if hasAlternatives(path) && name !in groupedNames -> {
                    val parent = path.parent ?: Path("")
                    val pathWithoutExtension = parent / path.nameWithoutExtension
                    val extensions = children.keys.filterNot(Path::isDirectory).map(Path::extension).toSet().toList()
                    newChildren[pathWithoutExtension] =
                        ResourceFileGroup(child.rootPath, pathWithoutExtension, extensions).apply {
                            this.parent = this@ResourceDirectory
                        }
                    groupedNames += name
                }

                else -> newChildren[path] = child
            }
        }

        children = children.filter { (_, child) -> child is ResourceDirectory } + newChildren
    }

    override fun toString(): String {
        var result = "ResourceDirectory(path=$path)"
        for ((_, child) in children) {
            result += "\n\t$child"
        }
        return result
    }
}