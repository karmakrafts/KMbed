package io.karma.kmbed.runtime

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pin

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

    operator fun get(path: String): Resource = requireNotNull(resources[path]) { "Resource $path does not exist" }

    fun getOrNull(path: String): Resource? = resources[path]
}