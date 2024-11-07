/*
 * Copyright 2024 connect2x GmbH
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

import java.time.Instant

val isCI = System.getenv("CI") != null
val isRelease = System.getenv("CI_COMMIT_TAG")?.matches("^v\\d+.\\d+.\\d+.*".toRegex()) ?: false
fun withVersionSuffix(version: String) = when {
    isRelease -> {
        val commitTagVersion = System.getenv("CI_COMMIT_TAG").removePrefix("v")
        check(version == commitTagVersion.substringBefore("-")) {
            "version from code ($version) does not match commit tag version ($commitTagVersion)"
        }
        commitTagVersion
    }

    isCI -> {
        val commitEpoch = Instant.parse(System.getenv("CI_COMMIT_TIMESTAMP")).epochSecond
        val commitCustomEpoch = commitEpoch - 1704067200 // 01.01.2024
        "$version-DEV-$commitCustomEpoch"
    }

    else -> "$version-LOCAL"
}
