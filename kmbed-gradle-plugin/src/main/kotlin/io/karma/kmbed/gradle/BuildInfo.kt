package io.karma.kmbed.gradle

import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

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

    val PLUGIN_ARTIFACT: SubpluginArtifact = SubpluginArtifact(GROUP, "kmbed-compiler-plugin", VERSION)
}