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

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class KmbedBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val logger: Logger = LoggerFactory.getLogger("KMbed Build Service")

    val coroutineScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            logger.error("Error while executing coroutine", throwable)
        })

    override fun close() {
        logger.info("Shutting down KMbed build service")
        coroutineScope.cancel()
    }
}