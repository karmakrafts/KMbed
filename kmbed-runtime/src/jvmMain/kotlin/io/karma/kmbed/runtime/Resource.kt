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

import kotlinx.io.files.Path
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists

/**
 * Unpacks this resource to a temporary file and
 * loads the created file as a shared library in
 * the current JVM process using [System.load].
 */
fun Resource.loadLibrary() {
    val tempFile = Files.createTempFile("kmbed", path.substringAfterLast('/'))
    unpackTo(Path(tempFile.absolutePathString()), true)
    System.load(tempFile.absolutePathString())
    Runtime.getRuntime().addShutdownHook(Thread {
        tempFile.deleteIfExists()
    })
}