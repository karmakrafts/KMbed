/*
 * Copyright 2025 Karma Krafts
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

package dev.karmakrafts.kmbed.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class KmbedResourceConfig @Inject constructor(
    objects: ObjectFactory
) {
    companion object {
        const val DEFAULT_COMPRESSION_THRESHOLD: Long = 256
    }

    val compression: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val compressionThreshold: Property<Long> =
        objects.property(Long::class.java).convention(DEFAULT_COMPRESSION_THRESHOLD)
    val export: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}