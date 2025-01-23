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

package io.karma.kmbed.runtime

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import platform.zlib.Z_FINISH
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit
import platform.zlib.z_stream
import kotlin.math.min

@ExperimentalForeignApi
internal class ZStreamingResource(
    override val path: String, val ref: Pinned<UByteArray>, override val uncompressedSize: Long
) : Resource, PinnedResource {
    override val isCompressed: Boolean = true

    override val address: COpaquePointer
        get() = ref.addressOf(0)

    override val size: Long
        get() = ref.get().size.toLong()

    @OptIn(UnsafeNumber::class)
    override fun asByteArray(): ByteArray = memScoped {
        require(uncompressedSize <= Int.MAX_VALUE) { "Resource $path is too big to fit inside a ByteArray" }
        return ByteArray(uncompressedSize.toInt()).apply {
            usePinned { pinnedArray ->
                val stream = alloc<z_stream>()
                require(inflateInit(stream.ptr) == Z_OK) { "Could not initialize decompression stream" }
                stream.apply {
                    avail_in = this@ZStreamingResource.size.toUInt()
                    next_in = address.reinterpret()
                    avail_out = uncompressedSize.toUInt()
                    next_out = pinnedArray.addressOf(0).reinterpret()
                }
                require(inflate(stream.ptr, Z_FINISH) == Z_STREAM_END) {
                    inflateEnd(stream.ptr)
                    "Could not decompress resource stream"
                }
                inflateEnd(stream.ptr)
            }
        }
    }

    override fun asSource(): RawSource = ZStreamingSource(this)

    override fun release() {
        ref.unpin()
    }
}

@ExperimentalForeignApi
internal class ZStreamingSource(
    private val resource: ZStreamingResource
) : RawSource {
    private val stream: z_stream = nativeHeap.alloc<z_stream> {
        require(inflateInit(ptr) == Z_OK) { "Could not initialize decompression stream" }
    }

    private var isClosed: Boolean = false
    private var read: Long = 0

    private inline val available: Long
        get() = resource.size - read

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (read == resource.uncompressedSize) return -1 // EOF
        return ByteArray(min(available, byteCount).toInt()).let {
            it.usePinned { pinnedBuffer ->
                stream.apply {
                    avail_in = available.toUInt()
                    next_in = resource.address.reinterpret()
                    avail_out = it.size.toUInt()
                    next_out = pinnedBuffer.addressOf(0).reinterpret()
                }
                inflate(stream.ptr, Z_NO_FLUSH)
            }
            val read = stream.avail_out.toInt()
            sink.write(it, 0, read)
            this@ZStreamingSource.read += read
            read.toLong()
        }
    }

    override fun close() {
        if (isClosed) return
        inflateEnd(stream.ptr)
        nativeHeap.free(stream)
        isClosed = true
    }
}