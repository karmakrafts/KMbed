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

package io.karma.kmbed

import io.karma.kmbed.runtime.Resource
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.resource(resource: Resource, path: String = "/${resource.path}") {
    head(path) {
        // Head requests for a static resource should always return 200
        call.respond(HttpStatusCode.OK)
    }
    get(path) {
        call.respondBytes( // @formatter:off
            bytes = resource.asByteArray(),
            contentType = ContentType.defaultForFilePath(resource.path),
            status = HttpStatusCode.OK
        ) // @formatter:on
    }
}