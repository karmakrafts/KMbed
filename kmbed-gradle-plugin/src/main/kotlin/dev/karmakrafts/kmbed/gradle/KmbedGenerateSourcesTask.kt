/*
 * Copyright 2025 Karma Krafts
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

package dev.karmakrafts.kmbed.gradle

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.karmakrafts.kmbed.gradle.tree.ResourceDirectory
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import java.io.File
import kotlin.io.path.exists

abstract class KmbedGenerateSourcesTask : AbstractKmbedGenerateSourcesTask() {
    @get:InputFiles
    abstract val commonInputDirectories: ConfigurableFileCollection

    override fun FileSpec.Builder.generateFile() {
        // @formatter:off
        val typeBuilder = TypeSpec.objectBuilder("Res")
            .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL)
            .superclass(RuntimeTypes.AbstractResourceIndex)
            .addProperty(PropertySpec.builder("namespace", String::class, KModifier.ACTUAL, KModifier.OVERRIDE)
                .initializer(""""${namespace.get()}"""")
                .build())
        val commonResources = commonInputDirectories.files
            .filter(File::exists)
            .map { commonInputDir -> ResourceDirectory.collect(
                path = commonInputDir.toPath(),
                maxDepth = maxRecursionDepth.get(),
                excludes = excludes.get().toList()
            ) }
        // @formatter:on
        for (inputDir in inputDirectories) {
            val inputDirPath = inputDir.toPath()
            if (!inputDirPath.exists()) continue // We can't collect from non-existent directories
            val resourceDir = ResourceDirectory.collect( // @formatter:off
                path = inputDirPath,
                maxDepth = maxRecursionDepth.get(),
                excludes = excludes.get().toList()
            ) // @formatter:on
            logger.lifecycle("Collected resource directory: $resourceDir")
            resourceDir.generateRoot(this, typeBuilder, false) { path -> commonResources.any { dir -> path in dir } }
        }
        addType(typeBuilder.build())
    }
}