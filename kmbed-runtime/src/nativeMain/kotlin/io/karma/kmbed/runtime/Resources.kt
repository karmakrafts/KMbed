package io.karma.kmbed.runtime

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

object Resources {
    private val resources: HashMap<String, Resource> = HashMap()

    init { // Initialize all resources when the Resource class is touched first
        initResources()
    }

    // WARNING: COMPILER ABI - DO NOT CHANGE SIGNATURE WITHOUT ALSO CHANGING IT IN THE COMPILER PLUGIN!
    private fun initResources() {}

    // WARNING: COMPILER ABI - DO NOT CHANGE SIGNATURE WITHOUT ALSO CHANGING IT IN THE COMPILER PLUGIN!
    @ExperimentalForeignApi
    internal fun add(path: String, address: COpaquePointer, size: Long, uncompressedSize: Long, isCompressed: Boolean) {
        require(path !in resources) { "Resource $path already exists" }
        resources[path] = if(isCompressed) ZStreamingResource(path, address, size, uncompressedSize)
        else StreamingResource(path, address, size)
    }

    operator fun get(path: String): Resource = requireNotNull(resources[path]) { "Resource $path does not exist" }

    fun getOrNull(path: String): Resource? = resources[path]
}