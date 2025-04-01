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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.DeflaterOutputStream
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText

abstract class KmbedGenerateSourcesTask @Inject constructor(
    private val serviceProvider: Provider<KmbedBuildService>
) : DefaultTask() {
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
        val sourceDirectory = sourceDirectory.get().asFile.toPath()
        if (sourceDirectory.exists()) sourceDirectory.deleteRecursively()

        val executor = serviceProvider.get().executor
        val processorFunction = if (needsEmbedding) ::generateSources else ::findSources
        val resources = ConcurrentHashMap<Path, ResourceInfo>()
        val futures = ArrayList<CompletableFuture<Void>>()

        for (resourceDir in resourceDirectories) {
            val resourceRoot = resourceDir.toPath()
            logger.info("Processing resources in $resourceRoot")
            futures += resourceRoot.walk().map { path ->
                CompletableFuture.runAsync({
                    processorFunction(path, resourceRoot, resources)
                }, executor)
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        generateIndexSources(resources)
    }

    private fun getGlobalData(path: Path): Pair<ByteArray, Int> {
        val fileSize = path.fileSize()
        require(fileSize <= Int.MAX_VALUE) { "Files > 4GB are not supported right now" }
        logger.info("Read uncompressed resource $path with $fileSize bytes")
        if (!extension.compression || path.fileSize() < extension.compressionThreshold) {
            return Pair(path.readBytes(), fileSize.toInt())
        }
        return ByteArrayOutputStream().use { bos ->
            DeflaterOutputStream(bos).use { dos ->
                path.inputStream().use { inputStream ->
                    inputStream.transferTo(dos)
                }
            }
            val compressedData = bos.toByteArray()
            val compressedSize = compressedData.size
            logger.info("Compressed resource $path from $fileSize to $compressedSize bytes")
            Pair(compressedData, fileSize.toInt())
        }
    }

    private fun SourceBuilder.indexImports() {
        import("io.karma.kmbed.runtime.AbstractResources")
        import("io.karma.kmbed.runtime.GeneratedKmbedApi")
        import("io.karma.kmbed.runtime.InternalKmbedApi")
        import("io.karma.kmbed.runtime.StreamingResource")
        import("io.karma.kmbed.runtime.ZStreamingResource")
        when (platformType) {
            KotlinPlatformType.native -> {
                import("io.karma.kmbed.runtime.PinnedResource")
                import("kotlinx.cinterop.staticCFunction")
                import("kotlinx.cinterop.ExperimentalForeignApi")
                import("platform.posix.atexit")
            }

            KotlinPlatformType.jvm -> {
                import("java.lang.Runtime")
                import("java.lang.Thread")
            }

            else -> {}
        }
    }

    private fun SourceBuilder.indexCleanup() {
        when (platformType) {
            KotlinPlatformType.native -> {
                formatterOff()
                line("""atexit(staticCFunction<Unit> { Resources.cleanup() })""")
                formatterOn()
            }

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

    private fun SourceBuilder.indexOptIns() {
        optIn(
            when (platformType) {
                KotlinPlatformType.native -> listOf("InternalKmbedApi", "ExperimentalForeignApi")
                KotlinPlatformType.jvm, KotlinPlatformType.js -> listOf("InternalKmbedApi", "ExperimentalUnsignedTypes")
                else -> listOf("InternalKmbedApi")
            }
        )
    }

    private fun generateIndexSources(resources: ConcurrentHashMap<Path, ResourceInfo>) {
        val sourcePath = sourceDirectory.get().asFile.toPath() / "__kmbed_resources.kt"
        logger.info("Generating resource index into $sourcePath")

        val source = SourceBuilder().apply {
            defaultHeader()
            newline()

            pkg(extension.resourceNamespace)
            newline()

            indexImports()
            newline()

            indexOptIns()
            line("""@GeneratedKmbedApi""")
            line("""private object Resources : AbstractResources(""")
            indent {
                indexParameters()
            }
            line(""") {""")
            indent {
                line("""init {""")
                indent {
                    for ((path, resourceInfo) in resources) {
                        val (_, fqn, _, uncompressedSize) = resourceInfo
                        if (needsEmbedding) line("""add("$path", $fqn, $uncompressedSize)""")
                        else line("""add("$path", UByteArray(0), 0)""")
                    }
                    indexCleanup()
                }
                line("""}""")
            }
            line("""}""")
            newline()

            optIn(listOf("GeneratedKmbedApi"))
            line("""@Suppress("PropertyName")""")
            line("""public actual val Res: AbstractResources""")
            line("""    get() = Resources""")
        }

        // Write out the new source file and update the entry's hash in the cache
        sourcePath.deleteIfExists()
        sourcePath.parent?.createDirectories()
        sourcePath.writeText(source.render())
    }

    private fun findSources(resourcePath: Path, resourceRoot: Path, resources: ConcurrentHashMap<Path, ResourceInfo>) {
        val relativePath = resourcePath.relativeTo(resourceRoot)
        require(!resources.containsKey(relativePath)) { "Resource $resourcePath already exists" }
        resources[relativePath] = ResourceInfo(
            relativePath.toString().replace(fieldNameReplacePattern, "_"), "", 0, 0
        )
    }

    private fun SourceBuilder.sourceImports() {
        import("io.karma.kmbed.runtime.GeneratedKmbedApi")
    }

    private fun SourceBuilder.sourceOptIns() {
        optIn(
            when (platformType) {
                KotlinPlatformType.jvm, KotlinPlatformType.js -> listOf("ExperimentalUnsignedTypes")
                else -> emptyList()
            }
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateSources(
        resourcePath: Path, resourceRoot: Path, resources: ConcurrentHashMap<Path, ResourceInfo>
    ) {
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
        val fullFieldName = "__kmbed_$fieldName"
        val source = SourceBuilder().apply {
            defaultHeader()
            newline()

            pkg(packageName)
            newline()

            sourceImports()
            newline()

            sourceOptIns()
            line("""@GeneratedKmbedApi""")
            line("""public val $fullFieldName: UByteArray = ubyteArrayOf(""")
            indent {
                // @formatter:off
                fieldData.joinToString(", ") { "0x${it.toHexString().uppercase()}U" }
                    .chunkedOnNextSpace(100)
                    .forEach(::line)
                // @formatter:on
            }
            line(""")""")
        }

        // Write out the new source file and update the entry's hash in the cache
        sourcePath.deleteIfExists()
        sourcePath.parent?.createDirectories()
        sourcePath.writeText(source.render())

        require(!resources.containsKey(relativePath)) { "Resource $resourcePath already exists" }
        resources[relativePath] =
            ResourceInfo(fieldName, "$packageName.$fullFieldName", fieldData.size, uncompressedSize)
    }
}