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

internal object BuildInfo {
    const val GROUP: String = "io.karma.kmbed"
    const val PLUGIN_NAME: String = "kmbed-gradle-plugin"

    val VERSION: String by lazy {
        try {
            BuildInfo::class.java.getResourceAsStream("/kmbed.version")?.bufferedReader().use {
                it?.readText()
            }!!
        }
        catch (_: Throwable) {
            "0.0.0.0" // Just let the error propagate like this
        }
    }
}