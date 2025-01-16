package io.karma.kmbed.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.deleteIfExists
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
            DeflaterOutputStream(it).use {
                it.write(path.readBytes())
            }
            it.toByteArray()
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateHeader(resourcePath: Path, resourceRoot: Path): File {
        val relativePath = resourcePath.relativeTo(resourceRoot)
        val fileName = relativePath.fileName.toString()
        val headerFileName = fileName.substringBeforeLast(".")
        val parentPath = relativePath.parent

        val headerBasePath = if (parentPath != null) headerDirectory.get().asFile.toPath() / parentPath
        else headerDirectory.get().asFile.toPath()

        val headerPath = headerBasePath / "$headerFileName.h"

        logger.info("Processing resource $resourcePath into $headerPath")

        // Generate a new C/C++ resource header which forces the data into the .data section of the binary
        val globalData = getGlobalData(resourcePath)
        val globalName = relativePath.toString().replace(fieldNameReplacePattern, "_")
        val header = ResourceHeader().apply {
            line("""__attribute__((section(".data"), visibility("default")))""")
            line("""static const char g_${globalName}[${globalData.size}] = {""")
            pushIndent()
            // @formatter:off
            globalData.joinToString(", ") { "0x${it.toHexString()}" }
                .chunkedOnNextSpace(100)
                .forEach(::line)
            // @formatter:on
            popIndent()
            line("}};")
        }

        // Write out the new header file and update the entry's hash in the cache
        headerPath.deleteIfExists()
        headerPath.outputStream(StandardOpenOption.CREATE).bufferedWriter().use {
            it.write(header.render())
            it.flush()
        }

        return headerPath.toFile()
    }
}