package io.karma.kmbed.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.apache.tools.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo

/**
 * @author Alexander Hinze
 * @since 07/11/2024
 */
abstract class KmbedGenerateHeadersTask : DefaultTask() {
    companion object {
        private val fieldNameReplacePattern: Regex = Regex("""[/\\=:.]""")
    }

    @get:InputFiles
    abstract val resourceDirectories: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val headerDirectory: DirectoryProperty

    internal fun listHeaderFiles(): List<File> {
        return headerDirectory.asFile.get().toPath().listDirectoryEntries("*.h").map { it.toFile() }
    }

    private val extension = project.extensions.getByType(KmbedProjectExtension::class.java)

    @TaskAction
    fun invoke() {
        for (resourceDir in resourceDirectories) {
            val resourceRoot = resourceDir.toPath()
            logger.info("Processing resources in $resourceRoot")
            for (path in resourceRoot.listDirectoryEntries()) {
                generateHeader(path, resourceRoot)
            }
        }
    }

    private fun getGlobalData(path: Path): ByteArray {
        // Zlib compression is only really effective for file sizes larger than ~256 bytes
        if (!extension.compression || path.fileSize() < 256) {
            return path.readBytes()
        }
        return ByteArrayOutputStream().use {
            ZipOutputStream(it).use {
                it.write(path.readBytes())
            }
            it.toByteArray()
        }
    }

    private fun generateHeader(resourcePath: Path, resourceRoot: Path): File {
        val relativePath = resourcePath.relativeTo(resourceRoot)
        val headerPath = headerDirectory.get().asFile.toPath() / relativePath

        logger.info("Processing resource $resourcePath")

        // Generate a new C/C++ resource header which forces the data into the .data section of the binary
        val globalData = getGlobalData(resourcePath)
        val globalName = relativePath.absolutePathString().replace(fieldNameReplacePattern, "_")
        val header = ResourceHeader()
        header.pushIndent()
        header.append(
            """
            __attribute__((section(".data"), visibility("default")))
            static const char g_${globalName}[${globalData.size}] = {${globalData.joinToString(", ")}};
        """.trimIndent()
        )
        header.popIndent()

        // Write out the new header file and update the entry's hash in the cache
        headerPath.outputStream(StandardOpenOption.TRUNCATE_EXISTING).use {
            it.bufferedWriter().write(header.render())
        }

        return headerPath.toFile()
    }
}