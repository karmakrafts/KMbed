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

import js.typedarrays.Uint8Array

@OptIn(ExperimentalUnsignedTypes::class)
@InternalKmbedApi
class ZStreamingResource(
    override val path: String, private val data: UByteArray, override val uncompressedSize: Long
) : Resource {
    override val isCompressed: Boolean = true
    override val size: Long = data.size.toLong()

    override fun asByteArray(): ByteArray {
        return pako.inflate(data.asByteArray().unsafeCast<Uint8Array<*>>()).toByteArray()
    }

    @ExperimentalUnsignedTypes
    override fun asUByteArray(): UByteArray = asByteArray().asUByteArray()
}