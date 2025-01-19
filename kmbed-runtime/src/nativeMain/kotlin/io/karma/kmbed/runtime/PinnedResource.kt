package io.karma.kmbed.runtime

/**
 * @author Alexander Hinze
 * @since 19/01/2025
 */
internal interface PinnedResource {
    fun release()
}