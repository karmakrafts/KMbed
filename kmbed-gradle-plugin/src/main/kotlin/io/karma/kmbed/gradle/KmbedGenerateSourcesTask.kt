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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText

abstract class KmbedGenerateSourcesTask : DefaultTask() {
    companion object {
        private val fieldNameReplacePattern: Regex = Regex("""[/\\=:.\s]""")
    }

    @get:Input
    abstract var platformType: KotlinPlatformType

    @get:InputFiles
    abstract val resourceDirectories: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val sourceDirectory: DirectoryProperty

    private val extension = project.extensions.getByType(KmbedProjectExtension::class.java)

    private val needsEmbedding: Boolean by lazy {
        when (platformType) {
            KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> false
            else -> true
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @TaskAction
    fun invoke() {
        val resources = HashMap<Path, ResourceInfo>()
        for (resourceDir in resourceDirectories) {
            val resourceRoot = resourceDir.toPath()
            logger.info("Processing resources in $resourceRoot")
            for (path in resourceRoot.walk()) {
                if (needsEmbedding) generateSources(path, resourceRoot, resources)
                else findSources(path, resourceRoot, resources)
            }
        }
        generateIndexSources(resources)
    }

    private fun getGlobalData(path: Path): Pair<ByteArray, Int> {
        if (!extension.compression || path.fileSize() < extension.compressionThreshold) {
            val data = path.readBytes()
            logger.info("Using uncompressed resource $path with ${data.size} bytes")
            return Pair(data, data.size)
        }
        val data = path.readBytes()
        return ByteArrayOutputStream().use { bos ->
            DeflaterOutputStream(bos).use { dos ->
                dos.write(data)
            }
            val compressedData = bos.toByteArray()
            val size = data.size
            val compressedSize = compressedData.size
            val percentage = ((size.toDouble() - compressedSize.toDouble()) / size.toDouble()) * 100.0
            logger.info("Compressed resource $path from $size to $compressedSize bytes ($percentage%)")
            Pair(compressedData, size)
        }
    }

    private fun String.chunkedOnNextSpace(length: Int): List<String> {
        val words = split(" ")
        val lines = ArrayList<String>()
        var currentLine = StringBuilder()
        for (word in words) {
            // If adding the word exceeds the line length, start a new line
            if (currentLine.length + word.length + (if (currentLine.isNotEmpty()) 1 else 0) > length) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
                continue
            }
            // If it fits, add the word to the current line
            if (currentLine.isNotEmpty()) {
                currentLine.append(" ")
            }
            currentLine.append(word)
        }
        // Add the last line if it's not empty
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun SourceBuilder.indexImports() {
        line("""import io.karma.kmbed.runtime.AbstractResources""")
        line("""import io.karma.kmbed.runtime.GeneratedKmbedApi""")
        line("""import io.karma.kmbed.runtime.InternalKmbedApi""")
        when (platformType) {
            KotlinPlatformType.native -> {
                line("""import io.karma.kmbed.runtime.PinnedResource""")
                line("""import kotlinx.cinterop.staticCFunction""")
                line("""import platform.posix.atexit""")
            }

            KotlinPlatformType.jvm -> {
                line("""import java.lang.Runtime""")
                line("""import java.lang.Thread""")
            }

            else -> {}
        }
    }

    private fun SourceBuilder.indexCleanup() {
        when (platformType) {
            KotlinPlatformType.native -> line("""atexit(staticCFunction<Unit> { Resources.cleanup() })""")
            KotlinPlatformType.jvm -> line("""Runtime.getRuntime().addShutdownHook(Thread(Resources::cleanup))""")
            else -> {}
        }
    }

    private fun SourceBuilder.indexParameters() {
        line("""::StreamingResource,""")
        line("""::ZStreamingResource,""")
        when (platformType) {
            KotlinPlatformType.native -> line("""{ (this as? PinnedResource)?.ref?.unpin() }""")
            else -> line("""{}""")
        }
    }

    private fun generateIndexSources(resources: HashMap<Path, ResourceInfo>) {
        val sourcePath = sourceDirectory.get().asFile.toPath() / "__kmbed_resources.kt"
        logger.info("Generating resource index into $sourcePath")

        val source = SourceBuilder().apply {
            defaultHeader()
            newline()

            line("""package ${extension.resourceNamespace}""")

            newline()

            indexImports()
            newline()

            line("""@OptIn(InternalKmbedApi::class)""")
            line("""@GeneratedKmbedApi""")
            line("""private object Resources : AbstractResources(""")
            pushIndent()
            indexParameters()
            popIndent()
            line(""") {""")
            pushIndent()
            line("""init {""")
            pushIndent()
            for ((path, resourceInfo) in resources) {
                val (_, fqn, _, uncompressedSize) = resourceInfo
                if (needsEmbedding) line("""add("$path", $fqn, $uncompressedSize)""")
                else line("""add("$path", "", 0)""")
            }
            line("""// @formatter:off""")
            indexCleanup()
            line("""// @formatter:on""")
            popIndent()
            line("""}""")
            popIndent()
            line("""}""")

            newline()

            line("""@OptIn(GeneratedKmbedApi::class)""")
            line("""@Suppress("PropertyName")""")
            line("""actual val Resources: AbstractResources""")
            line("""    get() = Resources""")
        }

        // Write out the new source file and update the entry's hash in the cache
        sourcePath.deleteIfExists()
        sourcePath.parent?.createDirectories()
        sourcePath.outputStream(StandardOpenOption.CREATE).bufferedWriter().use {
            it.write(source.render())
            it.flush()
        }
    }

    private fun findSources(resourcePath: Path, resourceRoot: Path, resources: HashMap<Path, ResourceInfo>) {
        val relativePath = resourcePath.relativeTo(resourceRoot)
        relativePath.apply {
            require(this !in resources) { "Resource $resourcePath already exists" }
            resources[this] = ResourceInfo(
                relativePath.toString().replace(fieldNameReplacePattern, "_"), "", 0, 0
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateSources(resourcePath: Path, resourceRoot: Path, resources: HashMap<Path, ResourceInfo>) {
        val relativePath = resourcePath.relativeTo(resourceRoot)
        val fileName = relativePath.fileName.toString()
        val sourceFileName = fileName.substringBeforeLast(".")
        val parentPath = relativePath.parent

        val packageName = parentPath?.let {
            "${extension.resourceNamespace}.rdata.${
                it.toString().lowercase().replace(File.separator, ".")
            }"
        } ?: "${extension.resourceNamespace}.rdata"

        val sourceDir = sourceDirectory.get().asFile.toPath()
        val sourceBasePath = parentPath?.let { sourceDir / it } ?: sourceDir
        val sourcePath = sourceBasePath / "$sourceFileName.kt"

        logger.info("Processing resource $resourcePath into $sourcePath")

        // Generate a new Kotlin source file
        val (fieldData, uncompressedSize) = getGlobalData(resourcePath)
        val fieldName = relativePath.toString().replace(fieldNameReplacePattern, "_")
        val source = SourceBuilder().apply {
            defaultHeader()
            newline()
            line("""package $packageName""")
            newline()
            line("""import io.karma.kmbed.runtime.GeneratedKmbedApi""")
            newline()
            line("""@GeneratedKmbedApi""")
            line("""val __kmbed_$fieldName: UByteArray = ubyteArrayOf(""")
            pushIndent()
            // @formatter:off
            fieldData.joinToString(", ") { "0x${it.toHexString().uppercase()}U" }
                .chunkedOnNextSpace(100)
                .forEach(::line)
            // @formatter:on
            popIndent()
            line(""")""")
        }

        // Write out the new source file and update the entry's hash in the cache
        sourcePath.deleteIfExists()
        sourcePath.parent?.createDirectories()
        sourcePath.writeText(source.render())

        relativePath.apply {
            require(this !in resources) { "Resource $resourcePath already exists" }
            resources[this] = ResourceInfo(
                fieldName, "$packageName.__kmbed_$fieldName", fieldData.size, uncompressedSize
            )
        }
    }
}