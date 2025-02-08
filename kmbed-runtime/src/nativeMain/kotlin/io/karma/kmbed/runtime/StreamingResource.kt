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

import io.karma.mman.RawMemorySource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.io.RawSource
import platform.posix.memcpy

@InternalKmbedApi
@OptIn(ExperimentalForeignApi::class)
class StreamingResource(
    override val path: String, data: UByteArray
) : NativeResource(data) {
    override val isCompressed: Boolean = false

    @OptIn(InternalKmbedApi::class)
    override val size: Long
        get() = ref.get().size.toLong()

    @OptIn(UnsafeNumber::class)
    override fun asByteArray(): ByteArray {
        require(size <= Int.MAX_VALUE) { "Resource $path is too big to fit inside a ByteArray" }
        return ByteArray(size.toInt()).apply {
            usePinned { pinnedArray ->
                memcpy(pinnedArray.addressOf(0), address, size.convert())
            }
        }
    }

    override fun asSource(): RawSource = RawMemorySource(address, size)
}