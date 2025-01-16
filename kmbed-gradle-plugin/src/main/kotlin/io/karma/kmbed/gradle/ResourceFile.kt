package io.karma.kmbed.gradle

import org.gradle.api.file.SourceDirectorySet
import java.nio.file.Path

/**
 * @author Alexander Hinze
 * @since 16/01/2025
 */
data class ResourceFile(
    val resourceRoot: Path,
    val path: Path
)
