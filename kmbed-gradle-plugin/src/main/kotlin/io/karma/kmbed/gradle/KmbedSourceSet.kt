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

package io.karma.kmbed.gradle

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.io.File

open class KmbedSourceSet(
    private val name: String
) : Named {
    /**
     * The Kotlin compilation associated with this resource set after project evaluation.
     */
    lateinit var compilation: KotlinCompilation<*>

    /**
     * May be used to override the default resource compression for all resources in this project.
     */
    var compression: Boolean = true

    /**
     * May be used to adjust the default deflate compression threshold.
     * Any resource data that's larger than the specified value in bytes will be automatically compressed
     * by the resource compiler.
     */
    var compressionThreshold: Int = 256

    /**
     * Allows adjusting the default per-module namespace used for generated resources.
     */
    var resourceNamespace: String = ""

    override fun getName(): String = name

    internal inline val isTest: Boolean
        get() = compilation.compilationName == KotlinCompilation.TEST_COMPILATION_NAME

    internal fun getResourceRoots(): List<File> { // @formatter:off
        return compilation.allKotlinSourceSets
            .flatMap { it.resources.srcDirs }
            .filter { it.exists() }
    } // @formatter:on
}