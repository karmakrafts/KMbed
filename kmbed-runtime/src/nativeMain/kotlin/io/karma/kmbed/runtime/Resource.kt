package io.karma.kmbed.runtime

import io.karma.mman.MemoryRegion
import io.karma.mman.PAGE_SIZE
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

interface Resource {
    val path: String

    val isCompressed: Boolean

    @ExperimentalForeignApi
    val address: COpaquePointer

    val size: Long

    val uncompressedSize: Long
        get() = size

    fun asByteArray(): ByteArray

    fun asSource(): RawSource

    fun unpackTo(path: Path, override: Boolean = false, bufferSize: Int = PAGE_SIZE.toInt()): Boolean {
        if (SystemFileSystem.exists(path)) {
            if (!override) return false
            SystemFileSystem.delete(path)
        }
        return asSource().buffered().transferTo(MemoryRegion.sink(path, bufferSize)) == uncompressedSize
    }
}