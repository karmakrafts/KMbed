package io.karma.kmbed.runtime

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit
import platform.zlib.uInt
import platform.zlib.voidpf
import platform.zlib.z_stream

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

    override fun asSource(bufferSize: Int): RawSource = ZStreamingSource(this, bufferSize)

    override fun release() {
        ref.unpin()
    }
}

@ExperimentalForeignApi
internal class ZStreamingSource(
    private val resource: ZStreamingResource, private val bufferSize: Int
) : RawSource {
    companion object {
        private fun zlibAlloc(base: voidpf?, size: uInt, alignment: uInt): voidpf? {
            require(alignment <= Int.MAX_VALUE.toUInt()) { "Alignment does not fit within signed 32-bit value" }
            return interpretCPointer(nativeHeap.alloc(size.toLong(), alignment.toInt()).rawPtr)
        }

        private fun zlibFree(base: voidpf?, address: voidpf?) {
            address?.let(nativeHeap::free)
        }
    }

    private val stream: z_stream = nativeHeap.alloc<z_stream> {
        zalloc = staticCFunction { base, size, alignment ->
            zlibAlloc(base, size, alignment)
        }
        zfree = staticCFunction { base, address ->
            zlibFree(base, address)
        }
    }.apply {
        require(inflateInit(ptr) == Z_OK) { "Could not initialize deflation stream" }
    }

    private var isClosed: Boolean = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        return 0
    }

    override fun close() {
        if (isClosed) return
        nativeHeap.free(stream)
        isClosed = true
    }
}