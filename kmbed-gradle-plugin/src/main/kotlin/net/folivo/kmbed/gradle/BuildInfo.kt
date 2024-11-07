/*
 * Copyright 2024 Karma Krafts & associates
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

package net.folivo.kmbed.gradle

import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

/**
 * Class which gets processed by Gradle before getting compiled
 * to insert the correct meta-informations so we only have to
 * update it in one place.
 */
internal object BuildInfo {
    const val GROUP: String = "{{GROUP}}"
    const val VERSION: String = "{{VERSION}}"

    const val PLUGIN_NAME: String = "{{PLUGIN_NAME}}"
    const val PLUGIN_ID: String = "$GROUP.$PLUGIN_NAME"
    val PLUGIN_ARTIFACT: SubpluginArtifact = SubpluginArtifact(GROUP, PLUGIN_NAME, VERSION)
}
