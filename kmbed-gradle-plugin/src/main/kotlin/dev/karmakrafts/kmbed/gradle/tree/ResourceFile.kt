/*
 * Copyright 2026 Karma Krafts
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

package dev.karmakrafts.kmbed.gradle.tree

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.karmakrafts.kmbed.gradle.getFriendlyName
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

internal data class ResourceFile( // @formatter:off
    override val rootPath: Path,
    override val path: Path
) : Resource { // @formatter:on
    override var parent: ResourceDirectory? = null

    // Check if the parent directory contains a directory with the same name as this file
    private fun hasDirectoryAlternative(): Boolean {
        val parent = this.parent ?: return false
        return parent.children.any { (path, _) -> path.isDirectory() && path.name == this.path.nameWithoutExtension }
    }

    override fun generate( // @formatter:off
        fileBuilder: FileSpec.Builder,
        typeBuilder: TypeSpec.Builder,
        isCommon: Boolean,
        isActual: (Path) -> Boolean
    ) { // @formatter:on
        var name = getFriendlyName(path.nameWithoutExtension)
        val isActual = isActual(path)
        // Compose modifiers
        val modifiers = EnumSet.of(KModifier.PUBLIC)
        if (isCommon) modifiers += KModifier.EXPECT
        if (isActual) modifiers += KModifier.ACTUAL
        // Check if we have a directory with the same name as this file
        if (hasDirectoryAlternative()) name = "_$name" // Add a prefix to prevent collision
        // Build the accessor property
        val propertyBuilder = PropertySpec.builder(name, String::class, modifiers)
        if (isActual) {
            val relativePath = path.relativeTo(rootPath)
            propertyBuilder.initializer(""""$relativePath"""")
        }
        typeBuilder.addProperty(propertyBuilder.build())
    }
}