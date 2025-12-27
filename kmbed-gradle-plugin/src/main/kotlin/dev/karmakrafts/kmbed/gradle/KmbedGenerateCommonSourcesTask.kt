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
import kotlin.io.path.nameWithoutExtension

abstract class KmbedGenerateCommonSourcesTask : AbstractKmbedGenerateSourcesTask() {
    override fun FileSpec.Builder.generateFile() {
        // @formatter:off
        val typeBuilder = TypeSpec.objectBuilder("Res")
            .addModifiers(KModifier.PUBLIC, KModifier.EXPECT)
            .superclass(abstractResourceIndexType)
            .addProperty(PropertySpec.builder("namespace", String::class, KModifier.OVERRIDE).build())
        // @formatter:on
        val files = gatherResources(inputDirectories.files.toList(), excludes.get(), maxRecursionDepth.get())
        for ((_, filePath) in files) {
            val fileName = filePath.nameWithoutExtension
            val propName = fileName.replace(wordBoundaryPattern, "_")
            // @formatter:off
            typeBuilder.addProperty(PropertySpec.builder(propName, String::class, KModifier.PUBLIC, KModifier.EXPECT)
                .build())
            // @formatter:on
        }
        addType(typeBuilder.build())
    }
}