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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pin

/**
 * Provides an API for querying resources from a given module.
 * This is implemented by the generated code to initialize and register all
 * resources for the current module/target.
 */
@OptIn(ExperimentalForeignApi::class)
abstract class AbstractResources {
    internal val resources: HashMap<String, Resource> = HashMap()

    @GeneratedKmbedApi
    fun cleanup() {
        for ((_, resource) in resources) {
            if (resource !is PinnedResource) continue
            resource.release()
        }
    }

    protected fun add(path: String, ref: UByteArray, uncompressedSize: Int) {
        require(path !in resources) { "Resource $path already exists" }
        resources[path] = if (ref.size != uncompressedSize) ZStreamingResource(
            path, ref.pin(), uncompressedSize.toLong()
        )
        else StreamingResource(path, ref.pin())
    }

    /**
     * Retrieves the resource with the given path within the module owning this instance.
     * This should be used if the resource is always assumed to exist.
     *
     * @param path The path the resource within the module owning this instance.
     * @return The requested resource at the given path.
     * @throws IllegalStateException If the requested resource does not exist.
     */
    operator fun get(path: String): Resource = requireNotNull(resources[path]) { "Resource $path does not exist" }

    /**
     * Retrieves the resource with the given path within the module owning this instance
     * or returns null if the resource does not exist.
     *
     * @param path The path the resource within the module owning this instance.
     * @return The requested resource at the given path, null if it doesn't exist.
     */
    fun getOrNull(path: String): Resource? = resources[path]
}