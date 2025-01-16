package io.karma.kmbed.gradle

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
