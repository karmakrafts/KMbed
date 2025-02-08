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

import io.karma.mman.MemoryRegion
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

@OptIn(InternalKmbedApi::class)
@ExperimentalForeignApi
abstract class NativeResource internal constructor(data: UByteArray) : Resource, PinnedResource {
    @InternalKmbedApi
    override val ref: Pinned<UByteArray> = data.pin()

    val address: COpaquePointer
        get() = ref.addressOf(0)

    override fun unpackTo(path: Path, override: Boolean, bufferSize: Int): Boolean {
        if (SystemFileSystem.exists(path)) {
            if (!override) return false
            SystemFileSystem.delete(path)
        }
        return asSource().buffered().transferTo(MemoryRegion.sink(path, bufferSize)) == uncompressedSize
    }
}