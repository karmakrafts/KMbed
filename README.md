# KMbed

[![](https://git.karmakrafts.dev/kk/kmbed/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/kmbed/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Ffiles.karmakrafts.dev%2Fmaven%2Fio%2Fkarma%2Fkmbed%2Fkmbed-runtime%2Fmaven-metadata.xml)](https://git.karmakrafts.dev/kk/kmbed/-/packages)

KMbed is a Gradle/KMP plugin for embedding resources in KMP modules.  
The runtime provides an API similar to Android's R-class, zlib compression
and [kotlinx.io](https://github.com/Kotlin/kotlinx-io) integration.

The runtime also provides a compatibility module
for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
to allow deserializing resources in various formats such as JSON.

### Platform support

* Windows x64
* Linux x64
* Linux arm64
* macOS x64
* macOS arm64
* iOS x64
* iOS arm64
* Android Native x64
* Android Native arm64
* Android Native arm32
* JVM
* JS
* WASM

### How to use it

First, add the required maven repository:

```kotlin
repositories {
    maven("https://files.karmakrafts.dev/maven")
}
```

Using KMbed is as simple as applying the KMbed Gradle Plugin and the runtime.

```kotlin
plugins {
    id("io.karma.kmbed.kmbed-gradle-plugin") version "<version>"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.karma.kmbed:kmbed-runtime:<version>")
            }
        }
    }
}
```

Resource may be accessed from any source set using the `Res` global:

```kotlin
import com.example.foo.Res // Generated resource index

fun main(args: Array<String>) {
    val data = Res["my_resource.bin"].asByteArray()
    // ...
}
```

### How to configure it

The following configuration properties are avaiable through the kmbed project extension:

```kotlin
kmbed {
    // Allows changing the namespace (package name) of the generated resources for the current module
    resourceNamespace = "com.example.foo"
    // Allows force-disabling resource compression in the resource compiler
    compression = true
    // Allows adjusting the threshold at which resources are compressed (in bytes)
    compressionThreshold = 256
    // Custom source sets
    kmbedSourceSets {
        defaultSourceSets(project)
        val customSourceSet by creating {
            resourceNamespace = "com.example.foo.bar"
            compression = false
        }
    }
}
```