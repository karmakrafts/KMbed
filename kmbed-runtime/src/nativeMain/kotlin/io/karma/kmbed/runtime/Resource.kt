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
import kotlinx.cinterop.convert
import platform.posix.memcpy

/**
 * The base address of this resource within the program image.
 * **Do not attempt to write to this address!**
 */
@ExperimentalForeignApi
inline val Resource.address: COpaquePointer
    get() = (this as NativeResource).address

/**
 * Copy the data of this resource to the given address.
 * This assumes that the destination has at least [Resource.uncompressedSize] bytes
 * of available space to copy to.
 *
 * @param address The address to copy the resource data to.
 */
@OptIn(UnsafeNumber::class)
@ExperimentalForeignApi
fun Resource.copyTo(address: COpaquePointer) {
    memcpy(address, this.address, uncompressedSize.convert())
}