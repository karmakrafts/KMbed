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
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit
import platform.zlib.z_stream

@InternalKmbedApi
@OptIn(ExperimentalForeignApi::class)
class ZStreamingResource(
    override val path: String, data: UByteArray, override val uncompressedSize: Long
) : NativeResource(data) {
    override val isCompressed: Boolean = true

    @OptIn(InternalKmbedApi::class)
    override val size: Long
        get() = ref.get().size.toLong()

    @OptIn(UnsafeNumber::class)
    override fun asUByteArray(): UByteArray = memScoped {
        require(uncompressedSize <= Int.MAX_VALUE) { "Resource $path is too big to fit inside a ByteArray" }
        return UByteArray(uncompressedSize.toInt()).apply {
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

    override fun asSource(): RawSource = ZStreamingSource(address, size, uncompressedSize)
}

@InternalKmbedApi
@OptIn(ExperimentalForeignApi::class)
internal class ZStreamingSource(
    private val address: COpaquePointer,
    private val size: Long,
    private val uncompressedSize: Long,
) : RawSource {
    private val stream: z_stream = nativeHeap.alloc<z_stream> {
        require(inflateInit(ptr) == Z_OK) { "Could not initialize decompression stream" }
    }

    private var isClosed: Boolean = false
    private var read: Long = 0

    private inline val available: Long
        get() = size - read

    @OptIn(UnsafeNumber::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (size == 0L || uncompressedSize == 0L || read == uncompressedSize) return -1 // EOF
        return ByteArray(byteCount.toInt()).let {
            it.usePinned { pinnedBuffer ->
                stream.apply {
                    avail_in = available.toUInt()
                    next_in = interpretCPointer(address.rawValue)
                    avail_out = it.size.toUInt()
                    next_out = pinnedBuffer.addressOf(0).reinterpret()
                }
                inflate(stream.ptr, Z_FINISH)
            }
            val read = stream.total_out.toInt()
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