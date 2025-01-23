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

val baseVersion: String = libs.versions.kmbed.get()

allprojects {
    group = "io.karma.kmbed"
    // @formatter:off
    version = System.getenv("CI_COMMIT_TAG")?.let { baseVersion }
        ?: "$baseVersion.${System.getenv("CI_PIPELINE_IID") ?: 0}-SNAPSHOT"
    // @formatter:on
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven("https://git.karmakrafts.dev/api/v4/projects/332/packages/maven") // Multiplatform mman
    }
}