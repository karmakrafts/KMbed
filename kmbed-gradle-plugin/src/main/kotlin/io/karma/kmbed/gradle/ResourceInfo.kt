package io.karma.kmbed.gradle

/**
 * @author Alexander Hinze
 * @since 19/01/2025
 */
internal data class ResourceInfo(
    val internalName: String,
    val fqn: String,
    val size: Int,
    val uncompressedSize: Int
)