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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.writeText

abstract class KmbedGenerateCommonSourcesTask : DefaultTask() {
    @get:OutputDirectory
    abstract val sourceDirectory: DirectoryProperty

    private val extension = project.extensions.getByType(KmbedProjectExtension::class.java)

    @TaskAction
    fun invoke() {
        val outputDir = sourceDirectory.get().asFile.toPath()
        outputDir.createDirectories()
        val sourcePath = outputDir / "__kmbed_resources.kt"

        val source = SourceBuilder().apply {
            defaultHeader()
            newline()

            pkg(extension.resourceNamespace)
            newline()

            import("io.karma.kmbed.runtime.AbstractResources")
            newline()

            line("""@Suppress("PropertyName")""")
            line("""public expect val Res: AbstractResources""")
        }

        sourcePath.deleteIfExists()
        sourcePath.parent?.createDirectories()
        sourcePath.writeText(source.render())
    }
}