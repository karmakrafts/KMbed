package io.karma.kmbed.gradle

/**
 * @author Alexander Hinze
 * @since 16/01/2025
 */
open class KmbedProjectExtension(
    defaultGroup: String
) {
    /**
     * May be used to force-disable resource compression for all resources in this project.
     */
    var compression: Boolean = true

    /**
     * May be used to adjust the default deflate compression threshold.
     * Any resource data that's larger than the specified value in bytes will be automatically compressed
     * by the resource compiler.
     */
    var compressionThreshold: Int = 256

    /**
     * Allows adjusting the per-module namespace used for generated resources.
     */
    var resourceNamespace: String = defaultGroup
}