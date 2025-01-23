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
import io.karma.mman.PAGE_SIZE
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * An instance of this interface represents a singular resource embedded
 * into the final executable.
 * It may be used to interact with the immutable data of the resource,
 * including obtaining a stable pointer to it.
 */
interface Resource {
    /**
     * The path of this resource within its owning module.
     */
    val path: String

    /**
     * True if this resource has been compressed.
     */
    val isCompressed: Boolean

    /**
     * A non-null pointer to the raw data of this resource.
     */
    @ExperimentalForeignApi
    val address: COpaquePointer

    /**
     * The size of this resource within the executable in bytes.
     */
    val size: Long

    /**
     * The size of this resource before it was compressed in bytes.
     * This is the "real" size of the resource and should usually be used over [size].
     */
    val uncompressedSize: Long
        get() = size

    /**
     * Retrieve the raw data of this resource as a [ByteArray].
     *
     * @return The raw data of this resource copied into a new [ByteArray] instance.
     */
    fun asByteArray(): ByteArray

    /**
     * Creates a new streaming [RawSource] for this resource using the
     * address of its data.
     * This source will automatically handle decompression on the fly
     * if the resource was previously compressed.
     *
     * @return A new source which reads from the data of this resource via its address.
     */
    fun asSource(): RawSource

    /**
     * Copies (and decompresses if needed) this resource into a new file created
     * at the specified location using MMIO.
     *
     * @param path The path of the new file to be created.
     * @param override If true, the file at the specified location will be
     *  overwritten if it already exists.
     * @param bufferSize The size of the buffer used for copying the
     *  data into the newly created file. Change with care.
     * @return True if the resource was successfully unpacked to the specified location.
     */
    fun unpackTo(path: Path, override: Boolean = false, bufferSize: Int = PAGE_SIZE.toInt()): Boolean {
        if (SystemFileSystem.exists(path)) {
            if (!override) return false
            SystemFileSystem.delete(path)
        }
        return asSource().buffered().transferTo(MemoryRegion.sink(path, bufferSize)) == uncompressedSize
    }
}