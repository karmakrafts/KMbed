import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvmToolchain(java.toolchain.languageVersion.get().asInt())
    jvm()
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    androidNativeX64()
    androidNativeArm64()
    androidNativeArm32()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                api(project(":kmbed-runtime"))
                api(libs.ktor.server.core)
            }
        }
    }
}

dokka {
    moduleName = project.name
    dokkaSourceSets {
        val commonMain by creating {
            sourceRoots.from(kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs)
        }
    }
    pluginsConfiguration {
        html {
            footerMessage = "(c) 2025 Karma Krafts & associates"
        }
    }
}

val dokkaJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks {
    System.getProperty("publishDocs.root")?.let { docsDir ->
        register("publishDocs", Copy::class) {
            dependsOn(dokkaJar)
            mustRunAfter(dokkaJar)
            from(zipTree(dokkaJar.get().outputs.files.first()))
            into(docsDir)
        }
    }
}

publishing {
    repositories {
        with(CI) { authenticatedPackageRegistry() }
    }
    publications.configureEach {
        if (this is MavenPublication) {
            artifact(dokkaJar)
            pom {
                name = project.name
                description = "Ktor extensions for the KMbed runtime to allow serving static, embedded content."
                url = System.getenv("CI_PROJECT_URL")
                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "kitsunealex"
                        name = "KitsuneAlex"
                        url = "https://git.karmakrafts.dev/KitsuneAlex"
                    }
                }
                scm {
                    url = this@pom.url
                }
            }
        }
    }
}