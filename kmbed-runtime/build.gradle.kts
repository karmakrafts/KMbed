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

import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.defaultDokkaConfig
import dev.karmakrafts.conventions.setProjectInfo
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
}

configureJava(libs.versions.java)
defaultDokkaConfig()

@OptIn(ExperimentalWasmDsl::class) kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xreturn-value-checker")
    }
    jvmToolchain(java.toolchain.languageVersion.get().asInt())
    jvm()
    androidLibrary {
        namespace = "$group.${rootProject.name}"
        compileSdk = libs.versions.androidCompileSDK.get().toInt()
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
    }
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    androidNativeX64()
    androidNativeArm64()
    androidNativeArm32()
    androidNativeX86()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js {
        useEsModules()
        browser()
        nodejs()
    }
    wasmJs {
        useEsModules()
        browser()
        nodejs()
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.io.bytestring)
                api(libs.kotlinx.io.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        nativeMain {
            dependencies {
                implementation(libs.kmmio.core)
            }
        }
        webMain {
            dependencies {
                implementation(libs.kompress.core)
            }
        }
    }
}

publishing {
    setProjectInfo(
        name = "kMbed Runtime",
        description = "Kotlin Multiplatform runtime for the kMbed resource compiler.",
        url = "https://git.karmakrafts.dev/kk/kmbed"
    )
}