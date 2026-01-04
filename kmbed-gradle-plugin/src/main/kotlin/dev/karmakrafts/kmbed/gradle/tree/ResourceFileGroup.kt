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
import com.squareup.kotlinpoet.TypeSpec
import dev.karmakrafts.kmbed.gradle.getFriendlyName
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

internal data class ResourceFileGroup( // @formatter:off
    override val rootPath: Path,
    override val path: Path,
    val extensions: List<String>
) : Resource { // @formatter:on
    override var parent: ResourceDirectory? = null

    override fun generate( // @formatter:off
        fileBuilder: FileSpec.Builder,
        typeBuilder: TypeSpec.Builder,
        isCommon: Boolean,
        isActual: (Path) -> Boolean
    ) { // @formatter:on
        val name = getFriendlyName(path.nameWithoutExtension)
        val modifiers = EnumSet.of(KModifier.PUBLIC)
        if (getPermutations().all(isActual)) modifiers += KModifier.ACTUAL
        val childTypeBuilder = TypeSpec.objectBuilder(name).addModifiers(modifiers)
        typeBuilder.addType(childTypeBuilder.build())
    }

    private fun getPermutations(): List<Path> =
        extensions.map { ext -> (path.parent ?: Path("")) / "${path.name}.$ext" }

    // We need to check against all possible permutations of the base path
    override operator fun contains(path: Path): Boolean {
        return path in getPermutations().toSet()
    }
}