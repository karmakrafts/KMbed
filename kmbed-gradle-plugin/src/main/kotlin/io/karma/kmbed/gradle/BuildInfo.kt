package io.karma.kmbed.gradle

import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

/**
 * Class which gets processed by Gradle before getting compiled
 * to insert the correct meta-informations so we only have to
 * update it in one place.
 */
internal object BuildInfo {
    const val GROUP: String = "io.karma.kmbed"
    const val PLUGIN_NAME: String = "kmbed-gradle-plugin"
    const val PLUGIN_ID: String = "$GROUP.$PLUGIN_NAME"

    val VERSION: String by lazy {
        try {
            BuildInfo::class.java.getResourceAsStream("/version")?.bufferedReader().use {
                it?.readText()
            }!!
        }
        catch (_: Throwable) {
            "0.0.0.0" // Just let the error propagate like this
        }
    }

    val PLUGIN_ARTIFACT: SubpluginArtifact = SubpluginArtifact(GROUP, PLUGIN_NAME, VERSION)
}