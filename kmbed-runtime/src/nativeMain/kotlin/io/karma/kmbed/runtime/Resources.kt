package io.karma.kmbed.runtime

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pin
import kotlinx.cinterop.staticCFunction
import platform.posix.atexit

@OptIn(ExperimentalForeignApi::class)
object Resources {
    private val resources: HashMap<String, Resource> = HashMap()

    init { // Initialize all resources when the Resource class is touched first
        registerResources()
        // Release all pinned resource so they can be GC'd if needed
        atexit(staticCFunction<Unit> {
            // @formatter:off
            for((_, resource) in Resources.resources) {
                if(resource !is PinnedResource) continue
                resource.release()
            }
            // @formatter:on
        })
    }

    // WARNING: COMPILER ABI - DO NOT CHANGE SIGNATURE WITHOUT ALSO CHANGING IT IN THE COMPILER PLUGIN!
    private fun registerResources() {}

    // WARNING: COMPILER ABI - DO NOT CHANGE SIGNATURE WITHOUT ALSO CHANGING IT IN THE COMPILER PLUGIN!
    internal fun add(path: String, ref: UByteArray, uncompressedSize: Long, isCompressed: Boolean) {
        require(path !in resources) { "Resource $path already exists" }
        resources[path] = if (isCompressed) ZStreamingResource(path, ref.pin(), uncompressedSize)
        else StreamingResource(path, ref.pin())
    }

    operator fun get(path: String): Resource = requireNotNull(resources[path]) { "Resource $path does not exist" }

    fun getOrNull(path: String): Resource? = resources[path]
}