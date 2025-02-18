import io.karma.kmbed.runtime.InternalKmbedApi
import io.karma.kmbed.runtime.ZStreamingSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

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

@OptIn(ExperimentalForeignApi::class, InternalKmbedApi::class)
class ZStreamingResourceTest {
    @Test
    fun `Read from empty buffer`() = memScoped {
        val memory = alloc<IntVar>()
        val buffer = Buffer()
        ZStreamingSource(memory.ptr, 0, 0).use {
            assertEquals(-1, it.readAtMostTo(buffer, 12))
            assertEquals(0, buffer.size)
        }
        ZStreamingSource(memory.ptr, 16, 0).use {
            assertEquals(-1, it.readAtMostTo(buffer, 12))
            assertEquals(0, buffer.size)
        }
        ZStreamingSource(memory.ptr, 0, 16).use {
            assertEquals(-1, it.readAtMostTo(buffer, 12))
            assertEquals(0, buffer.size)
        }
    }

    @Test
    fun `Read from populated buffer`() = memScoped {
        compressedData.asByteArray().usePinned { pinnedData ->
            ZStreamingSource(pinnedData.addressOf(0), compressedData.size.toLong(), compressedDataSize).use {
                val buffer = Buffer()
                assertEquals(compressedDataSize, it.readAtMostTo(buffer, compressedDataSize))
                println(buffer.readString())
            }
        }
    }
}