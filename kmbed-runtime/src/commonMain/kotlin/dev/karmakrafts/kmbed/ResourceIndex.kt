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

package dev.karmakrafts.kmbed

interface ResourceIndex {
    companion object {
        const val INVALID_ID: Int = -1
        private val indices: HashMap<String, ResourceIndex> = HashMap()

        fun findForModule(moduleName: String): ResourceIndex? = indices[moduleName]
    }

    val moduleName: String

    fun getIdFromPath(path: String): Int
    fun getPathFromId(id: Int): String

    operator fun get(id: Int): Resource
    operator fun get(path: String): Resource = this[getIdFromPath(path)]
}