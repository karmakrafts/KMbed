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

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.kotlinMultiplatformExtension: KotlinMultiplatformExtension
    get() = extensions.getByType(KotlinMultiplatformExtension::class.java)

internal fun String.chunkedOnNextSpace(length: Int): List<String> {
    val words = split(" ")
    val lines = ArrayList<String>()
    var currentLine = StringBuilder()
    for (word in words) {
        // If adding the word exceeds the line length, start a new line
        if (currentLine.length + word.length + (if (currentLine.isNotEmpty()) 1 else 0) > length) {
            lines.add(currentLine.toString())
            currentLine = StringBuilder(word)
            continue
        }
        // If it fits, add the word to the current line
        if (currentLine.isNotEmpty()) {
            currentLine.append(" ")
        }
        currentLine.append(word)
    }
    // Add the last line if it's not empty
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine.toString())
    }
    return lines
}