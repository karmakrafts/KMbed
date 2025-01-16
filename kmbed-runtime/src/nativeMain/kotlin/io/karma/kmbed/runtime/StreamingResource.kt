package io.karma.kmbed.runtime

import io.karma.mman.RawMemorySource
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.io.RawSource
import platform.posix.memcpy

@ExperimentalForeignApi
internal class StreamingResource(
    override val path: String, override val address: COpaquePointer, override val size: Long
) : Resource {
    override val isCompressed: Boolean = false

    @OptIn(UnsafeNumber::class)
    override fun asByteArray(): ByteArray {
        require(size <= Int.MAX_VALUE) { "Resource $path is too big to fit inside a ByteArray" }
        return ByteArray(size.toInt()).apply {
            usePinned { pinnedArray ->
                memcpy(pinnedArray.addressOf(0), address, size.convert())
            }
        }
    }

    override fun asSource(bufferSize: Int): RawSource = RawMemorySource(address, size, bufferSize)
}